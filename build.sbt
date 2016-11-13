name := "test"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.12.0",
  "org.reactivemongo" %% "reactivemongo-akkastream" % "0.12.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
