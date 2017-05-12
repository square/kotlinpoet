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
package com.squareup.kotlinpoet

import java.io.IOException
import java.lang.reflect.Type
import java.util.Collections
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeVariable
import kotlin.reflect.KClass

class TypeVariableName private constructor(
    val name: String,
    val bounds: List<TypeName>,
    annotations: List<AnnotationSpec> = emptyList())
  : TypeName(annotations) {

  override fun annotated(annotations: List<AnnotationSpec>): TypeVariableName {
    return TypeVariableName(name, bounds, annotations)
  }

  override fun withoutAnnotations(): TypeName {
    return TypeVariableName(name, bounds)
  }

  fun withBounds(vararg bounds: Type): TypeVariableName {
    return withBounds(bounds.map { get(it) })
  }

  fun withBounds(vararg bounds: KClass<*>): TypeVariableName {
    return withBounds(bounds.map { TypeName.get(it) })
  }

  fun withBounds(vararg bounds: TypeName): TypeVariableName {
    return withBounds(bounds.toList())
  }

  fun withBounds(bounds: List<TypeName>): TypeVariableName {
    return TypeVariableName(name, this.bounds + bounds, annotations)
  }

  @Throws(IOException::class)
  override fun abstractEmit(out: CodeWriter): CodeWriter {
    return out.emitAndIndent(name)
  }

  companion object {
    private fun of(name: String, bounds: List<TypeName>): TypeVariableName {
      // Strip java.lang.Object from bounds if it is present.
      return TypeVariableName(name, bounds.filter { it != ANY })
    }

    /** Returns type variable named `name` without bounds.  */
    @JvmStatic fun get(name: String): TypeVariableName {
      return TypeVariableName.of(name, emptyList<TypeName>())
    }

    /** Returns type variable named `name` with `bounds`.  */
    @JvmStatic fun get(name: String, vararg bounds: TypeName): TypeVariableName {
      return TypeVariableName.of(name, bounds.toList())
    }

    /** Returns type variable named `name` with `bounds`.  */
    @JvmStatic fun get(name: String, vararg bounds: KClass<*>): TypeVariableName {
      return TypeVariableName.of(name, bounds.map { TypeName.get(it) })
    }

    /** Returns type variable named `name` with `bounds`.  */
    @JvmStatic fun get(name: String, vararg bounds: Type): TypeVariableName {
      return TypeVariableName.of(name, bounds.map { get(it) })
    }

    /** Returns type variable equivalent to `mirror`.  */
    @JvmStatic fun get(mirror: TypeVariable): TypeVariableName {
      return get(mirror.asElement() as TypeParameterElement)
    }

    /**
     * Make a TypeVariableName for the given TypeMirror. This form is used internally to avoid
     * infinite recursion in cases like `Enum<E extends Enum<E>>`. When we encounter such a
     * thing, we will make a TypeVariableName without bounds and add that to the `typeVariables`
     * map before looking up the bounds. Then if we encounter this TypeVariable again while
     * constructing the bounds, we can just return it from the map. And, the code that put the entry
     * in `variables` will make sure that the bounds are filled in before returning.
     */
    @JvmStatic internal fun get(
        mirror: TypeVariable,
        typeVariables: MutableMap<TypeParameterElement, TypeVariableName>): TypeVariableName {
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
          bounds.add(TypeName.get(typeMirror, typeVariables))
        }
        bounds.remove(ANY)
      }
      return typeVariableName
    }

    /** Returns type variable equivalent to `element`.  */
    @JvmStatic fun get(element: TypeParameterElement): TypeVariableName {
      val name = element.simpleName.toString()
      val boundsMirrors = element.bounds

      val boundsTypeNames = mutableListOf<TypeName>()
      for (typeMirror in boundsMirrors) {
        boundsTypeNames.add(TypeName.get(typeMirror))
      }

      return TypeVariableName.of(name, boundsTypeNames)
    }

    /** Returns type variable equivalent to `type`.  */
    @JvmOverloads @JvmStatic internal fun get(
        type: java.lang.reflect.TypeVariable<*>,
        map: MutableMap<Type, TypeVariableName> = mutableMapOf())
        : TypeVariableName {
      var result: TypeVariableName? = map[type]
      if (result == null) {
        val bounds = mutableListOf<TypeName>()
        val visibleBounds = Collections.unmodifiableList(bounds)
        result = TypeVariableName(type.name, visibleBounds)
        map.put(type, result)
        for (bound in type.bounds) {
          bounds.add(TypeName.get(bound, map))
        }
        bounds.remove(ANY)
      }
      return result
    }
  }
}
