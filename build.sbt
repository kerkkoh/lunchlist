// @formatter:off

name := "Lunchlist"
//organization := "scalafx.org"
version := "1.0.5"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8")

resourceDirectory in Compile := (scalaSource in Compile).value
libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.102-R11",
  "org.scalafx" %% "scalafxml-core-sfx8" % "0.3"
)

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.10"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

shellPrompt := { state => System.getProperty("user.name") + s":${name.value}> " }
