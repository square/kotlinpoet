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

enum class KModifier(
    internal val keyword: String,
    vararg targets: Target) {
  // Modifier order defined here:
  // https://github.com/yole/kotlin-style-guide/issues/3.

  // Access.
  PUBLIC("public", Target.PROPERTY),
  PROTECTED("protected", Target.PROPERTY),
  PRIVATE("private", Target.PROPERTY),
  INTERNAL("internal", Target.PROPERTY),

  // Inheritance.
  FINAL("final", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  OPEN("open", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  ABSTRACT("abstract", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  OVERRIDE("override", Target.FUNCTION, Target.PROPERTY),

  // Type declarations.
  INNER("inner", Target.CLASS),
  ENUM("enum", Target.CLASS),
  ANNOTATION("annotation", Target.CLASS),
  DATA("data", Target.CLASS),
  SEALED("sealed", Target.CLASS),
  // TODO: should COMPANION be a modifier? If so, it goes here.

  // Call-site compiler tips.
  INLINE("inline", Target.FUNCTION),
  NOINLINE("noinline", Target.PARAMETER),
  CROSSINLINE("crossinline", Target.PARAMETER),
  REIFIED("reified", Target.TYPE_PARAMETER),

  // Call-site syntax.
  INFIX("infix", Target.FUNCTION),
  OPERATOR("operator", Target.FUNCTION),

  // Implementation details.
  LATEINIT("lateinit", Target.PROPERTY),
  CONST("const", Target.PROPERTY),
  EXTERNAL("external", Target.FUNCTION),
  SUSPEND("suspend", Target.FUNCTION),
  TAILREC("tailrec", Target.FUNCTION),

  // Type modifiers.
  IN("in", Target.VARIANCE_ANNOTATION),
  OUT("out", Target.VARIANCE_ANNOTATION),
  VARARG("vararg", Target.PARAMETER),

  // Multiplatform modules.
  HEADER("header", Target.CLASS),
  IMPL("impl", Target.CLASS);

  internal val targets = targets.toList()

  internal enum class Target {
    CLASS,
    VARIANCE_ANNOTATION,
    PARAMETER,
    TYPE_PARAMETER,
    FUNCTION,
    PROPERTY,
  }

  internal fun checkTarget(target: Target) {
    require(targets.contains(target)) { "unexpected modifier $this for $target" }
  }
}
