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
package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import kotlin.metadata.KmClass
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.KmFunction
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty

/**
 * Represents relevant information on a declaration container used for [ClassInspector]. Can only
 * ever be applied on a Kotlin type (i.e. is annotated with [Metadata]).
 *
 * @property declarationContainer the [KmDeclarationContainer] as parsed from the class's
 *   [@Metadata][Metadata] annotation.
 * @property annotations declared annotations on this class.
 * @property properties the mapping of [declarationContainer]'s properties to parsed [PropertyData].
 * @property methods the mapping of [declarationContainer]'s methods to parsed [MethodData].
 */
public interface ContainerData {
  public val declarationContainer: KmDeclarationContainer
  public val annotations: Collection<AnnotationSpec>
  public val properties: Map<KmProperty, PropertyData>
  public val methods: Map<KmFunction, MethodData>
}

/**
 * Represents relevant information on a Kotlin class used for [ClassInspector]. Can only ever be
 * applied on a class and not file facades.
 *
 * @property declarationContainer the [KmClass] as parsed from the class's [@Metadata][Metadata]
 *   annotation.
 * @property className the KotlinPoet [ClassName] of the class.
 * @property constructors the mapping of [declarationContainer]'s constructors to parsed
 *   [ConstructorData].
 */
public data class ClassData(
  override val declarationContainer: KmClass,
  val className: ClassName,
  override val annotations: Collection<AnnotationSpec>,
  override val properties: Map<KmProperty, PropertyData>,
  val constructors: Map<KmConstructor, ConstructorData>,
  override val methods: Map<KmFunction, MethodData>,
) : ContainerData

/**
 * Represents relevant information on a file facade used for [ClassInspector].
 *
 * @property declarationContainer the [KmClass] as parsed from the class's [@Metadata][Metadata]
 *   annotation.
 * @property className the KotlinPoet [ClassName] of the underlying facade class in JVM.
 * @property jvmName the `@JvmName` of the class or null if it does not have a custom name. Default
 *   will try to infer from the [className].
 */
public data class FileData(
  override val declarationContainer: KmPackage,
  override val annotations: Collection<AnnotationSpec>,
  override val properties: Map<KmProperty, PropertyData>,
  override val methods: Map<KmFunction, MethodData>,
  val className: ClassName,
  val jvmName: String? = if (!className.simpleName.endsWith("Kt")) className.simpleName else null,
) : ContainerData {

  /**
   * The file name of the container, defaults to [className]'s simple name + "Kt". If a [jvmName] is
   * specified, it will always defer to that.
   */
  val fileName: String = jvmName ?: className.simpleName.removeSuffix("Kt")
}

/**
 * Represents relevant information on a Kotlin enum entry.
 *
 * @property declarationContainer the [KmClass] as parsed from the entry's [@Metadata][Metadata]
 *   annotation.
 * @property annotations the annotations for the entry
 */
public data class EnumEntryData(
  val declarationContainer: KmClass?,
  val annotations: Collection<AnnotationSpec>,
)
