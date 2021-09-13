package com.squareup.kotlinpoet.ksp

import com.google.devtools.ksp.symbol.Visibility
import com.google.devtools.ksp.symbol.Visibility.INTERNAL
import com.google.devtools.ksp.symbol.Visibility.JAVA_PACKAGE
import com.google.devtools.ksp.symbol.Visibility.LOCAL
import com.google.devtools.ksp.symbol.Visibility.PRIVATE
import com.google.devtools.ksp.symbol.Visibility.PROTECTED
import com.google.devtools.ksp.symbol.Visibility.PUBLIC
import com.squareup.kotlinpoet.KModifier

/**
 * Returns the [KModifier] representation of this visibility or null if this is [JAVA_PACKAGE]
 * or [LOCAL] (which do not have obvious [KModifier] alternatives).
 */
public fun Visibility.toKModifier(): KModifier? {
  return when (this) {
    PUBLIC -> KModifier.PUBLIC
    PRIVATE -> KModifier.PRIVATE
    PROTECTED -> KModifier.PROTECTED
    INTERNAL -> KModifier.INTERNAL
    else -> null
  }
}
