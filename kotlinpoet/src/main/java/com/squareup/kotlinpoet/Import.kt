package com.squareup.kotlinpoet

public data class Import internal constructor(
  val qualifiedName: String,
  val alias: String? = null
) : Comparable<Import> {

  private val importString = buildString {
    append(qualifiedName.escapeSegmentsIfNecessary())
    if (alias != null) {
      append(" as ${alias.escapeIfNecessary()}")
    }
  }

  override fun toString(): String = importString

  override fun compareTo(other: Import): Int = importString.compareTo(other.importString)
}
