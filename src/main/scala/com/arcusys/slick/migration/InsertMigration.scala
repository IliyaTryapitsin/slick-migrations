package com.arcusys.slick.migration

import com.arcusys.slick.migration.dialect.Dialect

import scala.slick.driver.JdbcProfile
import scala.slick.lifted.Column

/**
 * Created by Iliya Tryapitsin on 18.04.15.
 */
object InsertMigration {
  def apply[T <: JdbcProfile#Table[_]](table: T,
    columnValuePairs: Map[Column[_], Any])(implicit dialect: Dialect[_]) = new InsertMigration[T](table, columnValuePairs, dialect)
}

class InsertMigration[T <: JdbcProfile#Table[_]](table: T,
    columnValuePairs: Map[Column[_], Any],
    dialect: Dialect[_]) extends CrudMigration(table, columnValuePairs)(dialect) {

  /**
   * The SQL statements to run
   */
  override def sql: Seq[String] = Seq(
    s"INSERT INTO ${dialect.quotedTableName(table.schemaName, table.tableName)} $columns VALUES ($values)"
  )
}