package com.arcusys.slick.migration

/**
 * Created by Iliya Tryapitsin on 28/01/15.
 */
//TODO mechanism other than exceptions?
case class MigrationException(message: String, cause: Throwable) extends RuntimeException(message, cause)
