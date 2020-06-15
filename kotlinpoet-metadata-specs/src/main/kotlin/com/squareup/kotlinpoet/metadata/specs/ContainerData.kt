package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmConstructor
import com.squareup.kotlinpoet.metadata.ImmutableKmDeclarationContainer
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.ImmutableKmPackage
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

/**
 * Represents relevant information on a declaration container used for [ClassInspector]. Can only
 * ever be applied on a Kotlin type (i.e. is annotated with [Metadata]).
 *
 * @property declarationContainer the [ImmutableKmDeclarationContainer] as parsed from the class's
 *           [@Metadata][Metadata] annotation.
 * @property annotations declared annotations on this class.
 * @property properties the mapping of [declarationContainer]'s properties to parsed [PropertyData].
 * @property methods the mapping of [declarationContainer]'s methods to parsed [MethodData].
 */
@KotlinPoetMetadataPreview
public interface ContainerData {
  public val declarationContainer: ImmutableKmDeclarationContainer
  public val annotations: Collection<AnnotationSpec>
  public val properties: Map<ImmutableKmProperty, PropertyData>
  public val methods: Map<ImmutableKmFunction, MethodData>
}

/**
 * Represents relevant information on a Kotlin class used for [ClassInspector]. Can only ever be
 * applied on a class and not file facades.
 *
 * @property declarationContainer the [ImmutableKmClass] as parsed from the class's
 *           [@Metadata][Metadata] annotation.
 * @property className the KotlinPoet [ClassName] of the class.
 * @property constructors the mapping of [declarationContainer]'s constructors to parsed
 * [ConstructorData].
 */
@KotlinPoetMetadataPreview
public data class ClassData(
  override val declarationContainer: ImmutableKmClass,
  val className: ClassName,
  override val annotations: Collection<AnnotationSpec>,
  override val properties: Map<ImmutableKmProperty, PropertyData>,
  val constructors: Map<ImmutableKmConstructor, ConstructorData>,
  override val methods: Map<ImmutableKmFunction, MethodData>
) : ContainerData

/**
 * Represents relevant information on a file facade used for [ClassInspector].
 *
 * @property declarationContainer the [ImmutableKmClass] as parsed from the class's
 *           [@Metadata][Metadata] annotation.
 * @property className the KotlinPoet [ClassName] of the underlying facade class in JVM.
 * @property jvmName the `@JvmName` of the class or null if it does not have a custom name.
 *           Default will try to infer from the [className].
 */
@KotlinPoetMetadataPreview
public data class FileData(
  override val declarationContainer: ImmutableKmPackage,
  override val annotations: Collection<AnnotationSpec>,
  override val properties: Map<ImmutableKmProperty, PropertyData>,
  override val methods: Map<ImmutableKmFunction, MethodData>,
  val className: ClassName,
  val jvmName: String? =
      if (!className.simpleName.endsWith("Kt")) className.simpleName else null
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
 * @property declarationContainer the [ImmutableKmClass] as parsed from the entry's
 * [@Metadata][Metadata] annotation.
 * @property annotations the annotations for the entry
 */
@KotlinPoetMetadataPreview
public data class EnumEntryData(
  val declarationContainer: ImmutableKmClass?,
  val annotations: Collection<AnnotationSpec>
)
