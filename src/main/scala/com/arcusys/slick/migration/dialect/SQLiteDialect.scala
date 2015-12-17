package com.arcusys.slick.migration.dialect

/**
 * Created by Iliya Tryapitsin on 20/01/15.
 */

import com.arcusys.slick.migration.ast.ColumnInfo

import scala.slick.driver._

class SQLiteDialect(driver: SQLiteDriver) extends Dialect[SQLiteDriver](driver) with SimulatedRenameIndex {
  override def columnType(ci: ColumnInfo): String =
    if (ci.autoInc) "INTEGER" else ci.sqlType.get
}

