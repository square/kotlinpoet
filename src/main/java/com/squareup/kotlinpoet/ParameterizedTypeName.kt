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
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedHashMap
import kotlin.reflect.KClass

class ParameterizedTypeName internal constructor(
    private val enclosingType: ParameterizedTypeName?,
    val rawType: ClassName,
    typeArguments: List<TypeName>,
    annotations: List<AnnotationSpec> = ArrayList<AnnotationSpec>())
  : TypeName(annotations) {

  val typeArguments: List<TypeName> = Util.immutableList(typeArguments)

  init {
    require(!this.typeArguments.isEmpty() || enclosingType != null) {
      "no type arguments: $rawType"
    }
  }

  override fun annotated(annotations: List<AnnotationSpec>)
      = ParameterizedTypeName(enclosingType, rawType, typeArguments, this.annotations + annotations)

  override fun withoutAnnotations()
      = ParameterizedTypeName(enclosingType, rawType, typeArguments, ArrayList<AnnotationSpec>())

  @Throws(IOException::class)
  override fun abstractEmit(out: CodeWriter): CodeWriter {
    if (enclosingType != null) {
      enclosingType.emitAnnotations(out)
      enclosingType.emit(out)
      out.emit("." + rawType.simpleName())
    } else {
      rawType.emitAnnotations(out)
      rawType.emit(out)
    }
    if (!typeArguments.isEmpty()) {
      out.emitAndIndent("<")
      var firstParameter = true
      for (parameter in typeArguments) {
        if (!firstParameter) out.emitAndIndent(", ")
        parameter.emitAnnotations(out)
        parameter.abstractEmit(out)
        firstParameter = false
      }
      out.emitAndIndent(">")
    }
    return out
  }

  /**
   * Returns a new [ParameterizedTypeName] instance for the specified `name` as nested inside this
   * class.
   */
  fun nestedClass(name: String)
      = ParameterizedTypeName(
      this, rawType.nestedClass(name), ArrayList<TypeName>(), ArrayList<AnnotationSpec>())

  /**
   * Returns a new [ParameterizedTypeName] instance for the specified `name` as nested inside this
   * class, with the specified `typeArguments`.
   */
  fun nestedClass(name: String, typeArguments: List<TypeName>)
      = ParameterizedTypeName(
      this, rawType.nestedClass(name), typeArguments, ArrayList<AnnotationSpec>())

  companion object {
    /** Returns a parameterized type, applying `typeArguments` to `rawType`.  */
    @JvmStatic fun get(rawType: ClassName, vararg typeArguments: TypeName)
        = ParameterizedTypeName(null, rawType, Arrays.asList(*typeArguments))

    /** Returns a parameterized type, applying `typeArguments` to `rawType`.  */
    @JvmStatic fun get(rawType: KClass<*>, vararg typeArguments: KClass<*>)
        = ParameterizedTypeName(null, ClassName.get(rawType), TypeName.list(*typeArguments))

    /** Returns a parameterized type, applying `typeArguments` to `rawType`.  */
    @JvmStatic fun get(rawType: Class<*>, vararg typeArguments: Type)
        = ParameterizedTypeName(null, ClassName.get(rawType), list(*typeArguments))

    /** Returns a parameterized type equivalent to `type`.  */
    @JvmStatic fun get(type: ParameterizedType)
        = get(type, LinkedHashMap<Type, TypeVariableName>())

    /** Returns a parameterized type equivalent to `type`.  */
    internal fun get(
        type: ParameterizedType,
        map: MutableMap<Type, TypeVariableName>): ParameterizedTypeName {
      val rawType = ClassName.get(type.rawType as Class<*>)
      val ownerType = if (type.ownerType is ParameterizedType
          && !Modifier.isStatic((type.rawType as Class<*>).modifiers))
        type.ownerType as ParameterizedType else
        null

      val typeArguments = TypeName.list(*type.actualTypeArguments, map = map)
      return if (ownerType != null)
        get(ownerType, map).nestedClass(rawType.simpleName(), typeArguments) else
        ParameterizedTypeName(null, rawType, typeArguments)
    }
  }
}
