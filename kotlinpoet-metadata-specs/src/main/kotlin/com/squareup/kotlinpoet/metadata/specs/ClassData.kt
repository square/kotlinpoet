package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmConstructor
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

/**
 * Represents relevant information on a class used for [ElementHandler]. Can only ever be applied on
 * a Kotlin type (i.e. is annotated with [Metadata]).
 *
 * @property kmClass the [ImmutableKmClass] as parsed from the class's [@Metadata][Metadata]
 *           annotation.
 * @property simpleName the simple name of the class. This is important to specify when possible
 *           since Kotlin allows for classes to contain characters like `$` or `-`.
 * @property properties the mapping of [kmClass]'s properties to parsed [PropertyData].
 * @property constructors the mapping of [kmClass]'s constructors to parsed [ConstructorData].
 * @property methods the mapping of [kmClass]'s methods to parsed [MethodData].
 */
@KotlinPoetMetadataPreview
data class ClassData(
  val kmClass: ImmutableKmClass,
  val simpleName: String,
  val properties: Map<ImmutableKmProperty, PropertyData>,
  val constructors: Map<ImmutableKmConstructor, ConstructorData>,
  val methods: Map<ImmutableKmFunction, MethodData>
)
