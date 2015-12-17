package com.arcusys.slick.migration.dialect

import com.arcusys.slick.migration.ast._

import scala.slick.ast.FieldSymbol
import scala.slick.driver.MySQLDriver

/**
 * Created by Iliya Tryapitsin on 20/01/15.
 */
class MySQLDialect(driver: MySQLDriver) extends Dialect[MySQLDriver](driver) with SimulatedRenameIndex {
  override def autoInc(ci: ColumnInfo) = if (ci.autoInc) " AUTO_INCREMENT" else ""

  override def quoteIdentifier(id: String): String = {
    val s = new StringBuilder(id.length + 4) append '`'
    for (c <- id) if (c == '"') s append "\"\"" else s append c
    (s append '`').toString
  }

  override def dropIndex(index: IndexInfo) =
    s"drop index ${quoteIdentifier(index.name)} on ${quoteTableName(index.table)}"

  override def renameColumn(table: TableInfo, from: ColumnInfo, to: String) = {
    val newCol = from.copy(name = to)
    s"""alter table ${quoteTableName(table)}
      | change ${quoteIdentifier(from.name)}
      | ${columnSql(newCol, false)}""".stripMargin
  }

  override def alterColumnNullability(table: TableInfo, column: ColumnInfo) =
    renameColumn(table, column, column.name)

  override def alterColumnType(table: TableInfo, column: ColumnInfo) =
    Seq(renameColumn(table, column, column.name))

  override def dropForeignKey(table: TableInfo, name: String) =
    s"alter table ${quoteTableName(table)} drop foreign key ${quoteIdentifier(name)}"

  override def createPrimaryKey(table: TableInfo, name: String, columns: Seq[FieldSymbol]) =
    s"""alter table ${quoteTableName(table)}
      | add constraint primary key
      | ${columnList(columns: _*)}""".stripMargin

  override def dropPrimaryKey(table: TableInfo, name: String) =
    s"alter table ${quoteTableName(table)} drop primary key"
}
