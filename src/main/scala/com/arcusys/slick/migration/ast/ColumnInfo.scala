package com.arcusys.slick.migration.ast

/**
 * Internal lightweight data structure, containing
 * information for schema manipulation about a column
 * @param name the column name
 * @param sqlType the column's data type, in the database's SQL dialect
 * @param notNull `true` for `NOT NULL`, `false` for `NULL` (nullable column).
 * @param autoInc `true` if the column is AUTOINCREMENT or equivalent. Corresponds to `O.AutoInc`
 * @param isPk `true` if the column should be declared a primary key inline with the column (in the column list).
 *             Corresponds to `O.PrimaryKey`
 * @param default An `Option`al default value, in the database's SQL syntax.
 *                Corresponds to `O.Default`
 */
private[migration] case class ColumnInfo(
  name: String,
  sqlType: Option[String] = None,
  notNull: Boolean = false,
  autoInc: Boolean = false,
  isPk: Boolean = false,
  default: Option[String] = None)
