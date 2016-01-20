package slick.migration.dialect

import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import slick.migration.table.TableMigration

import scala.slick.driver.MySQLDriver
import scala.slick.jdbc.meta._

class MysqlDialectSpec extends FlatSpec with BeforeAndAfterAll {
  val mysql = new DbInit("mysql", MySQLDriver)

  import mysql.driver.simple._

  implicit val dialect: Dialect[_] = new Dialect(MySQLDriver)

  override def beforeAll() { mysql.cleanUpBefore() }
  override def afterAll() { mysql.cleanUpAfter() }

  behavior of "MySQL dialect for slick's migration"

  it should "create a new table" in new TableTesting {
    val table = "test_create_table"
    val migration = initMigration

    getTables should have size 0

    mysql.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }

    val created = getTableNames
    created should have size 1
    created should contain(table)
  }

  it should "drop an existing table" in new TableTesting {
    val table = "test_drop_table"
    val migration = initMigration

    mysql.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    getTableNames should contain(table)

    mysql.conn withSession { implicit s =>
      migration.drop.apply()
    }
    getTableNames should not contain (table)
  }

  it should "rename an existing table" in new TableTesting {
    val table = "test_rename_table"
    val migration = initMigration

    mysql.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    getTableNames should contain(table)

    mysql.conn withSession { implicit s =>
      migration.rename("test_renamed_table").apply
    }
    getTableNames should not contain (table)
    getTableNames should contain("test_renamed_table")
  }

  it should "create primary key" in new PrimaryKeyTesting {
    val table = "test_add_primary_key"
    val migration = initMigration

    mysql.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).addPrimaryKeys(_.pk).apply
    }

    val pk = getPrimaryKey(table)
    pk.isDefined should be(true)
    pk.get.column should be("id")
    pk.get.pkName should be(Some(table + "_pk"))
  }

  it should "drop primary key" in new PrimaryKeyTesting {
    val table = "test_drop_primary_key"
    val migration = initMigration

    mysql.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).addPrimaryKeys(_.pk).apply
    }
    getPrimaryKey(table).isDefined should be(true)

    mysql.conn withSession { implicit s =>
      migration.dropPrimaryKeys(_.pk).apply
    }
    getPrimaryKey(table).isEmpty should be(true)
  }

  it should "add foreign keys" in new ForeignKeyTesting {
    val refTable = "test_add_foreign_keys1"
    val fkTable = "test_add_foreign_keys2"

    mysql.conn withSession { implicit s =>
      m1.create.addColumns(_.id).apply()
      m2.create.addColumns(_.id, _.num).addForeignKeys(_.fk).apply
    }

    val fk = getForeignKey(fkTable)
    fk.isDefined should be(true)
    fk.get.pkTable.name should be(refTable)
    fk.get.fkTable.name should be(fkTable)
    fk.get.fkColumn should be("num")
  }

  it should "drop foreign keys" in new ForeignKeyTesting {
    val refTable = "test_drop_foreign_keys1"
    val fkTable = "test_drop_foreign_keys2"

    mysql.conn withSession { implicit s =>
      m1.create.addColumns(_.id).apply()
      m2.create.addColumns(_.id, _.num).addForeignKeys(_.fk).apply
    }
    getForeignKey(fkTable).isDefined should be(true)

    mysql.conn withSession { implicit s =>
      m2.dropForeignKeys(_.fk).apply()
    }
    getForeignKey(fkTable).isDefined should be(false)
  }

  private def getTables =
    mysql.conn withSession { implicit s =>
      MTable.getTables(None, None, None, Some(Seq("TABLE"))).list
    }

  private def getPrimaryKey(table: String) = {
    val t = findTable(table)
    mysql.conn withSession { implicit s =>
      t.flatMap { mt => MPrimaryKey.getPrimaryKeys(mt.name).firstOption }
    }
  }

  private def getForeignKey(table: String) = {
    val t = findTable(table)
  mysql.conn withSession { implicit s =>
      t.flatMap { mt => MForeignKey.getImportedKeys(mt.name).firstOption }
    }
  }
  private def findTable(table: String) = getTables.find(_.name.name == table)

  private def getTableNames = getTables.map(_.name.name)

  trait TableTesting {
    val table: String

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text")
      def * = (id, text)
    }

    def initMigration = TableMigration(TableQuery[TestTable])
  }

  trait PrimaryKeyTesting {
    val table: String

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text")
      def * = (id, text)
      def pk = primaryKey(table + "_pk", id)
    }

    def initMigration = TableMigration(TableQuery[TestTable])
  }

  trait ForeignKeyTesting {
    val refTable: String
    val fkTable: String

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

    class TestTable(t: Tag) extends Table[(Int, Long)](t, table) {
      val id = column[Int]("id")
      val num = column[Long]("number")
      def * = (id, num)
    }

    lazy val migration = TableMigration(TableQuery[TestTable])
  }
}