import sbt.settingKey

object ProjectKeys {
  lazy val projectName = settingKey[String]("project name")
  lazy val groupId = settingKey[String]("artifact group ID")
  lazy val rootPkg = settingKey[String]("root package")
}
