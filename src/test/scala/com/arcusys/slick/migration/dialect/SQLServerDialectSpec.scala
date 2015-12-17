package com.arcusys.slick.migration.dialect

import scala.slick.jdbc.meta._
import scala.slick.jdbc.GetResult._
import scala.slick.jdbc.{ StaticQuery => Q }
import Q.interpolation
import Q.{ staticQueryToInvoker => staticQ }

import org.scalatest.{ FlatSpec, BeforeAndAfterAll }
import org.scalatest.Matchers._

import com.arcusys.slick.migration.table.TableMigration
import com.arcusys.slick.drivers.SQLServerDriver

class SQLServerDialectSpec extends FlatSpec with BeforeAndAfterAll {
  val sqlserver = new DbInit("sqlserver", SQLServerDriver)

  import sqlserver.driver.simple._

  implicit val dialect: Dialect[_] = Dialect(SQLServerDriver).get

  override def beforeAll() { sqlserver.cleanUpBefore() }
  override def afterAll() { sqlserver.cleanUpAfter() }

  behavior of "MS SQL Server dialect for slick's migration"

  it should "create a new table" in new TableTesting {
    val table = "test_create_table"
    val migration = initMigration

    getTables should have size 0

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }

    val created = getTableNames
    created should have size 1
    created should contain(table)
  }

  it should "drop an existing table" in new TableTesting {
    val table = "test_drop_table"
    val migration = initMigration

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    getTableNames should contain(table)

    sqlserver.conn withSession { implicit s =>
      migration.drop.apply()
    }
    getTableNames should not contain (table)
  }

  it should "rename an existing table" in new TableTesting {
    val table = "test_rename_table"
    val migration = initMigration

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    getTableNames should contain(table)

    sqlserver.conn withSession { implicit s =>
      migration.rename("test_renamed_table").apply
    }
    getTableNames should not contain (table)
    getTableNames should contain("test_renamed_table")
  }

  it should "create primary key" in new PrimaryKeyTesting {
    val table = "test_add_primary_key"
    val migration = initMigration

    sqlserver.conn withSession { implicit s =>
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

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).addPrimaryKeys(_.pk).apply
    }
    getPrimaryKey(table).isDefined should be(true)

    sqlserver.conn withSession { implicit s =>
      migration.dropPrimaryKeys(_.pk).apply
    }
    getPrimaryKey(table).isEmpty should be(true)
  }

  it should "add foreign keys" in new ForeignKeyTesting {
    val refTable = "test_add_foreign_keys1"
    val fkTable = "test_add_foreign_keys2"

    sqlserver.conn withSession { implicit s =>
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

    sqlserver.conn withSession { implicit s =>
      m1.create.addColumns(_.id).apply()
      m2.create.addColumns(_.id, _.num).addForeignKeys(_.fk).apply
    }
    getForeignKey(fkTable).isDefined should be(true)

    sqlserver.conn withSession { implicit s =>
      m2.dropForeignKeys(_.fk).apply()
    }
    getForeignKey(fkTable).isDefined should be(false)
  }

  it should "create new indexes" in new IndexTesting {
    val table = "test_create_index_table"
    val indexName = "test_create_idx"

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }

    val idx = getIndexes(table).find(_._1 == indexName)
    idx.isDefined should be(true)
    idx.get._2 should be(false)
  }

  it should "be able to create unique index" in new IndexTesting {
    val table = "test_create_un_index_table"
    val indexName = "test_create_un_idx"
    override val unique = true

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }

    val idx = getIndexes(table).find(_._1 == indexName)
    idx.isDefined should be(true)
    idx.get._2 should be(true)
  }

  it should "rename existing indexes" in new IndexTesting {
    val table = "test_rename_index_table"
    val indexName = "test_rename_idx"

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }
    getIndexes(table).find(_._1 == indexName).isDefined should be(true)

    sqlserver.conn withSession { implicit s =>
      migration.renameIndex(_.idx, "test_renamed_idx").apply()
    }
    val idx = getIndexes(table).find(_._1 == "test_renamed_idx")
    idx.isDefined should be(true)
  }

  it should "drop existing indexes" in new IndexTesting {
    val table = "test_drop_index_table"
    val indexName = "test_drop_idx"

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num, _.text).addIndexes(_.idx).apply
    }
    getIndexes(table).find(_._1 == indexName).isDefined should be(true)

    sqlserver.conn withSession { implicit s =>
      migration.dropIndexes(_.idx).apply
    }
    getIndexes(table).find(_._1 == indexName).isDefined should be(false)
  }

  it should "add a new column into existing table" in new ColumnTesting {
    val table = "test_add_column_table"

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id).apply
    }
    getColumns(table) should contain("id")

    sqlserver.conn withSession { implicit s =>
      migration.addColumns(_.column[String]("text")).apply
    }
    getColumns(table) should contain("text")
  }

  it should "drop existing column in a table" in new ColumnTesting {
    val table = "test_drop_column_table"

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    val cs = getColumns(table)
    cs should contain("id")
    cs should contain("number")

    sqlserver.conn withSession { implicit s =>
      migration.dropColumns(_.num).apply
    }
    val res = getColumns(table)
    res should contain("id")
    res should not contain ("number")
  }

  it should "rename existing column" in new ColumnTesting {
    val table = "test_rename_column_table"

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }
    getColumns(table) should contain("number")

    sqlserver.conn withSession { implicit s =>
      migration.renameColumn(_.num, "identifier").apply
    }
    val res = getColumns(table)
    res should have size 2
    res should not contain ("number")
    res should contain("identifier")
  }

  it should "alter column type of existing column" in new ColumnTesting {
    val table = "test_alter_column_type_table"

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.num).apply
    }

    val ci = getColumnInfo("number", table)
    ci.isDefined should be(true)
    ci.get._1 should be("bigint")
    ci.get._3 should be(false)

    sqlserver.conn withSession { implicit s =>
      migration.alterColumnTypes(_.column[String]("number")).apply
    }
    val res = getColumnInfo("number", table)
    res.isDefined should be(true)
    res.get._1 should be("varchar")
    res.get._3 should be(false)
  }

  it should "alter default value for an existing column" in {
    val table = "test_alter_default_value_table"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text", O.Default("testing_default"))
      def * = (id, text)
    }
    val migration = TableMigration(TableQuery[TestTable])

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    val ci = getColumnInfo("text", table)
    ci.isDefined should be(true)
    ci.get._2 should be("testing_default")

    sqlserver.conn withSession { implicit s =>
      val O = sqlserver.driver.columnOptions
      migration.alterColumnDefaults(_.column[String]("text", O.Default("new_default"))).apply
    }
    val res = getColumnInfo("text", table)
    res.isDefined should be(true)
    res.get._2 should be("new_default")
  }

  it should "altere nullability of existing column" in {
    val table = "test_alter_nullability_value_table"

    class TestTable(t: Tag) extends Table[(Int, String)](t, table) {
      val id = column[Int]("id")
      val text = column[String]("text", O.Nullable)
      def * = (id, text)
    }
    val migration = TableMigration(TableQuery[TestTable])

    sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
    }
    val ci = getColumnInfo("text", table)
    ci.isDefined should be(true)
    ci.get._3 should be(true)

    sqlserver.conn withSession { implicit s =>
      val O = sqlserver.driver.columnOptions
      migration.alterColumnNulls(_.column[String]("text", O.NotNull)).apply
    }
    val res = getColumnInfo("text", table)
    res.isDefined should be(true)
    res.get._3 should be(false)
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

    val result = sqlserver.conn withSession { implicit s =>
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

    val result = sqlserver.conn withSession { implicit s =>
      migration.create.addColumns(_.id, _.text).apply
      testTable ++= Seq((0, "first"), (0, "second"))
      testTable.list
    }

    result should have size 2
    result.head should be((1, "first"))
    result.tail.head should be((2, "second"))

    sqlserver.conn withSession { implicit s =>
      migration.dropColumns(_.id).apply
    }
  }

  private def getTables =
    sqlserver.conn withSession { implicit s =>
      MTable.getTables(None, None, None, Some(Seq("TABLE"))).list
    }

  private def getPrimaryKey(table: String) = {
    val t = findTable(table)
    sqlserver.conn withSession { implicit s =>
      t.flatMap { mt => MPrimaryKey.getPrimaryKeys(mt.name).firstOption }
    }
  }

  private def getForeignKey(table: String) = {
    val t = findTable(table)
    sqlserver.conn withSession { implicit s =>
      t.flatMap { mt => MForeignKey.getImportedKeys(mt.name).firstOption }
    }
  }

  private def getIndexes(table: String) = {
    sqlserver.conn withSession { implicit s =>
      val q = Q.query[String, (String, String)](
        """select name, is_unique from sys.indexes
        | where object_id = (
        | select object_id from sys.objects where name = ?)
        """.stripMargin)

      q(table).list.map { case (n, u) => (n, if (u == "1") true else false) }
    }
  }

  private def getColumns(table: String) = {
    sqlserver.conn withSession { implicit s =>
      val q = Q.query[String, String](
        """select column_name,* from information_schema.columns
        | where table_name = ?
        | order by ordinal_position""".stripMargin)
      q(table).list
    }
  }

  private def getColumnInfo(name: String, table: String) = {
    sqlserver.conn withSession { implicit s =>
      val q = Q.query[(String, String), (String, String, String)](
        """select data_type, column_default, is_nullable from information_schema.columns
        | where column_name = ? and table_name = ?
        | order by ordinal_position""".stripMargin)
      q((name, table)).firstOption.map {
        case (dt, df, n) =>
          (dt,
            if (df != null) df.stripPrefix("(").stripPrefix("'").stripSuffix(")").stripSuffix("'") else "",
            if (n == "NO") false else true)
      }
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