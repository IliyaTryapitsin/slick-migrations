package slick.migration.table

/**
 * Created by Iliya Tryapitsin on 20/01/15.
 */

import slick.migration.ast._

import scala.slick.ast.FieldSymbol
import scala.slick.lifted.ForeignKey

/**
 * Internal data structure that stores schema manipulation operations to be performed on a table
 */
protected[migration] case class TableMigrationData( // not reversible: insufficient info: don't know entire old table
    tableDrop: Boolean = false,

    // reverse: create=false, drop=true
    tableCreate: Boolean = false,

    // reverse: reverse rename
    tableRename: Option[String] = None,

    // reverse: drop instead of create
    columnsCreate: Seq[ColumnInfo] = Nil,

    // reverse: create instead of drop
    columnsDrop: Seq[ColumnInfo] = Nil,

    // reverse: reverse rename
    columnsRename: Map[ColumnInfo, String] = Map.empty,

    // not reversible: insufficient info: don't have old data
    columnsAlterType: Seq[ColumnInfo] = Nil,

    // not reversible: insufficient info: don't have old data
    columnsAlterDefault: Seq[ColumnInfo] = Nil,

    // not reversible: insufficient info: don't have old data
    columnsAlterNullability: Seq[ColumnInfo] = Nil,

    // reverse: drop instead of create
    foreignKeysCreate: Seq[ForeignKey] = Nil,

    // reverse: create instead of drop
    foreignKeysDrop: Seq[ForeignKey] = Nil,

    // reverse: drop instead of create
    primaryKeysCreate: Seq[(String, Seq[FieldSymbol])] = Nil,

    // reverse: create instead of drop
    primaryKeysDrop: Seq[(String, Seq[FieldSymbol])] = Nil,

    // reverse: drop instead of create
    indexesCreate: Seq[IndexInfo] = Nil,

    // reverse: create instead of drop
    indexesDrop: Seq[IndexInfo] = Nil,

    // reverse: reverse rename
    indexesRename: Map[IndexInfo, String] = Map.empty) {
  override def productPrefix = "Table Migration of: "

  /* (non-Javadoc)
 * @see java.lang.Object#toString()
 */

  private def ifStr(test: Boolean)(str: => String) = if (test) str else ""

  private def wrap(data: Iterable[String]) = data.mkString("(", ",", ")")

  private def wrap(data: String) = "(" + data + ")"

  override def toString = "" +
    ifStr(tableDrop)("dropTable") +
    ifStr(tableCreate)("createTable") +
    ifStr(tableRename.isDefined)("renameTable to " + wrap(tableRename.getOrElse("<error>"))) +
    ifStr(columnsCreate.nonEmpty)("addColumns" + wrap(columnsCreate.map(_.name))) +
    ifStr(columnsDrop.nonEmpty)("dropColumns" + wrap(columnsDrop.map(_.name))) +
    ifStr(columnsRename.nonEmpty)("renameColumns" + wrap(columnsRename.map { case (from, to) => from.name + " -> " + to })) +
    ifStr(columnsAlterType.nonEmpty)("alterColumnTypes" + wrap(columnsAlterType.map(c => c.name + "-" + c.sqlType))) +
    ifStr(columnsAlterDefault.nonEmpty)("alterColumnDefaults" + wrap(columnsAlterDefault.map(c => c.name + "-" + c.default.getOrElse("<null>")))) +
    ifStr(columnsAlterNullability.nonEmpty)("alterColumnNullability" + wrap(columnsAlterNullability.map(c => c.name + "-" + !c.notNull))) +
    ifStr(foreignKeysCreate.nonEmpty)("addForeignKeys" + wrap(foreignKeysCreate.map {
      _.name
    })) + //TODO the next few are messy, clean them up
    ifStr(foreignKeysDrop.nonEmpty)("dropForeignKeys" + wrap(foreignKeysDrop.map(_.name))) +
    ifStr(primaryKeysCreate.nonEmpty)("createPrimaryKeys" + wrap(primaryKeysCreate.map(_.toString))) +
    ifStr(primaryKeysDrop.nonEmpty)("dropPrimaryKeys" + wrap(primaryKeysDrop.map(_.toString))) +
    ifStr(indexesCreate.nonEmpty)("createIndexes" + wrap(indexesCreate.map(_.name))) +
    ifStr(indexesDrop.nonEmpty)("dropIndexes" + wrap(indexesCreate.map(_.name))) +
    ifStr(indexesRename.nonEmpty)("renameIndexes" + wrap(indexesRename.map { case (from, to) => from.name + " -> " + to }))
}