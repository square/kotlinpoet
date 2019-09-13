package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmConstructor
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isInterface

@KotlinPoetMetadataPreview
data class ClassData(
    val kmClass: ImmutableKmClass,
    val simpleName: String,
    val properties: Map<ImmutableKmProperty, PropertyData>,
    val constructors: Map<ImmutableKmConstructor, ConstructorData>,
    val methods: Map<ImmutableKmFunction, MethodData>
) {
  val isInterface: Boolean = kmClass.isInterface
}
