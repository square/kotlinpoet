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
package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.metadata.jvm.JvmMethodSignature

/** A basic interface for looking up information about JVM information about a given Class. */
@KotlinPoetMetadataPreview
interface ClassInformer {

  /**
   * Indicates if this element handler supports [AnnotationRetention.RUNTIME]-retained annotations.
   * This is used to indicate if manual inference of certain non-RUNTIME-retained annotations should
   * be done, such as [JvmName].
   */
  val supportsNonRuntimeRetainedAnnotations: Boolean

  /**
   * Creates a new [ClassData] instance for a given [className].
   *
   * @param className the [ClassName] of the target class to to read from.
   * @param parentClassName the parent [ClassName] name if [className] is nested, inner, or is a
   *        companion object.
   */
  fun classData(className: ClassName, parentClassName: ClassName?): ClassData {
    return classData(classFor(className), className, parentClassName)
  }

  /**
   * Creates a new [ClassData] instance for a given [kmClass].
   *
   * @param kmClass the source [ImmutableKmClass] to read from.
   * @param className the [ClassName] of the target class to to read from.
   * @param parentClassName the parent [ClassName] name if [kmClass] is nested, inner, or is a
   *        companion object.
   */
  fun classData(kmClass: ImmutableKmClass, className: ClassName, parentClassName: ClassName?): ClassData

  /**
   * Looks up other classes, such as for nested members. Note that this class would always be
   * Kotlin, so Metadata can be relied on for this.
   *
   * @param className The [ClassName] representation of the class.
   * @return the read [ImmutableKmClass] from its metadata. If no class was found, this should throw
   *         an exception.
   */
  fun classFor(className: ClassName): ImmutableKmClass

  /**
   * Looks up a class and returns whether or not it is an interface. Note that this class can be
   * Java or Kotlin, so Metadata should not be relied on for this.
   *
   * @param className The [ClassName] representation of the class.
   * @return whether or not it is an interface.
   */
  fun isInterface(className: ClassName): Boolean

  /**
   * Looks up the enum entry on a given enum given its member name.
   *
   * @param enumClassName The [ClassName] representation of the enum class.
   * @param memberName The simple member name.
   * @return the read [ImmutableKmClass] from its metadata, if any. For simple enum members with no
   *         class bodies, this should always be null.
   */
  fun enumEntry(enumClassName: ClassName, memberName: String): ImmutableKmClass?

  /**
   * Looks up if a given [methodSignature] within [className] exists.
   *
   * @param className The [ClassName] representation of the class.
   * @param methodSignature The method signature to check.
   * @return whether or not the method exists.
   */
  fun methodExists(className: ClassName, methodSignature: JvmMethodSignature): Boolean
}
