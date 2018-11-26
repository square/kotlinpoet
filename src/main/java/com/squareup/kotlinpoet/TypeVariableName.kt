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
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeVariable
import kotlin.reflect.KClass
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance

fun TypeVariableName.withBounds(vararg bounds: Type) = withBounds(bounds.map { it.asTypeName() })

fun TypeVariableName.withBounds(vararg bounds: KClass<*>) = withBounds(bounds.map { it.asTypeName() })

fun TypeVariableName.withBounds(vararg bounds: TypeName) = withBounds(bounds.toList())

fun TypeVariableName.withBounds(bounds: List<TypeName>) = TypeVariableName(name,
    (this.bounds + bounds).withoutImplicitBound(), variance, reified, nullable, annotations)

private fun List<TypeName>.withoutImplicitBound(): List<TypeName> {
  return if (size == 1) this else filterNot { it == NULLABLE_ANY }
}

fun TypeVariableName.reified(value: Boolean = true) =
    TypeVariableName(name, bounds, variance, value, nullable, annotations)

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

fun KTypeParameter.asTypeVariableName(): TypeVariableName {
  return TypeVariableName.of(name, upperBounds.map { it.asTypeName() },
      when(variance) {
        KVariance.INVARIANT -> null
        KVariance.IN -> KModifier.IN
        KVariance.OUT -> KModifier.OUT
      })
}