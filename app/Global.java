import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.kenshoo.play.metrics.JavaMetricsFilter;
import play.Application;
import play.GlobalSettings;
import play.api.mvc.EssentialFilter;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class Global extends GlobalSettings {

    GraphiteReporter reporter;

    // Attaches Metrics filter to report on endpoint usage
    @Override
    public <T extends EssentialFilter> Class<T>[] filters() {

        return new Class[]{JavaMetricsFilter.class};
    }

    @Override
    public void onStart(Application application) {
        super.onStart(application);

        // Add Graphite Reporter
        boolean graphiteEnabled = application.configuration().getBoolean("graphite.enabled");

        if (graphiteEnabled) {
            String   metricsName = application.configuration().getString("metrics.name");
            String   host        = application.configuration().getString("graphite.host");
            int      port        = application.configuration().getInt("graphite.port");
            String   prefix      = application.configuration().getString("graphite.prefix");
            long     period      = application.configuration().getLong("graphite.period");
            TimeUnit periodUnit  = TimeUnit.valueOf(application.configuration().getString("graphite.periodUnit"));

            final Graphite graphite = new Graphite(new InetSocketAddress(host, port));
            GraphiteReporter.Builder reportBuilder = GraphiteReporter.forRegistry(SharedMetricRegistries.getOrCreate(metricsName))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL);

            if (prefix != null && !prefix.isEmpty()) {
                reportBuilder.prefixedWith(prefix);
            }

            reporter = reportBuilder.build(graphite);

            reporter.start(period, periodUnit);
        }
    }

    @Override
    public void onStop(Application application) {
        if (reporter != null) {
            reporter.stop();
        }

        super.onStop(application);
    }
}