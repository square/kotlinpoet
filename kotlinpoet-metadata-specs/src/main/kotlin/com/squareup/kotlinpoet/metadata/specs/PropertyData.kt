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
    val fieldAnnotations = fieldData?.allAnnotations.orEmpty()
        .filterNot { it.className in higherScopedAnnotations }
        .associateByTo(LinkedHashMap()) { it.className }
    val getterAnnotations = getterData?.allAnnotations(GET).orEmpty()
        .filterNot { it.className in higherScopedAnnotations }
        .associateByTo(LinkedHashMap()) { it.className }

    val finalTopAnnotations = annotations.toMutableList()

    // If this is a val, and annotation is on both getter and field, we can move it to just the
    // regular annotations
    if (setterData == null && !isJvmField) {
      val sharedAnnotations = getterAnnotations.keys.intersect(fieldAnnotations.keys)
      for (sharedAnnotation in sharedAnnotations) {
        // Add it to the top-level annotations without a site-target
        finalTopAnnotations += getterAnnotations.getValue(sharedAnnotation).toBuilder()
            .useSiteTarget(null)
            .build()

        // Remove from field and getter
        fieldAnnotations.remove(sharedAnnotation)
        getterAnnotations.remove(sharedAnnotation)
      }
    }

    addAll(finalTopAnnotations)
    addAll(fieldAnnotations.values)
    addAll(getterAnnotations.values)
    addAll(setterData?.allAnnotations(SET).orEmpty()
        .filterNot { it.className in higherScopedAnnotations })
    if (isJvmField) {
      add(JVM_FIELD_SPEC)
    }
  }
}
