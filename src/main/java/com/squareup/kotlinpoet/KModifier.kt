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
  // TODO: sort these in source code order, fix targets.
  PRIVATE(Target.PROPERTY),
  PROTECTED(Target.PROPERTY),
  PUBLIC(Target.PROPERTY),
  INTERNAL(Target.PROPERTY),
  ABSTRACT(Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  ANNOTATION(Target.CLASS),
  CONST(Target.PROPERTY),
  CROSSINLINE(Target.PARAMETER),
  DATA(Target.CLASS),
  ENUM(Target.CLASS),
  EXTERNAL(Target.FUNCTION),
  FINAL(Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  IN(Target.VARIANCE_ANNOTATION),
  INFIX(Target.FUNCTION),
  INLINE(Target.FUNCTION),
  LATEINIT(Target.PROPERTY),
  NOINLINE(Target.PARAMETER),
  OPEN(Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  OPERATOR(Target.FUNCTION),
  OUT(Target.VARIANCE_ANNOTATION),
  OVERRIDE(Target.FUNCTION, Target.PROPERTY),
  REIFIED(Target.TYPE_PARAMETER),
  SEALED(Target.CLASS),
  SUSPEND(Target.FUNCTION),
  TAILREC(Target.FUNCTION),
  VARARG(Target.PARAMETER);

  val targets = targets.toList()

  enum class Target {
    CLASS,
    VARIANCE_ANNOTATION,
    PARAMETER,
    TYPE_PARAMETER,
    FUNCTION,
    PROPERTY,
  }

  fun checkTarget(target: Target) {
    require(targets.contains(target)) { "unexpected modifier $this for $target" }
  }
}
