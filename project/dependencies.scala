import sbt._

object Version {
  val scala        = "2.10.5"
  val slick        = "2.1.0"
  val slickDrivers = "2.1.0"
  val logback      = "0.9.28"
  val scalatest    = "2.2.4"
  val config       = "1.2.1"
  val oracle       = "11.2.0.4.0"
  val sqlserver    = "4.1.5605.100"
  val db2          = "10.5fp5"
}

object Libraries {
  val slick          = "com.typesafe.slick"         %%  "slick"           % Version.slick
  val slickDrivers   = "com.arcusys.slick"          %%  "slick-drivers"   % Version.slickDrivers
  val scalatest      = "org.scalatest"              %%  "scalatest"       % Version.scalatest
  val logback        = "ch.qos.logback"             %   "logback-classic" % Version.logback
  val config         = "com.typesafe"               %   "config"          % Version.config
  val oracle         = "com.oracle"                 %   "ojdbc6"          % Version.oracle
  val sqlserver      = "com.microsoft.sqlserver"    %   "sqljdbc4"        % Version.sqlserver
  val db2            = "com.ibm.db2"                %   "db2jcc4"         % Version.db2
}

object Dependencies {

  val common = Seq(
    Libraries.slick,
    Libraries.slickDrivers,
    Libraries.logback,
    Libraries.oracle % Test,
    Libraries.sqlserver % Test,
    Libraries.db2 % Test,
    Libraries.scalatest % Test,
    Libraries.config % Test
  )

}