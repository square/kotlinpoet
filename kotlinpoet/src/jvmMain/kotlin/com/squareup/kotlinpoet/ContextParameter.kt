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

/**
 * Represents a context parameter with a name and type.
 *
 * To create a new [ContextParameter], use the [ContextParameter] factory
 * function or [Builder] in case of Java.
 */
public sealed interface ContextParameter {
  public val name: String
  public val type: TypeName

  public fun toBuilder(
    name: String = this.name,
    type: TypeName = this.type,
  ): Builder = Builder()
    .setName(name)
    .setType(type)

  public class Builder internal constructor() {
    @set:JvmSynthetic
    public var name: String? = null

    @set:JvmSynthetic
    public var type: TypeName? = null

    public fun setName(name: String): Builder = apply { this.name = name }
    public fun setType(type: TypeName): Builder = apply { this.type = type }

    public fun build(): ContextParameter {
      val errors = buildList {
        if (name == null) add("name was not set")
        if (name?.isBlank() == true) add("name is blank")
        if (type == null) add("type was not set")
      }
      if (errors.isNotEmpty()) {
        throw IllegalArgumentException(errors.joinToString(", "))
      }
      return DefaultContextParameter(
        name = name!!,
        type = type!!,
      )
    }
  }

  private companion object {
    @JvmStatic fun builder(): Builder = Builder()
  }
}

/**
 * Creates a new [ContextParameter] with the given [name] and [type].
 */
@JvmSynthetic
public fun ContextParameter(
  name: String,
  type: TypeName,
): ContextParameter = DefaultContextParameter(name, type)

/**
 * A default implementation of [ContextParameter].
 */
private data class DefaultContextParameter(
  override val name: String,
  override val type: TypeName,
) : ContextParameter {
  override fun toString(): String = "$name: $type"
}

/**
 * A KotlinPoet spec type that can have context parameters.
 */
public interface ContextParameterizable {
  /**
   * The context parameters of this type.
   */
  @ExperimentalKotlinPoetApi
  public val contextParameters: List<ContextParameter>

  /**
   * The builder analogue to [ContextParameterizable] types.
   */
  public interface Builder<out T : Builder<T>> {
    /**
     * Mutable list of the current context parameters this builder contains.
     */
    @ExperimentalKotlinPoetApi
    public val contextParameters: MutableList<ContextParameter>

    /**
     * Adds the given [parameters] to this type's list of context parameters.
     */
    @Suppress("UNCHECKED_CAST")
    @ExperimentalKotlinPoetApi
    public fun contextParameters(parameters: Iterable<ContextParameter>): T = apply {
      contextParameters += parameters
    } as T

    /**
     * Adds a context parameter with the given [name] and [type] to this type's list of context parameters.
     */
    @ExperimentalKotlinPoetApi
    public fun contextParameter(name: String, type: TypeName): T =
      contextParameters(listOf(ContextParameter(name, type)))

    /**
     * Adds a context parameter with the given [name] and [type] to this type's list of context parameters.
     */
    @DelicateKotlinPoetApi(
      message = "Java reflection APIs don't give complete information on Kotlin types. Consider " +
        "using the kotlinpoet-metadata APIs instead.",
    )
    @ExperimentalKotlinPoetApi
    public fun contextParameter(name: String, type: Type): T =
      contextParameter(name, type.asTypeName())

    /**
     * Adds a context parameter with the given [name] and [type] to this type's list of context parameters.
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
internal value class ContextParameters(
  override val contextParameters: List<ContextParameter>,
) : ContextParameterizable
