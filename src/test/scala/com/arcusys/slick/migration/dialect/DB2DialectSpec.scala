package com.arcusys.slick.migration.dialect

import scala.slick.driver.{JdbcProfile, JdbcDriver}
import scala.slick.jdbc.meta.MTable
import scala.slick.jdbc.GetResult._
import scala.slick.jdbc.{ StaticQuery => Q }
import Q.interpolation

import org.scalatest.{ FlatSpec, BeforeAndAfterAll }
import org.scalatest.Matchers._

import com.arcusys.slick.migration.table.TableMigration
import com.arcusys.slick.drivers.DB2Driver

class DB2DialectSpec extends FlatSpec with BeforeAndAfterAll {
  val db2 = new Db2Init("db2", DB2Driver)
  implicit val dialect: Dialect[_] = Dialect(DB2Driver).get

  import DB2DialectSpec._
  import db2.driver.simple._

  override def afterAll() {
    db2.conn withSession { implicit s =>
      migrations foreach (_.drop.apply)
    }
    clearMigrations()
  }

  behavior of "IBM DB/2 dialect for slick's migration"

  it should "create a new table" in {
    val table = "test_create_table"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text")
      def * = (id, text)
    }
    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    db2.findTable(table).isDefined should be(true)
  }

  it should "drop an existing table" in {
    val table = "test_drop_table"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text")
      def * = (id, text)
    }
    val migration = TableMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    db2.findTable(table) should not be (None)

    db2.conn withSession { implicit s =>
      migration.drop.apply
    }
    db2.findTable(table) should be(None)
  }

  ignore should "rename an existing table" in {
    val table = "test_rename_table"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text")
      def * = (id, text)
    }
    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    db2.findTable(table).isDefined should be(true)

    db2.conn withSession { implicit s =>
      migration.rename("test_renamed_table").apply
    }
    val tables = db2.getTables.map(_.name.name)
    tables should contain("test_renamed_table")
    tables should not contain (table)

    // to clean it up later it should be renamed back to original name
    db2.conn withSession { implicit s =>
      migration.rename(table).apply
    }
  }

  it should "create primary key" in {
    val table = "test_create_pk"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id", O.PrimaryKey)
      val text = column[String]("text")
      def * = (id, text)
    }
    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }

    val pks = db2.getPrimaryKeys(table)
    pks should not be empty
    pks.map(_.column) should contain("id")
  }

  it should "drop primary key" in {
    val table = "test_drop_pk"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text")
      def * = (id, text)
      def pk = primaryKey(table + "_pk", id)
    }
    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).addPrimaryKeys(_.pk).apply
    }
    db2.getPrimaryKeys(table) map (_.column) should contain("id")

    db2.conn withSession { implicit s =>
      migration.dropPrimaryKeys(_.pk).apply
    }
    db2.getPrimaryKeys(table) map (_.column) should not contain ("id")
  }

  it should "add foreign keys" in {
    val refTable = "test_add_foreign_keys1"
    val fkTable = "test_add_foreign_keys2"

    class TestTable1(t: Tag) extends Table[Long](t, refTable) {
      val id = column[Long]("id_num", O.PrimaryKey)
      def * = id
    }

    class TestTable2(t: Tag) extends Table[(Int, Long)](t, fkTable) {
      val id = column[Int]("id", O.PrimaryKey)
      val num = column[Long]("num")
      def * = (id, num)
      def fk = foreignKey(fkTable + "_fk", num, TableQuery[TestTable1])(_.id)
    }
    val m2 = initMigration(TableQuery[TestTable2])
    val m1 = initMigration(TableQuery[TestTable1])

    db2.conn withSession { implicit s =>
      m1.create.addColumns(_.id).apply()
      m2.create.addColumns(_.id, _.num).addForeignKeys(_.fk).apply
    }

    val fks = db2.getForeignKeys(fkTable)
    fks.head.pkTable.name should be(refTable)
    fks.head.fkTable.name should be(fkTable)
    fks.head.fkColumn should be("num")
  }

  it should "drop foreign keys" in {
    val refTable = "test_drop_foreign_keys1"
    val fkTable = "test_drop_foreign_keys2"

    class TestTable1(t: Tag) extends Table[Long](t, refTable) {
      val id = column[Long]("id_num", O.PrimaryKey)
      def * = id
    }

    class TestTable2(t: Tag) extends Table[(Int, Long)](t, fkTable) {
      val id = column[Int]("id", O.PrimaryKey)
      val num = column[Long]("num")
      def * = (id, num)
      def fk = foreignKey(fkTable + "_fk", num, TableQuery[TestTable1])(_.id)
    }
    val m2 = initMigration(TableQuery[TestTable2])
    val m1 = initMigration(TableQuery[TestTable1])

    db2.conn withSession { implicit s =>
      m1.create.addColumns(_.id).apply()
      m2.create.addColumns(_.id, _.num).addForeignKeys(_.fk).apply
    }
    db2.getForeignKeys(fkTable).map(_.fkColumn) should contain("num")

    db2.conn withSession { implicit s =>
      m2.dropForeignKeys(_.fk).apply()
    }
    db2.getForeignKeys(fkTable).map(_.fkColumn) should not contain ("id")
  }

  it should "create new indexes" in {
    val table = "test_create_index_table"
    val indexName = "test_create_idx"

    class TestTable(t: Tag) extends Table[(Int, Long, String)](t, table) {
      val id = column[Int]("id", O.PrimaryKey)
      val num = column[Long]("number")
      val text = column[String]("text")
      def * = (id, num, text)
      def idx = index(indexName, (id, num))
    }

    lazy val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }

    val idxs = db2.getIndexes(table)
    idxs.map(_.indexName) should contain(Some(indexName))
  }

  it should "be able to create unique index" in {
    val table = "test_create_un_index_table"
    val indexName = "test_create_un_idx"

    class TestTable(t: Tag) extends Table[(Int, Long, String)](t, table) {
      val id = column[Int]("id", O.PrimaryKey)
      val num = column[Long]("number")
      val text = column[String]("text")
      def * = (id, num, text)
      def idx = index(indexName, (id, num), unique = true)
    }

    lazy val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }

    val idxs = db2.getIndexes(table)
    idxs.tail.head.indexName should be(Some(indexName))
    idxs.tail.head.nonUnique should be(false)
  }

  it should "rename existing indexes" in {
    val table = "test_rename_index_table"
    val indexName = "test_rename_idx"

    class TestTable(t: Tag) extends Table[(Int, Long, String)](t, table) {
      val id = column[Int]("id", O.PrimaryKey)
      val num = column[Long]("number")
      val text = column[String]("text")
      def * = (id, num, text)
      def idx = index(indexName, (id, num))
    }

    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }
    db2.getIndexes(table).map(_.indexName) should contain(Some(indexName))

    db2.conn withSession { implicit s =>
      migration.renameIndex(_.idx, "test_renamed_idx").apply()
    }
    db2.getIndexes(table).map(_.indexName) should not contain (Some(indexName))
    db2.getIndexes(table).map(_.indexName) should contain(Some("test_renamed_idx"))
  }

  it should "drop existing indexes" in {
    val table = "test_drop_index_table"
    val indexName = "test_drop_idx"

    class TestTable(t: Tag) extends Table[(Int, Long, String)](t, table) {
      val id = column[Int]("id", O.PrimaryKey)
      val num = column[Long]("number")
      val text = column[String]("text")
      def * = (id, num, text)
      def idx = index(indexName, (id, num))
    }

    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }
    db2.getIndexes(table).map(_.indexName) should contain(Some(indexName))

    db2.conn withSession { implicit s =>
      migration.dropIndexes(_.idx).apply
    }
    db2.getIndexes(table).map(_.indexName) should not contain (Some(indexName))
  }

  it should "add a new column into existing table as nullable if it is marked as 'not null' and default value is not provieded" in {
    val table = "test_add_column_table"

    class TestTable(t: Tag) extends Table[(Int, Long)](t, table) {
      val id = column[Int]("id")
      val num = column[Long]("number")
      def * = (id, num)
    }

    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    val curr = db2.getColumns(table)
    curr should have size 2
    curr.map(_.name) should contain only ("id", "number")

    db2.conn withSession { implicit s =>
      migration.addColumns(_.column[String]("text")).apply
    }
    val altered = db2.getColumns(table)
    altered should have size 3
    altered.map(_.name) should contain only ("id", "number", "text")
    altered.last.nullable should be(Some(true))
  }

  it should "add a new column into existing table with default value" in {
    val table = "test_add_column_with_default_table"

    class TestTable(t: Tag) extends Table[(Int, Long)](t, table) {
      val id = column[Int]("id")
      val num = column[Long]("number")
      def * = (id, num)
    }

    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    val curr = db2.getColumns(table)
    curr should have size 2
    curr.map(_.name) should contain only ("id", "number")

    db2.conn withSession { implicit s =>
      val O = db2.driver.columnOptions
      migration.addColumns(_.column[String]("text", O.Default("testing"))).apply
    }
    val altered = db2.getColumns(table)
    altered should have size 3
    altered.map(_.name) should contain only ("id", "number", "text")
    altered.last.nullable should be(Some(false))
    altered.last.columnDef should be(Some("'testing'"))
  }

  it should "drop existing column in a table" in {
    val table = "test_drop_column_table"

    class TestTable(t: Tag) extends Table[(Int, Long)](t, table) {
      val id = column[Int]("id")
      val num = column[Long]("number")
      def * = (id, num)
    }

    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    db2.getColumns(table) should have size 2

    db2.conn withSession { implicit s => migration.dropColumns(_.num).apply }
    val res = db2.getColumns(table)
    res should have size 1
    res.map(_.name) should contain only ("id")

  }

  it should "rename existing column" in {
    val table = "test_rename_column"

    class TestTable(t: Tag) extends Table[(Int, Long)](t, table) {
      val id = column[Int]("id")
      val num = column[Long]("number")
      def * = (id, num)
    }

    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    db2.getColumns(table).map(_.name) should contain only ("id", "number")

    db2.conn withSession { implicit s =>
      migration.renameColumn(_.num, "just_number").apply
    }
    db2.getColumns(table).map(_.name) should contain only ("id", "just_number")
  }

  it should "alter column type of existing column" in {
    val table = "test_alter_column_type"

    class TestTable(t: Tag) extends Table[(Int, Long)](t, table) {
      val id = column[Int]("id")
      val num = column[Long]("number")
      def * = (id, num)
    }
    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }

    val curr = db2.getColumns(table, "number").headOption
    curr.isDefined should be(true)
    curr.get.typeName should be("BIGINT")

    db2.conn withSession { implicit s =>
      migration.alterColumnTypes(_.column[String]("number")).apply
    }
    val res = db2.getColumns(table, "number").headOption
    res.isDefined should be(true)
    res.get.typeName should be("VARCHAR")
  }

  it should "alter default value for an existing column" in {
    val table = "test_alter_column_default_value"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text", O.Default("testing_default"))
      def * = (id, text)
    }
    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    val curr = db2.getColumns(table, "text").headOption
    curr.isDefined should be(true)
    curr.get.columnDef should be(Some("'testing_default'"))

    db2.conn withSession { implicit s =>
      val O = db2.driver.columnOptions
      migration.alterColumnDefaults(_.column[String]("text", O.Default("new_default"))).apply
    }
    val res = db2.getColumns(table, "text").headOption
    res.isDefined should be(true)
    res.get.columnDef should be(Some("'new_default'"))
  }

  it should "alter nullable column to 'not null' with default value provided" in {
    val table = "test_alter_column_nullability"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text", O.Nullable)
      def * = (id, text)
    }
    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    val curr = db2.getColumns(table, "text").headOption
    curr.isDefined should be(true)
    curr.get.isNullable should be(Some(true))

    db2.conn withSession { implicit s =>
      val O = db2.driver.columnOptions
      migration.alterColumnNulls(_.column[String]("text", O.NotNull, O.Default("testing"))).apply
    }
    val res = db2.getColumns(table, "text").headOption
    res.isDefined should be(true)
    res.get.isNullable should be(Some(false))
  }

  it should "alter a 'not null' column to nullable one" in {
    val table = "test_alter_column_nullability_2"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text", O.Default("testing"))
      def * = (id, text)
    }
    val migration = initMigration(TableQuery[TestTable])

    db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    val curr = db2.getColumns(table, "text").headOption
    curr.isDefined should be(true)
    curr.get.isNullable should be(Some(false))
    curr.get.columnDef should be(Some("'testing'"))

    db2.conn withSession { implicit s =>
      val O = db2.driver.columnOptions
      migration.alterColumnNulls(_.column[String]("text", O.Nullable)).apply
    }
    val res = db2.getColumns(table, "text").headOption
    res.isDefined should be(true)
    res.get.isNullable should be(Some(true))
  }

  it should "create table with identity column" in {
    val table = "test_identity_column_table"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      val text = column[String]("text")
      def * = (id, text)
    }
    val testTable = TableQuery[TestTable]
    val migration = initMigration(testTable)

    val result = db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
      testTable ++= Seq((0, "first"), (0, "second"))
      testTable.list
    }

    result should have size 2
    result.head should be((1, "first"))
    result.tail.head should be((2, "second"))
  }

  it should "allow to drop identity column" in {
    val table = "test_identity_drop_table"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id", O.AutoInc)
      val text = column[String]("text")
      def * = (id, text)
    }
    val testTable = TableQuery[TestTable]
    val migration = initMigration(testTable)

    val result = db2.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
      testTable ++= Seq((0, "first"), (0, "second"))
      testTable.list
    }

    result should have size 2
    result.head should be((1, "first"))
    result.tail.head should be((2, "second"))

    db2.conn withSession { implicit s =>
      migration.dropColumns(_.id).apply
    }
    db2.getColumns(table, "id") should have size 0
  }

}

object DB2DialectSpec {
  import scala.slick.lifted.TableQuery

  private[this] val ms = new collection.mutable.ListBuffer[TableMigration[_]]()

  def initMigration[T <: JdbcProfile#Table[_]](tableQuery: TableQuery[T])(implicit d: Dialect[_]) = {
    val m = TableMigration(tableQuery)
    migrations += m
    m
  }

  def migrations = ms
  def clearMigrations() = ms.clear()
}

class Db2Init(confName: String, driver: JdbcProfile) extends DbInit(confName, driver) {
  override def getTables =
    conn withSession { implicit s =>
      MTable.getTables(None, None, None, Some(Seq("TABLE")))
        .list.filter(_.name.schema.filter(_ == "SYSTOOLS").isEmpty)
    }
}