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
  val h2           = "1.3.170"
  val hsql         = "0.0.15"
  val mysql        = "5.1.34"
  val postgres     = System.getProperty("java.version") match {
    case version if version startsWith "1.6" => "9.1-901-1.jdbc4"
    case _                                   => "9.4-1201-jdbc41"
  }

}

object Libraries {
  //  val slickDrivers   = "com.arcusys.slick"          %%  "slick-drivers"              % Version.slickDrivers
  //  val oracle         = "com.oracle"                 %   "ojdbc6"                     % Version.oracle
  //  val sqlserver      = "com.microsoft.sqlserver"    %   "sqljdbc4"                   % Version.sqlserver
  //  val db2            = "com.ibm.db2"                %   "db2jcc4"                    % Version.db2
  // Db
  val scalatest      = "org.scalatest"              %%  "scalatest"                  % Version.scalatest
  val logback        = "ch.qos.logback"             %   "logback-classic"            % Version.logback
  val config         = "com.typesafe"               %   "config"                     % Version.config
  val h2             = "com.h2database"             %   "h2"                         % Version.h2
  val slick          = "com.typesafe.slick"         %%  "slick"                      % Version.slick
  val hsql           = "com.danidemi.jlubricant"    %   "jlubricant-embeddable-hsql" % Version.hsql
  val mysql          = "mysql"                      %   "mysql-connector-java"       % Version.mysql
  val postgres       = System.getProperty("java.version") match {
    case version if version startsWith "1.6" => "postgresql"     % "postgresql" % Version.postgres
    case _                                   => "org.postgresql" % "postgresql" % Version.postgres
  }

}

object Dependencies {

  val common = Seq(
    Libraries.slick,
//    Libraries.slickDrivers,
    Libraries.logback,
//    Libraries.oracle % Test,
//    Libraries.sqlserver % Test,
//    Libraries.db2 % Test,
    Libraries.scalatest % Test,
    Libraries.config % Test
  )
}