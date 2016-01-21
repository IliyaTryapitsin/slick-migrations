package slick.migration.table

import slick.migration._
import slick.migration.dialect.Dialect
import slick.migration.{SqlMigration, Migration}
import slick.migration.ast.{ AstHelpers, ColumnInfo, TableInfo }

import scala.slick.driver.JdbcProfile
import scala.slick.lifted._

/**
 * Factory for [[TableMigration]]s
 */
object TableMigration {
  /**
   * Creates a [[TableMigration]] that will perform migrations on `table`
   */
  def apply[T <: JdbcProfile#Table[_]](table: T)(implicit dialect: Dialect[_]) = new ReversibleTableMigration(table, TableMigrationData())

  /**
   * Creates a [[TableMigration]] that will perform migrations on the table
   * referenced by `tableQuery`
   */
  def apply[T <: JdbcProfile#Table[_]](tableQuery: TableQuery[T])(implicit dialect: Dialect[_]) = new ReversibleTableMigration(tableQuery.baseTableRow, TableMigrationData())
}

/**
 * The base class for table migrations.
 * A table migration is a [[Migration]] that performs changes to a table.
 *
 * See this class's methods for a list of available operations. Calling an operation method
 * returns a new `TableMigration` that has that operation added, over operations
 * contained in the original `TableMigration`. This allows for a nice method chaining syntax.
 *
 * Like all [[slick.migration.Migration]]s, you can run the resulting [[TableMigration]] by calling its
 * `apply` method (it expects an implicit `Session`). Being an [[SqlMigration]] you
 * can also call the `sql` method to see the SQL statement(s) it uses.
 *
 * This class is abstract; use its companion object as a factory to get an instance.
 *
 * @example {{{
 *                       object table1 extends Table[(Int, Int, Int)]("table1") {
 *                         def col1 = column[Int]("col1")
 *                         def col2 = column[Int]("col2")
 *                         def col3 = column[Int]("col3")
 *                         def * = col1 ~ col2 ~ col3
 *                       }
 *                       implicit val dialect = new H2Dialect
 *                       val migration = TableMigration(table1)
 *                                         .addColumns(_.col1, _.col2)
 *                                         .addColumns(_.col3)
 *          }}}
 * @groupname oper Schema Manipulation Operations
 */
abstract class TableMigration[T <: JdbcProfile#Table[_]](table: T)(implicit dialect: Dialect[_])
    extends SqlMigration with AstHelpers with Equals {
  outer =>
  /**
   * The concrete type of this `TableMigration` ([[ReversibleTableMigration]] or [[IrreversibleTableMigration]]).* Operations that are in of themselves reversible will return an instance of this type.
   */
  type Self <: TableMigration[T]

  def tableInfo = TableInfo(table.schemaName, table.tableName)

  def sql = dialect.migrateTable(tableInfo, data)

  protected[table] def data: TableMigrationData

  protected def withData(data: TableMigrationData): Self

  private def colInfo(f: T => Column[_]): ColumnInfo = {
    val col = f(table)
    colInfo(col)
  }

  private def colInfo(col: Column[_]) = fieldSym(col.toNode) match {
    case Some(c) =>
      table.tableProvider match {
        case driver: JdbcProfile => columnInfo(driver, c)
        case _                   => sys.error("Invalid table: " + table)
      }
    case None => sys.error("Invalid column: " + col)
  }

  /**
   * Create the table.
   * Note: drop + create is allowed.
   */
  def create = withData(data.copy(
    tableCreate = true
  ))

  /**
   * Drop the table.
   * Note: drop + create is allowed.
   */
  def drop = new IrreversibleTableMigration(
    table,
    tableInfo,
    data.copy(
      tableCreate = false,
      tableDrop = true
    )
  )

  /**
   * Rename the table
   * @param to the new name for the table
   * @group oper
   */
  def rename(to: String) = withData(data.copy(
    tableRename = Some(to)
  ))

  /**
   * Add columns to the table.
   * (If the table is being created, these may be incorporated into the `CREATE TABLE` statement.)
   * @param cols zero or more column-returning functions, which are passed the table object.
   * @example {{{ tblMig.addColumns(_.col1, _.col2, _.column[Int]("fieldNotYetInTableDef")) }}}
   */
  def addColumns(cols: (T => Column[_])*) = withData(data.copy(
    columnsCreate = data.columnsCreate ++
      cols.map(colInfo)
  ))

  /**
   * Add columns to the table.
   * (If the table is being created, these may be incorporated into the `CREATE TABLE` statement.)
   * @param cols zero or more columns, which are passed the table object.
   * @example
   *    {{{
   *         object table1 extends Table[(Int, Int, Int)]("table1") {
   *                         def col1 = column[Int]("col1")
   *                         def col2 = column[Int]("col2")
   *                         def col3 = column[Int]("col3")
   *                         def * = col1 ~ col2 ~ col3
   *                       }
   *         val columns = Seq(table1.col1, table1.col2, table1.col3)
   *         tblMig.addColumns(columns)
   *    }}}
   */
  def addColumns(cols: Iterable[Column[_]]) = withData(data.copy(
    columnsCreate = data.columnsCreate ++ cols.map(colInfo)
  ))

  /**
   * Add all columns to the table.
   * (If the table is being created, these may be incorporated into the `CREATE TABLE` statement.)
   * @example
   *    {{{
   *         object table1 extends Table[(Int, Int, Int)]("table1") {
   *                         def col1 = column[Int]("col1")
   *                         def col2 = column[Int]("col2")
   *                         def col3 = column[Int]("col3")
   *                         def * = col1 ~ col2 ~ col3
   *                       }
   *         tblMig.addColumns
   *    }}}
   */
  def addColumns = withData(data.copy(
    columnsCreate = data.columnsCreate ++ table.columns.map(colInfo)
  ))

  /**
   * Drop columns.
   * @param cols zero or more column-returning functions, which are passed the table object.
   * @example {{{ tblMig.dropColumns(_.col1, _.col2, _.column[Int]("oldFieldNotInTableDef")) }}}
   * @group oper
   */
  def dropColumns(cols: (T => Column[_])*) = withData(data.copy(
    columnsDrop = data.columnsDrop ++
      cols.map(colInfo)
  ))

  def dropColumns(cols: Iterable[Column[_]]) = withData(data.copy(
    columnsDrop = data.columnsDrop ++ cols.map(colInfo)
  ))

  private[migration] def dropColumn(col: String) = withData(data.copy(
    columnsDrop = data.columnsDrop ++ Seq(ColumnInfo(col))
  ))

  /**
   * Rename a column.
   * @param col a column-returning function, which is passed the table object.
   * @example {{{ tblMig.renameColumns(_.col1, "newName") }}}
   * @group oper
   */
  def renameColumn(col: T => Column[_], to: String) = withData(data.copy(
    columnsRename = data.columnsRename +
      (colInfo(col) -> to)
  ))

  /**
   * Changes the data type of columns based on the column definitions in `cols`
   * @param cols zero or more column-returning functions, which are passed the table object.
   * @example {{{ tblMig.alterColumnTypes(_.col1, _.column[NotTheTypeInTableDef]("col2")) }}}
   * @group oper
   */
  def alterColumnTypes(cols: (T => Column[_])*) = new IrreversibleTableMigration(
    table,
    tableInfo,
    data.copy(
      columnsAlterType = data.columnsAlterType ++
        cols.map(colInfo)
    )
  )

  /**
   * Changes the default value of columns based on the column definitions in `cols`
   * @param cols zero or more column-returning functions, which are passed the table object.
   * @example {{{ tblMig.alterColumnDefaults(_.col1, _.column[Int]("col2", O.Default("notTheDefaultInTableDef"))) }}}
   * @group oper
   */
  def alterColumnDefaults(cols: (T => Column[_])*) = new IrreversibleTableMigration(
    table,
    tableInfo,
    data.copy(
      columnsAlterDefault = data.columnsAlterDefault ++
        cols.map(colInfo)
    )
  )

  /**
   * Changes the nullability of columns based on the column definitions in `cols`
   * @param cols zero or more column-returning functions, which are passed the table object.
   * @example {{{ tblMig.alterColumnNulls(_.col1, _.column[Int]("col2", O.NotNull)) }}}
   * @group oper
   */
  def alterColumnNulls(cols: (T => Column[_])*) = new IrreversibleTableMigration(
    table,
    tableInfo,
    data.copy(
      columnsAlterNullability = data.columnsAlterNullability ++
        cols.map(colInfo)
    )
  )

  /**
   * Adds primary key constraints.
   * @param pks zero or more `PrimaryKey`-returning functions, which are passed the table object.
   * @example {{{ tblMig.addPrimaryKeys(_.pkDef) }}}
   */
  def addPrimaryKeys(pks: (T => PrimaryKey)*) = withData(data.copy(
    primaryKeysCreate = data.primaryKeysCreate ++
      pks.map { f =>
        val key = f(table)
        (key.name, key.columns flatMap (fieldSym(_)))
      }
  ))

  /**
   * Adds primary key constraints.
   * @param pks zero or more `PrimaryKey`es, which are passed the table object.
   * @example
   *    {{{
   *         object table1 extends Table[(Int, Int, Int)]("table1") {
   *                         def col1 = column[Int]("col1")
   *                         def col2 = column[Int]("col2")
   *                         def col3 = column[Int]("col3")
   *                         def col4 = column[Int]("col4")
   *                         def col5 = column[Int]("col5")
   *
   *                         def pk1 = primaryKey("pk1", (col1, col2))
   *                         def pk2 = primaryKey("p21", (col3, col4, col5))
   *
   *                         def * = col1 ~ col2 ~ col3 ~ col4 ~ col5
   *                       }
   *         val pks = Seq(table1.pk1, table1.pk2)
   *         tblMig.addPrimaryKeys(pks)
   *    }}}
   */
  def addPrimaryKeys(pks: Iterable[PrimaryKey]) = withData(data.copy(
    primaryKeysCreate = data.primaryKeysCreate ++ pks.map { key => (key.name, key.columns flatMap (fieldSym(_))) }
  ))

  /**
   * Drops primary key constraints.
   * @param pks zero or more `PrimaryKey`-returning functions, which are passed the table object.
   * @example {{{ tblMig.dropPrimaryKeys(_.pkDef) }}}
   * @group oper
   */
  def dropPrimaryKeys(pks: (T => PrimaryKey)*) = withData(data.copy(
    primaryKeysDrop = data.primaryKeysDrop ++
      pks.map { f =>
        val key = f(table)
        (key.name, key.columns flatMap (fieldSym(_)))
      }
  ))

  def dropPrimaryKeys(pks: Iterable[PrimaryKey]) = withData(data.copy(
    primaryKeysDrop = data.primaryKeysDrop ++ pks.map { key => (key.name, key.columns flatMap (fieldSym(_))) }
  ))

  /**
   * Adds foreign key constraints.
   * @param fkqs zero or more `ForeignKeyQuery`-returning functions, which are passed the table object.
   * @example {{{ tblMig.addForeignKeys(_.fkDef) }}}
   */
  def addForeignKeys(fkqs: (T => ForeignKeyQuery[_ <: AbstractTable[_], _])*) = withData(data.copy(
    foreignKeysCreate = data.foreignKeysCreate ++
      fkqs.flatMap { f =>
        val fkq = f(table)
        fkq.fks: Seq[ForeignKey]
      }
  ))

  /**
   * Adds foreign key constraints.
   * @param fkqs zero or more foreign keys, which are passed the table object.
   * @example
   *    {{{
   *         object table1 extends Table[(Int, Int, Int)]("table1") {
   *                         def col1 = column[Int]("col1")
   *                         def col2 = column[Int]("col2")
   *
   *                         def fkq1 = foreignKey("fkq1", col1, ... )
   *                         def fkq2 = foreignKey("fkq2", col2, ... )
   *
   *                         def * = col1 ~ col2
   *                       }
   *         val fkqs = Seq(table1.fkq1, table1.fkq2)
   *         tblMig.addForeignKeys(fkqs)
   *    }}}
   */
  def addForeignKeys(fkqs: Iterable[ForeignKey]) = withData(data.copy(
    foreignKeysCreate = data.foreignKeysCreate ++ fkqs
  ))

  /**
   * Drops foreign key constraints.
   * @param fkqs zero or more `ForeignKeyQuery`-returning functions, which are passed the table object.
   * @example {{{ tblMig.dropForeignKeys(_.fkDef) }}}
   * @group oper
   */
  def dropForeignKeys(fkqs: (T => ForeignKeyQuery[_ <: AbstractTable[_], _])*) = withData(data.copy(
    foreignKeysDrop = data.foreignKeysDrop ++
      fkqs.flatMap { f =>
        val fkq = f(table)
        fkq.fks: Seq[ForeignKey]
      }
  ))

  /**
   * Drops foreign key constraints.
   * @param fkqs zero or more `ForeignKeyQuery`-returning functions, which are passed the table object.
   * @example {{{ tblMig.dropForeignKeys(_.fkDef) }}}
   * @group oper
   */
  def dropForeignKeys(fkqs: Iterable[ForeignKey]) = withData(data.copy(
    foreignKeysDrop = data.foreignKeysDrop ++ fkqs
  ))

  /**
   * Adds indexes
   * @param indexes zero or more `Index`-returning functions, which are passed the table object.
   * @example {{{ tblMig.addIndexes(_.idxDef) }}}
   */
  def addIndexes(indexes: (T => Index)*) = withData(data.copy(
    indexesCreate = data.indexesCreate ++
      indexes.map { f =>
        val i = f(table)
        indexInfo(i)
      }
  ))

  /**
   * Adds indexes
   * @param indexes zero or more indexes, which are passed the table object.
   * @example
   *    {{{
   *         object table1 extends Table[(Int, Int, Int)]("table1") {
   *                         def col1 = column[Int]("col1")
   *                         def col2 = column[Int]("col2")
   *
   *                         def idxDef1 = index("idxDef1", col1)
   *                         def idxDef2 = index("idxDef2", col2)
   *
   *                         def * = col1 ~ col2
   *                       }
   *         val indexes = Seq(table1.idxDef1, table1.idxDef2)
   *         tblMig.addIndexes(indexes)
   *    }}}
   */
  def addIndexes(indexes: Iterable[Index]) = withData(data.copy(
    indexesCreate = data.indexesCreate ++ indexes.map(indexInfo)
  ))

  /**
   * Drops indexes
   * @param indexes zero or more `Index`-returning functions, which are passed the table object.
   * @example {{{ tblMig.dropIndexes(_.idxDef) }}}
   * @group oper
   */
  def dropIndexes(indexes: (T => Index)*) = withData(data.copy(
    indexesDrop = data.indexesDrop ++
      indexes.map { f =>
        val i = f(table)
        indexInfo(i)
      }
  ))

  def dropIndexes(indexes: Iterable[Index]) = withData(data.copy(
    indexesDrop = data.indexesDrop ++ indexes.map(indexInfo)
  ))

  /**
   * Renames an index
   * @param index an `Index`-returning function, which is passed the table object.
   * @example {{{ tblMig.renameIndex(_.idxDef, "newName") }}}
   * @group oper
   */
  def renameIndex(index: (T => Index), to: String) = withData(data.copy(
    indexesRename = data.indexesRename +
      (indexInfo(index(table)) -> to)
  ))

  def canEqual(that: Any) = that.isInstanceOf[TableMigration[_]]

  override def equals(a: Any) = a match {
    case that: TableMigration[_] if that canEqual this =>
      (that.tableInfo, that.data) == (this.tableInfo, this.data)
    case _ => false
  }
}