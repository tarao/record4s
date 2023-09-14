ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "com.github.tarao"

lazy val dict = (project in file("."))
  .settings(
    name := "record4s",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  )
