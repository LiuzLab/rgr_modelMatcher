package ubc.pavlab.rdp.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;

public class MetricsUpdater {

    private MeterRegistry registry;

    public MetricsUpdater(MeterRegistry registry) {
        this.registry = registry;
        registerJvmMetrics();
    }

    private void registerJvmMetrics() {
        new JvmMemoryMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
    }

    public void updateGauge(String name, double value) {
        registry.gauge(name, value);
    }

    public void incrementCounter(String name) {
        registry.counter(name).increment();
    }
    // Additional methods to handle other types of metrics can be added here

}
