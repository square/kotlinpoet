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
    private val receiver: TypeName? = null,
    private val parameters: List<TypeName> = emptyList(),
    private val returnType: TypeName = UNIT,
    nullable: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations) {
  override fun asNullable() = LambdaTypeName(receiver, parameters, returnType, true, annotations)

  override fun asNonNullable()
      = LambdaTypeName(receiver, parameters, returnType, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>)
      = LambdaTypeName(receiver, parameters, returnType, nullable, annotations)

  override fun withoutAnnotations()
      = LambdaTypeName(receiver, parameters, returnType, nullable)

  override fun emit(out: CodeWriter): CodeWriter {
    emitAnnotations(out)
    if (nullable) {
      out.emit("(")
    }

    receiver?.let {
      out.emitCode("%T.", it)
    }

    val params = parameters.map { CodeBlock.of("%T", it) }.joinToCode()
    out.emitCode("(%L) -> %T", params, returnType)

    if (nullable) {
      out.emit(")")
    }
    return out
  }

  companion object {
    /** Returns a lambda type with `returnType` and parameters of listed in `parameters`. */
    @JvmStatic fun get(
        receiver: TypeName? = null,
        parameters: List<TypeName> = emptyList(),
        returnType: TypeName)
        = LambdaTypeName(receiver, parameters, returnType)

    /** Returns a lambda type with `returnType` and parameters of listed in `parameters`. */
    @JvmStatic fun get(
        receiver: TypeName? = null,
        vararg parameters: TypeName = emptyArray(),
        returnType: TypeName)
        = LambdaTypeName(receiver, parameters.toList(), returnType)
  }
}
