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
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.NoType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleTypeVisitor7
import kotlin.reflect.KClass

/**
 * Any type in Kotlin's type system. This class identifies simple types like `Int` and `String`,
 * nullable types like `Int?`, composite types like `Array<String>` and `Set<String>`, and
 * unassignable types like `Unit`.
 *
 * Type names are dumb identifiers only and do not model the values they name. For example, the
 * type name for `kotlin.List` doesn't know about the `size()` function, the fact that lists are
 * collections, or even that it accepts a single type parameter.
 *
 * Instances of this class are immutable value objects that implement `equals()` and `hashCode()`
 * properly.
 *
 * Referencing existing types
 * --------------------------
 *
 * In an annotation processor you can get a type name instance for a type mirror by calling
 * [TypeName.get]. In reflection code, you can use [TypeName.get].

 * Defining new types
 * ------------------
 *
 * Create new reference types like `com.example.HelloWorld` with [ClassName.get]. To build composite
 * types like `Set<Long>`, use the factory methods on [ParameterizedTypeName], [TypeVariableName],
 * and [WildcardTypeName].
 */
abstract class TypeName internal constructor(
    val nullable: Boolean, annotations: List<AnnotationSpec>) {
  val annotations: List<AnnotationSpec> = annotations.toImmutableList()

  /** Lazily-initialized toString of this type name.  */
  internal val cachedString: String by lazy {
    val resultBuilder = StringBuilder()
    val codeWriter = CodeWriter(resultBuilder)
    emitAnnotations(codeWriter)
    emit(codeWriter)
    if (nullable) resultBuilder.append("?")
    resultBuilder.toString()
  }

  fun annotated(vararg annotations: AnnotationSpec): TypeName {
    return annotated(annotations.toList())
  }

  abstract fun asNullable(): TypeName

  abstract fun asNonNullable(): TypeName

  abstract fun annotated(annotations: List<AnnotationSpec>): TypeName

  abstract fun withoutAnnotations(): TypeName

  val isAnnotated: Boolean
    get() = annotations.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int {
    return toString().hashCode()
  }

  override fun toString(): String {
    return cachedString
  }

  @Throws(IOException::class)
  internal abstract fun emit(out: CodeWriter): CodeWriter

  @Throws(IOException::class)
  @JvmName("emitAnnotations") internal fun emitAnnotations(out: CodeWriter) {
    for (annotation in annotations) {
      annotation.emit(out, true)
      out.emit(" ")
    }
  }

  @Throws(IOException::class)
  @JvmName("emitNullable") internal fun emitNullable(out: CodeWriter) {
    if (nullable) {
      out.emit("?")
    }
  }

  companion object {
    /** Returns a type name equivalent to `mirror`.  */
    @JvmOverloads @JvmStatic fun get(
        mirror: TypeMirror,
        typeVariables: MutableMap<TypeParameterElement, TypeVariableName> = mutableMapOf())
        : TypeName {
      return mirror.accept(object : SimpleTypeVisitor7<TypeName, Void?>() {
        override fun visitPrimitive(t: PrimitiveType, p: Void?): TypeName {
          return when (t.kind) {
            TypeKind.BOOLEAN -> BOOLEAN
            TypeKind.BYTE -> BYTE
            TypeKind.SHORT -> SHORT
            TypeKind.INT -> INT
            TypeKind.LONG -> LONG
            TypeKind.CHAR -> CHAR
            TypeKind.FLOAT -> FLOAT
            TypeKind.DOUBLE -> DOUBLE
            else -> throw AssertionError()
          }
        }

        override fun visitDeclared(t: DeclaredType, p: Void?): TypeName {
          val rawType: ClassName = ClassName.get(t.asElement() as TypeElement)
          val enclosingType = t.enclosingType
          val enclosing = if (enclosingType.kind != TypeKind.NONE
              && !t.asElement().modifiers.contains(Modifier.STATIC))
            enclosingType.accept(this, null) else
            null
          if (t.typeArguments.isEmpty() && enclosing !is ParameterizedTypeName) {
            return rawType
          }

          val typeArgumentNames = mutableListOf<TypeName>()
          for (typeArgument in t.typeArguments) {
            typeArgumentNames += get(typeArgument, typeVariables)
          }
          return if (enclosing is ParameterizedTypeName)
            enclosing.nestedClass(rawType.simpleName(), typeArgumentNames) else
            ParameterizedTypeName(null, rawType, typeArgumentNames)
        }

        override fun visitError(t: ErrorType, p: Void?): TypeName {
          return visitDeclared(t, p)
        }

        override fun visitArray(t: ArrayType, p: Void?): ParameterizedTypeName {
          return ParameterizedTypeName.get(ARRAY, TypeName.get(t.componentType, typeVariables))
        }

        override fun visitTypeVariable(t: javax.lang.model.type.TypeVariable, p: Void?): TypeName {
          return TypeVariableName.get(t, typeVariables.toMutableMap())
        }

        override fun visitWildcard(t: javax.lang.model.type.WildcardType, p: Void?): TypeName {
          return WildcardTypeName.get(t, typeVariables)
        }

        override fun visitNoType(t: NoType, p: Void?): TypeName {
          if (t.kind == TypeKind.VOID) return UNIT
          return super.visitUnknown(t, p)
        }

        override fun defaultAction(e: TypeMirror?, p: Void?): TypeName {
          throw IllegalArgumentException("Unexpected type mirror: " + e!!)
        }
      }, null)
    }

    /** Returns a type name equivalent to `type`.  */
    @JvmStatic fun get(type: KClass<*>) = ClassName.get(type)

    /** Returns a type name equivalent to `type`.  */
    @JvmOverloads @JvmStatic fun get(
        type: Type,
        map: MutableMap<Type, TypeVariableName> = mutableMapOf())
        : TypeName {
      when (type) {
        is Class<*> -> {
          when {
            type === Void.TYPE -> return UNIT
            type === Boolean::class.javaPrimitiveType -> return BOOLEAN
            type === Byte::class.javaPrimitiveType -> return BYTE
            type === Short::class.javaPrimitiveType -> return SHORT
            type === Int::class.javaPrimitiveType -> return INT
            type === Long::class.javaPrimitiveType -> return LONG
            type === Char::class.javaPrimitiveType -> return CHAR
            type === Float::class.javaPrimitiveType -> return FLOAT
            type === Double::class.javaPrimitiveType -> return DOUBLE
            type.isArray -> return ParameterizedTypeName.get(ARRAY, get(type.componentType, map))
            else -> return ClassName.get(type)
          }
        }
        is ParameterizedType -> return ParameterizedTypeName.get(type, map)
        is WildcardType -> return WildcardTypeName.get(type, map)
        is TypeVariable<*> -> return TypeVariableName.get(type, map)
        is GenericArrayType -> return ParameterizedTypeName.get(ARRAY,
            get(type.genericComponentType, map))
        else -> throw IllegalArgumentException("unexpected type: " + type)
      }
    }

    /** Returns the array component of `type`, or null if `type` is not an array.  */
    @JvmStatic fun arrayComponent(type: TypeName): TypeName? {
      return if (type is ParameterizedTypeName && type.rawType == ARRAY)
        type.typeArguments.single() else
        null
    }
  }
}

@JvmField val ANY = ClassName.get("kotlin", "Any")
@JvmField val ARRAY = ClassName.get("kotlin", "Array")
@JvmField val UNIT = ClassName.get(Unit::class)
@JvmField val BOOLEAN = ClassName.get("kotlin", "Boolean")
@JvmField val BYTE = ClassName.get("kotlin", "Byte")
@JvmField val SHORT = ClassName.get("kotlin", "Short")
@JvmField val INT = ClassName.get("kotlin", "Int")
@JvmField val LONG = ClassName.get("kotlin", "Long")
@JvmField val CHAR = ClassName.get("kotlin", "Char")
@JvmField val FLOAT = ClassName.get("kotlin", "Float")
@JvmField val DOUBLE = ClassName.get("kotlin", "Double")
