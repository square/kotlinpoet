/*
 * Copyright (C) 2015 Square, Inc.
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
@file:JvmName("TypeVariableNames")

package com.squareup.kotlinpoet

import java.lang.reflect.Type
import java.util.Collections
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeVariable
import kotlin.reflect.KClass

class TypeVariableName private constructor(
  val name: String,
  val bounds: List<TypeName>,
  val variance: KModifier? = null,
  val reified: Boolean = false,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations) {

  override fun asNullable() = TypeVariableName(name, bounds, variance, reified, true, annotations)

  override fun asNonNullable() = TypeVariableName(name, bounds, variance, reified, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>) =
      TypeVariableName(name, bounds, variance, reified, nullable, annotations)

  override fun withoutAnnotations() = TypeVariableName(name, bounds, variance, reified, nullable)

  fun withBounds(vararg bounds: Type) = withBounds(bounds.map { it.asTypeName() })

  fun withBounds(vararg bounds: KClass<*>) = withBounds(bounds.map { it.asTypeName() })

  fun withBounds(vararg bounds: TypeName) = withBounds(bounds.toList())

  fun withBounds(bounds: List<TypeName>) =
      TypeVariableName(name, this.bounds + bounds, variance, reified, nullable, annotations)

  fun reified(value: Boolean = true) =
      TypeVariableName(name, bounds, variance, value, nullable, annotations)

  override fun emit(out: CodeWriter) = out.emit(name)

  companion object {
    internal fun of(name: String, bounds: List<TypeName>, variance: KModifier?): TypeVariableName {
      require(variance == null || variance.isOneOf(KModifier.IN, KModifier.OUT)) {
        "$variance is an invalid variance modifier, the only allowed values are in and out!"
      }
      // Strip java.lang.Object from bounds if it is present.
      return TypeVariableName(name, bounds.filter { it != ANY }, variance)
    }

    /** Returns type variable named `name` with `variance` and without bounds.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, variance: KModifier? = null) =
        TypeVariableName.of(name, emptyList(), variance)

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, vararg bounds: TypeName, variance: KModifier? = null) =
        TypeVariableName.of(name, bounds.toList(), variance)

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, vararg bounds: KClass<*>, variance: KModifier? = null) =
        TypeVariableName.of(name, bounds.map { it.asTypeName() }, variance)

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, vararg bounds: Type, variance: KModifier? = null) =
        TypeVariableName.of(name, bounds.map { it.asTypeName() }, variance)

    /**
     * Make a TypeVariableName for the given TypeMirror. This form is used internally to avoid
     * infinite recursion in cases like `Enum<E extends Enum<E>>`. When we encounter such a
     * thing, we will make a TypeVariableName without bounds and add that to the `typeVariables`
     * map before looking up the bounds. Then if we encounter this TypeVariable again while
     * constructing the bounds, we can just return it from the map. And, the code that put the entry
     * in `variables` will make sure that the bounds are filled in before returning.
     */
    internal fun get(
      mirror: TypeVariable,
      typeVariables: MutableMap<TypeParameterElement, TypeVariableName>
    ): TypeVariableName {
      val element = mirror.asElement() as TypeParameterElement
      var typeVariableName: TypeVariableName? = typeVariables[element]
      if (typeVariableName == null) {
        // Since the bounds field is public, we need to make it an unmodifiableList. But we control
        // the List that that wraps, which means we can change it before returning.
        val bounds = mutableListOf<TypeName>()
        val visibleBounds = Collections.unmodifiableList(bounds)
        typeVariableName = TypeVariableName(element.simpleName.toString(), visibleBounds)
        typeVariables.put(element, typeVariableName)
        for (typeMirror in element.bounds) {
          bounds += TypeName.get(typeMirror, typeVariables)
        }
        bounds.remove(ANY)
      }
      return typeVariableName
    }

    /** Returns type variable equivalent to `type`.  */
    internal fun get(
      type: java.lang.reflect.TypeVariable<*>,
      map: MutableMap<Type, TypeVariableName> = mutableMapOf()
    ): TypeVariableName {
      var result: TypeVariableName? = map[type]
      if (result == null) {
        val bounds = mutableListOf<TypeName>()
        val visibleBounds = Collections.unmodifiableList(bounds)
        result = TypeVariableName(type.name, visibleBounds)
        map.put(type, result)
        for (bound in type.bounds) {
          bounds += TypeName.get(bound, map)
        }
        bounds.remove(ANY)
      }
      return result
    }
  }
}

/** Returns type variable equivalent to `mirror`.  */
@JvmName("get")
fun TypeVariable.asTypeVariableName()
    = (asElement() as TypeParameterElement).asTypeVariableName()

/** Returns type variable equivalent to `element`.  */
@JvmName("get")
fun TypeParameterElement.asTypeVariableName(): TypeVariableName {
  val name = simpleName.toString()
  val boundsTypeNames = bounds.map { it.asTypeName() }
  return TypeVariableName.of(name, boundsTypeNames, variance = null)
}
