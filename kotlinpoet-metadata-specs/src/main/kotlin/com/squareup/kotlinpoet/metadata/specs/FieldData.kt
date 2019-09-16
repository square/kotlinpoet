package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FIELD
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
data class FieldData(
  private val annotations: List<AnnotationSpec>,
  val isSynthetic: Boolean,
  val jvmModifiers: Set<JvmFieldModifier>,
  val constant: CodeBlock?
) {

  val allAnnotations = ElementHandler.createAnnotations(
      FIELD) {
    addAll(annotations)
    if (isSynthetic) {
      add(JVM_SYNTHETIC_SPEC)
    }
    addAll(jvmModifiers.map { it.annotationSpec() })
  }

  companion object {
    val SYNTHETIC = FieldData(
        annotations = emptyList(),
        isSynthetic = true,
        jvmModifiers = emptySet(),
        constant = null
    )
  }
}
