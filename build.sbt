
import Settings._

// === PROJECT DEFINITION
lazy val `slick-migration-core` = (project in file("core"))
  .settings(getSettings { name := productName } : _*)
// .settings(scalariformSettings: _*)

lazy val `slick-migration-test-utils` = (project in file("test-utils"))
  .settings(getSettings { name := s"$productName-test-utils" } : _*)

// === Drivers
lazy val `slick-migration-derby-driver` = (project in file("drivers/derby"))
  .settings(getSettings { name := s"$productName-derby" } : _*)
  .dependsOn(`slick-migration-core`)

lazy val `slick-migration-h2-driver` = (project in file("drivers/h2"))
  .settings(getSettings { name := s"$productName-h2" } : _*)
  .dependsOn(`slick-migration-core`)

lazy val `slick-migration-hsql-driver` = (project in file("drivers/hsql"))
  .settings(getSettings { name := s"$productName-hsql" } : _*)
  .dependsOn(`slick-migration-core`)

lazy val `slick-migration-mysql-driver` = (project in file("drivers/mysql"))
  .settings(getSettings { name := s"$productName-mysql" } : _*)
  .dependsOn(`slick-migration-core`)

lazy val `slick-migration-postgres-driver` = (project in file("drivers/postgres"))
  .settings(getSettings { name := s"$productName-postgres" } : _*)
  .dependsOn(`slick-migration-core`)

lazy val `slick-migration-sqlite-driver` = (project in file("drivers/sqlite"))
  .settings(getSettings { name := s"$productName-sqlite" } : _*)
  .dependsOn(`slick-migration-core`)

//lazy val `slick-migration-oracle-driver` = (project in file("drivers/oracle"))
//  .settings(getSettings { name := s"$productName-oracle" } : _*)
//  .dependsOn(`slick-migration-core`)

//lazy val `slick-migration-db2-driver` = (project in file("drivers/db2"))
//  .settings(getSettings { name := s"$productName-db2" } : _*)
//  .dependsOn(`slick-migration-core`)

//lazy val `slick-migration-sqlserver-driver` = (project in file("drivers/sqlserver"))
//  .settings(getSettings { name := s"$productName-sqlserver" } : _*)
//  .dependsOn(`slick-migration-core`)

//lazy val `slick-migration-sybase-driver` = (project in file("drivers/sybase"))
//  .settings(getSettings { name := s"$productName-sybase" } : _*)
//  .dependsOn(`slick-migration-core`)

//=== SBT SHELL PROMPT
shellPrompt in ThisBuild := { state => "[" + sbt.Project.extract(state).currentRef.project + "]> " }