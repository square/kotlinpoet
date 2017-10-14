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

import java.lang.reflect.Type
import kotlin.reflect.KClass

class LambdaSpec private constructor(builder: Builder) {
  val parameters = builder.parameters.toImmutableList()
  val body = builder.body.build()

  fun toCodeBlock() = with(CodeBlock.Builder()) {
    add("{")
    if (parameters.isNotEmpty()) {
      add(parameters.joinToCode(prefix = " ", separator = ", ", suffix = " ->\n%>"))
      add(body)
      add("\n%<")
    } else if (body.isNotEmpty()) {
      add(" ")
      add(body)
      add(" ")
    }
    add("}")
    build()
  }

  internal fun emit(codeWriter: CodeWriter) =
      codeWriter.emitCode(toCodeBlock())

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString { emit(CodeWriter(this)) }

  fun toBuilder(): Builder {
    val builder = Builder()
    builder.parameters += parameters
    builder.body.add(body)
    return builder
  }

  class Builder internal constructor() {
    internal val parameters = mutableListOf<CodeBlock>()
    internal val body = CodeBlock.builder()

    fun addParameter(name: String, type: KClass<*>) =
        addParameter(name, type.asTypeName())

    fun addParameter(name: String, type: Type) =
        addParameter(name, type.asTypeName())

    fun addParameter(name: String, type: TypeName) = apply {
      parameters += CodeBlock.of("%N: %T", name, type)
    }

    fun addBody(format: String, vararg args: Any) = apply {
      addBody(CodeBlock.of(format, *args))
    }

    fun addBody(codeBlock: CodeBlock) = apply {
      body.add(codeBlock)
    }

    fun build(): LambdaSpec {
      check(parameters.isEmpty() || body.isNotEmpty()) {
        "a lambda expression with parameters must have a body!"
      }
      return LambdaSpec(this)
    }
  }

  companion object {

    @JvmStatic fun builder() = Builder()

    @JvmStatic fun builder(bodyFormat: String, vararg bodyArgs: Any) =
        builder(CodeBlock.of(bodyFormat, bodyArgs))

    @JvmStatic fun builder(codeBlock: CodeBlock) = Builder().apply {
      body.add(codeBlock)
    }
  }
}
