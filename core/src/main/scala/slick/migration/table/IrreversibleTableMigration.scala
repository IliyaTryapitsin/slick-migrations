package slick.migration.table

import slick.migration.ast.TableInfo
import slick.migration.dialect.Dialect

import scala.slick.driver.JdbcProfile

/**
 * The concrete [[TableMigration]] class used when irreversible operations are to be performed
 * (such as dropping a table)
 */
final class IrreversibleTableMigration[T <: JdbcProfile#Table[_]] private[table](table: T,
                                                                                 override val tableInfo: TableInfo,
                                                                                 protected[table] val data: TableMigrationData)(implicit dialect: Dialect[_]) extends TableMigration[T](table) {
  type Self = IrreversibleTableMigration[T]

  protected def withData(d: TableMigrationData) = new IrreversibleTableMigration(table, tableInfo, d)
}
