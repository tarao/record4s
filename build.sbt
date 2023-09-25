ThisBuild / scalaVersion := "3.3.1"
ThisBuild / organization := "com.github.tarao"

lazy val root = (project in file("."))
  .settings(
    name := "record4s",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  )
