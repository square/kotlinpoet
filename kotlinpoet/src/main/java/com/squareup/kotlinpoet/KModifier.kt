package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PROTECTED
import com.squareup.kotlinpoet.KModifier.PUBLIC
import java.util.EnumSet

public enum class KModifier(
  internal val keyword: String,
  private vararg val targets: Target
) {
  // Modifier order defined here:
  // https://kotlinlang.org/docs/reference/coding-conventions.html#modifiers

  // Access.
  PUBLIC("public", Target.PROPERTY),
  PROTECTED("protected", Target.PROPERTY),
  PRIVATE("private", Target.PROPERTY),
  INTERNAL("internal", Target.PROPERTY),

  // Multiplatform modules.
  EXPECT("expect", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  ACTUAL("actual", Target.CLASS, Target.FUNCTION, Target.PROPERTY),

  FINAL("final", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  OPEN("open", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  ABSTRACT("abstract", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  SEALED("sealed", Target.CLASS),
  CONST("const", Target.PROPERTY),

  EXTERNAL("external", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  OVERRIDE("override", Target.FUNCTION, Target.PROPERTY),
  LATEINIT("lateinit", Target.PROPERTY),
  TAILREC("tailrec", Target.FUNCTION),
  VARARG("vararg", Target.PARAMETER),
  SUSPEND("suspend", Target.FUNCTION),
  INNER("inner", Target.CLASS),

  ENUM("enum", Target.CLASS),
  ANNOTATION("annotation", Target.CLASS),
  VALUE("value", Target.CLASS),
  FUN("fun", Target.INTERFACE),

  COMPANION("companion", Target.CLASS),

  // Call-site compiler tips.
  INLINE("inline", Target.FUNCTION),
  NOINLINE("noinline", Target.PARAMETER),
  CROSSINLINE("crossinline", Target.PARAMETER),
  REIFIED("reified", Target.TYPE_PARAMETER),

  INFIX("infix", Target.FUNCTION),
  OPERATOR("operator", Target.FUNCTION),

  DATA("data", Target.CLASS),

  IN("in", Target.VARIANCE_ANNOTATION),
  OUT("out", Target.VARIANCE_ANNOTATION),
  ;

  internal enum class Target {
    CLASS,
    VARIANCE_ANNOTATION,
    PARAMETER,
    TYPE_PARAMETER,
    FUNCTION,
    PROPERTY,
    INTERFACE,
  }

  internal fun checkTarget(target: Target) {
    require(target in targets) { "unexpected modifier $this for $target" }
  }
}

internal val VISIBILITY_MODIFIERS: Set<KModifier> = EnumSet.of(PUBLIC, INTERNAL, PROTECTED, PRIVATE)
