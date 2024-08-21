import ProjectKeys._
import Implicits._

ThisBuild / tlBaseVersion := "0.14"

ThisBuild / projectName := "record4s"
ThisBuild / groupId     := "com.github.tarao"
ThisBuild / rootPkg     := s"${groupId.value}.${projectName.value}"

ThisBuild / organization     := groupId.value
ThisBuild / organizationName := "record4s authors"
ThisBuild / homepage         := Some(url("https://github.com/tarao/record4s"))
ThisBuild / startYear        := Some(2023)
ThisBuild / licenses         := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("tarao", "INA Lintaro"),
  tlGitHubDev("windymelt", "Windymelt"),
)

val Scala_3 = "3.3.3"
val Scala_2_13 = "2.13.14"
val Scala_2_11 = "2.11.12"

ThisBuild / scalaVersion       := Scala_3
ThisBuild / crossScalaVersions := Seq(Scala_3)
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("8"),
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
)
val BenchmarkJavaVersion = JavaSpec.temurin("11")

val circeVersion = "0.14.9"
val scalaTestVersion = "3.2.19"

lazy val compileSettings = Def.settings(
  // Default options are set by sbt-typelevel-settings
  tlFatalWarnings := true,
  scalacOptions --= Seq(
    "-Ykind-projector:underscores", // https://github.com/lampepfl/dotty/issues/14952
    "-Xkind-projector:underscores",
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
  .aggregate(core, circe, upickle)
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
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.getshaka" %%% "native-converter" % "0.9.0",
    ),
  )

lazy val circe = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .dependsOn(core % "compile->compile;test->test")
  .asModule
  .settings(commonSettings)
  .settings(
    description := "circe integration for record4s",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core"    % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion % Test,
      "io.circe" %%% "circe-parser"  % circeVersion % Test,
    ),
  )

lazy val upickle = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .dependsOn(core % "compile->compile;test->test")
  .asModule
  .settings(commonSettings)
  .settings(
    description := "uPickle / uJson integration for record4s",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % "3.3.1",
    ),
  )

lazy val benchmark_3 = (project in file("modules/benchmark_3"))
  .dependsOn(core.jvm)
  .settings(commonSettings)
  .enablePlugins(JmhPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    Compile / run / fork := true,
    Compile / run / javaOptions ++= Seq(
      "-Xss10m",
    ),
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
    Compile / run / javaOptions ++= Seq(
      "-Xss10m",
    ),
    libraryDependencies ++= Seq(
      "com.chuusai"          %% "shapeless"      % "2.3.10",
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    ),
  )

lazy val benchmark_2_11 = (project in file("modules/benchmark_2_11"))
  .enablePlugins(JmhPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    scalaVersion    := Scala_2_11,
    tlFatalWarnings := false,
    Compile / run / javaOptions ++= Seq(
      "-Xss10m",
    ),
    libraryDependencies ++= Seq(
      "ch.epfl.lamp"         %% "scala-records"  % "0.4",
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    ),
  )

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    id     = "build-benchmark",
    name   = "Build and Test Benchmarks",
    javas  = List(BenchmarkJavaVersion),
    scalas = Nil,
    matrixAdds = Map(
      "project" -> List(benchmark_2_11.id, benchmark_2_13.id, benchmark_3.id),
    ),
    steps = List(WorkflowStep.Checkout) ++ WorkflowStep.SetupJava(
      List(BenchmarkJavaVersion),
    ) ++ githubWorkflowGeneratedCacheSteps.value ++ List(
      WorkflowStep.Run(
        name     = Some("sbt update (project)"),
        commands = List("sbt 'project ${{ matrix.project }}' update"),
      ),
      WorkflowStep.Run(
        name = Some("Test"),
        commands =
          List("sbt 'project ${{ matrix.project }}' 'Jmh / run TimersBench'"),
      ),
    ),
  ),
)

ThisBuild / githubWorkflowTargetBranches := Seq("master")
ThisBuild / tlCiReleaseBranches          := Seq() // publish only tags

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    id     = "coverage",
    name   = "Generate coverage report",
    javas  = List(githubWorkflowJavaVersions.value.last),
    scalas = githubWorkflowScalaVersions.value.toList,
    steps = List(WorkflowStep.Checkout) ++ WorkflowStep.SetupJava(
      List(githubWorkflowJavaVersions.value.last),
    ) ++ githubWorkflowGeneratedCacheSteps.value ++ List(
      WorkflowStep.Sbt(List("coverage", "rootJVM/test", "coverageAggregate")),
      WorkflowStep.Use(
        UseRef.Public(
          "codecov",
          "codecov-action",
          "v4",
        ),
        params = Map(
          "flags" -> List("${{matrix.scala}}").mkString(","),
        ),
        env = Map(
          "CODECOV_TOKEN" -> "${{secrets.CODECOV_TOKEN}}",
        ),
      ),
    ),
  ),
)

