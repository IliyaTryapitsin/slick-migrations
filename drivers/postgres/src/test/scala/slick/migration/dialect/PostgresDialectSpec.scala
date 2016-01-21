package slick.migration.dialect

import scala.slick.driver.PostgresDriver

class PostgresDialectSpec extends DialectSpec("postgres", PostgresDriver) {
  override implicit val dialect: Dialect[_] = new PostgresDialect()
}