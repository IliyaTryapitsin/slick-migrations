package com.arcusys.slick.migration

/**
 * Created by Iliya Tryapitsin on 20/01/15.
 */

import scala.slick.jdbc.JdbcBackend

/**
 * A [[Migration]] defined in terms of SQL commands.
 * This trait implements `apply` and instead defines an
 * abstract [[SqlMigration.sql]] method.
 */
trait SqlMigration extends Migration {
  /**
   * The SQL statements to run
   */
  def sql: Seq[String]

  /**
   * Runs all the SQL statements in a single transaction
   */
  def apply()(implicit session: JdbcBackend#Session) = {
    val sq = sql
    session.withTransaction {
      session.withStatement() { st =>
        for (s <- sq)
          try st execute s.replace("\n", "").replace("\t", "").replace("\r", "")
          catch {
            case e: java.sql.SQLException =>
              throw MigrationException(s"Could not execute sql: '$s'", e)
          }
      }
    }
  }

  override def toString = sql.mkString(";\n")
}

/**
 * Convenience factory for [[SqlMigration]]
 * @example {{{ SqlMigration("drop table t1", "update t2 set x=10 where y=20") }}}
 */
object SqlMigration {
  def apply(sql: String*) = {
    def sql0 = sql
    new SqlMigration {
      def sql = sql0
    }
  }
}