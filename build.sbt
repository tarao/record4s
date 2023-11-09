import ProjectKeys._
import Implicits._

ThisBuild / tlBaseVersion := "0.9"

ThisBuild / projectName := "record4s"
ThisBuild / groupId     := "com.github.tarao"
ThisBuild / rootPkg     := "${groupId.value}.${projectName.value}"

ThisBuild / organization     := groupId.value
ThisBuild / organizationName := "record4s authors"
ThisBuild / homepage         := Some(url("https://github.com/tarao/record4s"))
ThisBuild / startYear        := Some(2023)
ThisBuild / licenses         := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("tarao", "INA Lintaro"),
  tlGitHubDev("windymelt", "Windymelt"),
)

val Scala_3 = "3.3.1"
val Scala_2_13 = "2.13.12"
val Scala_2_11 = "2.11.12"

ThisBuild / scalaVersion       := Scala_3
ThisBuild / crossScalaVersions := Seq(Scala_3)
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("8"),
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
)

val circeVersion = "0.14.6"
val scalaTestVersion = "3.2.17"

lazy val compileSettings = Def.settings(
  // Default options are set by sbt-typelevel-settings
  tlFatalWarnings := true,
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
  compileSettings,
  initialCommands := s"""
    import ${rootPkg.value}.*
  """,
)

lazy val root = tlCrossRootProject
  .aggregate(core, circe)
  .settings(commonSettings)
  .settings(
    console        := (core.jvm / Compile / console).value,
    Test / console := (core.jvm / Test / console).value,
    ThisBuild / Test / parallelExecution := false,
  )

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .asModuleWithoutSuffix
  .settings(commonSettings)
  .settings(
    description := "Extensible records for Scala",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
    ),
  )

lazy val circe = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .dependsOn(core % "compile->compile;test->test")
  .asModule
  .settings(commonSettings)
  .settings(
    description := "Circe integration for record4s",
    libraryDependencies ++= Seq(
      "io.circe"      %%% "circe-core"    % circeVersion,
      "io.circe"      %%% "circe-generic" % circeVersion     % Test,
      "io.circe"      %%% "circe-parser"  % circeVersion     % Test,
      "org.scalatest" %%% "scalatest"     % scalaTestVersion % Test,
    ),
  )

lazy val benchmark_3 = (project in file("modules/benchmark_3"))
  .dependsOn(core.jvm)
  .settings(commonSettings)
  .enablePlugins(JmhPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    Compile / run / fork := true,
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
    libraryDependencies ++= Seq(
      "com.chuusai"          %% "shapeless"      % "2.3.10",
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    ),
  )

lazy val benchmark_2_11 = (project in file("modules/benchmark_2_11"))
  .enablePlugins(JmhPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    scalaVersion := Scala_2_11,
    libraryDependencies ++= Seq(
      "ch.epfl.lamp"         %% "scala-records"  % "0.4",
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    ),
  )

ThisBuild / githubWorkflowTargetBranches := Seq("master")
ThisBuild / tlCiReleaseBranches          := Seq() // publish only tags
