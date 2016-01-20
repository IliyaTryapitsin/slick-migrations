package slick.migration.dialect

import slick.migration.ast._

import scala.slick.ast.FieldSymbol
import scala.slick.model.ForeignKeyAction

class SQLServerDialect(driver: SQLServerDriver) extends Dialect[SQLServerDriver](driver) {

  override def autoInc(ci: ColumnInfo) = if (ci.autoInc) " IDENTITY" else ""

  override def renameTable(table: TableInfo, to: String) =
    s"sp_rename ${quoteTableName(table)}, ${quoteIdentifier(to)}"

  override def createForeignKey(sourceTable: TableInfo, name: String, sourceColumns: Seq[FieldSymbol], targetTable: TableInfo, targetColumns: Seq[FieldSymbol], onUpdate: ForeignKeyAction, onDelete: ForeignKeyAction): String =
    s"""alter table ${quoteTableName(sourceTable)}
      | add constraint ${quoteIdentifier(name)}
      | foreign key ${columnList(sourceColumns: _*)}
      | references ${quoteTableName(targetTable)}
      | (${quotedColumnNames(targetColumns: _*) mkString ", "})
      | on update
      | ${if (onUpdate == ForeignKeyAction.Restrict) "NO ACTION" else onUpdate.action}
      | on delete
      | ${if (onDelete == ForeignKeyAction.Restrict) "NO ACTION" else onDelete.action}""".stripMargin

  override def renameIndex(old: IndexInfo, newName: String): Seq[String] = List(
    s"""sp_rename ${quoteName(old.table, old.name)}, ${quoteIdentifier(newName)}, 'INDEX'"""
  )

  override def dropIndex(index: IndexInfo) =
    s"drop index ${quoteIdentifier(index.name)} on ${quoteTableName(index.table)}"

  override def addColumn(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)} add ${columnSql(column, false)}"""

  override def renameColumn(table: TableInfo, from: ColumnInfo, to: String) =
    s"""sp_rename ${quoteName(table, from.name)}, ${quoteIdentifier(to)}, 'COLUMN'"""

  override def alterColumnType(table: TableInfo, column: ColumnInfo): Seq[String] =
    List(alterColumnNullability(table, column))

  override def alterColumnDefault(table: TableInfo, column: ColumnInfo) =
    s"""BEGIN
    | declare @TableName nvarchar(256)
    | declare @ColumnName nvarchar(256)
    | set @TableName = N'${table.tableName}'
    | set @ColumnName = N'${column.name}'
    | declare @ConstraintName nvarchar(256)
    |
    | select @ConstraintName = d.name
    | from sys.tables t
    | join sys.default_constraints d
    | on d.parent_object_id = t.object_id
    | join
    | sys.columns c
    | on c.object_id = t.object_id
    | and c.column_id = d.parent_column_id
    | where t.name = @TableName
    | and c.name = @ColumnName
    |
    | declare @SqlCmd nvarchar(256)
    | SET @SqlCmd = N'ALTER TABLE ' + @TableName + N' DROP CONSTRAINT ' + @ConstraintName
    | EXEC sp_executesql @SqlCmd
    |
    | ALTER TABLE ${quoteTableName(table)}
    | ADD CONSTRAINT ${column.name}_DEFAULT DEFAULT (${column.default getOrElse "null"})
    | FOR ${quoteIdentifier(column.name)}
    |
    | END """.stripMargin

  override def alterColumnNullability(table: TableInfo, column: ColumnInfo) =
    s"""alter table ${quoteTableName(table)}
      | alter column ${quoteIdentifier(column.name)}
      | ${column.sqlType.get}
      | ${if (column.notNull) "NOT NULL" else "NULL"}""".stripMargin

  private def quoteName(t: TableInfo, name: String) = {
    val tableName = (t.schemaName match {
      case Some(s) => s + "." + t.tableName
      case None    => t.tableName
    }) + "."

    quoteIdentifier(tableName + name)
  }
}