/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.isPrimary
import com.squareup.kotlinpoet.tags.TypeAliasTag
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmClassifier.Class
import kotlin.metadata.KmClassifier.TypeAlias
import kotlin.metadata.KmClassifier.TypeParameter
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFlexibleTypeUpperBound
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeParameter
import kotlin.metadata.KmTypeProjection
import kotlin.metadata.KmVariance
import kotlin.metadata.KmVariance.IN
import kotlin.metadata.KmVariance.INVARIANT
import kotlin.metadata.KmVariance.OUT
import kotlin.metadata.isNullable
import kotlin.metadata.isReified
import kotlin.metadata.isSuspend
import kotlin.metadata.jvm.annotations
import kotlin.metadata.jvm.signature

/**
 * `true` if this is an extension type (i.e. String.() -> Unit vs (String) -> Unit).
 *
 * See details:
 * https://discuss.kotlinlang.org/t/announcing-kotlinx-metadata-jvm-library-for-reading-modifying-metadata-of-kotlin-jvm-class-files/7980/27
 */
public val KmType.isExtensionType: Boolean
  get() {
    return annotations.any { it.className == "kotlin/ExtensionFunctionType" }
  }

internal val KmClass.primaryConstructor: KmConstructor?
  get() = constructors.find { it.isPrimary }

internal fun KmVariance.toKModifier(): KModifier? {
  return when (this) {
    IN -> KModifier.IN
    OUT -> KModifier.OUT
    INVARIANT -> null
  }
}

internal fun KmTypeProjection.toTypeName(typeParamResolver: TypeParameterResolver): TypeName {
  val typename = type?.toTypeName(typeParamResolver) ?: STAR
  return when (variance) {
    IN -> WildcardTypeName.consumerOf(typename)
    OUT -> WildcardTypeName.producerOf(typename)
    INVARIANT -> typename
    null -> STAR
  }
}

/**
 * Converts a given [KmType] into a KotlinPoet representation, attempting to give a correct "source"
 * representation. This includes converting [functions][kotlin.Function] and `suspend` types to
 * appropriate [lambda representations][LambdaTypeName].
 */
