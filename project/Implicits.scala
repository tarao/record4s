import sbt._
import sbt.Keys._
import sbtcrossproject.{CrossPlugin, CrossProject}
import scala.language.implicitConversions
import ProjectKeys.projectName

object Implicits {
  implicit class CrossProjectOps(private val p: CrossProject) extends AnyVal {
    def asModuleWithoutSuffix: CrossProject = asModule(true)

    def asModule: CrossProject = asModule(false)

    private def asModule(noSuffix: Boolean): CrossProject = {
      val project = p.componentProjects(0)
      val s = project.settings(0)
      p
        .settings(
          moduleName := {
            if (noSuffix)
              (ThisBuild / projectName).value
            else
              s"${(ThisBuild / projectName).value}-${(project / name).value}"
          },
          CrossPlugin.autoImport.crossProjectBaseDirectory := {
            val dir = file(s"modules/${(project / name).value}")
            IO.resolve((LocalRootProject / baseDirectory).value, dir)
          },
        )
        .configure(project =>
          project.in(file("modules") / project.base.getPath),
        )
    }
  }

  implicit def builderOps(b: CrossProject.Builder): CrossProjectOps =
    new CrossProjectOps(b.build())
}
