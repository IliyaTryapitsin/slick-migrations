package com.arcusys.slick.migration.dialect

import com.arcusys.slick.migration.ast.{ AstHelpers, ColumnInfo, IndexInfo, TableInfo }
import com.arcusys.slick.migration.table.TableMigrationData
import com.arcusys.slick.migration._
import scala.slick.ast.FieldSymbol
import scala.slick.driver._
import scala.slick.model.ForeignKeyAction
import com.arcusys.slick.drivers._

/**
 * Base class for database dialects.
 * Provides methods that return the dialect-specific SQL strings
 * for performing various database operations.
 * The most important method is perhaps [[migrateTable]], which is called from
 * [[com.arcusys.slick.migration.table.TableMigration#sql]].
 * These methods are to be overriden in database-specific subclasses as needed.
 * @tparam D The corresponding Slick driver type.
 *           Not used, but may come in handy in certain situations.
 */
class Dialect[D <: JdbcDriver](val driver: JdbcDriver) extends AstHelpers {

  def quoteIdentifier(id: String): String = {
    val s = new StringBuilder(id.length + 4) append '"'
    for (c <- id) if (c == '"') s append "\"\"" else s append c
    (s append '"').toString
  }

  protected def quoteTableName(t: TableInfo): String = t.schemaName match {
    case Some(s) => quoteIdentifier(s) + "." + quoteIdentifier(t.tableName)
    case None    => quoteIdentifier(t.tableName)
  }

  def quotedTableName(schemaName: Option[String],
    tableName: String) = quoteTableName(new TableInfo(schemaName, tableName))

  protected def quotedColumnNames(ns: FieldSymbol*) = ns.map(fs => quoteIdentifier(fs.name))

  def columnType(ci: ColumnInfo): String = ci.sqlType.get

  def autoInc(ci: ColumnInfo) = if (ci.autoInc) " AUTOINCREMENT" else ""

  def primaryKey(ci: ColumnInfo, newTable: Boolean) =
    (if (newTable && ci.isPk) " PRIMARY KEY" else "") + autoInc(ci)

  def notNull(ci: ColumnInfo) = if (ci.notNull) " NOT NULL" else ""

  def columnSql(ci: ColumnInfo, newTable: Boolean = true): String = {
    def name = quoteIdentifier(ci.name)
    def typ = columnType(ci)
    def default = ci.default.map(" DEFAULT " + _).getOrElse("")
    s"$name $typ$default${notNull(ci)}${primaryKey(ci, newTable)}"
  }

  def columnList(columns: FieldSymbol*) =
    quotedColumnNames(columns: _*).mkString("(", ", ", ")")

  def createTable(table: TableInfo, columns: Seq[ColumnInfo]): String =
    s"""create table ${quoteTableName(table)} (
      | ${
      columns map {
        columnSql(_, true)
      } mkString ", "
    }
      |)""".stripMargin

  def dropTable(table: TableInfo): String =
    s"drop table ${quoteTableName(table)}"

  def renameTable(table: TableInfo, to: String) =
    s"""alter table ${quoteTableName(table)}
      | rename to ${quoteIdentifier(to)}""".stripMargin

  def createForeignKey(sourceTable: TableInfo, name: String, sourceColumns: Seq[FieldSymbol], targetTable: TableInfo, targetColumns: Seq[FieldSymbol], onUpdate: ForeignKeyAction, onDelete: ForeignKeyAction): String =
    s"""alter table ${quoteTableName(sourceTable)}
      | add constraint ${quoteIdentifier(name)}
      | foreign key ${columnList(sourceColumns: _*)}
      | references ${quoteTableName(targetTable)}
      | (${quotedColumnNames(targetColumns: _*) mkString ", "})
      | on update ${onUpdate.action} on delete ${onDelete.action}""".stripMargin

  def dropConstraint(table: TableInfo, name: String) =
    s"alter table ${quoteTableName(table)} drop constraint ${quoteIdentifier(name)}"

  def dropForeignKey(sourceTable: TableInfo, name: String) =
    dropConstraint(sourceTable, name)

  def createPrimaryKey(table: TableInfo, name: String, columns: Seq[FieldSymbol]) =
    s"""alter table ${quoteTableName(table)}
      | add constraint ${quoteIdentifier(name)} primary key
      | ${columnList(columns: _*)}""".stripMargin

