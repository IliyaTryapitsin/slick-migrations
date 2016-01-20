package slick.migration

/**
 * Created by Iliya Tryapitsin on 28/01/15.
 */
object CanConcatMigrations extends CanConcatMigrationsLow {
  implicit def reversible[A <: ReversibleMigration, B <: ReversibleMigration]: CanConcatMigrations[A, B, ReversibleMigrationSeq] = new CanConcatMigrations({
    case (rms: ReversibleMigrationSeq, b) => new ReversibleMigrationSeq(rms.migrations :+ b: _*)
    case (a, b)                           => new ReversibleMigrationSeq(a, b)
  })
}

/**
 * A typeclass to determine the best way to combine migrations,
 * either into a [[ReversibleMigrationSeq]] or just a [[MigrationSeq]].
 * Used when you call '&' on [[Migration]]s.
 * Note that the migrations will be flattened; you will not end up with
 * something like `MigrationSeq(MigrationSeq(MigrationSeq(migA, migB), migC), migD)`.
 */
class CanConcatMigrations[-A, -B, +C](val f: (A, B) => C)