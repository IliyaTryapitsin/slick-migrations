import sbt._

object Resolvers {
  val MavenRepository             = "Maven Central Repository" at "https://repo1.maven.org/maven2/"
  val SonatypeSnapshotsRepository = "Sonatype Snapshots Nexus" at "https://oss.sonatype.org/content/repositories/snapshots"
  val SonatypeReleasesRepository  = "Sonatype Releases Nexus"  at "https://oss.sonatype.org/content/repositories/releases"
}