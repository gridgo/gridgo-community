package io.gridgo.extras.prometheus;

import org.joo.promise4j.Deferred;

import io.gridgo.framework.execution.ExecutionStrategyInstrumenter;
import io.gridgo.framework.support.Message;
import io.prometheus.client.Counter;
import lombok.Getter;

public class PrometheusErrorCounterInstrumenter implements ExecutionStrategyInstrumenter {

    @Getter
    private Counter counter;

    public PrometheusErrorCounterInstrumenter(String name, String help) {
        this.counter = Counter.build(name, help).register();
    }

    public PrometheusErrorCounterInstrumenter(Counter counter) {
        this.counter = counter;
    }

    @Override
    public Runnable instrument(Message msg, Deferred<Message, Exception> deferred, Runnable runnable) {
        return () -> {
            deferred.promise() //
                    .fail(e -> counter.inc());
            runnable.run();
        };
    }
}
