package slick.migration.dialect

import scala.slick.ast.ColumnOption
import scala.slick.driver.{JdbcProfile, JdbcDriver}
import scala.slick.jdbc.meta.MTable
import scala.slick.jdbc.GetResult._
import scala.slick.jdbc.{ StaticQuery => Q }
import Q.interpolation

import org.scalatest.{ FlatSpec, BeforeAndAfterAll }
import org.scalatest.Matchers._

import slick.migration.table.TableMigration
import com.arcusys.slick.drivers.SybaseDriver

class SybaseAnywhereDialectSpec extends FlatSpec with BeforeAndAfterAll {
  val sybase = new SybaseInit
  implicit val dialect: Dialect[_] = Dialect(SybaseDriver).get

  import sybase.driver.simple._

  override def afterAll() { sybase.cleanUpAfter() }

  behavior of "Sybase SQL Anywhere dialect for slick's migration"


  it should "create a new table" in new TableTesting {
    val table = "test_create_table"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    sybase.findTable(table).isDefined should be (true)
  }

  it should "drop an existing table" in new TableTesting {
    val table = "test_drop_table"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    sybase.findTable(table) should not be (None)

    sybase.conn withSession { implicit s =>
      migration.drop.apply
    }
    sybase.findTable(table) should be (None)
  }

  it should "rename an existing table" in new TableTesting {
    val table = "test_rename_table"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    sybase.findTable(table).isDefined should be(true)

    sybase.conn withSession { implicit s =>
      migration.rename("test_renamed_table").apply
    }
    val tables = sybase.getTables.map(_.name.name)
    tables should contain("test_renamed_table")
    tables should not contain (table)
  }


