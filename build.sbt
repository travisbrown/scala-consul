name := """scala-consul"""

version := "1.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

crossScalaVersions := Seq(scalaVersion.value)

scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-deprecation", "-feature"
  ,"-Xfuture" //, "-Xverify", "-Xcheck-null"
  ,"-Ybackend:GenBCode"
  ,"-Ydelambdafy:method"
  ,"-Ywarn-unused-import"
)

resolvers += "Bintray Typesafe Repo" at "http://dl.bintray.com/typesafe/maven-releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "io.spray" %% "spray-client" % "1.3.3"
)

organization := "com.codacy"
organizationName := "Codacy"
organizationHomepage := Some(new URL("https://www.codacy.com"))

description := "Consul Scala Client"
