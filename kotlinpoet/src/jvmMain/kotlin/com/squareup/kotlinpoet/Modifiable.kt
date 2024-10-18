/*
 * Copyright (C) 2024 Square, Inc.
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

/** A spec that has a set of [KModifier]s attached to it. */
public interface Modifiable {
  public val modifiers: Set<KModifier>

  public interface Builder<out T : Builder<T>> {
    public val modifiers: MutableSet<KModifier>

    /**
     * Add one or multiple modifiers to this spec.
     *
     * Note that not all [KModifier]s can be applied to a specific spec, and specs may or may not
     * validate modifiers at runtime. Consult Kotlin documentation on which modifiers are allowed on
     * specific Kotlin constructs.
     */
    @Suppress("UNCHECKED_CAST")
    public fun addModifiers(vararg modifiers: KModifier): T = apply {
      this.modifiers += modifiers
    } as T

    /**
     * Add a collection of modifiers to this spec.
     *
     * Note that not all [KModifier]s can be applied to a specific spec, and specs may or may not
     * validate modifiers at runtime. Consult Kotlin documentation on which modifiers are allowed on
     * specific Kotlin constructs.
     */
    @Suppress("UNCHECKED_CAST")
    public fun addModifiers(modifiers: Iterable<KModifier>): T = apply {
      this.modifiers += modifiers
    } as T
  }
}
