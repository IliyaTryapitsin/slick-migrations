import sbt.Keys._
import sbt._

object Settings {
  val organizationName = "slick.migration"
  val productName = "slick-migration"
  val currentVersion = "2.1.0"
  val locales = Seq("-Duser.language=en", "-Duser.region=us")
  val scalaVersions = Seq(Version.scala, "2.11.5")
  val publishSettings = { None
//    val nexus = "https://nexus.intra.arcusys.fi/mvn/content/repositories/"
//    if (version.value.trim.endsWith("SNAPSHOT"))
//      Some("snapshots" at nexus + "arcusys-snapshots/")
//    else
//      Some("releases"  at nexus + "arcusys-releases/")
  }

  val basicSettings = Seq(
    organization         := Settings.organizationName,
    version              := currentVersion,
    scalaVersion         := Version.scala,
    crossScalaVersions   := scalaVersions,
    javaOptions in Test ++= locales,
    resolvers            += Resolvers.MavenRepository,
    libraryDependencies ++= Dependencies.common,
    publishMavenStyle    := true,
    publishTo            := publishSettings
  )

  def getSettings(customSettings: Setting[_]*): Seq[Setting[_]] = basicSettings ++ customSettings
}