internal fun KmType.toTypeName(typeParamResolver: TypeParameterResolver): TypeName {
  val argumentList = arguments.map { it.toTypeName(typeParamResolver) }
  val type: TypeName =
    when (val valClassifier = classifier) {
      is TypeParameter -> {
        typeParamResolver[valClassifier.id]
      }
      is KmClassifier.Class -> {
        flexibleTypeUpperBound?.toTypeName(typeParamResolver)?.let {
          return it
        }
        outerType?.toTypeName(typeParamResolver)?.let {
          return it
        }
        var finalType: TypeName = ClassInspectorUtil.createClassName(valClassifier.name)
        if (argumentList.isNotEmpty()) {
          val finalTypeString = finalType.toString()
          if (finalTypeString.startsWith("kotlin.Function")) {
            // It's a lambda type!
            finalType =
              if (finalTypeString == "kotlin.FunctionN") {
                TODO("unclear how to express this one since it has arity")
              } else {
                val (parameters, returnType) =
                  if (isSuspend) {
                    // Coroutines always adds an `Any?` return type, but we kind of just want the
                    // source representation, so we trick it here and ignore the last.
                    argumentList.dropLast(2).toTypedArray() to
                      argumentList.dropLast(1).last().let {
                        // Coroutines makes these a `Continuation<T>` of the type, so we want the
                        // parameterized type
                        check(it is ParameterizedTypeName)
                        it.typeArguments[0]
                      }
                  } else {
                    argumentList.dropLast(1).toTypedArray() to argumentList.last()
                  }
                val lambdaType =
                  if (isExtensionType) {
                    // Extension function type! T.(). First parameter is actually the receiver.
                    LambdaTypeName.get(
                      receiver = parameters[0],
                      parameters = parameters.drop(1).toTypedArray(),
                      returnType = returnType,
                    )
                  } else {
                    LambdaTypeName.get(
                      receiver = null,
                      parameters = parameters,
                      returnType = returnType,
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

  val annotations =
    ClassInspectorUtil.createAnnotations {
        for (annotation in annotations) {
          add(annotation.toAnnotationSpec())
        }
      }
      .toList()
  val finalType = type.copy(nullable = isNullable, annotations = annotations)
  return abbreviatedType?.let {
    // This is actually an alias! The "abbreviated type" is the alias and how it's actually
    // represented in source. So instead - we'll return the abbreviated type but store the "real"
    // type in tags for reference.
    val abbreviatedTypeName = it.toTypeName(typeParamResolver)
    abbreviatedTypeName.copy(tags = mapOf(TypeAliasTag::class to TypeAliasTag(finalType)))
  } ?: finalType
}

internal fun KmTypeParameter.toTypeVariableName(
  typeParamResolver: TypeParameterResolver
): TypeVariableName {
  val finalVariance = variance.toKModifier()
  val typeVariableName =
    TypeVariableName(
      name = name,
      bounds = upperBounds.map { it.toTypeName(typeParamResolver) },
      variance = finalVariance,
    )
  val annotations =
    ClassInspectorUtil.createAnnotations {
        for (annotation in annotations) {
          add(annotation.toAnnotationSpec())
        }
      }
      .toList()
  return typeVariableName.copy(
    reified = isReified,
    tags = mapOf(KmTypeParameter::class to this),
    annotations = annotations,
  )
}

private fun KmFlexibleTypeUpperBound.toTypeName(
  typeParamResolver: TypeParameterResolver
): TypeName {
  // TODO tag typeFlexibilityId somehow?
  return WildcardTypeName.producerOf(type.toTypeName(typeParamResolver))
}

internal interface TypeParameterResolver {
  val parametersMap: Map<Int, TypeVariableName>

  operator fun get(index: Int): TypeVariableName

  companion object {
    val EMPTY =
      object : TypeParameterResolver {
        override val parametersMap: Map<Int, TypeVariableName> = emptyMap()

        override fun get(index: Int): TypeVariableName =
          throw NoSuchElementException("No type parameters!")
      }
  }
}

internal fun List<KmTypeParameter>.toTypeParameterResolver(
  fallback: TypeParameterResolver? = null
): TypeParameterResolver {
  val parametersMap = LinkedHashMap<Int, TypeVariableName>()
  val typeParamResolver = { id: Int ->
    parametersMap[id]
      ?: fallback?.get(id)
      ?: throw IllegalStateException("No type argument found for $id!")
  }

  val resolver =
    object : TypeParameterResolver {
      override val parametersMap: Map<Int, TypeVariableName> = parametersMap

      override operator fun get(index: Int): TypeVariableName = typeParamResolver(index)
    }

  // Fill the parametersMap. Need to do sequentially and allow for referencing previously defined
  // params
  for (typeParam in this) {
    // Put the simple typevar in first, then it can be referenced in the full toTypeVariable()
    // replacement later that may add bounds referencing this.
    parametersMap[typeParam.id] = TypeVariableName(typeParam.name)
  }

  for (typeParam in this) {
    // Now replace it with the full version.
    parametersMap[typeParam.id] = typeParam.toTypeVariableName(resolver)
  }

  return resolver
}

internal val KM_PROPERTY_COMPARATOR =
  Comparator<KmProperty> { o1, o2 ->
    // No need to check fields, getters, etc as properties must have distinct names
    o1.name.compareTo(o2.name)
  }

internal val KM_FUNCTION_COMPARATOR =
  Comparator<KmFunction> { o1, o2 ->
    var result = o1.name.compareTo(o2.name)
    if (result != 0) return@Comparator result

    val signature1 = o1.signature
    val signature2 = o2.signature
    if (signature1 != null && signature2 != null) {
      result = signature1.toString().compareTo(signature2.toString())
      if (result != 0) return@Comparator result
    }

    // Fallback - calculate signature
    val manualSignature1 = o1.computeSignature()
    val manualSignature2 = o2.computeSignature()
    manualSignature1.compareTo(manualSignature2)
  }

internal val KM_CONSTRUCTOR_COMPARATOR =
  Comparator<KmConstructor> { o1, o2 ->
    val signature1 = o1.signature
    val signature2 = o2.signature
    if (signature1 != null && signature2 != null) {
      val result = signature1.toString().compareTo(signature2.toString())
      if (result != 0) return@Comparator result
    }

    // Fallback - calculate signature
    val manualSignature1 = o1.computeSignature()
    val manualSignature2 = o2.computeSignature()
    manualSignature1.compareTo(manualSignature2)
  }

// Computes a simple signature string good enough for hashing
private fun KmFunction.computeSignature(): String {
  return "$name(${valueParameters.joinToString(",") { it.type.simpleName }})${returnType.simpleName}"
}

private fun KmConstructor.computeSignature(): String {
  return "$<init>(${valueParameters.joinToString(",") { it.type.simpleName }})"
}

private val KmType?.simpleName: String
  get() {
    if (this == null) return "void"
    return when (val c = classifier) {
      is Class -> c.name
      is TypeParameter -> "Object"
      is TypeAlias -> c.name
    }
  }
