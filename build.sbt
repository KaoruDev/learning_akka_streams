name := "learning_akka_streams"

version := "0.1"

scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.23",
  "org.clapper" %% "grizzled-slf4j" % "1.3.4",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
)