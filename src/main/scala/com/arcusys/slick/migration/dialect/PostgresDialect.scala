package com.arcusys.slick.migration.dialect

import com.arcusys.slick.migration.ast.{ ColumnInfo, TableInfo }

import scala.slick.driver.PostgresDriver

/**
 * Created by Iliya Tryapitsin on 20/01/15.
 */
class PostgresDialect(driver: PostgresDriver) extends Dialect[PostgresDriver](driver) {
  override def columnType(ci: ColumnInfo): String =
    if (ci.autoInc) "SERIAL" else ci.sqlType.get

  override def autoInc(ci: ColumnInfo) = ""

  override def renameColumn(table: TableInfo, from: ColumnInfo, to: String) =
    s"""alter table ${quoteTableName(table)}
      | rename column ${quoteIdentifier(from.name)}
      | to ${quoteIdentifier(to)}""".stripMargin
}
