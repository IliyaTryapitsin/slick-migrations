package com.arcusys.slick.migration

/**
 * A [[Migration]] that can be reversed; that is,
 * it can provide a corresponding `Migration` that
 * will undo whatever this migration will do.
 */
trait ReversibleMigration extends Migration {
  def reverse: Migration
}
