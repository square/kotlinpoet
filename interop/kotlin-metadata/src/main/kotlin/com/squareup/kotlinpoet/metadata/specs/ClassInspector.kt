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
import kotlin.metadata.KmClass
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.jvm.JvmMethodSignature

/** A basic interface for looking up JVM information about a given Class. */
public interface ClassInspector {

  /**
   * Indicates if this [ClassInspector] supports [AnnotationRetention.RUNTIME]-retained annotations.
   * This is used to indicate if manual inference of certain non-RUNTIME-retained annotations should
   * be done, such as [JvmName].
   */
  public val supportsNonRuntimeRetainedAnnotations: Boolean

  /**
   * Creates a new [ContainerData] instance for a given [declarationContainer].
   *
   * @param declarationContainer the source [KmDeclarationContainer] to read from.
   * @param className the [ClassName] of the target class to to read from.
   * @param parentClassName the parent [ClassName] name if [declarationContainer] is nested, inner,
   *   or is a companion object.
   */
  public fun containerData(
    declarationContainer: KmDeclarationContainer,
    className: ClassName,
    parentClassName: ClassName?,
  ): ContainerData

  /**
   * Looks up other declaration containers, such as for nested members. Note that this class would
   * always be Kotlin, so Metadata can be relied on for this.
   *
   * @param className The [ClassName] representation of the class.
   * @return the read [KmDeclarationContainer] from its metadata. If no class or facade file was
   *   found, this should throw an exception.
   */
  public fun declarationContainerFor(className: ClassName): KmDeclarationContainer

  /**
   * Looks up a class and returns whether or not it is an interface. Note that this class can be
   * Java or Kotlin, so Metadata should not be relied on for this.
   *
   * @param className The [ClassName] representation of the class.
   * @return whether or not it is an interface.
   */
  public fun isInterface(className: ClassName): Boolean

  /**
   * Looks up the enum entry on a given enum given its member name.
   *
   * @param enumClassName The [ClassName] representation of the enum class.
   * @param memberName The simple member name.
   * @return the [EnumEntryData]
   */
  public fun enumEntry(enumClassName: ClassName, memberName: String): EnumEntryData

  /**
   * Looks up if a given [methodSignature] within [className] exists.
   *
   * @param className The [ClassName] representation of the class.
   * @param methodSignature The method signature to check.
   * @return whether or not the method exists.
   */
  public fun methodExists(className: ClassName, methodSignature: JvmMethodSignature): Boolean
}

/**
 * Creates a new [ContainerData] instance for a given [className].
 *
 * @param className the [ClassName] of the target class to to read from.
 * @param parentClassName the parent [ClassName] name if [className] is nested, inner, or is a
 *   companion object.
 */
public fun ClassInspector.containerData(
  className: ClassName,
  parentClassName: ClassName?,
): ContainerData {
  return containerData(declarationContainerFor(className), className, parentClassName)
}

/**
 * Looks up other classes, such as for nested members. Note that this class would always be Kotlin,
 * so Metadata can be relied on for this.
 *
 * @param className The [ClassName] representation of the class.
 * @return the read [KmClass] from its metadata. If no class was found, this should throw an
 *   exception.
 */
public fun ClassInspector.classFor(className: ClassName): KmClass {
  val container = declarationContainerFor(className)
  check(container is KmClass) { "Container is not a class! Was ${container.javaClass.simpleName}" }
  return container
}
