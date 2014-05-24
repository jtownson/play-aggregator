name := "play-aggregator"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.apache.velocity" % "velocity" % "[1.7,)"
)

play.Project.playScalaSettings
