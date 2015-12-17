package com.arcusys.slick.migration.dialect

import com.arcusys.slick.migration.ast.IndexInfo

/**
 * Created by Iliya Tryapitsin on 20/01/15.
 */
trait SimulatedRenameIndex {
  this: Dialect[_] =>
  override def renameIndex(old: IndexInfo, newName: String) =
    List(dropIndex(old), createIndex(old.copy(name = newName)))
}