  it should "create primary key" in new PrimaryKeyTesting {
    val table = "test_create_pk"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).addPrimaryKeys(_.pk).apply
    }

    val pks = sybase.getPrimaryKeys(table)
    pks should not be empty
    pks.map(_.column) should contain("id")
  }

  it should "drop primary key" in new PrimaryKeyTesting {
    val table = "test_drop_primary_key"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).addPrimaryKeys(_.pk).apply
    }
    sybase.getPrimaryKeys(table) map (_.column) should contain("id")

    sybase.conn withSession { implicit s =>
      migration.dropPrimaryKeys(_.pk).apply
    }
    sybase.getPrimaryKeys(table) map (_.column) should not contain ("id")
  }


  it should "add foreign keys" in new ForeignKeyTesting {
    val pkTable = "test_add_foreign_keys1"
    val fkTable = "test_add_foreign_keys2"

    sybase.conn withSession { implicit s =>
      m1.create.addColumns(_.id).apply()
      m2.create.addColumns(_.id, _.num).addForeignKeys(_.fk).apply
    }

    val fks = sybase.getForeignKeys(fkTable)
    fks.head.pkTable.name should be(pkTable)
    fks.head.fkTable.name should be(fkTable)
    fks.head.fkColumn should be("num")
  }

  it should "drop foreign keys" in new ForeignKeyTesting {
    val pkTable = "test_drop_foreign_keys1"
    val fkTable = "test_drop_foreign_keys2"

    sybase.conn withSession { implicit s =>
      m1.create.addColumns(_.id).apply()
      m2.create.addColumns(_.id, _.num).addForeignKeys(_.fk).apply
    }
    sybase.getForeignKeys(fkTable).map(_.fkColumn) should contain("num")

    sybase.conn withSession { implicit s =>
      m2.dropForeignKeys(_.fk).apply()
    }
    sybase.getForeignKeys(fkTable).map(_.fkColumn) should not contain ("id")
  }


  it should "create new indexes" in new IndexTesting {
    val table = "test_create_index_table"
    val indexName = "test_create_idx"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }

    val idxs = sybase.getIndexes(table)
    idxs.map(_.indexName) should contain(Some(indexName))
  }

  it should "be able to create unique index" in new IndexTesting {
    val table = "test_create_un_index_table"
    val indexName = "test_create_un_idx"
    override val unique = true

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }

    val idxs = sybase.getIndexes(table)
    idxs.tail.head.indexName should be(Some(indexName))
    idxs.tail.head.nonUnique should be(false)
  }

  it should "rename existing indexes" in new IndexTesting {
    val table = "test_rename_index_table"
    val indexName = "test_rename_idx"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }
    sybase.getIndexes(table).map(_.indexName) should contain(Some(indexName))

    sybase.conn withSession { implicit s =>
      migration.renameIndex(_.idx, "test_renamed_idx").apply()
    }
    sybase.getIndexes(table).map(_.indexName) should not contain (Some(indexName))
    sybase.getIndexes(table).map(_.indexName) should contain(Some("test_renamed_idx"))
  }

  it should "drop existing indexes" in new IndexTesting {
    val table = "test_drop_index_table"
    val indexName = "test_drop_idx"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }
    sybase.getIndexes(table).map(_.indexName) should contain(Some(indexName))

    sybase.conn withSession { implicit s =>
      migration.dropIndexes(_.idx).apply
    }
    sybase.getIndexes(table).map(_.indexName) should not contain (Some(indexName))
  }


  it should "add a new column into existing table with default value" in new ColumnTesting {
    val table = "test_add_column_table"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    val curr = sybase.getColumns(table)
    curr should have size 2
    curr.map(_.name) should contain only ("id", "number")

    sybase.conn withSession { implicit s =>
      val O = sybase.driver.columnOptions
      migration.addColumns(_.column[String]("text", O.Default("testing"))).apply
    }
    val altered = sybase.getColumns(table)
    altered should have size 3
    altered.map(_.name) should contain only ("id", "number", "text")
    altered.last.nullable should be(Some(false))
    altered.last.columnDef should be(Some("'testing'"))
  }

  it should "drop existing column in a table" in new ColumnTesting {
    val table = "test_drop_column_table"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    sybase.getColumns(table) should have size 2

    sybase.conn withSession { implicit s => migration.dropColumns(_.num).apply }
    val res = sybase.getColumns(table)
    res should have size 1
    res.map(_.name) should contain only ("id")

  }

  it should "rename existing column" in new ColumnTesting {
    val table = "test_rename_column"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    sybase.getColumns(table).map(_.name) should contain only ("id", "number")

    sybase.conn withSession { implicit s =>
      migration.renameColumn(_.num, "just_number").apply
    }
    sybase.getColumns(table).map(_.name) should contain only ("id", "just_number")
  }

  it should "alter column type of existing column" in new ColumnTesting {
    val table = "test_alter_column_type_table"

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }

    val curr = sybase.getColumns(table, "number").headOption
    curr.isDefined should be(true)
    curr.get.typeName should be("bigint")

    sybase.conn withSession { implicit s =>
      migration.alterColumnTypes(_.column[String]("number")).apply
    }
    val res = sybase.getColumns(table, "number").headOption
    res.isDefined should be(true)
    res.get.typeName should be("varchar")
  }

  it should "alter default value for an existing column" in new ColumnTesting {
    val table = "test_alter_column_default_value"
    val columnName = "number"
    override val default = true

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    val curr = sybase.getColumns(table, columnName).headOption
    curr.isDefined should be(true)
    curr.get.columnDef should be(Some("111"))

    sybase.conn withSession { implicit s =>
      val O = sybase.driver.columnOptions
      migration.alterColumnDefaults(_.column[Long](columnName, O.Default(222L))).apply
    }
    val res = sybase.getColumns(table, columnName).headOption
    res.isDefined should be(true)
    res.get.columnDef should be(Some("222"))
  }

  it should "alter nullable column to 'not null' with default value provided" in new ColumnTesting {
    val table = "test_alter_column_nullability"
    val columnName = "number"
    override val nullable = true

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    val curr = sybase.getColumns(table, columnName).headOption
    curr.isDefined should be(true)
    curr.get.isNullable should be(Some(true))

    sybase.conn withSession { implicit s =>
      val O = sybase.driver.columnOptions
      migration.alterColumnNulls(_.column[Long](columnName, O.NotNull, O.Default(222L))).apply
    }
    val res = sybase.getColumns(table, columnName).headOption
    res.isDefined should be(true)
    res.get.isNullable should be(Some(false))
  }

  it should "alter a 'not null' column to nullable one" in new ColumnTesting {
    val table = "test_alter_column_nullability_2"
    val columnName = "number"
    override val default = true

    sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    val curr = sybase.getColumns(table, columnName).headOption
    curr.isDefined should be(true)
    curr.get.isNullable should be(Some(false))
    curr.get.columnDef should be(Some("111"))

    sybase.conn withSession { implicit s =>
      val O = sybase.driver.columnOptions
      migration.alterColumnNulls(_.column[Long](columnName, O.Nullable)).apply
    }
    val res = sybase.getColumns(table, columnName).headOption
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
    val migration = TableMigration(testTable)

    val result = sybase.conn withSession { implicit s =>
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
    val migration = TableMigration(testTable)

    val result = sybase.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
      testTable ++= Seq((0, "first"), (0, "second"))
      testTable.list
    }

    result should have size 2
    result.head should be((1, "first"))
    result.tail.head should be((2, "second"))

    sybase.conn withSession { implicit s =>
      migration.dropColumns(_.id).apply
    }
    sybase.getColumns(table, "id") should have size 0
  }

  // === helper classes ===
  trait TableTesting {
    val table: String

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text")
      def * = (id, text)
    }

    lazy val migration = TableMigration(TableQuery[TestTable])
  }

  trait PrimaryKeyTesting {
    val table: String

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text")
      def * = (id, text)
      def pk = primaryKey(table + "_pk", id)
    }

    lazy val migration = TableMigration(TableQuery[TestTable])
  }

  trait ForeignKeyTesting {
    val pkTable: String
    val fkTable: String

    class TestTable1(t: Tag) extends Table[Long](t, pkTable) {
      val id = column[Long]("id_num", O.PrimaryKey)
      def * = id
    }

    class TestTable2(t: Tag) extends Table[(Int, Long)](t, fkTable) {
      val id = column[Int]("id", O.PrimaryKey)
      val num = column[Long]("num")
      def * = (id, num)
      def fk = foreignKey(fkTable + "_fk", num, TableQuery[TestTable1])(_.id)
    }

    lazy val m1 = TableMigration(TableQuery[TestTable1])
    lazy val m2 = TableMigration(TableQuery[TestTable2])
  }

  trait IndexTesting {
    val table: String
    val indexName: String
    val unique = false

    class TestTable(t: Tag) extends Table[(Int, Long, String)](t, table) {
      val id = column[Int]("id", O.PrimaryKey)
      val num = column[Long]("number")
      val text = column[String]("text")
      def * = (id, num, text)
      def idx = index(indexName, (id, num), unique)
    }

    lazy val migration = TableMigration(TableQuery[TestTable])
  }

  trait ColumnTesting {
    val table: String
    val default: Boolean = false
    val nullable: Boolean = false
    private val O = sybase.driver.columnOptions

    def params: Seq[ColumnOption[Long]] = (default, nullable) match {
      case (true, true)  =>  Seq(O.Default(111L), O.Nullable)
      case (true, false) =>  Seq(O.Default(111L), O.NotNull)
      case (false, true) =>  Seq(O.Nullable)
      case (false, false) => Seq(O.NotNull)
    }

    class TestTable(t: Tag) extends Table[(Int, Long)](t, table) {
      val id = column[Int]("id")
      val num = column[Long]("number", params: _*)
      def * = (id, num)
    }

    lazy val migration = TableMigration(TableQuery[TestTable])
  }

}

class SybaseInit extends DbInit("sybase-anywhere", SybaseDriver) {
  override def cleanUpAfter() = databaseFor("adminConn") withSession { implicit session =>
    val tables =
      Q.query[Unit, String]("select name from sysobjects where type='U' and uid = 1").list
    val seqs =
      Q.query[Unit, String]("select sequence_name from syssequence").list

    tables foreach { t => (Q.u + "drop table" + quoteIdentifier(t)).execute }
    seqs foreach { s => (Q.u + "drop sequence" + quoteIdentifier(s)).execute }
  }

  override def cleanUpBefore() = cleanUpAfter()
}
