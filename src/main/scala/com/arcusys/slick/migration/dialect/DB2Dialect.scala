package com.arcusys.slick.migration.dialect

import com.arcusys.slick.drivers.DB2Driver
import com.arcusys.slick.migration.ast._

class DB2Dialect(driver: DB2Driver) extends Dialect[DB2Driver](driver) {
  override def autoInc(ci: ColumnInfo) =
    if (ci.autoInc) " GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1)" else ""

  /** It cannot be rename if there is any foreign keys and/or any othert table that is referncing on table to remove */
  override def renameTable(table: TableInfo, to: String) =
    s"RENAME TABLE ${quoteTableName(table)} TO ${quoteIdentifier(to)}"

  override def renameIndex(old: IndexInfo, newName: String): Seq[String] = List(
    s"RENAME INDEX ${quoteIdentifier(old.name)} TO ${quoteIdentifier(newName)}"
  )

  override def addColumn(table: TableInfo, column: ColumnInfo) = {
    s"ALTER TABLE ${quoteTableName(table)} ADD ${addColumnSql(column)}"
  }

  override def renameColumn(table: TableInfo, from: ColumnInfo, to: String) =
    s"""ALTER TABLE ${quoteTableName(table)}
      | RENAME COLUMN ${quoteIdentifier(from.name)}
      | TO ${quoteIdentifier(to)}""".stripMargin

  /**
   * If column is marked as "NOT NULL" and default is not provided then add such column as nullable,
   * otherwise add by default.
   */
  private def addColumnSql(ci: ColumnInfo): String = {
    def name = quoteIdentifier(ci.name)
    def typ = columnType(ci)
    def default = ci.default.map(" DEFAULT " + _).getOrElse("")

    if (ci.notNull && default.isEmpty)
      s"$name $typ NULL ${primaryKey(ci, false)}"
    else
      s"$name $typ$default${notNull(ci)}${primaryKey(ci, false)}"
  }
}