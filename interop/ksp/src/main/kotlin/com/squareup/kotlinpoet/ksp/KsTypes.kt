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

import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.symbol.Variance.CONTRAVARIANT
import com.google.devtools.ksp.symbol.Variance.COVARIANT
import com.google.devtools.ksp.symbol.Variance.INVARIANT
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.tags.TypeAliasTag

/**
 * Returns the [ClassName] representation of this [KSType] IFF it's a [KSClassDeclaration] or
 * [KSTypeAlias].
 */
public fun KSType.toClassName(): ClassName {
  requireNotErrorType()
  check(arguments.isEmpty()) {
    "KSType '$this' has type arguments, which are not supported for ClassName conversion. Use KSType.toTypeName()."
  }
  return when (val decl = declaration) {
    is KSClassDeclaration -> decl.toClassName()
    is KSTypeAlias -> decl.toClassName()
    is KSTypeParameter -> error("Cannot convert KSTypeParameter to ClassName: '$this'")
    else -> error("Could not compute ClassName for '$this'")
  }.copy(nullable = isMarkedNullable) as ClassName
}

/**
 * Returns the [ClassName] representation of this [KSType] IFF it's a [KSClassDeclaration] or
 * [KSTypeAlias].
 *
 * If it's unable to resolve to a [ClassName] for any reason, this returns null.
 */
public fun KSType.toClassNameOrNull(): ClassName? {
  if (isError) return null
  if (arguments.isNotEmpty()) return null
  return when (val decl = declaration) {
    is KSClassDeclaration -> decl.toClassName()
    is KSTypeAlias -> decl.toClassName()
    is KSTypeParameter -> null
    else -> null
  }?.let { it.copy(nullable = isMarkedNullable) as ClassName }
}

/**
 * Returns the [TypeName] representation of this [KSType].
 *
 * @param typeParamResolver an optional resolver for enclosing declarations' type parameters. Parent
 *   declarations can be anything with generics that child nodes declare as defined by
 *   [KSType.arguments].
 * @see toTypeParameterResolver
 */
public fun KSType.toTypeName(
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
): TypeName = toTypeName(typeParamResolver, arguments)

internal fun KSType.toTypeName(
  typeParamResolver: TypeParameterResolver,
  typeArguments: List<KSTypeArgument>,
): TypeName {
  requireNotErrorType()
  val type =
    when (val decl = declaration) {
      is KSClassDeclaration -> {
        decl.toClassName().withTypeArguments(arguments.map { it.toTypeName(typeParamResolver) })
      }
      is KSTypeParameter -> typeParamResolver[decl.name.getShortName()]
      is KSTypeAlias -> {
        var typeAlias: KSTypeAlias = decl
        var arguments = arguments

        var resolvedType: KSType
        var mappedArgs: List<KSTypeArgument>
        var extraResolver: TypeParameterResolver = typeParamResolver
        while (true) {
          resolvedType = typeAlias.type.resolve()
          mappedArgs =
            mapTypeArgumentsFromTypeAliasToAbbreviatedType(
              typeAlias = typeAlias,
              typeAliasTypeArguments = arguments,
              abbreviatedType = resolvedType,
            )
          extraResolver =
            if (typeAlias.typeParameters.isEmpty()) {
              extraResolver
            } else {
              typeAlias.typeParameters.toTypeParameterResolver(extraResolver)
            }

          typeAlias = resolvedType.declaration as? KSTypeAlias ?: break
          arguments = mappedArgs
        }

        val abbreviatedType =
          resolvedType
            .toTypeName(extraResolver)
            .copy(nullable = isMarkedNullable)
            .rawType()
            .withTypeArguments(mappedArgs.map { it.toTypeName(extraResolver) })

        val aliasArgs = typeArguments.map { it.toTypeName(typeParamResolver) }

        decl
          .toClassName()
          .withTypeArguments(aliasArgs)
          .copy(tags = mapOf(TypeAliasTag::class to TypeAliasTag(abbreviatedType)))
      }
      else -> error("Unsupported type: $declaration")
    }

  return type.copy(nullable = isMarkedNullable)
}

private fun mapTypeArgumentsFromTypeAliasToAbbreviatedType(
  typeAlias: KSTypeAlias,
  typeAliasTypeArguments: List<KSTypeArgument>,
  abbreviatedType: KSType,
): List<KSTypeArgument> {
  return abbreviatedType.arguments.map { typeArgument ->
    // Check if type argument is a reference to a typealias type parameter, and not an actual type.
    val typeAliasTypeParameterIndex =
      typeAlias.typeParameters.indexOfFirst { typeAliasTypeParameter ->
        typeAliasTypeParameter.name.asString() == typeArgument.type.toString()
      }
    if (typeAliasTypeParameterIndex >= 0) {
      typeAliasTypeArguments[typeAliasTypeParameterIndex]
    } else {
      typeArgument
    }
  }
}

/**
 * Returns a [TypeVariableName] representation of this [KSTypeParameter].
 *
 * @param typeParamResolver an optional resolver for enclosing declarations' type parameters. Parent
 *   declarations can be anything with generics that child nodes declare as defined by
 *   [KSType.arguments].
 * @see toTypeParameterResolver
 */
public fun KSTypeParameter.toTypeVariableName(
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
): TypeVariableName {
  val typeVarName = name.getShortName()
  val typeVarBounds = bounds.map { it.toTypeName(typeParamResolver) }.toList()
  val typeVarVariance =
    when (variance) {
      COVARIANT -> KModifier.OUT
      CONTRAVARIANT -> KModifier.IN
      else -> null
    }
  return TypeVariableName(typeVarName, bounds = typeVarBounds, variance = typeVarVariance)
}

/**
 * Returns a [TypeName] representation of this [KSTypeArgument].
 *
 * @param typeParamResolver an optional resolver for enclosing declarations' type parameters. Parent
 *   declarations can be anything with generics that child nodes declare as defined by
 *   [KSType.arguments].
 * @see toTypeParameterResolver
 */
public fun KSTypeArgument.toTypeName(
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
): TypeName {
  val type = this.type ?: return STAR
  return when (variance) {
    COVARIANT -> WildcardTypeName.producerOf(type.toTypeName(typeParamResolver))
    CONTRAVARIANT -> WildcardTypeName.consumerOf(type.toTypeName(typeParamResolver))
    Variance.STAR -> STAR
    INVARIANT -> type.toTypeName(typeParamResolver)
  }
}

/**
 * Returns a [TypeName] representation of this [KSTypeReference].
 *
 * @param typeParamResolver an optional resolver for enclosing declarations' type parameters. Parent
 *   declarations can be anything with generics that child nodes declare as defined by
 *   [KSType.arguments].
 * @see toTypeParameterResolver
 */
public fun KSTypeReference.toTypeName(
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
): TypeName {
  val type = resolve()
  val elem = element
  // Don't wrap in a lambda if this is a typealias, even if the underlying type is a function type.
  return if (elem is KSCallableReference && type.declaration !is KSTypeAlias) {
    LambdaTypeName.get(
        receiver = elem.receiverType?.toTypeName(typeParamResolver),
        parameters =
          elem.functionParameters.map {
            ParameterSpec.unnamed(it.type.toTypeName(typeParamResolver))
          },
        returnType = elem.returnType.toTypeName(typeParamResolver),
      )
      .copy(nullable = type.isMarkedNullable, suspending = type.isSuspendFunctionType)
  } else {
    type.toTypeName(typeParamResolver, type.arguments)
  }
}
