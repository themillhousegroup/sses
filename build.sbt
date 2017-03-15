name := "sses"

// If the CI supplies a "build.version" environment variable, inject it as the rev part of the version number:
version := s"${sys.props.getOrElse("build.majorMinor", "1.0")}.${sys.props.getOrElse("build.version", "SNAPSHOT")}"

scalaVersion := "2.11.7"

organization := "com.themillhousegroup"

val targetPlayVersion = "2.5.12"


libraryDependencies ++= Seq(
  "com.typesafe.play"           %%  "play"                  % targetPlayVersion       % "provided",
  "com.typesafe.play"           %%  "play-specs2"           % targetPlayVersion       % "test",
  "ch.qos.logback"              %   "logback-classic"       % "1.1.5",
  "com.typesafe.scala-logging"  %%  "scala-logging"         % "3.1.0"
)

resolvers ++= Seq(  "oss-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
                    "oss-releases"  at "https://oss.sonatype.org/content/repositories/releases",
                    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/")

jacoco.settings

publishArtifact in (Compile, packageDoc) := false

seq(bintraySettings:_*)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalariformSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

