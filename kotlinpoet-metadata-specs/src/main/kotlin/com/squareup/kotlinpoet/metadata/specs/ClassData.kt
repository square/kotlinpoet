package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmConstructor
import com.squareup.kotlinpoet.metadata.ImmutableKmDeclarationContainer
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil

/**
 * Represents relevant information on a class used for [ClassInspector]. Can only ever be applied on
 * a Kotlin type (i.e. is annotated with [Metadata]).
 *
 * @property declarationContainer the [ImmutableKmDeclarationContainer] as parsed from the class's
 *           [@Metadata][Metadata] annotation.
 * @property className the KotlinPoet [ClassName] of the class.
 * @property annotations declared annotations on this class.
 * @property properties the mapping of [declarationContainer]'s properties to parsed [PropertyData].
 * @property constructors the mapping of [declarationContainer]'s constructors to parsed [ConstructorData].
 * @property methods the mapping of [declarationContainer]'s methods to parsed [MethodData].
 * @property jvmName the `@JvmName` of the class.
 */
@KotlinPoetMetadataPreview
data class ClassData(
    val declarationContainer: ImmutableKmDeclarationContainer,
    val className: ClassName,
    val annotations: Collection<AnnotationSpec>,
    val properties: Map<ImmutableKmProperty, PropertyData>,
    val constructors: Map<ImmutableKmConstructor, ConstructorData>,
    val methods: Map<ImmutableKmFunction, MethodData>
) {
  val jvmName: String? = annotations.find { it.className == ClassInspectorUtil.JVM_NAME }
      ?.let { jvmNameAnnotation ->
        // AnnotationSpecs don't have indexed members, so manually pull the name out
        jvmNameAnnotation.members[0].toString()
            .removePrefix("name = ")
            .removePrefix("\"")
            .removeSuffix("\"")
      }
}
