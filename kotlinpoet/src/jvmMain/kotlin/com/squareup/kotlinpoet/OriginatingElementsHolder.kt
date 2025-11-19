/*
 * Copyright (C) 2019 Square, Inc.
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

import javax.lang.model.element.Element

/** A type that can have originating [elements][Element]. */
public interface OriginatingElementsHolder {

  /** The originating elements of this type. */
  public val originatingElements: List<Element>

  /** The builder analogue to [OriginatingElementsHolder] types. */
  @JvmDefaultWithCompatibility
  public interface Builder<out T : Builder<T>> {

    /** Mutable map of the current originating elements this builder contains. */
    public val originatingElements: MutableList<Element>

    /** Adds an [originatingElement] to this type's list of originating elements. */
    @Suppress("UNCHECKED_CAST")
    public fun addOriginatingElement(originatingElement: Element): T =
      apply { originatingElements += originatingElement } as T
  }
}

internal fun OriginatingElementsHolder.Builder<*>.buildOriginatingElements() =
  OriginatingElements(originatingElements.toImmutableList())

internal fun List<Element>.buildOriginatingElements() = OriginatingElements(toImmutableList())

@JvmInline
internal value class OriginatingElements(override val originatingElements: List<Element>) :
  OriginatingElementsHolder
