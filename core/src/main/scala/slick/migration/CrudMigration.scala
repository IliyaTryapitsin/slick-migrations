package slick.migration


import slick.migration.dialect.Dialect

import scala.slick.driver.JdbcProfile
import scala.slick.lifted.Column

/**
 * Created by Iliya Tryapitsin on 18.04.15.
 */
private[migration] abstract class CrudMigration[T <: JdbcProfile#Table[_]](table: T, columnValuePairs: Map[Column[_], Any])(implicit dialect: Dialect[_]) extends SqlMigration {

  protected val pairs = columnValuePairs.map { pair => (pair._1, dialect.driver.valueToSQLLiteral(pair._2, pair._1.tpe)) }
  protected val columns = dialect.columnList(pairs.map { p => p._1.fieldSymbol }.toSeq: _*)
  protected val values = pairs.map { p => p._2 }.mkString(", ")
}
