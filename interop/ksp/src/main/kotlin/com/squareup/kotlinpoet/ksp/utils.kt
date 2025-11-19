/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.kotlinpoet.ksp

import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

internal fun TypeName.rawType(): ClassName {
  return findRawType() ?: throw IllegalArgumentException("Cannot get raw type from $this")
}

internal fun TypeName.findRawType(): ClassName? {
  return when (this) {
    is ClassName -> this
    is ParameterizedTypeName -> rawType
    is LambdaTypeName -> {
      var count = parameters.size
      if (receiver != null) {
        count++
      }
      val functionSimpleName =
        if (count >= 23) {
          "FunctionN"
        } else {
          "Function$count"
        }
      ClassName("kotlin.jvm.functions", functionSimpleName)
    }
    else -> null
  }
}

internal fun ClassName.withTypeArguments(arguments: List<TypeName>): TypeName {
  return if (arguments.isEmpty()) {
    this
  } else {
    this.parameterizedBy(arguments)
  }
}

internal fun KSDeclaration.toClassNameInternal(): ClassName {
  require(!isLocal()) { "Local/anonymous classes are not supported!" }

  if (this is KSClassDeclaration && classKind == ClassKind.ENUM_ENTRY) {
    val simpleName = this.simpleName.asString()
    val parent = parentDeclaration!!.toClassNameInternal()
    return parent.nestedClass(simpleName)
  }

  val pkgName = packageName.asString()

  val typesString = checkNotNull(qualifiedName).asString().removePrefix("$pkgName.")

  val simpleNames = typesString.split(".")
  return ClassName(pkgName, simpleNames)
}

internal fun KSType.requireNotErrorType() {
  require(!isError) { "Error type '$this' is not resolvable in the current round of processing." }
}

/** Resolves the [KSClassDeclaration] for this type, including following typealiases as needed. */
internal fun KSType.resolveKSClassDeclaration(): KSClassDeclaration? {
  requireNotErrorType()
  return declaration.resolveKSClassDeclaration()
}

/**
 * Resolves the [KSClassDeclaration] representation of this declaration, including following
 * typealiases as needed.
 *
 * [KSTypeParameter] types will return null. If you expect one here, you should check the
 * declaration directly.
 */
internal fun KSDeclaration.resolveKSClassDeclaration(): KSClassDeclaration? {
  return when (val declaration = unwrapTypealiases()) {
    is KSClassDeclaration -> declaration
    is KSTypeParameter -> null
    else -> error("Unexpected declaration type: $this")
  }
}

/** Returns the resolved declaration following any typealiases. */
internal tailrec fun KSDeclaration.unwrapTypealiases(): KSDeclaration =
  when (this) {
    is KSTypeAlias -> type.resolve().declaration.unwrapTypealiases()
    else -> this
  }
