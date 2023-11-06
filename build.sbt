val groupId = "com.github.tarao"
val projectName = "record4s"
val rootPkg = s"$groupId.$projectName"

ThisBuild / organization := groupId
ThisBuild / organizationName := "tarao"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("tarao", "INA Lintaro"),
  tlGitHubDev("windymelt", "Windymelt"),
)

lazy val metadataSettings = Def.settings(
  name := projectName,
  organization := groupId,
  description := "Extensible records for Scala",
  homepage := Some(url("https://github.com/tarao/record4s")),
)

val Scala_3 = "3.3.1"
val Scala_2_13 = "2.13.12"
val Scala_2_11 = "2.11.12"

ThisBuild / scalaVersion := Scala_3
ThisBuild / crossScalaVersions := Seq(Scala_3)

lazy val compileSettings = Def.settings(
  // Default options are set by sbt-typelevel-settings
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
  ),
  scalacOptions --= Seq(
    "-Ykind-projector:underscores", // https://github.com/lampepfl/dotty/issues/14952
  ),
  Test / scalacOptions --= Seq(
    "-Wunused:locals",
  ),
  Compile / console / scalacOptions --= Seq(
    "-Wunused:imports",
  ),
)

lazy val commonSettings = Def.settings(
  metadataSettings,
  compileSettings,
  initialCommands := s"""
    import $rootPkg.*
  """
)

lazy val root = (project in file("."))
  .aggregate(core)
  .settings(commonSettings)
  .settings(
    console := (core / Compile / console).value,
    Test / console := (core / Test / console).value,
    ThisBuild / Test / parallelExecution := false
  )

lazy val core = (project in file("modules/core"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
    )
  )

lazy val benchmark_3 = (project in file("modules/benchmark_3"))
  .dependsOn(core)
  .settings(commonSettings)
  .enablePlugins(JmhPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    Compile / run / fork := true,
    javaOptions+= "-Xss10m",
    scalacOptions ++= Seq("-Xmax-inlines", "1000"),
    libraryDependencies ++= Seq(
      scalaOrganization.value %% "scala3-compiler" % scalaVersion.value,
    ),
  )

lazy val benchmark_2_13 = (project in file("modules/benchmark_2_13"))
  .enablePlugins(JmhPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    scalaVersion := Scala_2_13,
    javaOptions+= "-Xss10m",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.10",
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    ),
  )

lazy val benchmark_2_11 = (project in file("modules/benchmark_2_11"))
  .enablePlugins(JmhPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    scalaVersion := Scala_2_11,
    javaOptions+= "-Xss10m",
    libraryDependencies ++= Seq(
      "ch.epfl.lamp" %% "scala-records" % "0.4",
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    ),
  )
