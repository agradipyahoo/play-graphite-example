name := """graphite-play-sample"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

herokuAppName in Compile := "radiant-chamber-5841" // This should be replaced with the name of your heroku instance

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8",
  "io.dropwizard.metrics" % "metrics-graphite" % "3.1.0"
)