  def dropPrimaryKey(table: TableInfo, name: String) =
    dropConstraint(table, name)

  def createIndex(index: IndexInfo) =
    s"""create ${if (index.unique) "unique" else ""}
      | index ${quoteIdentifier(index.name)} on ${quoteTableName(index.table)}
      | ${columnList(index.columns: _*)}""".stripMargin

  def dropIndex(index: IndexInfo) =
    s"drop index ${quoteIdentifier(index.name)}"

  def renameIndex(old: IndexInfo, newName: String): Seq[String] = List(
    s"alter index ${quoteIdentifier(old.name)} rename to ${quoteIdentifier(newName)}"
  )

  def addColumn(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
      | add column ${columnSql(column, false)}""".stripMargin

  def dropColumn(table: TableInfo, column: String) =
    s"""alter table ${quoteTableName(table)}
      | drop column ${quoteIdentifier(column)}""".stripMargin

  def renameColumn(table: TableInfo, from: ColumnInfo, to: String) =
    s"""alter table ${quoteTableName(table)}
      | alter column ${quoteIdentifier(from.name)}
      | rename to ${quoteIdentifier(to)}""".stripMargin

  def alterColumnType(table: TableInfo, column: ColumnInfo): Seq[String] = List(
    s"""alter table ${quoteTableName(table)}
      | alter column ${quoteIdentifier(column.name)}
      | set data type ${column.sqlType.get}""".stripMargin
  )

  def alterColumnDefault(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
      | alter column ${quoteIdentifier(column.name)}
      | set default ${column.default getOrElse "null"}""".stripMargin

  def alterColumnNullability(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
      | alter column ${quoteIdentifier(column.name)}
      | ${if (column.notNull) "set" else "drop"} not null""".stripMargin

  /*
   *  DROP | CREATE | RENAME | ACTION
   *  false| false  | None   | assume table exists and do things via alter table
   *  false| false  | Some   | rename table, plus as above
   *  false| true   | None   | put as much as possible in the create table statement
   *  false| true   | Some   | create with new name, plus as above
   *  true | false  | None   | just drop it
   *  true | false  | Some   | let it error (type safety??)
   *  true | true   | None   | drop, then create table with as much as possible
   *  true | true   | Some   | drop, then create table with new name, with as much as possible
   *
   *
   * In summary:
   *  - if drop, do the drop then continue
   *  - if !drop, continue
   *  - if create, incorporate as much as possible into the create statement, including any rename
   *  - if !create, do rename + other alters
   */
  def migrateTable[E <: D](table: TableInfo, tmd: TableMigrationData): Seq[String] = {
    val drop =
      if (!tmd.tableDrop) Nil
      else Seq(dropTable(table))

    val createColumns = if (tmd.tableCreate) {
      val tbl = tmd.tableRename match {
        case Some(name) => table.copy(tableName = name)
        case None       => table
      }
      Seq(createTable(tbl, tmd.columnsCreate))
    } else
      tmd.tableRename.map {
        renameTable(table, _)
      }.toList ++
        tmd.columnsCreate.map {
          addColumn(table, _)
        }
    val modifyColumns =
      tmd.columnsDrop.map { c => dropColumn(table, c.name) } ++
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

object Dialect {
  def apply(jdbcDriver: JdbcDriver): Option[Dialect[_]] = jdbcDriver match {
    case driver: H2Driver        => Some(new H2Dialect(driver))
    case driver: HsqldbDriver    => Some(new HsqldbDialect(driver))
    case driver: DerbyDriver     => Some(new DerbyDialect(driver))
    case driver: MySQLDriver     => Some(new MySQLDialect(driver))
    case driver: SQLiteDriver    => Some(new SQLiteDialect(driver))
    case driver: PostgresDriver  => Some(new PostgresDialect(driver))
    case driver: OracleDriver    => Some(new OracleDialect(driver))
    case driver: SQLServerDriver => Some(new SQLServerDialect(driver))
    case driver: DB2Driver       => Some(new DB2Dialect(driver))
    case driver: SybaseDriver    => Some(new SybaseAnywhereDialect(driver))
    case _                       => None
  }

  def apply(jdbcDriver: JdbcDriver,
    additionalDialects: Map[JdbcDriver, Dialect[_]]): Option[Dialect[_]] =
    Dialect(jdbcDriver) orElse additionalDialects.get(jdbcDriver)
}