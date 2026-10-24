/*
 * Copyright (C) 2022 Square, Inc.
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

/** A KotlinPoet spec type that can have a context receiver. */
@Deprecated(
  message =
    "Context receivers have been superseded by context parameters. This interface will be removed in KotlinPoet 3.0."
)
public interface ContextReceivable {

  /** The originating elements of this type. */
  @Deprecated(
    message =
      "Context receivers have been superseded by context parameters. Use ContextParameterizable.contextParameters instead. This property will be removed in KotlinPoet 3.0."
  )
  @ExperimentalKotlinPoetApi
  public val contextReceiverTypes: List<TypeName>

  /** The builder analogue to [ContextReceivable] types. */
  @Deprecated(
    message =
      "Context receivers have been superseded by context parameters. This interface will be removed in KotlinPoet 3.0."
  )
  @Suppress("DEPRECATION")
  public interface Builder<out T : Builder<T>> {

    /** Mutable map of the current originating elements this builder contains. */
    @Deprecated(
      message =
        "Context receivers have been superseded by context parameters. Use ContextParameterizable.Builder.contextParameters instead. This property will be removed in KotlinPoet 3.0."
    )
    @ExperimentalKotlinPoetApi
    public val contextReceiverTypes: MutableList<TypeName>

    /** Adds the given [receiverTypes] to this type's list of originating elements. */
    @Deprecated(
      message =
        "Context receivers have been superseded by context parameters. Use contextParameters() instead. This function will be removed in KotlinPoet 3.0."
    )
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    @ExperimentalKotlinPoetApi
    public fun contextReceivers(receiverTypes: Iterable<TypeName>): T =
      apply { contextReceiverTypes += receiverTypes } as T

    /** Adds the given [receiverTypes] to this type's list of originating elements. */
    @Deprecated(
      message =
        "Context receivers have been superseded by context parameters. Use contextParameters() instead. This function will be removed in KotlinPoet 3.0."
    )
    @Suppress("DEPRECATION")
    @ExperimentalKotlinPoetApi
    public fun contextReceivers(vararg receiverTypes: TypeName): T =
      contextReceivers(receiverTypes.toList())
  }
}

@Suppress("DEPRECATION")
@ExperimentalKotlinPoetApi
internal fun ContextReceivable.Builder<*>.buildContextReceivers() =
  ContextReceivers(contextReceiverTypes.toImmutableList())

@Suppress("DEPRECATION")
@JvmInline
@ExperimentalKotlinPoetApi
internal value class ContextReceivers(
  @Deprecated(
    message =
      "Context receivers have been superseded by context parameters. Use ContextParameterizable.contextParameters instead. This property will be removed in KotlinPoet 3.0."
  )
  override val contextReceiverTypes: List<TypeName>
) : ContextReceivable
