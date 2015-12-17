
// === PROJECT DEFINITION
lazy val `slick-migration` = (project in file(".")).
  settings(
    organization := "com.arcusys.slick",
    name := "slick-migration",
    version := "2.1.0.2",
    scalaVersion := Version.scala,
    crossScalaVersions := Seq(Version.scala, "2.11.5"),
    javaOptions in Test       ++= Seq("-Duser.language=en", "-Duser.region=us"),
    resolvers                 ++= Resolvers.ArcusysRepositories,
    resolvers                 += Resolvers.MavenRepository,
    libraryDependencies       ++= Dependencies.common,
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://nexus.intra.arcusys.fi/mvn/content/repositories/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "arcusys-snapshots/")
      else
        Some("releases"  at nexus + "arcusys-releases/")
    }
  )
// .settings(scalariformSettings: _*)

//=== SBT SHELL PROMPT
shellPrompt in ThisBuild := { state => "[" + sbt.Project.extract(state).currentRef.project + "]> " }