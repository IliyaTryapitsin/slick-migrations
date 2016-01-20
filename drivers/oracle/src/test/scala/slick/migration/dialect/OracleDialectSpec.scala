package slick.migration.dialect

import org.scalatest.{ FlatSpec, BeforeAndAfterAll }
import org.scalatest.Matchers._

import slick.migration.table.TableMigration
import com.arcusys.slick.drivers.OracleDriver

class OracleDialectSpec extends FlatSpec with BeforeAndAfterAll {
  val oracle = new DbInit("oracle", OracleDriver) with OracleQueries
  import oracle.driver.simple._

  implicit val dialect: Dialect[_] = Dialect(OracleDriver).get

  override def beforeAll() { oracle.cleanUpBefore() }
  override def afterAll() { oracle.cleanUpAfter() }

  behavior of "Oracle dialect for slick's migration"

  it should "create trigger to model auto-incrementing behaviour if a column is marked as autoincremented" in new AutoInc {
    val table = "create_autoinc"
    val testM = TableMigration(TableQuery[AutoIncTestTable])

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id, _.num).apply()
    }

    oracle.getTriggers(table) should contain(trg.toUpperCase)
  }

  it should "drop trigger in case of auto-incremented column if table is dropped" in new AutoInc {
    val table = "drop_autoinc"
    val testM = TableMigration(TableQuery[AutoIncTestTable])

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id, _.num).apply()
    }

    oracle.conn withSession { implicit s =>
      testM.drop.apply()
    }

    oracle.getTriggers(table) should not contain (trg.toUpperCase)
  }

  it should "allow auto-incrementing data insertion" in new AutoInc {
    val table = "autoinc_insert"
    val testT = TableQuery[AutoIncTestTable]
    val testM = TableMigration(testT)

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id, _.num).apply()
    }

    val result = oracle.conn withSession { implicit s =>
      testT ++= Seq((0, 12345), (0, 6789))
      testT.list
    }

    result should have size 2
    result.head should be((1, 12345))
    result.tail.head should be((2, 6789))
  }

  it should "allow auto-incremened column renaming and insertion after that (on hold till v2.4)" in (pending)
  it should "drop trigger if auto-incremented column is dropped (on hold till v2.4)" in (pending)
  it should "reset trigger (sequence) for auto-incremented column if table is renamed (on hold till v2.4)" in (pending)
  it should "continue insertion of auto-incremented values in column after table renaming (on hold till v2.4)" in (pending)

  it should "allow to create another auto-incremented columns along with primary key auto-incremented column" in new AutoInc {
    val table = "create_a_autoinc"
    val testM = TableMigration(TableQuery[AutoIncTestTable])

    oracle.conn withSession { implicit s =>
      val O = oracle.driver.columnOptions
      testM.create.addColumns(_.id, _.num).apply()
      testM.addColumns(_.column[Long]("acolumn", O.AutoInc)).apply()
    }

    val triggers = oracle.getTriggers(table)
    triggers should contain(trg.toUpperCase)
    triggers should contain((table + "__acolumn" + "_trg").toUpperCase)
  }

  it should "allow to insert into multiple auto-incremented columns" in {
    val table = "autoinc_mult_insert"

    class TestTable(t: Tag) extends Table[(Int, Int, Long)](t, table) {
      val id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      val num = column[Int]("num")
      val num2 = column[Long]("num2", O.AutoInc)
      def * = (id, num, num2)
    }

    val testT = TableQuery[TestTable]
    val testM = TableMigration(testT)

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id, _.num).apply()

    }

    val result = oracle.conn withSession { implicit s =>
      testM.addColumns(_.num2).apply()
      testT ++= Seq((0, 12345, 0), (0, 6789, 0))
      testT.list
    }

    result should have size 2
    result.head should be((1, 12345, 1))
    result.tail.head should be((2, 6789, 2))
  }

  it should """create foreign key with 'on delete cascade' in case of delete action is 'cascade' """ in {
    class TestTable1(t: Tag) extends Table[Int](t, "test_fk_1") {
      val id = column[Int]("id", O.PrimaryKey)
      def * = id
    }

    class TestTable2(t: Tag) extends Table[(Int, Option[Int])](t, "test_fk_2") {
      val id = column[Int]("id", O.PrimaryKey)
      val test = column[Option[Int]]("test")
      def * = (id, test)
      def testFK = foreignKey("test_fk", test, TableQuery[TestTable1])(_.id, onDelete = ForeignKeyAction.Cascade)
    }

    val testM1 = TableMigration(TableQuery[TestTable1])
    val testM2 = TableMigration(TableQuery[TestTable2])

    oracle.conn withSession { implicit s =>
      testM1.create.addColumns(_.id).apply()
      testM2.create.addColumns(_.id, _.test).addForeignKeys(_.testFK).apply()
    }

    oracle.findForeignKeys("test_fk_2") should contain("test")
    oracle.getDeleteRuleFor("test_fk") should contain("CASCADE")
  }

  it should """create foreign key with 'on delete set null' in case of delete action is 'set null' """ in {
    class TestTable1(t: Tag) extends Table[Int](t, "test2_fk_1") {
      val id = column[Int]("id", O.PrimaryKey)
      def * = id
    }

    class TestTable2(t: Tag) extends Table[(Int, Option[Int])](t, "test2_fk_2") {
      val id = column[Int]("id", O.PrimaryKey)
      val test = column[Option[Int]]("test")
      def * = (id, test)
      def testFK = foreignKey("test2_fk", test, TableQuery[TestTable1])(_.id, onDelete = ForeignKeyAction.SetNull)
    }

    val testM1 = TableMigration(TableQuery[TestTable1])
    val testM2 = TableMigration(TableQuery[TestTable2])

    oracle.conn withSession { implicit s =>
      testM1.create.addColumns(_.id).apply()
      testM2.create.addColumns(_.id, _.test).addForeignKeys(_.testFK).apply()
    }

    oracle.findForeignKeys("test2_fk_2") should contain("test")
    oracle.getDeleteRuleFor("test2_fk") should contain("SET NULL")
  }

  it should """create foreign key with 'initially deferred' in case of update action is 'cascade' """ in (pending)

  it should "drop foreign key" in {
    class TestTable1(t: Tag) extends Table[Int](t, "test3_fk_1") {
      val id = column[Int]("id", O.PrimaryKey)
      def * = id
    }

    class TestTable2(t: Tag) extends Table[(Int, Option[Int])](t, "test3_fk_2") {
      val id = column[Int]("id", O.PrimaryKey)
      val test = column[Option[Int]]("test")
      def * = (id, test)
      def testFK = foreignKey("test3_fk", test, TableQuery[TestTable1])(_.id)
    }

    val testM1 = TableMigration(TableQuery[TestTable1])
    val testM2 = TableMigration(TableQuery[TestTable2])

    oracle.conn withSession { implicit s =>
      testM1.create.addColumns(_.id).apply()
      testM2.create.addColumns(_.id, _.test).addForeignKeys(_.testFK).apply()
    }

    oracle.findForeignKeys("test3_fk_2") should contain("test")

    oracle.conn withSession { implicit s =>
      testM2.dropForeignKeys(_.testFK).apply()
    }

    oracle.findForeignKeys("test3_fk_2") should have size 0
  }

  it should "create index and constraint with a given name of index itself if index is unique" in {
    val tableName = "test_creating_unique_index"
    val indexName = "test_unique_index"

    val testM = TableMigration(testIdxTable(tableName, indexName, true))

    def isUniqueIndex = oracle.isUniqueIndex(tableName, Seq("id"))
    def indexes = oracle.findIndexes(tableName)
    def constraints = oracle.getConstraints(tableName)

    isUniqueIndex should be(false)
    indexes should have size 0
    constraints should not contain (indexName)

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id).addIndexes(_.idx).apply()
    }

    indexes should have size 1
    indexes should contain(indexName)
    constraints should contain(indexName)
    isUniqueIndex should be(true)
  }

  it should "create index without constraint if it is not unique" in {
    val tableName = "test_creating_index"

    val testM = TableMigration(testIdxTable(tableName, "test_index"))

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id).addIndexes(_.idx).apply()
    }

    val indexes = oracle.findIndexes(tableName)

    oracle.isUniqueIndex(tableName, Seq("id")) should be(false)
    indexes should have size 1
    indexes should contain("test_index")
  }

  it should "drop index as constraint if index is unique" in {
    val tableName = "test_dropping_unique_index"
    val indexName = "test_drop_unique_index"

    val testM = TableMigration(testIdxTable(tableName, indexName, true))

    def constraints = oracle.getConstraints(tableName)
    def indexes = oracle.findIndexes(tableName)

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id).addIndexes(_.idx).apply()
    }

    constraints should contain(indexName)
    indexes should contain(indexName)

    oracle.conn withSession { implicit s =>
      testM.dropIndexes(_.idx).apply()
    }

    constraints should not contain (indexName)
    indexes should not contain (indexName)
  }

  it should "just drop index itself if it is not unique" in {
    val tableName = "test_dropping_index"
    val indexName = "test_drop_index"

    val testM = TableMigration(testIdxTable(tableName, indexName))

    def constraints = oracle.getConstraints(tableName)
    def indexes = oracle.findIndexes(tableName)

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id).addIndexes(_.idx).apply()
    }

    constraints should not contain (indexName)
    indexes should contain(indexName)

    oracle.conn withSession { implicit s =>
      testM.dropIndexes(_.idx).apply()
    }

    constraints should not contain (indexName)
    indexes should not contain (indexName)
  }

  it should "rename index and rename constraint if index is unique" in {
    val tableName = "test_renaming_unique_index"
    val indexName = "test_rename_unique_index"
    val renamedIndex = "test_renamed_unique_index"

    val testM = TableMigration(testIdxTable(tableName, indexName, true))

    def constraints = oracle.getConstraints(tableName)
    def indexes = oracle.findIndexes(tableName)

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id).addIndexes(_.idx).apply()
    }

    constraints should contain(indexName)
    indexes should contain(indexName)

    oracle.conn withSession { implicit s =>
      testM.renameIndex(_.idx, renamedIndex).apply()
    }

    constraints should not contain (indexName)
    indexes should not contain (indexName)
    constraints should contain(renamedIndex)
    indexes should contain(renamedIndex)
  }

  it should "just rename index itself if it is not unique" in {
    val tableName = "test_renaming_index"
    val indexName = "test_rename_index"
    val renamedIndex = "test_renamed_index"

    val testM = TableMigration(testIdxTable(tableName, indexName, false))

    def constraints = oracle.getConstraints(tableName)
    def indexes = oracle.findIndexes(tableName)

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id).addIndexes(_.idx).apply()
    }

    constraints should not contain (indexName)
    indexes should contain(indexName)

    oracle.conn withSession { implicit s =>
      testM.renameIndex(_.idx, renamedIndex).apply()
    }

    indexes should not contain (indexName)
    constraints should not contain (renamedIndex)
    indexes should contain(renamedIndex)
  }

  it should "add new column into existing table" in {
    val tableName = "test_adding_new_column"
    val testM = TableMigration(testTable(tableName))

    def columns = oracle.getColumns(tableName)
    columns should have size 1
    columns.head should be("id")

    oracle.conn withSession { implicit s =>
      testM.addColumns(_.column[String]("text")).apply()
    }

    columns should have size 2
    columns.head should be("id")
    columns.tail.head should be("text")
  }

  it should "rename existing column in a table" in {
    val tableName = "test_renaming_existing_column"
    val testM = TableMigration(testTable(tableName))

    def idExists = oracle.getColumns(tableName).contains("id")
    idExists should be(true)

    oracle.conn withSession { implicit s => testM.renameColumn(_.id, "number").apply() }
    idExists should be(false)
    oracle.getColumns(tableName).contains("number") should be(true)
  }

  it should "alter data type of existing column in a table" in {
    val tableName = "test_altering_column_datatype"
    val testM = TableMigration(testTable(tableName))

    def idType = oracle.getColumnType(tableName, "id")
    idType should be(Some("NUMBER"))

    oracle.conn withSession { implicit s =>
      testM.alterColumnTypes(_.column[String]("id")).apply()
    }

    idType should be(Some("VARCHAR2"))
  }

  it should "alter defaut value for column's data type" in {
    val tableName = "test_altering_column_default"

    class TestTable(t: Tag) extends Table[String](t, tableName) {
      val text = column[String]("text", O.Default("testing_default"))
      def * = text
    }
    val testM = TableMigration(TableQuery[TestTable])

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.text).apply()
    }

    def default = oracle.getColumnDefault(tableName, "text").map(_.trim.stripPrefix("'").stripSuffix("'"))
    default should be(Some("testing_default"))

    oracle.conn withSession { implicit s =>
      val O = oracle.driver.columnOptions
      testM.alterColumnDefaults(_.column[String]("text", O.Default("altered_value"))).apply()
    }

    default should be(Some("altered_value"))
  }

  it should "alter column nullability" in {
    val tableName = "test_altering_nullability"
    val testM = TableMigration(testTable(tableName))

    def isNullable = oracle.isColumnNullable(tableName, "id")
    isNullable should be(false)

    oracle.conn withSession { implicit s =>
      val O = oracle.driver.columnOptions
      testM.alterColumnNulls(_.column[Int]("id", O.Nullable)).apply()
    }
    isNullable should be(true)
  }

  it should "create new table" in {
    val testM = TableMigration(testTable("test_creating_new_table"))

    oracle.conn withSession { implicit s => testM.create.addColumns(_.id).apply() }
    oracle.tableExists("test_creating_new_table") should be(true)
  }

  it should "rename existing table" in {
    val testM = TableMigration(testTable("test_renaming_1"))

    oracle.tableExists("test_renaming_1") should be(true)
    oracle.tableExists("test_renaming_2") should be(false)

    oracle.conn withSession { implicit s => testM.rename("test_renaming_2").apply() }
    oracle.tableExists("test_renaming_1") should be(false)
    oracle.tableExists("test_renaming_2") should be(true)
  }

  it should "drop existing table" in {
    val testM = TableMigration(testTable("test_dropping_existing_table"))

    oracle.tableExists("test_dropping_existing_table") should be(true)
    oracle.conn withSession { implicit s => testM.drop.apply() }
    oracle.tableExists("test_dropping_existing_table") should be(false)
  }

  it should "create primary key" in {
    val tableName = "test_creating_pk"

    class TestTable(t: Tag) extends Table[Int](t, tableName) {
      val id = column[Int]("id")
      def * = id
      def pk = primaryKey("test_pk", id)
    }

    val testM = TableMigration(TableQuery[TestTable])

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id).addPrimaryKeys(_.pk).apply()
    }

    val isPrimaryKey = oracle.isPrimaryKey(tableName, Seq("id"))
    isPrimaryKey should be(true)
  }

  it should "create compound primary key" in {
    val tableName = "test_creating_compound_pk"

    class TestTable(t: Tag) extends Table[(Int, Int)](t, tableName) {
      val id = column[Int]("id")
      val num = column[Int]("num")
      def * = (id, num)
      def pk = primaryKey("test_compound_pk", (id, num))
    }

    val testM = TableMigration(TableQuery[TestTable])

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id, _.num).addPrimaryKeys(_.pk).apply()
    }

    val isPrimaryKey = oracle.isPrimaryKey(tableName, Seq("id", "num"))
    isPrimaryKey should be(true)
  }

  it should "drop primary key" in {
    val tableName = "test_dropping_primary_key"

    class TestTable(t: Tag) extends Table[Int](t, tableName) {
      val id = column[Int]("id")
      def * = id
      def pk = primaryKey("test_dropping_pk", id)
    }

    val testM = TableMigration(TableQuery[TestTable])

    oracle.conn withSession { implicit s =>
      testM.create.addColumns(_.id).addPrimaryKeys(_.pk).apply()
    }

    def isPrimaryKey = oracle.isPrimaryKey(tableName, Seq("id"))
    isPrimaryKey should be(true)

    oracle.conn withSession { implicit s =>
      testM.dropPrimaryKeys(_.pk).apply()
    }
    isPrimaryKey should be(false)
  }

  private def testTable(name: String) = TableQuery(t => new Table[Int](t, name) {
    val id = column[Int]("id")
    def * = id
  })

  private def testIdxTable(name: String, idxName: String, isUnique: Boolean = false) =
    TableQuery(t => new Table[Int](t, name) {
      val id = column[Int]("id")
      def * = id
      def idx = index(idxName, id, unique = isUnique)
    })

  trait AutoInc {
    val table: String
    val autoInc = "id"
    lazy val trg = s"${table}__${autoInc}_trg"

    class AutoIncTestTable(t: Tag) extends Table[(Int, Int)](t, table) {
      val id = column[Int](autoInc, O.PrimaryKey, O.AutoInc)
      val num = column[Int]("num")
      def * = (id, num)
    }
  }
}
