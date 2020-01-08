/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.kotlinpoet.metadata.specs.internal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmConstructor
import com.squareup.kotlinpoet.metadata.ImmutableKmFlexibleTypeUpperBound
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.ImmutableKmTypeParameter
import com.squareup.kotlinpoet.metadata.ImmutableKmTypeProjection
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isNullable
import com.squareup.kotlinpoet.metadata.isPrimary
import com.squareup.kotlinpoet.metadata.isReified
import com.squareup.kotlinpoet.metadata.isSuspend
import com.squareup.kotlinpoet.metadata.specs.TypeNameAliasTag
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmClassifier.TypeAlias
import kotlinx.metadata.KmClassifier.TypeParameter
import kotlinx.metadata.KmVariance
import kotlinx.metadata.KmVariance.IN
import kotlinx.metadata.KmVariance.INVARIANT
import kotlinx.metadata.KmVariance.OUT

@KotlinPoetMetadataPreview
internal val ImmutableKmClass.primaryConstructor: ImmutableKmConstructor?
  get() = constructors.find { it.isPrimary }

internal fun KmVariance.toKModifier(): KModifier? {
  return when (this) {
    IN -> KModifier.IN
    OUT -> KModifier.OUT
    INVARIANT -> null
  }
}

@KotlinPoetMetadataPreview
internal fun ImmutableKmTypeProjection.toTypeName(
  typeParamResolver: TypeParameterResolver
): TypeName {
  val typename = type?.toTypeName(typeParamResolver) ?: STAR
  return when (variance) {
    IN -> WildcardTypeName.consumerOf(typename)
    OUT -> WildcardTypeName.producerOf(typename)
    INVARIANT -> typename
    null -> STAR
  }
}

/**
 * Converts a given [ImmutableKmType] into a KotlinPoet representation, attempting to give a correct
 * "source" representation. This includes converting [functions][kotlin.Function] and `suspend`
 * types to appropriate [lambda representations][LambdaTypeName].
 */
@KotlinPoetMetadataPreview
internal fun ImmutableKmType.toTypeName(
  typeParamResolver: TypeParameterResolver
): TypeName {
  val argumentList = arguments.map { it.toTypeName(typeParamResolver) }
  val type: TypeName = when (val valClassifier = classifier) {
    is TypeParameter -> {
      typeParamResolver[valClassifier.id]
    }
    is KmClassifier.Class -> {
      flexibleTypeUpperBound?.toTypeName(typeParamResolver)?.let { return it }
      outerType?.toTypeName(typeParamResolver)?.let { return it }
      var finalType: TypeName = ClassInspectorUtil.createClassName(valClassifier.name)
      if (argumentList.isNotEmpty()) {
        val finalTypeString = finalType.toString()
        if (finalTypeString.startsWith("kotlin.Function")) {
          // It's a lambda type!
          finalType = if (finalTypeString == "kotlin.FunctionN") {
            TODO("unclear how to express this one since it has arity")
          } else {
            val (parameters, returnType) = if (isSuspend) {
              // Coroutines always adds an `Any?` return type, but we kind of just want the
              // source representation, so we trick it here and ignore the last.
              argumentList.dropLast(2).toTypedArray() to argumentList.dropLast(1).last().let {
                // Coroutines makes these a `Continuation<T>` of the type, so we want the parameterized type
                check(it is ParameterizedTypeName)
                it.typeArguments[0]
              }
            } else {
              argumentList.dropLast(1).toTypedArray() to argumentList.last()
            }
            val lambdaType = if (isExtensionType) {
              // Extension function type! T.(). First parameter is actually the receiver.
              LambdaTypeName.get(
                  receiver = parameters[0],
                  parameters = *parameters.drop(1).toTypedArray(),
                  returnType = returnType
              )
            } else {
              LambdaTypeName.get(
                  receiver = null,
                  parameters = *parameters,
                  returnType = returnType
              )
            }
            lambdaType.copy(suspending = isSuspend)
          }
        } else {
          finalType = (finalType as ClassName).parameterizedBy(argumentList)
        }
      }
      finalType
    }
    is TypeAlias -> {
      ClassInspectorUtil.createClassName(valClassifier.name)
    }
  }

  val annotations = ClassInspectorUtil.createAnnotations {
    for (annotation in annotations) {
      add(annotation.toAnnotationSpec())
    }
  }.toList()
  val finalType = type.copy(nullable = isNullable, annotations = annotations)
  return abbreviatedType?.let {
    // This is actually an alias! The "abbreviated type" is the alias and how it's actually
    // represented in source. So instead - we'll return the abbreviated type but store the "real"
    // type in tags for reference.
    val abbreviatedTypeName = it.toTypeName(typeParamResolver)
    abbreviatedTypeName.copy(
        tags = mapOf(TypeNameAliasTag::class to TypeNameAliasTag(finalType))
    )
  } ?: finalType
}

@KotlinPoetMetadataPreview
internal fun ImmutableKmTypeParameter.toTypeVariableName(
  typeParamResolver: TypeParameterResolver
): TypeVariableName {
  val finalVariance = variance.toKModifier()
  val typeVariableName = TypeVariableName(
      name = name,
      bounds = upperBounds.map { it.toTypeName(typeParamResolver) },
      variance = finalVariance
  )
  val annotations = ClassInspectorUtil.createAnnotations {
    for (annotation in annotations) {
      add(annotation.toAnnotationSpec())
    }
  }.toList()
  return typeVariableName.copy(
      reified = isReified,
      tags = mapOf(ImmutableKmTypeParameter::class to this),
      annotations = annotations
  )
}

@KotlinPoetMetadataPreview
private fun ImmutableKmFlexibleTypeUpperBound.toTypeName(
  typeParamResolver: TypeParameterResolver
): TypeName {
  // TODO tag typeFlexibilityId somehow?
  return WildcardTypeName.producerOf(type.toTypeName(typeParamResolver))
}
internal interface TypeParameterResolver {
  val parametersMap: Map<Int, TypeVariableName>
  operator fun get(index: Int): TypeVariableName

  companion object {
    val EMPTY = object : TypeParameterResolver {
      override val parametersMap: Map<Int, TypeVariableName> = emptyMap()

      override fun get(index: Int): TypeVariableName = throw NoSuchElementException("No type parameters!")
    }
  }
}

@KotlinPoetMetadataPreview
internal fun List<ImmutableKmTypeParameter>.toTypeParameterResolver(
  fallback: TypeParameterResolver? = null
): TypeParameterResolver {
  val parametersMap = LinkedHashMap<Int, TypeVariableName>()
  val typeParamResolver = { id: Int ->
    parametersMap[id]
        ?: fallback?.get(id)
        ?: throw IllegalStateException("No type argument found for $id!")
  }

  val resolver = object : TypeParameterResolver {
    override val parametersMap: Map<Int, TypeVariableName> = parametersMap

    override operator fun get(index: Int): TypeVariableName = typeParamResolver(index)
  }

  // Fill the parametersMap. Need to do sequentially and allow for referencing previously defined params
  forEach {
    // Put the simple typevar in first, then it can be referenced in the full toTypeVariable()
    // replacement later that may add bounds referencing this.
    parametersMap[it.id] = TypeVariableName(it.name)
    // Now replace it with the full version.
    parametersMap[it.id] = it.toTypeVariableName(resolver)
  }

  return resolver
}
