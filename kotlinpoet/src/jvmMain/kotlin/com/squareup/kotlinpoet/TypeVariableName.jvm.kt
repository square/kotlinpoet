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
@file:JvmName("TypeVariableNames")
@file:JvmMultifileClass

package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.jvm.alias.JvmType
import java.util.Collections
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable

private val JAVA_OBJECT = ClassName("java.lang", "Object")

/**
 * Make a TypeVariableName for the given TypeMirror. This form is used internally to avoid
 * infinite recursion in cases like `Enum<E extends Enum<E>>`. When we encounter such a
 * thing, we will make a TypeVariableName without bounds and add that to the `typeVariables`
 * map before looking up the bounds. Then if we encounter this TypeVariable again while
 * constructing the bounds, we can just return it from the map. And, the code that put the entry
 * in `variables` will make sure that the bounds are filled in before returning.
 */
internal fun TypeVariableName.Companion.get(
  mirror: javax.lang.model.type.TypeVariable,
  typeVariables: MutableMap<TypeParameterElement, TypeVariableName>,
): TypeVariableName {
  val element = mirror.asElement() as TypeParameterElement
  var typeVariableName: TypeVariableName? = typeVariables[element]
  if (typeVariableName == null) {
    // Since the bounds field is public, we need to make it an unmodifiableList. But we control
    // the List that that wraps, which means we can change it before returning.
    val bounds = mutableListOf<TypeName>()
    val visibleBounds = Collections.unmodifiableList(bounds)
    typeVariableName = TypeVariableName(element.simpleName.toString(), visibleBounds)
    typeVariables[element] = typeVariableName
    for (typeMirror in element.bounds) {
      bounds += TypeName.get(typeMirror, typeVariables)
    }
    bounds.remove(ANY)
    bounds.remove(JAVA_OBJECT)
    if (bounds.isEmpty()) {
      bounds.add(NULLABLE_ANY)
    }
  }
  return typeVariableName
}

/** Returns type variable equivalent to `type`.  */
internal fun TypeVariableName.Companion.get(
  type: java.lang.reflect.TypeVariable<*>,
  map: MutableMap<JvmType, TypeVariableName> = mutableMapOf(),
): TypeVariableName {
  var result: TypeVariableName? = map[type]
  if (result == null) {
    val bounds = mutableListOf<TypeName>()
    val visibleBounds = Collections.unmodifiableList(bounds)
    result = TypeVariableName(type.name, visibleBounds)
    map[type] = result
    for (bound in type.bounds) {
      bounds += TypeName.get(bound, map)
    }
    bounds.remove(ANY)
    bounds.remove(JAVA_OBJECT)
    if (bounds.isEmpty()) {
      bounds.add(NULLABLE_ANY)
    }
  }
  return result
}

/** Returns type variable equivalent to `mirror`. */
@DelicateKotlinPoetApi(
  message = "Java reflection APIs don't give complete information on Kotlin types. Consider using" +
    " the kotlinpoet-metadata APIs instead.",
)
@JvmName("get")
public fun TypeVariable.asTypeVariableName(): TypeVariableName =
  (asElement() as TypeParameterElement).asTypeVariableName()

/** Returns type variable equivalent to `element`. */
@DelicateKotlinPoetApi(
  message = "Element APIs don't give complete information on Kotlin types. Consider using" +
    " the kotlinpoet-metadata APIs instead.",
)
@JvmName("get")
public fun TypeParameterElement.asTypeVariableName(): TypeVariableName {
  val name = simpleName.toString()
  val boundsTypeNames = bounds.map(TypeMirror::asTypeName)
    .ifEmpty(TypeVariableName.Companion::NULLABLE_ANY_LIST)
  return TypeVariableName.of(name, boundsTypeNames, variance = null)
}
