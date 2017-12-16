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

class LambdaTypeName internal constructor(
  val receiver: TypeName? = null,
  parameters: List<ParameterSpec> = emptyList(),
  val returnType: TypeName = UNIT,
  nullable: Boolean = false,
  val suspending: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations) {
  val parameters = parameters.toImmutableList()

  init {
    for (param in parameters) {
      require(param.annotations.isEmpty()) { "Parameters with annotations are not allowed" }
      require(param.modifiers.isEmpty()) { "Parameters with modifiers are not allowed" }
      require(param.defaultValue == null) { "Parameters with default values are not allowed" }
    }
  }

  override fun asNullable() = LambdaTypeName(receiver, parameters, returnType, true, suspending,
      annotations)

  override fun asNonNullable()
      = LambdaTypeName(receiver, parameters, returnType, false, suspending, annotations)

  fun asSuspending() = LambdaTypeName(receiver, parameters, returnType, nullable, true, annotations)

  fun asNonSuspending() =
      LambdaTypeName(receiver, parameters, returnType, nullable, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>)
      = LambdaTypeName(receiver, parameters, returnType, nullable, suspending, annotations)

  override fun withoutAnnotations()
      = LambdaTypeName(receiver, parameters, returnType, nullable, suspending)

  override fun emit(out: CodeWriter): CodeWriter {
    emitAnnotations(out)

    if (nullable) {
      out.emit("(")
    }

    if (suspending) {
      out.emit("suspend ")
    }

    receiver?.let {
      out.emitCode("%T.", it)
    }

    parameters.emit(out)
    out.emitCode(" -> %T", returnType)

    if (nullable) {
      out.emit(")")
    }
    return out
  }

  companion object {
    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
      receiver: TypeName? = null,
      parameters: List<ParameterSpec> = emptyList(),
      returnType: TypeName
    ) = LambdaTypeName(receiver, parameters, returnType)

    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
      receiver: TypeName? = null,
      vararg parameters: TypeName = emptyArray(),
      returnType: TypeName
    ): LambdaTypeName {
      return LambdaTypeName(
          receiver,
          parameters.toList().map { ParameterSpec.unnamed(it) },
          returnType)
    }

    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
      receiver: TypeName? = null,
      vararg parameters: ParameterSpec = emptyArray(),
      returnType: TypeName
    ) = LambdaTypeName(receiver, parameters.toList(), returnType)
  }
}
