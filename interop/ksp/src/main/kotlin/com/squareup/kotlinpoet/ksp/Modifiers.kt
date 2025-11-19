/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet.ksp

import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Modifier.ABSTRACT
import com.google.devtools.ksp.symbol.Modifier.ACTUAL
import com.google.devtools.ksp.symbol.Modifier.ANNOTATION
import com.google.devtools.ksp.symbol.Modifier.CROSSINLINE
import com.google.devtools.ksp.symbol.Modifier.DATA
import com.google.devtools.ksp.symbol.Modifier.ENUM
import com.google.devtools.ksp.symbol.Modifier.EXPECT
import com.google.devtools.ksp.symbol.Modifier.EXTERNAL
import com.google.devtools.ksp.symbol.Modifier.FINAL
import com.google.devtools.ksp.symbol.Modifier.FUN
import com.google.devtools.ksp.symbol.Modifier.IN
import com.google.devtools.ksp.symbol.Modifier.INFIX
import com.google.devtools.ksp.symbol.Modifier.INLINE
import com.google.devtools.ksp.symbol.Modifier.INNER
import com.google.devtools.ksp.symbol.Modifier.INTERNAL
import com.google.devtools.ksp.symbol.Modifier.LATEINIT
import com.google.devtools.ksp.symbol.Modifier.NOINLINE
import com.google.devtools.ksp.symbol.Modifier.OPEN
import com.google.devtools.ksp.symbol.Modifier.OPERATOR
import com.google.devtools.ksp.symbol.Modifier.OUT
import com.google.devtools.ksp.symbol.Modifier.OVERRIDE
import com.google.devtools.ksp.symbol.Modifier.PRIVATE
import com.google.devtools.ksp.symbol.Modifier.PROTECTED
import com.google.devtools.ksp.symbol.Modifier.PUBLIC
import com.google.devtools.ksp.symbol.Modifier.REIFIED
import com.google.devtools.ksp.symbol.Modifier.SEALED
import com.google.devtools.ksp.symbol.Modifier.SUSPEND
import com.google.devtools.ksp.symbol.Modifier.TAILREC
import com.google.devtools.ksp.symbol.Modifier.VALUE
import com.google.devtools.ksp.symbol.Modifier.VARARG
import com.squareup.kotlinpoet.KModifier

/**
 * Returns the [KModifier] representation of this [Modifier] or null if this is a Java-only modifier
 * (i.e. prefixed with `JAVA_`), which do not have obvious [KModifier] analogues.
 */
public fun Modifier.toKModifier(): KModifier? {
  return when (this) {
    PUBLIC -> KModifier.PUBLIC
    PRIVATE -> KModifier.PRIVATE
    INTERNAL -> KModifier.INTERNAL
    PROTECTED -> KModifier.PROTECTED
    IN -> KModifier.IN
    OUT -> KModifier.OUT
    OVERRIDE -> KModifier.OVERRIDE
    LATEINIT -> KModifier.LATEINIT
    ENUM -> KModifier.ENUM
    SEALED -> KModifier.SEALED
    ANNOTATION -> KModifier.ANNOTATION
    DATA -> KModifier.DATA
    INNER -> KModifier.INNER
    FUN -> KModifier.FUN
    VALUE -> KModifier.VALUE
    SUSPEND -> KModifier.SUSPEND
    TAILREC -> KModifier.TAILREC
    OPERATOR -> KModifier.OPERATOR
    INFIX -> KModifier.INFIX
    INLINE -> KModifier.INLINE
    EXTERNAL -> KModifier.EXTERNAL
    ABSTRACT -> KModifier.ABSTRACT
    FINAL -> KModifier.FINAL
    OPEN -> KModifier.OPEN
    VARARG -> KModifier.VARARG
    NOINLINE -> KModifier.NOINLINE
    CROSSINLINE -> KModifier.CROSSINLINE
    REIFIED -> KModifier.REIFIED
    EXPECT -> KModifier.EXPECT
    ACTUAL -> KModifier.ACTUAL
    else -> null
  }
}
