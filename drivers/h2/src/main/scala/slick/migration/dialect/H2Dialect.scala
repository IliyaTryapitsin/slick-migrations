package slick.migration.dialect

import slick.migration.ast.ColumnInfo

import scala.slick.driver.H2Driver

/**
 * Created by Iliya Tryapitsin on 20/01/15.
 */
class H2Dialect(driver: H2Driver = H2Driver) extends Dialect[H2Driver](driver) {
  override def columnType(ci: ColumnInfo): String =
    if (ci.autoInc) "SERIAL" else ci.sqlType.get

  override def autoInc(ci: ColumnInfo) = ""
}
