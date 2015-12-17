package com.arcusys.slick.migration.dialect

import com.arcusys.slick.drivers.OracleDriver
import com.arcusys.slick.migration.ast.{ TableInfo, _ }
import com.arcusys.slick.migration.table.TableMigrationData

import scala.slick.ast.FieldSymbol
import scala.slick.model.ForeignKeyAction

class OracleDialect(driver: OracleDriver) extends Dialect[OracleDriver](driver) {
  private val TableNameLength = 30
  private val ForeignKeyNameLength = 30
  private val IndexNameLength = 30
  private val FieldNameLength = 30

  private def autoIncNames(t: TableInfo, c: ColumnInfo) =
    (s"${t.tableName}__${c.name}_seq", s"${t.tableName}__${c.name}_trg")

  /** increments a column via sequence and trigger */
  def createAutoInc(table: TableInfo, column: ColumnInfo) =
    if (column.autoInc) {
      val (seq, trg) = autoIncNames(table, column)
      val tab = quoteTableName(table)
      val col = quoteIdentifier(column.name)

      List(
        s"CREATE SEQUENCE $seq START WITH 1 INCREMENT BY 1",
        s"""CREATE OR REPLACE TRIGGER $trg
        | BEFORE INSERT
        | ON $tab
        | REFERENCING NEW AS NEW
        | FOR EACH ROW WHEN (NEW.$col IS NULL)
        | BEGIN SELECT $seq.nextval INTO :NEW.$col FROM sys.dual;
        | END;""".stripMargin
      )
    } else Nil

  def dropTrigger(table: TableInfo, column: ColumnInfo) =
    if (column.autoInc) {
      val (_, trg) = autoIncNames(table, column)
      List(s"drop trigger $trg")
    } else Nil

  override def createTable(table: TableInfo, columns: Seq[ColumnInfo]): String = {
    require(table.tableName.length <= TableNameLength, s"Table name '${table.tableName}' length of more that ${TableNameLength}")

    super.createTable(table, columns)
  }

  override def renameTable(table: TableInfo, to: String) = {
    require(to.length <= TableNameLength, s"Table name '${table.tableName}' length of more that ${TableNameLength}")

    super.renameTable(table, to)
  }

  override def autoInc(ci: ColumnInfo) = ""

  override def createForeignKey(
    sourceTable: TableInfo,
    name: String,
    sourceColumns: Seq[FieldSymbol],
    targetTable: TableInfo,
    targetColumns: Seq[FieldSymbol],
    onUpdate: ForeignKeyAction,
    onDelete: ForeignKeyAction): String = {

    require(name.length <= ForeignKeyNameLength, s"Foreign key name '${name}' length of more that ${ForeignKeyNameLength}")

    val constraint = new StringBuilder(
      s"""alter table ${quoteTableName(sourceTable)}
      | add constraint ${quoteIdentifier(name)}
      | foreign key ${columnList(sourceColumns: _*)}
      | references ${quoteTableName(targetTable)}
      | (${quotedColumnNames(targetColumns: _*) mkString ", "})""".stripMargin
    )

    if (onDelete == ForeignKeyAction.Cascade)
      constraint append " on delete cascade "
    if (onDelete == ForeignKeyAction.SetNull)
      constraint append " on delete set null "
    if (onUpdate == ForeignKeyAction.Cascade)
      constraint append " initially deferred "

    constraint.toString
  }

  override def createIndex(index: IndexInfo) = {
    require(index.name.length <= IndexNameLength, s"Index name '${index.name}' length of more that $IndexNameLength")

    if (index.unique)
      s"""alter table ${quoteTableName(index.table)}
      | add constraint ${quoteIdentifier(index.name)} unique
      | ${columnList(index.columns: _*)}""".stripMargin
    else super.createIndex(index)
  }

  override def dropIndex(index: IndexInfo) =
    if (index.unique)
      s"""alter table ${quoteTableName(index.table)}
      | drop constraint ${quoteIdentifier(index.name)}""".stripMargin
    else super.dropIndex(index)

