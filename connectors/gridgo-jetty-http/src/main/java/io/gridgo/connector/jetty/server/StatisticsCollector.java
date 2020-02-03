package io.gridgo.connector.jetty.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.server.handler.StatisticsHandler;

import io.prometheus.client.Collector;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Collect metrics from jetty's
 * org.eclipse.jetty.server.handler.StatisticsHandler.
 * <p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     Server server = new Server(8080);
 *
 *     ServletContextHandler context = new ServletContextHandler();
 *     context.setContextPath("/");
 *     server.setHandler(context);
 *
 *     HandlerCollection handlers = new HandlerCollection();
 *
 *     StatisticsHandler statisticsHandler = new StatisticsHandler();
 *     statisticsHandler.setServer(server);
 *     handlers.addHandler(statisticsHandler);
 *
 * // Register collector.
 *     new JettyStatisticsCollector(statisticsHandler).register();
 *
 *     server.setHandler(handlers);
 *
 *     server.start();
 * }
 * </pre>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StatisticsCollector extends Collector {

    private static final List<String> EMPTY_LIST = Collections.emptyList();

    private final @NonNull StatisticsHandler s;
    private final String prefix;

    public static StatisticsCollector newStatisticsCollector(StatisticsHandler statisticsHandler, String prefix) {
        return new StatisticsCollector(statisticsHandler, prefix);
    }

    private String prefix_(String name) {
        return prefix + "_" + name;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return Arrays.asList( //
                // generic info
                gauge("stats", "Time in ms stats have been collected for", s.getStatsOnMs()),
                gauge("stats_seconds", "Time in seconds stats have been collected for", s.getStatsOnMs() / 1000.0), //
                counter("responses_bytes_total", "Total number of bytes across all responses",
                        s.getResponsesBytesTotal()), //

                // status
                buildStatusCounter(),

                // request count
                counter("requests_total", "Number of requests", s.getRequests()), //
                gauge("requests_active", "Number of requests currently active", s.getRequestsActive()),
                gauge("requests_active_max", "Maximum number of requests that have been active at once",
                        s.getRequestsActiveMax()),

                // request time
                gauge("request_time_mean", "Mean time spent handling requests (ms)", s.getRequestTimeMean()), //
                gauge("request_time_max", "Maximum time spent handling requests (ms)", s.getRequestTimeMax()),
                gauge("request_time_std_dev", "Request time standard deviation", s.getRequestTimeStdDev()),
                counter("request_time_total", "Total time spent in all request handling (ms)", s.getRequestTimeTotal()),

                // dispatched info
                counter("dispatched_total", "Number of dispatches", s.getDispatched()),
                gauge("dispatched_active", "Number of dispatches currently active", s.getDispatchedActive()),
                gauge("dispatched_active_max", "Maximum number of active dispatches being handled",
                        s.getDispatchedActiveMax()),

                gauge("dispatched_time_mean", "Mean dispatched time", s.getDispatchedTimeMean()),
                gauge("dispatched_time_max", "Maximum dispatched time", s.getDispatchedTimeMax()),
                gauge("dispatched_time_std_dev", "Dispatched time standard deviation", s.getDispatchedTimeStdDev()),
                counter("dispatched_time_total", "Total dispatched time (ms)", s.getDispatchedTimeTotal()),

                // async request info
                counter("async_requests_total", "Total number of async requests", s.getAsyncRequests()),
                gauge("async_requests_waiting", "Currently waiting async requests", s.getAsyncRequestsWaiting()),
                gauge("async_requests_waiting_max", "Maximum number of waiting async requests",
                        s.getAsyncRequestsWaitingMax()),
                counter("async_dispatches_total", "Number of requested that have been asynchronously dispatched",
                        s.getAsyncDispatches()),
                counter("expires_total", "Number of async requests requests that have expired", s.getExpires()));
    }

    private MetricFamilySamples gauge(String name, String help, double value) {
        name = prefix_(name);
        return new MetricFamilySamples(name, Type.GAUGE, help,
                Collections.singletonList(new MetricFamilySamples.Sample(name, EMPTY_LIST, EMPTY_LIST, value)));
    }

    private MetricFamilySamples counter(String name, String help, double value) {
        name = prefix_(name);
        return new MetricFamilySamples(name, Type.COUNTER, help,
                Collections.singletonList(new MetricFamilySamples.Sample(name, EMPTY_LIST, EMPTY_LIST, value)));
    }

    private MetricFamilySamples buildStatusCounter() {
        var name = prefix_("responses_total");
        return new MetricFamilySamples(name, Type.COUNTER, "Number of requests with response status",
                Arrays.asList(buildStatusSample(name, "1xx", s.getResponses1xx()),
                        buildStatusSample(name, "2xx", s.getResponses2xx()),
                        buildStatusSample(name, "3xx", s.getResponses3xx()),
                        buildStatusSample(name, "4xx", s.getResponses4xx()),
                        buildStatusSample(name, "5xx", s.getResponses5xx())));
    }

    private MetricFamilySamples.Sample buildStatusSample(String name, String status, double value) {
        return new MetricFamilySamples.Sample(name, Collections.singletonList("code"),
                Collections.singletonList(status), value);
    }
}
