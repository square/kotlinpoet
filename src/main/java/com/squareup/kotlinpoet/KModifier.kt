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

enum class KModifier(vararg targets: Target) {
  // Modifier order defined here:
  // https://github.com/yole/kotlin-style-guide/issues/3.

  // Access.
  PUBLIC(Target.PROPERTY),
  PROTECTED(Target.PROPERTY),
  PRIVATE(Target.PROPERTY),
  INTERNAL(Target.PROPERTY),

  // Inheritance.
  FINAL(Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  OPEN(Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  ABSTRACT(Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  OVERRIDE(Target.FUNCTION, Target.PROPERTY),

  // Type declarations.
  INNER(Target.CLASS),
  ENUM(Target.CLASS),
  ANNOTATION(Target.CLASS),
  DATA(Target.CLASS),
  SEALED(Target.CLASS),
  // TODO: should COMPANION be a modifier? If so, it goes here.

  // Call-site compiler tips.
  INLINE(Target.FUNCTION),
  NOINLINE(Target.PARAMETER),
  CROSSINLINE(Target.PARAMETER),
  REIFIED(Target.TYPE_PARAMETER),

  // Call-site syntax.
  INFIX(Target.FUNCTION),
  OPERATOR(Target.FUNCTION),

  // Implementation details.
  LATEINIT(Target.PROPERTY),
  CONST(Target.PROPERTY),
  EXTERNAL(Target.FUNCTION),
  SUSPEND(Target.FUNCTION),
  TAILREC(Target.FUNCTION),

  // Type modifiers.
  IN(Target.VARIANCE_ANNOTATION),
  OUT(Target.VARIANCE_ANNOTATION),
  VARARG(Target.PARAMETER);

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
