package com.arcusys.slick.migration.dialect

import com.arcusys.slick.drivers.SybaseDriver
import com.arcusys.slick.migration.ast._

import scala.slick.ast.FieldSymbol
import scala.slick.model.ForeignKeyAction

class SybaseAnywhereDialect(driver: SybaseDriver) extends Dialect(driver) {

  override def autoInc(ci: ColumnInfo) = if (ci.autoInc) " DEFAULT AUTOINCREMENT" else ""

  override def renameTable(table: TableInfo, to: String) =
    s"alter table ${quoteTableName(table)} rename ${quoteIdentifier(to)}"

  override def createForeignKey(
    sourceTable: TableInfo,
    name: String,
    sourceColumns: Seq[FieldSymbol],
    targetTable: TableInfo,
    targetColumns: Seq[FieldSymbol],
    onUpdate: ForeignKeyAction,
    onDelete: ForeignKeyAction): String = {

    s"""alter table ${quoteTableName(sourceTable)}
      | add constraint ${quoteIdentifier(name)}
      | foreign key ${columnList(sourceColumns: _*)}
      | references ${quoteTableName(targetTable)}
      | (${quotedColumnNames(targetColumns: _*) mkString ", "})
      | on update
      | ${if (onUpdate == ForeignKeyAction.NoAction) "RESTRICT" else onUpdate.action}
      | on delete
      | ${if (onDelete == ForeignKeyAction.NoAction) "RESTRICT" else onDelete.action}""".stripMargin
  }

  override def renameIndex(old: IndexInfo, newName: String): Seq[String] = List(
    s"""alter index ${quoteIdentifier(old.name)}
      | on ${quoteTableName(old.table)}
      | rename to ${quoteIdentifier(newName)}""".stripMargin
  )

  override def addColumn(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
      | add ${columnSql(column, false)}""".stripMargin

  override def dropColumn(table: TableInfo, column: String) =
    s"""alter table ${quoteTableName(table)}
      | drop ${quoteIdentifier(column)}""".stripMargin

  override def renameColumn(table: TableInfo, from: ColumnInfo, to: String) =
    s"""alter table ${quoteTableName(table)}
      | rename ${quoteIdentifier(from.name)}
      | to ${quoteIdentifier(to)}""".stripMargin

  override def alterColumnType(table: TableInfo, column: ColumnInfo): Seq[String] = List(
    s"""alter table ${quoteTableName(table)}
      | modify ${quoteIdentifier(column.name)} ${column.sqlType.get}""".stripMargin
  )

  override def alterColumnDefault(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
      | modify ${quoteIdentifier(column.name)}
      | ${column.default.map {"DEFAULT " + _} getOrElse "NULL"}""".stripMargin

  override def alterColumnNullability(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
      | modify ${quoteIdentifier(column.name)}
      | ${if (column.notNull) "NOT NULL" else "NULL"}""".stripMargin

}