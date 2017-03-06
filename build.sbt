import sbt._
import Process._
import Keys._

val akkaVersion = "2.4.17"
val akkaHttpVersion = "10.0.4"
val slf4jVersion = "1.7.24"

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "akka-http-file-server",
    organization := "com.example.labs",
    scalaVersion := "2.12.1",
    version := {
      (version in ThisBuild).value
    },
    libraryDependencies ++= dependencies
  )

releaseSettings

publishMavenStyle := true

//val commonSettings = Seq(jacoco.settings:_*) ++ sonatypeSettings  ++ net.virtualvoid.sbt.graph.Plugin.graphSettings ++
val commonSettings = Seq(
  resolvers ++= Seq(
    "patriknw at bintray" at "http://dl.bintray.com/patriknw/maven",
    "maven-repo" at "http://repo.maven.apache.org/maven2",
    "maven1-repo" at "http://repo1.maven.org/maven2",
    "maven2-repo" at "http://mvnrepository.com/artifact",
    "sonatype" at "https://oss.sonatype.org/content/repositories/releases",
    "bintray/non" at "http://dl.bintray.com/non/maven"
  )
)

val dependencies = Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-log4j12" % slf4jVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-agent" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-jackson" % akkaHttpVersion,
  "com.google.guava" % "guava" % "21.0"
)
