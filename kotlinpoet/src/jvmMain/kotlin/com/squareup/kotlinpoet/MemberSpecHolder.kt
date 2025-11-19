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

import java.lang.reflect.Type
import kotlin.reflect.KClass

/** A spec which can contain [PropertySpec]s and [FunSpec]s. */
public interface MemberSpecHolder {
  public val propertySpecs: List<PropertySpec>
  public val funSpecs: List<FunSpec>

  public interface Builder<out T : Builder<T>> {
    @Suppress("UNCHECKED_CAST")
    public fun addProperties(propertySpecs: Iterable<PropertySpec>): T =
      apply { propertySpecs.map(::addProperty) } as T

    public fun addProperty(propertySpec: PropertySpec): T

    public fun addProperty(name: String, type: TypeName, vararg modifiers: KModifier): T =
      addProperty(PropertySpec.builder(name, type, *modifiers).build())

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    public fun addProperty(name: String, type: Type, vararg modifiers: KModifier): T =
      addProperty(name, type.asTypeName(), *modifiers)

    public fun addProperty(name: String, type: KClass<*>, vararg modifiers: KModifier): T =
      addProperty(name, type.asTypeName(), *modifiers)

    public fun addProperty(name: String, type: TypeName, modifiers: Iterable<KModifier>): T =
      addProperty(PropertySpec.builder(name, type, modifiers).build())

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    public fun addProperty(name: String, type: Type, modifiers: Iterable<KModifier>): T =
      addProperty(name, type.asTypeName(), modifiers)

    public fun addProperty(name: String, type: KClass<*>, modifiers: Iterable<KModifier>): T =
      addProperty(name, type.asTypeName(), modifiers)

    @Suppress("UNCHECKED_CAST")
    public fun addFunctions(funSpecs: Iterable<FunSpec>): T =
      apply { funSpecs.forEach(::addFunction) } as T

    public fun addFunction(funSpec: FunSpec): T
  }
}
