package com.arcusys.slick.migration.table

import com.arcusys.slick.migration.ReversibleMigration
import com.arcusys.slick.migration.dialect.Dialect

import scala.slick.driver.JdbcProfile

/**
 * The concrete [[TableMigration]] class used when all operations are reversible.
 * This class extends [[ReversibleMigration]] and as such includes a [[reverse]] method
 * that returns a `TableMigration` that performs the inverse operations ("down migration").
 */
final class ReversibleTableMigration[T <: JdbcProfile#Table[_]] private[table] (table: T,
    protected[table] val data: TableMigrationData)(implicit dialect: Dialect[_]) extends TableMigration[T](table) with ReversibleMigration {
  outer =>

  /*
   * This should be redundant since the constructor is private[migration] and should only be called
   * when these requirements are known to hold.
   */
  require(data.tableDrop == false)
  require(data.columnsAlterType.isEmpty)
  require(data.columnsAlterDefault.isEmpty)
  require(data.columnsAlterNullability.isEmpty)

  type Self = ReversibleTableMigration[T]

  protected def withData(d: TableMigrationData) = new ReversibleTableMigration(table, d)

  def reverse = {
    new IrreversibleTableMigration(
      table,
      outer.data.tableRename match {
        case Some(name) => outer.tableInfo.copy(tableName = name)
        case None       => outer.tableInfo
      },
      data.copy(
        tableDrop = data.tableCreate,
        tableCreate = false,
        tableRename = data.tableRename.map(_ => tableInfo.tableName),
        columnsCreate = data.columnsDrop.reverse,
        columnsDrop = data.columnsCreate.reverse,
        columnsRename = data.columnsRename.map {
          case (ci, s) => (ci.copy(name = s), ci.name)
        },
        columnsAlterType = Nil,
        columnsAlterDefault = Nil,
        columnsAlterNullability = Nil,
        foreignKeysCreate = data.foreignKeysDrop.reverse,
        foreignKeysDrop = data.foreignKeysCreate.reverse,
        primaryKeysCreate = data.primaryKeysDrop.reverse,
        primaryKeysDrop = data.primaryKeysCreate.reverse,
        indexesCreate = data.indexesDrop.reverse,
        indexesDrop = data.indexesCreate.reverse,
        indexesRename = data.indexesRename.map {
          case (ii, s) => (ii.copy(name = s), ii.name)
        }
      )
    )
  }
}
