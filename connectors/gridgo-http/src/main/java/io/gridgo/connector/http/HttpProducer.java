package io.gridgo.connector.http;

import static io.gridgo.connector.httpcommon.HttpCommonConstants.HEADER_PATH;
import static io.gridgo.connector.httpcommon.HttpCommonConstants.HEADER_STATUS;
import static io.gridgo.connector.httpcommon.HttpCommonConstants.HEADER_STATUS_CODE;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Param;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.joo.promise4j.Promise;
import org.joo.promise4j.impl.CompletableDeferredObject;

import io.gridgo.bean.BArray;
import io.gridgo.bean.BElement;
import io.gridgo.bean.BObject;
import io.gridgo.connector.httpcommon.AbstractHttpProducer;
import io.gridgo.connector.httpcommon.support.exceptions.ConnectionException;
import io.gridgo.connector.support.config.ConnectorContext;
import io.gridgo.framework.support.Message;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.resolver.NameResolver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpProducer extends AbstractHttpProducer {

    private static final String DEFAULT_METHOD = "GET";

    private String endpointUri;

    private AsyncHttpClient asyncHttpClient;

    private final boolean selfCreateHttpClient;

    private Builder config;

    private NameResolver<InetAddress> nameResolver;

    private String defaultMethod;

    @lombok.Builder
    private HttpProducer( //
            ConnectorContext context, //
            String endpointUri, //
            Builder config, //
            String format, //
            NameResolver<InetAddress> nameResolver, //
            String defaultMethod, //
            AsyncHttpClient asyncHttpClient) {

        super(context, format);
        this.endpointUri = endpointUri;
        this.config = config;
        this.nameResolver = nameResolver;
        this.defaultMethod = defaultMethod != null ? defaultMethod : DEFAULT_METHOD;
        this.asyncHttpClient = asyncHttpClient;
        this.selfCreateHttpClient = this.asyncHttpClient == null;
    }

    private Message buildMessage(Response response) {
        var headers = buildHeaders(response.getHeaders()).setAny(HEADER_STATUS, response.getStatusText())
                .setAny(HEADER_STATUS_CODE, response.getStatusCode());
        var body = deserialize(response.getResponseBodyAsBytes());
        return createMessage(headers, body);
    }

    private BObject buildHeaders(HttpHeaders headers) {
        var obj = BObject.ofEmpty();
        if (headers == null)
            return obj;
        var entries = headers.entries();
        if (entries == null)
            return obj;
        entries.forEach(e -> obj.putAny(e.getKey(), e.getValue()));
        return obj;
    }

    private List<Param> buildParams(BObject object) {
        return object.entrySet().stream() //
                .filter(e -> e.getValue().isValue()) //
                .map(e -> new Param(e.getKey(), e.getValue().asValue().getString())) //
                .collect(Collectors.toList());
    }

    private Request buildRequest(Message message) {
        var builder = createBuilder(message);
        if (nameResolver != null)
            builder.setNameResolver(nameResolver);
        return builder.build();
    }

    @Override
    public Promise<Message, Exception> call(Message message) {
        var request = buildRequest(message);

        var future = asyncHttpClient //
                .executeRequest(request) //
                .toCompletableFuture() //
                .thenApply(this::buildMessage);

        return new CompletableDeferredObject<>(future) //
                .filterFail(ConnectionException::new);
    }

    private RequestBuilder createBuilder(Message message) {

        if (message == null)
            return new RequestBuilder().setUrl(endpointUri);

        var endpointUri = this.endpointUri + message.headers().getString(HEADER_PATH, "");
        var method = getMethod(message, defaultMethod);
        var headers = getHeaders(message);
        var params = buildParams(getQueryParams(message));
        var body = serialize(message.body());

        return new RequestBuilder(method) //
                .setUrl(endpointUri) //
                .setBody(body) //
                .setHeaders(headers) //
                .setQueryParams(params);
    }

    private Map<CharSequence, List<String>> getHeaders(Message message) {
        var headers = message.headers();
        var map = new HashMap<CharSequence, List<String>>();
        for (var entry : headers.entrySet()) {
            var list = map.computeIfAbsent(entry.getKey(), key -> new ArrayList<>());
            if (entry.getValue().isArray()) {
                putMultiHeaders(list, entry.getValue().asArray());
            } else {
                putHeader(list, entry.getValue());
            }
        }
        return map;
    }

    private void putMultiHeaders(List<String> list, BArray arr) {
        for (var e : arr) {
            putHeader(list, e);
        }
    }

    private void putHeader(List<String> list, BElement e) {
        if (e.isValue())
            list.add(e.asValue().getString());
    }

    @Override
    protected String generateName() {
        return "consumer." + endpointUri;
    }

    @Override
    protected void onStart() {
        if (this.asyncHttpClient == null)
            this.asyncHttpClient = Dsl.asyncHttpClient(config);
    }

    @Override
    protected void onStop() {
        if (selfCreateHttpClient)
            try {
                asyncHttpClient.close();
            } catch (IOException e) {
                log.error("Error when closing AsyncHttpClient", e);
            }
    }

    @Override
    public void send(Message message) {
        var request = buildRequest(message);
        asyncHttpClient.executeRequest(request);
    }

    @Override
    public Promise<Message, Exception> sendWithAck(Message message) {
        var deferred = new CompletableDeferredObject<Message, Exception>();
        var request = buildRequest(message);
        asyncHttpClient.executeRequest(request, new AsyncHandler<Object>() {

            @Override
            public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                ack(deferred);
                return State.CONTINUE;
            }

            @Override
            public Object onCompleted() throws Exception {
                ack(deferred);
                return State.CONTINUE;
            }

            @Override
            public State onHeadersReceived(HttpHeaders headers) throws Exception {
                ack(deferred);
                return State.CONTINUE;
            }

            @Override
            public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                ack(deferred);
                return State.CONTINUE;
            }

            @Override
            public void onThrowable(Throwable t) {
                ack(deferred, new ConnectionException(t));
            }
        });
        return deferred.promise();
    }
}
