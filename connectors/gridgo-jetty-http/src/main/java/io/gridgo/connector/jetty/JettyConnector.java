package io.gridgo.connector.jetty;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.gridgo.connector.impl.AbstractConnector;
import io.gridgo.connector.jetty.impl.DefaultJettyConsumer;
import io.gridgo.connector.jetty.server.JettyServletContextHandlerOption;
import io.gridgo.connector.support.annotations.ConnectorEndpoint;
import io.gridgo.utils.support.HostAndPort;

@ConnectorEndpoint(scheme = "jetty", syntax = "http://{host}[:{port}][/{path}]")
public class JettyConnector extends AbstractConnector {

    private static final String TRUE = "true";
    private static final String FALSE = "false";

    @Override
    protected void onInit() {
        var host = getPlaceholder("host");
        var portStr = getPlaceholder("port");
        int port = portStr == null ? 80 : Integer.parseInt(portStr);

        var path = getPlaceholder("path");
        if (path == null || path.isBlank())
            path = "/*";

        var jettyConsumer = DefaultJettyConsumer.builder() //
                .http2Enabled(Boolean.valueOf(getParam("http2Enabled", TRUE))) //
                .mmapEnabled(Boolean.valueOf(getParam("mmapEnabled", TRUE))) //
                .address(HostAndPort.newInstance(host, port)) //
                .options(readJettyOptions()) //
                .context(getContext()) //
                .path(path) //
                .format(getParam("format", null)) //
                .charsetName(getParam("charset", "UTF-8")) //
                .enablePrometheus(Boolean.valueOf(getParam("enablePrometheus", "false"))) //
                .prometheusPrefix(getParam("prometheusPrefix", "jetty")) //
                .stringBufferSize(Integer.valueOf(getParam("stringBufferSize", "65536"))) //
                .build();

        this.consumer = Optional.of(jettyConsumer);
        this.producer = Optional.of(jettyConsumer.getResponder());
    }

    private Set<JettyServletContextHandlerOption> readJettyOptions() {
        var options = new HashSet<JettyServletContextHandlerOption>();

        if (Boolean.parseBoolean(getParam("session", FALSE))) {
            options.add(JettyServletContextHandlerOption.SESSIONS);
        } else {
            options.add(JettyServletContextHandlerOption.NO_SESSIONS);
        }

        if (Boolean.parseBoolean(getParam("security", FALSE))) {
            options.add(JettyServletContextHandlerOption.SECURITY);
        } else {
            options.add(JettyServletContextHandlerOption.NO_SECURITY);
        }

        if (Boolean.parseBoolean(getParam("gzip", FALSE))) {
            options.add(JettyServletContextHandlerOption.GZIP);
        }

        return options;
    }
}
