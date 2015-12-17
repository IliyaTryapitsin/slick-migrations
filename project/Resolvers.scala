import sbt._

object Resolvers {

  val ArcusysSnapshots =
    "Arcusys snapshots" at "https://nexus.intra.arcusys.fi/mvn/content/repositories/arcusys-snapshots/"

  val Arcusys3dpartyNonfree =
    "Arcusys 3d-party nonfree" at "https://nexus.intra.arcusys.fi/mvn/content/repositories/thirdparty-nonfree/"

  val ArcusysReleases =
    "Arcusys releases" at "https://nexus.intra.arcusys.fi/mvn/content/repositories/arcusys-releases/"

  val ArcusysRepositories = Seq(ArcusysSnapshots, ArcusysReleases, Arcusys3dpartyNonfree)

  val MavenRepository =
    "Maven Central Repository" at "https://repo1.maven.org/maven2/"
}