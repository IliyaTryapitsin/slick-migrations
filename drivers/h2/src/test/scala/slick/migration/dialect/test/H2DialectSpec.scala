package slick.migration.dialect.test

import slick.migration.dialect.{H2Dialect, Dialect, DialectSpec}
import scala.slick.driver.H2Driver

class H2DialectSpec extends DialectSpec("h2", H2Driver) {
  override implicit val dialect: Dialect[_] = new H2Dialect()
}