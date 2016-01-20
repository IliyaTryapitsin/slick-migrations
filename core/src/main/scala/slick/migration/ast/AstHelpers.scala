package slick.migration.ast

/**
 * Created by Iliya Tryapitsin on 20/01/15.
 */
import slick.migration._
import scala.slick.ast._
import scala.slick.driver.JdbcProfile
import scala.slick.lifted.{ Column, Index }

/**
 * Helper trait for converting various representations of tables, columns, and indexes
 */
private[migration] trait AstHelpers {
  /**
   * @param table a Slick table object whose qualified name is needed
   * @return a `TableInfo` representing the qualified name of `table`
   */
  protected def tableInfo(table: TableNode): TableInfo = TableInfo(table.schemaName, table.tableName)

  /**
   * @return if `node` represents a reference to a table's column, that is, it is a `Select(_, f: FieldSymbol)`,
   *         then `Some(f)`; otherwise `None`
   */
  protected def fieldSym(node: Node): Option[FieldSymbol] = node match {
    case Select(_, f: FieldSymbol) => Some(f)
    case _                         => None
  }

  /**
   * @return a `FieldSymbol` representing the column
   */
  def fieldSym(column: Column[_]): FieldSymbol =
    fieldSym(column.toNode) getOrElse sys.error("Invalid column: " + column)

  /**
   * @param driver a Slick driver, used to extract `ColumnInfo#sqlType` and `ColumnInfo#notNull`
   *               by calling `typeInfoFor`
   * @return a `ColumnInfo` representing the relevant information in `column`
   */
  protected def columnInfo(driver: JdbcProfile, column: FieldSymbol): ColumnInfo = {
    column.tpe match {
      case driver.JdbcType(ti, isOpt) =>
        val initial = ColumnInfo(column.name, ti.sqlTypeName.toOption, !(isOpt || ti.scalaType.nullable), false, false, None)
        column.options.foldLeft(initial) {
          case (ci, ColumnOption.DBType(s))  => ci.copy(sqlType = Some(s))
          case (ci, ColumnOption.NotNull)    => ci.copy(notNull = true)
          case (ci, ColumnOption.Nullable)   => ci.copy(notNull = false)
          case (ci, ColumnOption.AutoInc)    => ci.copy(autoInc = true)
          case (ci, ColumnOption.PrimaryKey) => ci.copy(isPk = true)
          case (ci, ColumnOption.Default(v)) => ci.copy(default = Some(ti.valueToSQLLiteral(v)))
          case (ci, _)                       => ci
        }
    }
  }

  /**
   * @return an `IndexInfo` containing the relevant information from a Slick `Index`
   */
  protected def indexInfo(index: Index) =
    IndexInfo(tableInfo(index.table.tableNode), index.name, index.unique, index.on flatMap (fieldSym(_)))
}