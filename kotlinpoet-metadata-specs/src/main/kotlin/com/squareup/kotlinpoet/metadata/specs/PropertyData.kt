package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.GET
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.SET
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
data class PropertyData(
    val annotations: List<AnnotationSpec>,
    val fieldData: FieldData?,
    val getterData: MethodData?,
    val setterData: MethodData?,
    val isJvmField: Boolean
) {
  val isOverride = (getterData?.isOverride ?: false) || (setterData?.isOverride ?: false)
  val allAnnotations: Collection<AnnotationSpec> = ElementHandler.createAnnotations {
    // Don't add annotations that are already defined on the parent
    val higherScopedAnnotations = annotations.associateBy { it.className }
    addAll(annotations)
    addAll(fieldData?.allAnnotations.orEmpty()
        .filterNot { it.className in higherScopedAnnotations })
    addAll(getterData?.allAnnotations(GET).orEmpty()
        .filterNot { it.className in higherScopedAnnotations })
    addAll(setterData?.allAnnotations(SET).orEmpty()
        .filterNot { it.className in higherScopedAnnotations })
    if (isJvmField) {
      add(JVM_FIELD_SPEC)
    }
  }
}
