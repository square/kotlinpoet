/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.KModifier.PUBLIC
import kotlin.reflect.KClass

/** A companion block that can be added to a type. */
@ExperimentalKotlinPoetApi
public class CompanionBlockSpec
private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap(),
) : Taggable by tagMap, Documentable, MemberSpecHolder {

  override val kdoc: CodeBlock = builder.kdoc.build()
  override val propertySpecs: List<PropertySpec> = builder.propertySpecs.toImmutableList()
  override val funSpecs: List<FunSpec> = builder.funSpecs.toImmutableList()

  internal fun emit(codeWriter: CodeWriter) {
    codeWriter.emitKdoc(kdoc)
    codeWriter.emit("companion {\n")
    codeWriter.indent()

    for (propertySpec in propertySpecs) {
      propertySpec.emit(codeWriter, implicitModifiers = setOf(PUBLIC))
    }
    if (propertySpecs.isNotEmpty() && funSpecs.isNotEmpty()) codeWriter.emit("\n")
    for (funSpec in funSpecs) {
      funSpec.emit(codeWriter, enclosingName = null, implicitModifiers = setOf(PUBLIC))
    }

    codeWriter.unindent()
    codeWriter.emit("}\n")
  }

  public fun toBuilder(): Builder {
    val builder = Builder()
    builder.tags += tagMap.tags
    builder.kdoc.add(kdoc)
    builder.propertySpecs += propertySpecs
    builder.funSpecs += funSpecs
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int = toString().hashCode()

  override fun toString(): String = buildCodeString { emit(this) }

  public class Builder internal constructor() :
    Taggable.Builder<Builder>, Documentable.Builder<Builder>, MemberSpecHolder.Builder<Builder> {

    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()
    override val kdoc: CodeBlock.Builder = CodeBlock.builder()
    public val propertySpecs: MutableList<PropertySpec> = mutableListOf()
    public val funSpecs: MutableList<FunSpec> = mutableListOf()

    override fun addProperty(propertySpec: PropertySpec): Builder = apply {
      propertySpecs += propertySpec
    }

    override fun addFunction(funSpec: FunSpec): Builder = apply {
      funSpecs += funSpec
    }

    public fun build(): CompanionBlockSpec = CompanionBlockSpec(this)
  }
}
