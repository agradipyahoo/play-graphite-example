package filters;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import play.api.libs.iteratee.Execution;
import play.api.libs.iteratee.Iteratee;
import play.api.mvc.EssentialAction;
import play.api.mvc.EssentialFilter;
import play.api.mvc.RequestHeader;
import play.api.mvc.Result;
import scala.Function1;
import scala.runtime.AbstractFunction1;

import static com.codahale.metrics.MetricRegistry.name;

public class MetricsFilter implements EssentialFilter {

    private final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate("play-metrics");

    private final Counter counter = metricRegistry.counter(name("Requests"));

    public EssentialAction apply(final EssentialAction next) {

        return new MetricsAction() {

            @Override
            public EssentialAction apply() {
                return next.apply();
            }

            @Override
            public Iteratee<byte[], Result> apply(final RequestHeader rh) {

                return next.apply(rh).map(new AbstractFunction1<Result, Result>() {

                    @Override
                    public Result apply(Result result) {
                        counter.inc();
                        return result;
                    }

                    @Override
                    public <A> Function1<Result, A> andThen(Function1<Result, A> result) {
                        return result;
                    }

                    @Override
                    public <A> Function1<A, Result> compose(Function1<A, Result> result) {
                        return result;
                    }

                }, Execution.defaultExecutionContext());
            }


        };
    }

    public abstract class MetricsAction extends
        AbstractFunction1<RequestHeader, Iteratee<byte[], Result>>
        implements EssentialAction {}
}