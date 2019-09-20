package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FIELD
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInformerUtil

/**
 * Represents relevant information on a field used for [ClassInspector]. Should only be
 * associated with a [PropertyData].
 *
 * @param annotations declared annotations on this field.
 * @property isSynthetic indicates if this field is synthetic or not.
 * @property jvmModifiers set of [JvmMethodModifiers][JvmMethodModifier] on this field.
 * @property constant the constant value of this field, if available. Note that this is does not
 *           strictly imply that the associated property is `const`.
 */
@KotlinPoetMetadataPreview
data class FieldData(
  private val annotations: List<AnnotationSpec>,
  val isSynthetic: Boolean,
  val jvmModifiers: Set<JvmFieldModifier>,
  val constant: CodeBlock?
) {

  /**
   * A collection of all annotations on this method, including any derived from [jvmModifiers]
   * and [isSynthetic].
   */
  val allAnnotations: Collection<AnnotationSpec> = ClassInformerUtil.createAnnotations(
      FIELD) {
    addAll(annotations)
    if (isSynthetic) {
      add(ClassInformerUtil.JVM_SYNTHETIC_SPEC)
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
