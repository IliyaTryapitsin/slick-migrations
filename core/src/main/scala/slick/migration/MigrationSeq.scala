package slick.migration

import scala.slick.jdbc.JdbcBackend

/**
 * Holds a sequence of [[Migration]]s and performs them one after the other.
 */
case class MigrationSeq(migrations: Migration*) extends Migration {
  final def apply()(implicit session: JdbcBackend#Session) = migrations foreach (_())

  override def toString = migrations.mkString(";\n")
}
