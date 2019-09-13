package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
data class ConstructorData(
    val annotations: List<AnnotationSpec>,
    val parameterAnnotations: Map<Int, List<AnnotationSpec>>,
    val isSynthetic: Boolean,
    val jvmModifiers: Set<JvmMethodModifier>,
    val exceptions: List<TypeName>
) {

  val allAnnotations = ElementHandler.createAnnotations {
    addAll(annotations)
    if (isSynthetic) {
      add(JVM_SYNTHETIC_SPEC)
    }
    addAll(jvmModifiers.map { it.annotationSpec() })
    exceptions.takeIf { it.isNotEmpty() }
        ?.let {
          add(ElementHandler.createThrowsSpec(it))
        }
  }

  companion object {
    val SYNTHETIC = ConstructorData(
        annotations = emptyList(),
        parameterAnnotations = emptyMap(),
        isSynthetic = true,
        jvmModifiers = emptySet(),
        exceptions = emptyList()
    )
    val EMPTY = ConstructorData(
        annotations = emptyList(),
        parameterAnnotations = emptyMap(),
        isSynthetic = false,
        jvmModifiers = emptySet(),
        exceptions = emptyList()
    )
  }
}