  override def renameIndex(old: IndexInfo, newName: String) = {
    require(newName.length <= IndexNameLength, s"Index name '$newName' length of more that $IndexNameLength")

    if (old.unique) super.renameIndex(old, newName) ++ List(
      s"""alter table ${quoteTableName(old.table)}
        | rename constraint ${quoteIdentifier(old.name)} to ${quoteIdentifier(newName)}""".stripMargin)
    else super.renameIndex(old, newName)
  }

  override def addColumn(table: TableInfo, column: ColumnInfo) = {
    require(column.name.length <= FieldNameLength, s"Column name '${column.name}' length of more that ${FieldNameLength}")

    s"""alter table ${quoteTableName(table)}
    | add ${columnSql(column, false)}""".stripMargin
  }

  override def renameColumn(table: TableInfo, from: ColumnInfo, to: String) = {
    require(from.name.length <= FieldNameLength, s"Column name '${from.name}' length of more that ${FieldNameLength}")

    s"""alter table ${quoteTableName(table)}
    | rename column ${quoteIdentifier(from.name)} to ${quoteIdentifier(to)}""".stripMargin
  }

  override def alterColumnType(table: TableInfo, column: ColumnInfo): Seq[String] = List(
    s"""alter table ${quoteTableName(table)}
    | modify (${quoteIdentifier(column.name)} ${column.sqlType.get})""".stripMargin
  )

  override def alterColumnDefault(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
    | modify (${quoteIdentifier(column.name)} default ${column.default getOrElse "null"})""".stripMargin

  override def alterColumnNullability(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
    | modify (${quoteIdentifier(column.name)} ${if (column.notNull) "not null" else "null"})""".stripMargin

  /** This is the only right place to put autoincrement creation for new columns and for renaming columns */
  override def migrateTable[E <: OracleDriver](table: TableInfo, tmd: TableMigrationData): Seq[String] = {
    val drop =
      if (!tmd.tableDrop) Nil
      else Seq(dropTable(table))

    val createColumns = if (tmd.tableCreate) {
      val tbl = tmd.tableRename match {
        case Some(name) => table.copy(tableName = name)
        case None       => table
      }
      Seq(createTable(tbl, tmd.columnsCreate)) ++
        tmd.columnsCreate.flatMap(createAutoInc(table, _))
    } else
      tmd.tableRename.map {
        renameTable(table, _)
      }.toList ++
        tmd.columnsCreate.map {
          addColumn(table, _)
        } ++
        tmd.columnsCreate.flatMap {
          createAutoInc(table, _)
        }

    val modifyColumns =
      tmd.columnsDrop.map { c => dropColumn(table, c.name) } ++
        tmd.columnsDrop.flatMap { c => dropTrigger(table, c) } ++
        tmd.columnsRename.flatMap { case (k, v) => dropTrigger(table, k) } ++
        tmd.columnsRename.map { case (k, v) => renameColumn(table, k, v) } ++
        tmd.columnsAlterType.flatMap {
          alterColumnType(table, _)
        } ++
        tmd.columnsAlterDefault.map {
          alterColumnDefault(table, _)
        } ++
        tmd.columnsAlterNullability.map {
          alterColumnNullability(table, _)
        }

    val migrateIndexes =
      tmd.primaryKeysDrop.map { pk => dropPrimaryKey(table, pk._1) } ++
        tmd.primaryKeysCreate.map { case (name, cols) => createPrimaryKey(table, name, cols) } ++
        tmd.foreignKeysDrop.map { fk => dropForeignKey(table, fk.name) } ++
        tmd.foreignKeysCreate.map { fk =>
          createForeignKey(
            table,
            fk.name,
            fk.linearizedSourceColumns.flatMap(fieldSym(_).toSeq),
            tableInfo(fk.targetTable),
            fk.linearizedTargetColumnsForOriginalTargetTable.flatMap(fieldSym(_).toSeq),
            fk.onUpdate,
            fk.onDelete
          )
        } ++
        tmd.indexesDrop.map {
          dropIndex
        } ++
        tmd.indexesCreate.map {
          createIndex
        } ++
        tmd.indexesRename.flatMap { case (k, v) => renameIndex(k, v) }

    drop ++
      createColumns ++
      modifyColumns ++
      migrateIndexes
  }
}