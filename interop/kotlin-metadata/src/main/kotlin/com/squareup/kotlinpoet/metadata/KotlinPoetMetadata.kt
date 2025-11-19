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

package com.squareup.kotlinpoet.metadata

import javax.lang.model.element.TypeElement
import kotlin.metadata.KmClass
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import kotlin.reflect.KClass

/**
 * @param lenient see docs on [KotlinClassMetadata.readStrict] and [KotlinClassMetadata.readLenient]
 *   for more details.
 * @return a new [KmClass] representation of the Kotlin metadata for [this] class.
 */
internal fun KClass<*>.toKmClass(lenient: Boolean): KmClass = java.toKmClass(lenient)

/**
 * @param lenient see docs on [KotlinClassMetadata.readStrict] and [KotlinClassMetadata.readLenient]
 *   for more details.
 * @return a new [KmClass] representation of the Kotlin metadata for [this] class.
 */
internal fun Class<*>.toKmClass(lenient: Boolean): KmClass =
  readMetadata(::getAnnotation).toKmClass(lenient)

/**
 * @param lenient see docs on [KotlinClassMetadata.readStrict] and [KotlinClassMetadata.readLenient]
 *   for more details.
 * @return a new [KmClass] representation of the Kotlin metadata for [this] type.
 */
internal fun TypeElement.toKmClass(lenient: Boolean): KmClass =
  readMetadata(::getAnnotation).toKmClass(lenient)

/**
 * @param lenient see docs on [KotlinClassMetadata.readStrict] and [KotlinClassMetadata.readLenient]
 *   for more details.
 */
internal fun Metadata.toKmClass(lenient: Boolean): KmClass {
  return toKotlinClassMetadata<KotlinClassMetadata.Class>(lenient).kmClass
}

/**
 * @param lenient see docs on [KotlinClassMetadata.readStrict] and [KotlinClassMetadata.readLenient]
 *   for more details.
 */
internal inline fun <reified T : KotlinClassMetadata> Metadata.toKotlinClassMetadata(
  lenient: Boolean
): T {
  val expectedType = T::class
  val metadata = readKotlinClassMetadata(lenient)
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
 *
 * @param lenient see docs on [KotlinClassMetadata.readStrict] and [KotlinClassMetadata.readLenient]
 *   for more details.
 */
internal fun Metadata.readKotlinClassMetadata(lenient: Boolean): KotlinClassMetadata {
  return if (lenient) {
    KotlinClassMetadata.readLenient(this)
  } else {
    KotlinClassMetadata.readStrict(this)
  }
}

private inline fun readMetadata(lookup: ((Class<Metadata>) -> Metadata?)): Metadata {
  return checkNotNull(lookup.invoke(Metadata::class.java)) {
    "No Metadata annotation found! Must be Kotlin code built with the standard library on the classpath."
  }
}