ThisBuild / tlSitePublishBranch := None // update on tag releases
lazy val docs = project
  .in(file("site"))
  .dependsOn(core.jvm, circe.jvm, upickle.jvm)
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    scalacOptions --= Seq(
      "-Wunused:locals",
    ),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core"    % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser"  % circeVersion,
    ),
    mdocExtraArguments ++= Seq(
      "--exclude",
      ".*.md",
    ),
    tlSiteApiModule := Some((core.jvm / projectID).value),
    laikaConfig := {
      import laika.config.{ApiLinks, LinkConfig, SourceLinks}
      val version = mdocVariables.value("VERSION")
      val apiUrl = {
        val group = groupId.value
        val project = projectName.value
        val scalaVer = scalaBinaryVersion.value
        val base = s"https://javadoc.io/doc/${group}/${project}_${scalaVer}"
        s"${base}/${version}/"
      }
      val sourceUrl = {
        val scmUrl =
          scmInfo.value.map(_.browseUrl.toString).orElse(homepage.value).get
        val moduleBase = {
          val rootDir = (root.all / baseDirectory).value.toString
          val crossDir = (core.jvm / crossProjectBaseDirectory).value.toString
          crossDir.stripPrefix(s"${rootDir}/")
        }
        val base = (core.jvm / baseDirectory).value
        val dir = (core.jvm / Compile / scalaSource)
          .value
          .toString
          .stripPrefix(s"${base}/")

        s"${scmUrl}/tree/v${version}/${moduleBase}/${dir}/"
      }

      laikaConfig
        .value
        .withRawContent
        .withConfigValue(
          LinkConfig
            .empty
            .addApiLinks(
              ApiLinks(apiUrl),
            )
            .addApiLinks(
              ApiLinks(s"https://scala-lang.org/api/${scalaVersion.value}/")
                .withPackagePrefix("scala"),
            )
            .addSourceLinks(
              SourceLinks(
                baseUri = sourceUrl,
                suffix  = "scala",
              ),
            ),
        )
    },
    laikaTheme := {
      import laika.ast.LengthUnit._
      import laika.ast.Path.Root
      import laika.ast._
      import laika.helium.config._

      val home = Root / "index.md"
      val logo = Root / "img" / "record4s.svg"
      val copyright =
        s"Copyright &copy; ${startYear.value.get} ${organizationName.value}"

      tlSiteHelium
        .value
        .site
        .internalCSS(Root / "css" / "site.css")
        .site
        .favIcons(Favicon.internal(logo))
        .site
        .topNavigationBar(
          homeLink = LinkGroup.create(
            ImageLink.internal(home, Image.internal(logo)),
            TextLink.internal(home, projectName.value),
          ),
        )
        .site
        .footer(copyright)
        .site
        .fontSizes(
          body    = px(16),
          code    = em(1),
          title   = pt(36),
          header2 = pt(24),
          header3 = pt(20),
          header4 = pt(18),
          small   = pt(10),
        )
        .build
    },
  )
githubWorkflowGenerate := {
  val sbt =
    if (githubWorkflowUseSbtThinClient.value) {
      githubWorkflowSbtCommand.value + " --client"
    } else {
      githubWorkflowSbtCommand.value
    }

  def indent(output: String, level: Int): String = {
    val space = (0 until level * 2).map(_ => ' ').mkString
    (space + output.replace("\n", s"\n$space"))
      .replaceAll("""\n[ ]+\n""", "\n\n")
  }

  val site = {
    val job = githubWorkflowGeneratedCI.value.find(_.id == "site").get
    WorkflowJob(
      id   = job.id,
      name = job.name,
      steps = job.steps.map { step =>
        if (step.name == Some("Publish site")) {
          step.withCond(step.cond.map { cond =>
            s"github.event_name == 'workflow_dispatch' || ( ${cond} )"
          })
        } else {
          step
        }
      },
      sbtStepPreamble   = job.sbtStepPreamble,
      cond              = job.cond,
      permissions       = job.permissions,
      env               = job.env,
      oses              = job.oses,
      scalas            = job.scalas,
      javas             = job.javas,
      needs             = job.needs,
      matrixFailFast    = job.matrixFailFast,
      matrixAdds        = job.matrixAdds,
      matrixIncs        = job.matrixIncs,
      matrixExcs        = job.matrixExcs,
      runsOnExtraLabels = job.runsOnExtraLabels,
      container         = job.container,
      environment       = job.environment,
      concurrency       = job.concurrency,
      timeoutMinutes    = job.timeoutMinutes,
    )
  }
  val yml = baseDirectory.value / ".github" / "workflows" / "site.yml"
  val rendered = GenerativePlugin.compileJob(site, sbt)

  IO.write(
    yml,
    s"""
       |# This file was automatically generated by sbt-github-actions using the
       |# githubWorkflowGenerate task. You should add and commit this file to
       |# your git repository. It goes without saying that you shouldn't edit
       |# this file by hand! Instead, if you wish to make changes, you should
       |# change your sbt build configuration to revise the workflow description
       |# to meet your needs, then regenerate this file.
       |
       |name: Manual Site Generation
       |
       |on:
       |  workflow_dispatch:
       |
       |env:
       |  GITHUB_TOKEN: $${{ secrets.GITHUB_TOKEN }}
       |
       |jobs:
       |${indent(rendered, 1)}
       |""".stripMargin,
  )

  githubWorkflowGenerate.value
}
