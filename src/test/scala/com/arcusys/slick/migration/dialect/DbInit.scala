package com.arcusys.slick.migration.dialect

import java.net.{URL, URLClassLoader}
import java.sql.Driver

import com.typesafe.config.Config

import scala.collection.mutable
import scala.slick.driver.{JdbcProfile => SlickDriver}
import scala.slick.jdbc.meta._
import scala.slick.jdbc.{StaticQuery => Q}

class DbInit(confName: String, val driver: SlickDriver) {
  private lazy val database = driver.backend.Database
  private lazy val config: Config = TestConfig.testConfig(confName)

  private val jdbcDriver = confString("driver")
  private val create = confStrings("create")
  private val postCreate = confStrings("postCreate")
  private val drop = confStrings("drop")

  override def toString = confString("testConn.url")

  def confOptionalString(path: String) = if (config.hasPath(path)) Some(config.getString(path)) else None
  def confString(path: String) = confOptionalString(path).getOrElse(null)
  def confStrings(path: String) = TestConfig.getStrings(config, path).getOrElse(Nil)
  def databaseFor(path: String): driver.Backend#Database = database.forConfig(path, config, loadCustomDriver().getOrElse(null))

  def conn = databaseFor("testConn")

  def cleanUpBefore() = databaseFor("adminConn") withSession { implicit session =>
    if (drop.nonEmpty || create.nonEmpty) {
      println(s"[Creating test database $this]")
      for (s <- drop) (Q.u + s).execute
      for (s <- create) (Q.u + s).execute
    }
    if (postCreate.nonEmpty) {
      conn withSession { implicit session =>
        for (s <- postCreate) (Q.u + s).execute
      }
    }
  }

  def cleanUpAfter() = databaseFor("adminConn") withSession { implicit session =>
    if (drop.nonEmpty) {
      println(s"[Dropping test database $this]")
      for (s <- drop) (Q.u + s).execute
    }
  }

  def getTables =
    conn withSession { implicit s =>
      MTable.getTables(None, None, None, Some(Seq("TABLE"))).list
    }

  def getPrimaryKeys(table: String): Seq[MPrimaryKey] = {
    val t = findTable(table)
    conn withSession { implicit s =>
      t.map { mt =>
        MPrimaryKey.getPrimaryKeys(mt.name).list
      } getOrElse Nil
    }
  }

  def getForeignKeys(table: String): Seq[MForeignKey] = {
    val t = findTable(table)
    conn withSession { implicit s =>
      t.map { mt =>
        MForeignKey.getImportedKeys(mt.name).list
      } getOrElse Nil
    }
  }

  def getIndexes(table: String, unique: Boolean = false): Seq[MIndexInfo] = {
    val t = findTable(table)
    conn withSession { implicit s =>
      t.map { mt =>
        MIndexInfo.getIndexInfo(mt.name, unique).list
      } getOrElse Nil
    }
  }

  def getColumns(table: String, column: String = "%"): Seq[MColumn] = {
    val t = findTable(table)
    conn withSession { implicit s =>
      t.map { mt =>
        MColumn.getColumns(mt.name, column).list
      } getOrElse Nil
    }
  }

  def findTable(table: String) = getTables.find(_.name.name == table)

  def quoteIdentifier(id: String): String = {
    val s = new StringBuilder(id.length + 4) append '"'
    for(c <- id) if(c == '"') s append "\"\"" else s append c
    (s append '"').toString
  }

  private def loadCustomDriver() = confOptionalString("driverJar").map { jar =>
    DbInit.getCustomDriver(jar, jdbcDriver)
  }
}

object DbInit {
  // A cache for custom drivers to avoid excessive reloading and memory leaks
  private[this] val driverCache = new mutable.HashMap[(String, String), Driver]()

  def getCustomDriver(url: String, driverClass: String): Driver = synchronized {
    driverCache.getOrElseUpdate((url, driverClass),
      new URLClassLoader(Array(new URL(url)), getClass.getClassLoader).loadClass(driverClass).newInstance.asInstanceOf[Driver]
    )
  }
}