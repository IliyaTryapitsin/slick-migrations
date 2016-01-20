import sbt.Keys._
import sbt._
import Resolvers._

object Settings {
  (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield
    credentials += Credentials(
      SonatypeSnapshotsRepository.name,
      "oss.sonatype.org",
      username,
      password
    )
  ) getOrElse credentials

  val organizationName = "slick.migration"
  val productName      = "slick-migration"
  val currentVersion   = "2.1.0-SNAPSHOT"
  val locales          = "-Duser.language=en" :: "-Duser.region=us" :: Nil
  val scalaVersions    = Version.scala        :: "2.11.5"           :: Nil
  val noPublishing = seq(
    publish := (),
    publishLocal := (),
    publishTo := None
  )

  val basicSettings = seq(
    version                 := currentVersion,
    crossScalaVersions      := scalaVersions,
    javaOptions in Test    ++= locales,
    libraryDependencies    ++= Dependencies.common,
    scalaVersion            := Version.scala,
    organization            := Settings.organizationName,
    publishMavenStyle       := true,
    publishArtifact in Test := false,
    pomIncludeRepository    := { _ => false },

    resolvers +=
      Resolvers.MavenRepository   ::
      SonatypeReleasesRepository  ::
      SonatypeSnapshotsRepository ::
      Nil,

    publishTo := {
      if (isSnapshot.value)
      Some(SonatypeSnapshotsRepository)
      else
      Some(SonatypeReleasesRepository)
    },

    pomExtra :=
      <url>http://jsuereth.com/scala-arm</url>
      <licenses>
        <license>
          <name>Apache</name>
          <url>https://raw.githubusercontent.com/itryapitsin/slick-migration/master/LICENSE</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:itryapitsin/slick-migration.git</url>
        <connection>scm:git:git@github.com:itryapitsin/slick-migration.git</connection>
      </scm>
      <developers>
        <developer>
          <id>itryapitsin</id>
          <name>Iliya Tryapitsin</name>
          <email>iliya.tryapitsin@gmail.com</email>
          <url>https://github.com/itryapitsin</url>
        </developer>
      </developers>
  )

  def getSettings(customSettings: Setting[_]*): Seq[Setting[_]] = basicSettings ++ customSettings
}