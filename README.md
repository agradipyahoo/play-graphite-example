# Play application reporting to graphite

Example Java [Play](https://www.playframework.com/) application which reports to [graphite](http://graphite.wikidot.com/)
for monitoring, setup to be hosted on [heroku](heroku.com) with [free addon](https://addons.heroku.com/hostedgraphite).
This allows you to quickly test out using graphite and see what you get from monitoring with proper metrics.

Based the implementation from the [metrics-play](https://github.com/kenshoo/metrics-play) play plugin, which is written in Scala.
I wanted a clear Java Play implementation which gave me control over the metrics names, but if you want to quickly add
metrics into your Play application without fuss this is a good plugin.

To run:

```
./go
```

Runs at `http://localhost:9090/`, with three endpoints; [/](http://localhost:9090/), [/hello/name](http://localhost:9090/hello/name),[/return400](http://localhost:9090/return400). Enable console logging of metrics (`metrics.console=true`) to see examples of data which is sent to graphite.

## Heroku

Requires Heroku toolkit installed.

```
heroku login

heroku create -n
# will give name/url of instance e.g. radiant-chamber-5841, put name in build.sbt herokuAppName setting

heroku addons:add hostedgraphite --app radiant-chamber-5841
# enables hosted graphite for your application, usage is free for 50 metrics but you need to verify payment information

heroku config:get HOSTEDGRAPHITE_APIKEY --app radiant-chamber-5841
# gets the api key you will need to put in conf/application.conf to report to the graphite instance

./activator stage deployHeroku
# after deploy your application will be available, use this command to redeploy for changes

heroku open --app radiant-chamber-5841
# opens your application in browser

heroku addons:open hostedgraphite --app radiant-chamber-5841
# opens your hosted graphite in browser
```

You'll need to generate some requests on your application to cause it to start and see some data being reported.

## Detail

See the documentation of [dropwizard-metrics](https://dropwizard.github.io/metrics) for detail about metrics.
This example creates metrics registries for JVM, Logback and request details by hooking into the Play application using the `Global.java` file, using the `filters()` and `onStart` methods.

```
public class Global extends GlobalSettings {
...
    @Override
    public <T extends EssentialFilter> Class<T>[] filters() {
        return new Class[]{MetricsFilter.class};
    }
...
    @Override
    public void onStart(Application application) {
        super.onStart(application);

        setupMetrics(application.configuration());

        setupGraphiteReporter(application.configuration());
    }
...
    private void setupMetrics(Configuration configuration) {
        ...
        if (metricsJvm) {
            metricRegistry.registerAll(new GarbageCollectorMetricSet());
            metricRegistry.registerAll(new MemoryUsageGaugeSet());
            metricRegistry.registerAll(new ThreadStatesGaugeSet());
        }

        if (metricsLogback) {
            InstrumentedAppender appender = new InstrumentedAppender(metricRegistry);

            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)Logger.underlying();
            appender.setContext(logger.getLoggerContext());
            appender.start();
            logger.addAppender(appender);
        }

        if (metricsConsole) {
            ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
            consoleReporter.start(1, TimeUnit.SECONDS);
        }
    }
...
    private void setupGraphiteReporter(Configuration configuration) {
        boolean graphiteEnabled = configuration.getBoolean("graphite.enabled", false);

        if (graphiteEnabled) {
            ...
            final Graphite graphite = new Graphite(new InetSocketAddress(host, port));
            graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
            	.prefixedWith(prefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);

            graphiteReporter.start(period, periodUnit);
        }
    }
}
```

Metrics about the requests are captured using a Filter `MetricsFilter`, which is applied to all requests hitting the application and can see both the request header and result data.

```
public class MetricsFilter implements EssentialFilter {

    private final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate("play-metrics");

    private final Counter activeRequests = metricRegistry.counter(name("activeRequests"));
    private final Timer   requestTimer   = metricRegistry.timer(name("requestsTimer"));

    private final Map<String, Meter> statusMeters = new HashMap<String, Meter>() {{
        put("1", metricRegistry.meter(name("1xx-responses")));
        put("2", metricRegistry.meter(name("2xx-responses")));
        put("3", metricRegistry.meter(name("3xx-responses")));
        put("4", metricRegistry.meter(name("4xx-responses")));
        put("5", metricRegistry.meter(name("5xx-responses")));
    }};

    public EssentialAction apply(final EssentialAction next) {

        return new MetricsAction() {

            @Override
            public EssentialAction apply() {
                return next.apply();
            }

            @Override
            public Iteratee<byte[], Result> apply(final RequestHeader requestHeader) {
                activeRequests.inc();
                final Context requestTimerContext = requestTimer.time();

                return next.apply(requestHeader).map(new AbstractFunction1<Result, Result>() {

                    @Override
                    public Result apply(Result result) {
                        activeRequests.dec();
                        requestTimerContext.stop();
                        String statusFirstCharacter = String.valueOf(result.header().status()).substring(0,1);
                        if (statusMeters.containsKey(statusFirstCharacter)) {
                            statusMeters.get(statusFirstCharacter).mark();
                        }
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
```

## Customisation and improvements

This example gives basic metrics on the Application, but for your own solution you would probably want to get specific metrics about controller actions. You can do this by either creating your own Play Filters and attaching them to the action methods or coding metrics directly into the actions. I used the Dropwizard Metrics own style for reporting on requests (`2xx-responses`) but you may be interested in specific results or requests and can use the Filter to intercept and report on these.
