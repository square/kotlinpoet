/*
 * Copyright (C) 2025 Square, Inc.
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

import java.lang.reflect.Type
import kotlin.reflect.KClass

/** Represents a context parameter with a name and type. */
public class ContextParameter(public val name: String, public val type: TypeName) {
  public constructor(type: TypeName) : this(name = "_", type)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ContextParameter

    if (name != other.name) return false
    if (type != other.type) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + type.hashCode()
    return result
  }

  override fun toString(): String = "$name: $type"
}

/** A KotlinPoet spec type that can have context parameters. */
public interface ContextParameterizable {
  /** The context parameters of this type. */
  @ExperimentalKotlinPoetApi public val contextParameters: List<ContextParameter>

  /** The builder analogue to [ContextParameterizable] types. */
  public interface Builder<out T : Builder<T>> {
    /** Mutable list of the current context parameters this builder contains. */
    @ExperimentalKotlinPoetApi public val contextParameters: MutableList<ContextParameter>

    /** Adds the given [parameters] to this type's list of context parameters. */
    @Suppress("UNCHECKED_CAST")
    @ExperimentalKotlinPoetApi
    public fun contextParameters(parameters: Iterable<ContextParameter>): T =
      apply { contextParameters += parameters } as T

    /**
     * Adds a context parameter with the given [name] and [type] to this type's list of context
     * parameters.
     */
    @ExperimentalKotlinPoetApi
    public fun contextParameter(name: String, type: TypeName): T =
      contextParameters(listOf(ContextParameter(name, type)))

    /**
     * Adds a context parameter with the name "_" and [type] to this type's list of context
     * parameters.
     */
    @ExperimentalKotlinPoetApi
    public fun contextParameter(type: TypeName): T =
      contextParameters(listOf(ContextParameter(type)))

    /** Adds the given [ContextParameter] to this type's list of context parameters. */
    @ExperimentalKotlinPoetApi
    public fun contextParameter(contextParameter: ContextParameter): T =
      contextParameters(listOf(contextParameter))

    /**
     * Adds a context parameter with the given [name] and [type] to this type's list of context
     * parameters.
     */
    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    @ExperimentalKotlinPoetApi
    public fun contextParameter(name: String, type: Type): T =
      contextParameter(name, type.asTypeName())

    /**
     * Adds a context parameter with the given [name] and [type] to this type's list of context
     * parameters.
     */
    @ExperimentalKotlinPoetApi
    public fun contextParameter(name: String, type: KClass<*>): T =
      contextParameter(name, type.asTypeName())
  }
}

@ExperimentalKotlinPoetApi
internal fun ContextParameterizable.Builder<*>.buildContextParameters() =
  ContextParameters(contextParameters.toImmutableList())

@JvmInline
@ExperimentalKotlinPoetApi
internal value class ContextParameters(override val contextParameters: List<ContextParameter>) :
  ContextParameterizable
