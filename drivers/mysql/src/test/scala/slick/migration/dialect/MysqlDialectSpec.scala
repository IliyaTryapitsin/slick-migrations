package slick.migration.dialect

import scala.slick.driver.MySQLDriver

class MysqlDialectSpec extends DialectSpec("mysql", MySQLDriver) {
  override implicit val dialect: Dialect[_] = new MySQLDialect()
}