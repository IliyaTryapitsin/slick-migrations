package com.arcusys.slick.migration.ast

import scala.slick.ast.FieldSymbol

/**
 * Internal lightweight data structure, containing
 * information for schema manipulation about an index
 * @param table The Slick table object
 * @param name The name of the index
 * @param unique Whether the column can contain duplicates
 * @param columns The columns that this index applies to, as `scala.slick.ast.FieldSymbol`
 */
private[migration] case class IndexInfo(table: TableInfo, name: String, unique: Boolean = false, columns: Seq[FieldSymbol] = Seq())
