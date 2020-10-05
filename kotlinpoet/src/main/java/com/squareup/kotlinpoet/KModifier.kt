/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.KModifier.Target.ACCESSOR
import com.squareup.kotlinpoet.KModifier.Target.CLASS
import com.squareup.kotlinpoet.KModifier.Target.CLASS_TYPE_PARAMETER
import com.squareup.kotlinpoet.KModifier.Target.FUNCTION
import com.squareup.kotlinpoet.KModifier.Target.FUNCTION_TYPE_PARAMETER
import com.squareup.kotlinpoet.KModifier.Target.INTERFACE
import com.squareup.kotlinpoet.KModifier.Target.OBJECT
import com.squareup.kotlinpoet.KModifier.Target.PARAMETER
import com.squareup.kotlinpoet.KModifier.Target.PROPERTY
import com.squareup.kotlinpoet.KModifier.Target.TYPE_ALIAS

public enum class KModifier(
  internal val keyword: String,
  private vararg val targets: Target
) {
  // Modifier order defined here:
  // https://kotlinlang.org/docs/reference/coding-conventions.html#modifiers

  // Access.
  PUBLIC("public", *Target.declarations, TYPE_ALIAS, PARAMETER),
  PROTECTED("protected", *Target.declarations, PARAMETER),
  PRIVATE("private", *Target.declarations, TYPE_ALIAS, PARAMETER),
  INTERNAL("internal", *Target.declarations, TYPE_ALIAS, PARAMETER),

  // Multiplatform modules.
  EXPECT("expect", *Target.declarations),
  ACTUAL("actual", *Target.declarations, TYPE_ALIAS),

  FINAL("final", CLASS, FUNCTION, PROPERTY, PARAMETER),
  OPEN("open", CLASS, FUNCTION, PROPERTY, PARAMETER),
  ABSTRACT("abstract", CLASS, FUNCTION, PROPERTY, PARAMETER),
  SEALED("sealed", CLASS),
  CONST("const", PROPERTY),

  EXTERNAL("external", *Target.declarations),
  OVERRIDE("override", FUNCTION, PROPERTY, PARAMETER),
  LATEINIT("lateinit", PROPERTY),
  TAILREC("tailrec", FUNCTION),
  VARARG("vararg", PARAMETER),
  SUSPEND("suspend", FUNCTION),
  INNER("inner", CLASS),

  ENUM("enum", CLASS),
  ANNOTATION("annotation", CLASS),
  FUN("fun", INTERFACE),

  COMPANION("companion", OBJECT),

  // Call-site compiler tips.
  INLINE("inline", FUNCTION, CLASS, PROPERTY, ACCESSOR),
  NOINLINE("noinline", PARAMETER),
  CROSSINLINE("crossinline", PARAMETER),
  REIFIED("reified", FUNCTION_TYPE_PARAMETER),

  INFIX("infix", FUNCTION),
  OPERATOR("operator", FUNCTION),

  DATA("data", CLASS),

  IN("in", CLASS_TYPE_PARAMETER),
  OUT("out", CLASS_TYPE_PARAMETER),
  ;

  internal enum class Target {
    CLASS,
    OBJECT,
    INTERFACE,
    TYPE_ALIAS,

    FUNCTION,
    CONSTRUCTOR,
    ACCESSOR,

    PROPERTY,

    PARAMETER,
    FUNCTION_TYPE_PARAMETER,
    CLASS_TYPE_PARAMETER,
    ;

    companion object {
      val declarations = arrayOf(CLASS, OBJECT, INTERFACE, FUNCTION, CONSTRUCTOR, ACCESSOR, PROPERTY)
    }
  }

  internal fun checkTarget(target: Target) {
    require(target in targets) { "unexpected modifier $this for $target" }
  }
}

internal fun Iterable<KModifier>.checkTarget(target: KModifier.Target) {
  this.forEach { it.checkTarget(target) }
}
