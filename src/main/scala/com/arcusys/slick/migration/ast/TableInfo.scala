package com.arcusys.slick.migration.ast

/**
 * Internal lightweight data structure, containing
 * information for schema manipulation about a table per se
 * @param schemaName the name of the database schema (namespace) the table is in, if any
 * @param tableName the name of the table itself
 */
private[migration] case class TableInfo(schemaName: Option[String],
  tableName: String)
