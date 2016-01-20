package slick.migration.dialect

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.GetResult._
import scala.slick.jdbc.StaticQuery.{interpolation, staticQueryToInvoker => staticQ}
import scala.slick.jdbc.{StaticQuery => Q}

trait OracleQueries {
  import OracleQueries._

  val driver: JdbcProfile
  def conn: driver.Backend#Database

  def tableExists(name: String) = findTables.contains(name)

  def findTables = conn withSession { implicit s =>
    staticQ(sql"select TABLE_NAME from USER_TABLES".as[String])
      .list.map(_.toString)
  }

  def getColumns(table: String) =
    conn withSession { implicit session =>
      staticQ(
        sql"select COLUMN_NAME from USER_TAB_COLUMNS where TABLE_NAME = $table".as[String]
      ).list.map(_.toString)
    }

  def getColumnType(table: String, column: String) =
    conn withSession { implicit session =>
      columnQ(select.DataType)((table, column)).firstOption.map(_.toString)
    }

  def getColumnDefault(table: String, column: String) =
    conn withSession { implicit session =>
      columnQ(select.Default)((table, column)).firstOption.map(_.toString)
    }

  def isColumnNullable(table: String, column: String) =
    conn withSession { implicit s =>
      columnQ(select.Nullable)((table, column))
        .firstOption.map(r => if (r == "N") false else true).getOrElse(false)
    }

  def isPrimaryKey(table: String, columns: Seq[String]) = {
    val result = conn withSession { implicit session =>
      constraintQ(constraintType.PrimaryKey)(table).list.map(_.toString)
    }
    (result.isEmpty && columns.isEmpty) || (result.nonEmpty && result.forall(r => columns.contains(r)))
  }

  def isUniqueIndex(table: String, indexes: Seq[String]) = {
    val result = conn withSession { implicit session =>
      constraintQ(constraintType.UniqueIndex)(table).list.map(_.toString)
    }
    (result.isEmpty && indexes.isEmpty) || (result.nonEmpty && result.forall(r => indexes.contains(r)))
  }

  def getConstraints(table: String) =
    conn withSession { implicit session =>
      staticQ(
        sql"select CONSTRAINT_NAME from USER_CONSTRAINTS where TABLE_NAME = $table".as[String]
      ).list.map(_.toString)
    }

  def findIndexes(table: String) =
    conn withSession { implicit session =>
      staticQ(
        sql"select INDEX_NAME from USER_IND_COLUMNS where TABLE_NAME = $table".as[String]
      ).list.map(_.toString)
    }

  def findForeignKeys(table: String) =
    conn withSession { implicit session =>
      constraintQ(constraintType.ForeignKey)(table).list.map(_.toString)
    }

  def getDeleteRuleFor(constraint: String) =
    conn withSession { implicit session =>
      deleteActionQ(constraint).firstOption.map(_.toString)
    }

  def getTriggers(table: String) =
    conn withSession { implicit session =>
      staticQ(
        sql"select TRIGGER_NAME from USER_TRIGGERS where TABLE_NAME = $table".as[String]
      ).list.map(_.toString)
    }

  private def columnQ(sel: select.Value) = {
    Q.query[(String, String), String](sel.toString +
      " from USER_TAB_COLUMNS where TABLE_NAME = ? and COLUMN_NAME = ?")
  }

  private def constraintQ(cons: constraintType.Value) = {
    Q.query[String, String](s"""select COLUMN_NAME
      | from ALL_CONS_COLUMNS
      | where CONSTRAINT_NAME = (
      |  select CONSTRAINT_NAME from USER_CONSTRAINTS
      |  where TABLE_NAME = ? and $cons)""".stripMargin)
  }

  private def deleteActionQ = {
    Q.query[String, String]("""select DELETE_RULE
      | from USER_CONSTRAINTS
      | where CONSTRAINT_NAME = ?""".stripMargin)
  }
}

object OracleQueries {

  object select extends Enumeration {
    type select = Value
    val Column = Value("select COLUMN_NAME ")
    val DataType = Value("select DATA_TYPE ")
    val Default = Value("select DATA_DEFAULT ")
    val Nullable = Value("select NULLABLE ")
  }

  object constraintType extends Enumeration {
    type constraintType = Value
    val PrimaryKey = Value("CONSTRAINT_TYPE = 'P'")
    val UniqueIndex = Value("CONSTRAINT_TYPE = 'U'")
    val ForeignKey = Value("CONSTRAINT_TYPE = 'R'")
  }
}