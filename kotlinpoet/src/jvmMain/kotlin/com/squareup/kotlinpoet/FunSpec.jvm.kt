/*
 * Copyright (C) 2015 Square, Inc.
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

import com.squareup.kotlinpoet.FunSpec.Companion.builder
import com.squareup.kotlinpoet.FunSpec.Companion.overriding
import com.squareup.kotlinpoet.jvm.alias.JvmDeclaredType
import com.squareup.kotlinpoet.jvm.alias.JvmExecutableElement
import com.squareup.kotlinpoet.jvm.alias.JvmModifier
import com.squareup.kotlinpoet.jvm.alias.JvmTypes
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeVariable

internal actual fun doOverriding(method: JvmExecutableElement): FunSpec.Builder {
  var modifiers: Set<JvmModifier> = method.getModifiers()
  require(
    JvmModifier.PRIVATE !in modifiers &&
      JvmModifier.FINAL !in modifiers &&
      JvmModifier.STATIC !in modifiers,
  ) {
    "cannot override method with modifiers: $modifiers"
  }

  val methodName = method.simpleName.toString()
  val funBuilder = builder(methodName)

  funBuilder.addModifiers(KModifier.OVERRIDE)

  modifiers = modifiers.toMutableSet()
  modifiers.remove(JvmModifier.ABSTRACT)
  funBuilder.jvmModifiers(modifiers)

  method.typeParameters
    .map { it.asType() as TypeVariable }
    .map { it.asTypeVariableName() }
    .forEach { funBuilder.addTypeVariable(it) }

  funBuilder.returns(method.returnType.asTypeName())
  funBuilder.addParameters(ParameterSpec.parametersOf(method))
  if (method.isVarArgs) {
    funBuilder.parameters[funBuilder.parameters.lastIndex] = funBuilder.parameters.last()
      .toBuilder()
      .addModifiers(KModifier.VARARG)
      .build()
  }

  if (method.thrownTypes.isNotEmpty()) {
    val throwsValueString = method.thrownTypes.joinToString { "%T::class" }
    funBuilder.addAnnotation(
      AnnotationSpec.builder(Throws::class)
        .addMember(throwsValueString, *method.thrownTypes.toTypedArray())
        .build(),
    )
  }

  return funBuilder
}

internal actual fun doOverriding(
  method: JvmExecutableElement,
  enclosing: JvmDeclaredType,
  types: JvmTypes,
): FunSpec.Builder {
  val executableType = types.asMemberOf(enclosing, method) as ExecutableType
  val resolvedParameterTypes = executableType.parameterTypes
  val resolvedReturnType = executableType.returnType

  val builder = overriding(method)
  builder.returns(resolvedReturnType.asTypeName())
  var i = 0
  val size = builder.parameters.size
  while (i < size) {
    val parameter = builder.parameters[i]
    val type = resolvedParameterTypes[i].asTypeName()
    builder.parameters[i] = parameter.toBuilder(parameter.name, type).build()
    i++
  }

  return builder
}
