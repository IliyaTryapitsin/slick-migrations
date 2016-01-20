package slick

import slick.migration.ast.{IndexInfo, TableInfo}
import slick.migration.dialect.Dialect
import slick.migration.table.TableMigration

import scala.slick.ast.FieldSymbol
import scala.slick.driver.JdbcProfile
import scala.slick.lifted._

/**
 *  Created by Iliya Tryapitsin on 16.04.15.
 */
package object migration {
  implicit class MigrationExtension[T <: JdbcProfile#Table[_]](table: T) {

    def alterColumnType(cols: (T => Column[_])*)(implicit dialect: Dialect[_]) = TableMigration(table).alterColumnTypes(cols: _*)

    /**
     * Add columns to the table.
     * (If the table is being created, these may be incorporated into the `CREATE TABLE` statement.)
     * @param cols zero or more column-returning functions, which are passed the table object.
     * @example {{{ tblMig.addColumns(_.col1, _.col2, _.column[Int]("fieldNotYetInTableDef")) }}}
     */
    def addColumns(cols: (T => Column[_])*)(implicit dialect: Dialect[_]) = TableMigration(table).addColumns(cols: _*)

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
     *         val columns = Seq(table1.col, table1.col2, table1.col3)
     *         tblMig.addColumns(columns)
     *    }}}
     */
    def addColumns(cols: Iterable[Column[_]])(implicit dialect: Dialect[_]) = TableMigration(table).addColumns(cols)

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
    def addColumns(implicit dialect: Dialect[_]) = TableMigration(table).addColumns(table.columns)

    def dropColumns(cols: Iterable[Column[_]])(implicit dialect: Dialect[_]) = TableMigration(table).dropColumns(cols)

    def dropColumns(cols: (T => Column[_])*)(implicit dialect: Dialect[_]) = TableMigration(table).dropColumns(cols: _*)

    def dropColumns(implicit dialect: Dialect[_]) = TableMigration(table).dropColumns(table.columns)

    def dropColumn(columnName: String)(implicit dialect: Dialect[_]) = TableMigration(table).dropColumn(columnName)

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
     *         tblMig.addIndexes(_.idxDef1, _.idxDef2)
     *    }}}
     */
    def addIndexes(indexes: (T => Index)*)(implicit dialect: Dialect[_]) = TableMigration(table).addIndexes(indexes: _*)

    /**
     * Add indexes
     * @param indexes Indexes, which are passed the table object. If list is empty then apply all indexes in table
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
    def addIndexes(indexes: Iterable[Index])(implicit dialect: Dialect[_]) =
      if (indexes.isEmpty) TableMigration(table).addIndexes(table.indexes)
      else TableMigration(table).addIndexes(indexes)

    /**
     * Add all indexes in table
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
     *         tblMig.addIndexes
     *    }}}
     */
    def addIndexes(implicit dialect: Dialect[_]) = TableMigration(table).addIndexes(table.indexes)

    def dropIndexes(indexes: Iterable[Index])(implicit dialect: Dialect[_]) = TableMigration(table).dropIndexes(indexes)

    def dropIndexes(indexes: (T => Index)*)(implicit dialect: Dialect[_]) = TableMigration(table).dropIndexes(indexes: _*)

    def dropIndexes(implicit dialect: Dialect[_]) = TableMigration(table).dropIndexes(table.indexes)

    /**
     * Adds primary key constraints.
     * @param pks zero or more `PrimaryKey`-returning functions, which are passed the table object.
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
    def addPrimaryKeys(pks: (T => PrimaryKey)*)(implicit dialect: Dialect[_]) = TableMigration(table).addPrimaryKeys(pks: _*)

    /**
     * Add primary keys
     * @param pks Primary keys, which are passed the table object. If list is empty then apply all primary keys in table
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
    def addPrimaryKeys(pks: Iterable[PrimaryKey])(implicit dialect: Dialect[_]) = TableMigration(table).addPrimaryKeys(pks)

    /**
     * Add all primary keys
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
     *         tblMig.addPrimaryKeys
     *    }}}
     */
    def addPrimaryKeys(implicit dialect: Dialect[_]) = TableMigration(table).addPrimaryKeys(table.primaryKeys)

    def dropPrimaryKeys(implicit dialect: Dialect[_]) = TableMigration(table).dropPrimaryKeys(table.primaryKeys)

    def dropPrimaryKeys(pks: (T => PrimaryKey)*)(implicit dialect: Dialect[_]) = TableMigration(table).dropPrimaryKeys(pks: _*)

    def dropPrimaryKeys(pks: Iterable[PrimaryKey])(implicit dialect: Dialect[_]) = TableMigration(table).dropPrimaryKeys(pks)

    /**
     * Adds foreign key constraints.
     * @param fkqs zero or more `ForeignKeyQuery`es, which are passed the table object.
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
    def addForeignKeys(fkqs: (T => ForeignKeyQuery[_ <: AbstractTable[_], _])*)(implicit dialect: Dialect[_]) = TableMigration(table).addForeignKeys(fkqs: _*)

    /**
     * Adds foreign key constraints.
     * @param fkqs foreign keys, which are passed the table object. If list is empty then apply all primary keys in table
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
    def addForeignKeys(fkqs: Iterable[ForeignKey])(implicit dialect: Dialect[_]) = TableMigration(table).addForeignKeys(fkqs)

    /**
     * Adds foreign key constraints.
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
     *         tblMig.addForeignKeys
     *    }}}
     */
    def addForeignKeys(implicit dialect: Dialect[_]) = TableMigration(table).addForeignKeys(table.foreignKeys)

    def dropForeignKeys(fkqs: (T => ForeignKeyQuery[_ <: AbstractTable[_], _])*)(implicit dialect: Dialect[_]) = TableMigration(table).dropForeignKeys(fkqs: _*)

    def dropForeignKeys(fkqs: Iterable[ForeignKey])(implicit dialect: Dialect[_]) = TableMigration(table).dropForeignKeys(fkqs)

    def dropForeignKeys(implicit dialect: Dialect[_]) = TableMigration(table).dropForeignKeys(table.foreignKeys)

    /**
     * Drop the table.
     * Note: drop + create is allowed.
     */
    def drop(implicit dialect: Dialect[_]) = TableMigration(table).drop

    /**
     * Create the table.
     * Note: drop + create is allowed.
     */
    def create(implicit dialect: Dialect[_]) = TableMigration(table).create

    def columns = table.getClass.getMethods
      .filter { m => m.getReturnType == classOf[Column[_]] && m.getParameterTypes.length == 0 }
      .map { m => m.invoke(table).asInstanceOf[Column[_]] } ++
      table.getClass.getFields
      .filter { f => f.getType == classOf[Column[_]] }
      .map { f => f.asInstanceOf[Column[_]] }

    def insert(values: ((T) => Column[_], Any)*)(implicit dialect: Dialect[_]) = InsertMigration(table, values.map { p => (p._1.apply(table), p._2) }.toMap)
  }

  def dropTable(name: String,
    schema: Option[String] = None)(implicit dialect: Dialect[_]) = SqlMigration(dialect.dropTable(TableInfo(schema, name)))

  def dropForeignKey(foreignKeyName: String,
    tableName: String,
    schema: Option[String] = None)(implicit dialect: Dialect[_]) = SqlMigration(dialect.dropForeignKey(TableInfo(schema, tableName), foreignKeyName))

  def dropIndex(indexName: String,
    tableName: String,
    schema: Option[String] = None,
    unique: Boolean = false,
    columns: Seq[String] = Seq())(implicit dialect: Dialect[_]) = {
    val cols: Seq[FieldSymbol] = columns.map { x => FieldSymbol(x)(Seq(), null) }
    val indexInfo = IndexInfo(TableInfo(schema, tableName), indexName, unique, cols)
    SqlMigration(dialect.dropIndex(indexInfo))
  }

  def dropColumn(name: String,
    tableName: String,
    schema: Option[String] = None)(implicit dialect: Dialect[_]) = {
    SqlMigration(dialect.dropColumn(TableInfo(schema, tableName), name))
  }

  def dropPrimaryKey(name: String,
    tableName: String,
    schema: Option[String] = None)(implicit dialect: Dialect[_]) = {
    SqlMigration(dialect.dropPrimaryKey(TableInfo(schema, tableName), name))
  }

  implicit class ColumnExtensions(column: Column[_]) {
    def fieldSymbol(implicit dialect: Dialect[_]) = dialect.fieldSym(column)
  }

  implicit class AnyExtension[T](val obj: T) extends AnyVal {
    def toOption = Some(obj)
  }
}
