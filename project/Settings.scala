import sbt.Keys._
import sbt._
import Resolvers._
import scoverage.ScoverageKeys._
import org.scoverage.coveralls.Imports.CoverallsKeys._

object Settings {

  val organizationName = "com.github.itryapitsin"
  val productName      = "slick-migration"
  val currentVersion   = "2.1.0-SNAPSHOT"
  val locales          = "-Duser.language=en" :: "-Duser.region=us" :: Nil
  val scalaVersions    = Version.scala        :: "2.11.5"           :: Nil

  val basicSettings = seq(
    version                 := currentVersion,
    crossScalaVersions      := scalaVersions,
    javaOptions in Test    ++= locales,
    libraryDependencies    ++= Dependencies.common,
    scalaVersion            := Version.scala,
    organization            := Settings.organizationName,
    publishMavenStyle       := true,
    publishArtifact in Test := false,
    coverageFailOnMinimum   := false,
    coverageEnabled         := true,
    coverageMinimum         := 70,
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    coverallsToken          := Option(sys.env("COVERALL_TOKEN")),
    pomIncludeRepository    := { _ => false },
    credentials             ++= {
      (for {
        username <- Option(sys.env("SONATYPE_USERNAME"))
        password <- Option(sys.env("SONATYPE_PASSWORD"))
      } yield {
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)
      }).toSeq
    },
    coverageHighlighting    := {
      if(scalaBinaryVersion.value == "2.11") true
      else false
    },
    resolvers ++= Seq(
      Resolvers.MavenRepository,
      SonatypeReleasesRepository,
      SonatypeSnapshotsRepository),

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