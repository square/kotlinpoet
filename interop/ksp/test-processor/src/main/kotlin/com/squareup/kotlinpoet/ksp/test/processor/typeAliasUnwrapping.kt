package com.squareup.kotlinpoet.ksp.test.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.tag
import com.squareup.kotlinpoet.tags.TypeAliasTag
import java.util.TreeSet

/*
 * Example implementation of how to unwrap a typealias from TypeNameAliasTag
 */

internal fun TypeName.unwrapTypeAliasReal(): TypeName {
  return tag<TypeAliasTag>()?.abbreviatedType?.let { unwrappedType ->
    // If any type is nullable, then the whole thing is nullable
    var isAnyNullable = isNullable
    // Keep track of all annotations across type levels. Sort them too for consistency.
    val runningAnnotations = TreeSet<AnnotationSpec>(compareBy { it.toString() }).apply {
      addAll(annotations)
    }
    val nestedUnwrappedType = unwrappedType.unwrapTypeAlias()
    runningAnnotations.addAll(nestedUnwrappedType.annotations)
    isAnyNullable = isAnyNullable || nestedUnwrappedType.isNullable
    nestedUnwrappedType.copy(nullable = isAnyNullable, annotations = runningAnnotations.toList())
  } ?: this
}

// TypeVariableName gets a special overload because these usually need to be kept in a type-safe
// manner.
internal fun TypeVariableName.unwrapTypeAlias(): TypeVariableName {
  return TypeVariableName(
    name = name,
    bounds = bounds.map { it.unwrapTypeAlias() },
    variance = variance
  )
    .copy(nullable = isNullable, annotations = annotations, tags = tags)
}

internal fun TypeName.unwrapTypeAlias(): TypeName {
  return when (this) {
    is ClassName -> unwrapTypeAliasReal()
    is ParameterizedTypeName -> unwrapTypeAliasReal()
    is TypeVariableName -> unwrapTypeAlias()
    is WildcardTypeName -> unwrapTypeAliasReal()
    is LambdaTypeName -> unwrapTypeAliasReal()
    else -> throw UnsupportedOperationException("Type '${javaClass.simpleName}' is illegal. Only classes, parameterized types, wildcard types, or type variables are allowed.")
  }
}
