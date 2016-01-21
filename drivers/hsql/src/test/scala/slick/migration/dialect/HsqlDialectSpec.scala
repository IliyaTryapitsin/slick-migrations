package slick.migration.dialect

import scala.slick.driver.HsqldbDriver

class HsqlDialectSpec extends DialectSpec("hsql", HsqldbDriver) {
  override implicit val dialect: Dialect[_] = new HsqldbDialect()
}