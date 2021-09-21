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
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.tags.TypeAliasTag

/** Returns the [ClassName] representation of this [KSType] IFF it's a [KSClassDeclaration]. */
@KotlinPoetKspPreview
public fun KSType.toClassName(): ClassName {
  val decl = declaration
  check(decl is KSClassDeclaration) {
    "Declaration was not a KSClassDeclaration: $this"
  }
  return decl.toClassName()
}

/**
 * Returns the [TypeName] representation of this [KSType].
 *
 * @see toTypeParameterResolver
 * @param typeParamResolver an optional resolver for enclosing declarations' type parameters. Parent
 *                          declarations can be anything with generics that child nodes declare as
 *                          defined by [KSType.arguments].
 */
@KotlinPoetKspPreview
public fun KSType.toTypeName(
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
): TypeName = toTypeName(typeParamResolver, emptyList())

@KotlinPoetKspPreview
internal fun KSType.toTypeName(
  typeParamResolver: TypeParameterResolver,
  typeArguments: List<KSTypeArgument>,
): TypeName {
  val type = when (val decl = declaration) {
    is KSClassDeclaration -> {
      decl.toClassName().withTypeArguments(arguments.map { it.toTypeName(typeParamResolver) })
    }
    is KSTypeParameter -> typeParamResolver[decl.name.getShortName()]
    is KSTypeAlias -> {
      val extraResolver = if (decl.typeParameters.isEmpty()) {
        typeParamResolver
      } else {
        decl.typeParameters.toTypeParameterResolver(typeParamResolver)
      }
      val mappedArgs = arguments.map { it.toTypeName(typeParamResolver) }

      val abbreviatedType = decl.type.resolve()
        .toTypeName(extraResolver)
        .copy(nullable = isMarkedNullable)
        .rawType()
        .withTypeArguments(mappedArgs)

      val aliasArgs = typeArguments.map { it.toTypeName(typeParamResolver) }

      decl.toClassNameInternal()
        .withTypeArguments(aliasArgs)
        .copy(tags = mapOf(TypeAliasTag::class to TypeAliasTag(abbreviatedType)))
    }
    else -> error("Unsupported type: $declaration")
  }

  return type.copy(nullable = isMarkedNullable)
}

/**
 * Returns a [TypeVariableName] representation of this [KSTypeParameter].
 *
 * @see toTypeParameterResolver
 * @param typeParamResolver an optional resolver for enclosing declarations' type parameters. Parent
 *                          declarations can be anything with generics that child nodes declare as
 *                          defined by [KSType.arguments].
 */
@KotlinPoetKspPreview
public fun KSTypeParameter.toTypeVariableName(
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
): TypeVariableName {
  val typeVarName = name.getShortName()
  val typeVarBounds = bounds.map { it.toTypeName(typeParamResolver) }.toList()
  val typeVarVariance = when (variance) {
    COVARIANT -> KModifier.OUT
    CONTRAVARIANT -> KModifier.IN
    else -> null
  }
  return TypeVariableName(typeVarName, bounds = typeVarBounds, variance = typeVarVariance)
}

/**
 * Returns a [TypeName] representation of this [KSTypeArgument].
 *
 * @see toTypeParameterResolver
 * @param typeParamResolver an optional resolver for enclosing declarations' type parameters. Parent
 *                          declarations can be anything with generics that child nodes declare as
 *                          defined by [KSType.arguments].
 */
@KotlinPoetKspPreview
public fun KSTypeArgument.toTypeName(
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
): TypeName {
  val typeName = type?.toTypeName(typeParamResolver) ?: return STAR
  return when (variance) {
    COVARIANT -> WildcardTypeName.producerOf(typeName)
    CONTRAVARIANT -> WildcardTypeName.consumerOf(typeName)
    Variance.STAR -> STAR
    INVARIANT -> typeName
  }
}

/**
 * Returns a [TypeName] representation of this [KSTypeReference].
 *
 * @see toTypeParameterResolver
 * @param typeParamResolver an optional resolver for enclosing declarations' type parameters. Parent
 *                          declarations can be anything with generics that child nodes declare as
 *                          defined by [KSType.arguments].
 */
@KotlinPoetKspPreview
public fun KSTypeReference.toTypeName(
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
): TypeName {
  return resolve().toTypeName(typeParamResolver, element?.typeArguments.orEmpty())
}
