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
@file:JvmName("KotlinPoetMetadata")
@file:Suppress("unused")

package com.squareup.kotlinpoet.metadata

import javax.lang.model.element.TypeElement
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.reflect.KClass
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata

/**
 * Indicates that a given API is part of the experimental KotlinPoet metadata support. This exists
 * because kotlinx-metadata is not a stable API, and will remain in place until it is.
 */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY)
public annotation class KotlinPoetMetadataPreview

/** @return a new [KmClass] representation of the Kotlin metadata for [this] class. */
@KotlinPoetMetadataPreview
public fun KClass<*>.toKmClass(): KmClass = java.toKmClass()

/** @return a new [KmClass] representation of the Kotlin metadata for [this] class. */
@KotlinPoetMetadataPreview
public fun Class<*>.toKmClass(): KmClass = readMetadata(::getAnnotation).toKmClass()

/** @return a new [KmClass] representation of the Kotlin metadata for [this] type. */
@KotlinPoetMetadataPreview
public fun TypeElement.toKmClass(): KmClass = readMetadata(::getAnnotation).toKmClass()

@KotlinPoetMetadataPreview
public fun Metadata.toKmClass(): KmClass {
  return toKotlinClassMetadata<KotlinClassMetadata.Class>()
    .toKmClass()
}

@KotlinPoetMetadataPreview
public inline fun <reified T : KotlinClassMetadata> Metadata.toKotlinClassMetadata(): T {
  val expectedType = T::class
  val metadata = readKotlinClassMetadata()
  return when (expectedType) {
    KotlinClassMetadata.Class::class -> {
      check(metadata is KotlinClassMetadata.Class)
      metadata as T
    }
    KotlinClassMetadata.FileFacade::class -> {
      check(metadata is KotlinClassMetadata.FileFacade)
      metadata as T
    }
    KotlinClassMetadata.SyntheticClass::class ->
      throw UnsupportedOperationException("SyntheticClass isn't supported yet!")
    KotlinClassMetadata.MultiFileClassFacade::class ->
      throw UnsupportedOperationException("MultiFileClassFacade isn't supported yet!")
    KotlinClassMetadata.MultiFileClassPart::class ->
      throw UnsupportedOperationException("MultiFileClassPart isn't supported yet!")
    KotlinClassMetadata.Unknown::class ->
      throw RuntimeException("Recorded unknown metadata type! $metadata")
    else -> TODO("Unrecognized KotlinClassMetadata type: $expectedType")
  }
}

/**
 * Returns the [KotlinClassMetadata] this represents. In general you should only use this function
 * when you don't know what the underlying [KotlinClassMetadata] subtype is, otherwise you should
 * use one of the more direct functions like [toKmClass].
 */
@KotlinPoetMetadataPreview
public fun Metadata.readKotlinClassMetadata(): KotlinClassMetadata {
  val metadata = KotlinClassMetadata.read(asClassHeader())
  checkNotNull(metadata) {
    "Could not parse metadata! Try bumping kotlinpoet and/or kotlinx-metadata version."
  }
  return metadata
}

private inline fun readMetadata(lookup: ((Class<Metadata>) -> Metadata?)): Metadata {
  return checkNotNull(lookup.invoke(Metadata::class.java)) {
    "No Metadata annotation found! Must be Kotlin code built with the standard library on the classpath."
  }
}

private fun Metadata.asClassHeader(): KotlinClassHeader {
  return KotlinClassHeader(
    kind = kind,
    metadataVersion = metadataVersion,
    data1 = data1,
    data2 = data2,
    extraString = extraString,
    packageName = packageName,
    extraInt = extraInt,
  )
}
