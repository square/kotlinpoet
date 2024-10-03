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

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.alias.JvmType
import com.squareup.kotlinpoet.jvm.alias.JvmTypeElement
import com.squareup.kotlinpoet.jvm.alias.JvmTypeMirror
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.NoType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleTypeVisitor8

/** Returns a [TypeName] equivalent to this [TypeMirror]. */
internal actual fun JvmTypeMirror.asTypeNameInternal(): TypeName = TypeName.get(this, mutableMapOf())

/** Returns a [TypeName] equivalent to this [JvmType].  */
internal actual fun JvmType.asTypeNameInternal(): TypeName = TypeName.get(this, mutableMapOf())

internal fun TypeName.Companion.get(
  mirror: TypeMirror,
  typeVariables: Map<TypeParameterElement, TypeVariableName>,
): TypeName {
  return mirror.accept(
    object : SimpleTypeVisitor8<TypeName, Void?>() {
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
        val rawType: ClassName = (t.asElement() as JvmTypeElement).asClassName()
        val enclosingType = t.enclosingType
        val enclosing = if (enclosingType.kind != TypeKind.NONE &&
          Modifier.STATIC !in t.asElement().modifiers
        ) {
          enclosingType.accept(this, null)
        } else {
          null
        }
        if (t.typeArguments.isEmpty() && enclosing !is ParameterizedTypeName) {
          return rawType
        }

        val typeArgumentNames = mutableListOf<TypeName>()
        for (typeArgument in t.typeArguments) {
          typeArgumentNames += get(typeArgument, typeVariables)
        }
        return if (enclosing is ParameterizedTypeName) {
          enclosing.nestedClass(rawType.simpleName, typeArgumentNames)
        } else {
          ParameterizedTypeName(null, rawType, typeArgumentNames)
        }
      }

      override fun visitError(t: ErrorType, p: Void?): TypeName {
        return visitDeclared(t, p)
      }

      override fun visitArray(t: ArrayType, p: Void?): ParameterizedTypeName {
        return ARRAY.parameterizedBy(get(t.componentType, typeVariables))
      }

      override fun visitTypeVariable(
        t: javax.lang.model.type.TypeVariable,
        p: Void?,
      ): TypeName {
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
    },
    null,
  )
}

internal fun TypeName.Companion.get(type: JvmType, map: MutableMap<JvmType, TypeVariableName>): TypeName {
  return when (type) {
    is Class<*> -> when {
      type === Void.TYPE -> UNIT
      type === Boolean::class.javaPrimitiveType -> BOOLEAN
      type === Byte::class.javaPrimitiveType -> BYTE
      type === Short::class.javaPrimitiveType -> SHORT
      type === Int::class.javaPrimitiveType -> INT
      type === Long::class.javaPrimitiveType -> LONG
      type === Char::class.javaPrimitiveType -> CHAR
      type === Float::class.javaPrimitiveType -> FLOAT
      type === Double::class.javaPrimitiveType -> DOUBLE
      type.isArray -> ARRAY.parameterizedBy(get(type.componentType, map))
      else -> type.asClassName()
    }

    is ParameterizedType -> ParameterizedTypeName.get(type, map)
    is java.lang.reflect.WildcardType -> WildcardTypeName.get(type, map)
    is TypeVariable<*> -> TypeVariableName.get(type, map)
    is GenericArrayType -> ARRAY.parameterizedBy(get(type.genericComponentType, map))
    else -> throw IllegalArgumentException("unexpected type: $type")
  }
}
