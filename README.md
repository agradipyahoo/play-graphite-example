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

Runs at `http://localhost:9090/`, with three endpoints; [/](http://localhost:9090/), [/hello/name](http://localhost:9090/hello/name),[/return400](http://localhost:9090/return400).

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
