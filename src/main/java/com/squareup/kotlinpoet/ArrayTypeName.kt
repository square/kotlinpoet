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
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.LinkedHashMap
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.ArrayType

class ArrayTypeName private constructor(
    val componentType: TypeName,
    annotations: List<AnnotationSpec> = ArrayList<AnnotationSpec>()) : TypeName(annotations) {

  override fun annotated(annotations: List<AnnotationSpec>): ArrayTypeName {
    return ArrayTypeName(componentType, this.annotations + annotations)
  }

  override fun withoutAnnotations(): TypeName {
    return ArrayTypeName(componentType)
  }

  @Throws(IOException::class)
  override fun abstractEmit(out: CodeWriter): CodeWriter {
    return out.emit("%T<%T>", ARRAY_TYPE, componentType)
  }

  companion object {
    private val ARRAY_TYPE = ClassName.get("kotlin", "Array")

    /** Returns an array type whose elements are all instances of `componentType`.  */
    @JvmStatic fun of(componentType: TypeName): ArrayTypeName {
      return ArrayTypeName(componentType)
    }

    /** Returns an array type whose elements are all instances of `componentType`.  */
    @JvmStatic fun of(componentType: Type): ArrayTypeName {
      return of(TypeName.get(componentType))
    }

    /** Returns an array type equivalent to `mirror`.  */
    @JvmStatic internal fun get(
        mirror: ArrayType,
        typeVariables: MutableMap<TypeParameterElement, TypeVariableName>
        = LinkedHashMap<TypeParameterElement, TypeVariableName>())
        : ArrayTypeName {
      return ArrayTypeName(TypeName.get(mirror.componentType, typeVariables))
    }

    /** Returns an array type equivalent to `type`.  */
    @JvmStatic internal fun get(
        type: GenericArrayType,
        map: MutableMap<Type, TypeVariableName> = LinkedHashMap<Type, TypeVariableName>())
        : ArrayTypeName {
      return ArrayTypeName.of(TypeName.get(type.genericComponentType, map = map))
    }
  }
}
