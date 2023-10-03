val groupId = "com.github.tarao"
val projectName = "record4s"
val rootPkg = s"$groupId.$projectName"

val Scala_3 = "3.3.1"

ThisBuild / scalaVersion := Scala_3
ThisBuild / crossScalaVersions := Seq(Scala_3)

lazy val metadataSettings = Def.settings(
  name := projectName,
  organization := groupId,
  description := "Extensible records for Scala",
  homepage := Some(url("https://github.com/tarao/record4s")),
)

lazy val compileSettings = Def.settings(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-unchecked",
    "-Xfatal-warnings",
    "-Wvalue-discard",
    "-Wunused:imports",
    "-Wunused:implicits,explicits,privates",
  ),
  Compile / console / scalacOptions -= "-Wunused:imports",
)

lazy val commonSettings = Def.settings(
  metadataSettings,
  compileSettings,
  initialCommands := s"""
    import $rootPkg.*
  """
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
    )
  )
