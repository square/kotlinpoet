package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
data class MethodData(
    private val annotations: List<AnnotationSpec>,
    val parameterAnnotations: Map<Int, List<AnnotationSpec>>,
    val isSynthetic: Boolean,
    val jvmModifiers: Set<JvmMethodModifier>,
    val isOverride: Boolean,
    val exceptions: List<TypeName>
) {

  fun allAnnotations(useSiteTarget: UseSiteTarget? = null): Collection<AnnotationSpec> {
    return ElementHandler.createAnnotations(
        useSiteTarget) {
      addAll(annotations)
      if (isSynthetic) {
        add(JVM_SYNTHETIC_SPEC)
      }
      addAll(jvmModifiers.map { it.annotationSpec() })
      exceptions.takeIf { it.isNotEmpty() }
          ?.let {
            add(ElementHandler.createThrowsSpec(it,
                useSiteTarget))
          }
    }
  }

  companion object {
    val SYNTHETIC = MethodData(
        annotations = emptyList(),
        parameterAnnotations = emptyMap(),
        isSynthetic = true,
        jvmModifiers = emptySet(),
        isOverride = false,
        exceptions = emptyList()
    )
    val EMPTY = MethodData(
        annotations = emptyList(),
        parameterAnnotations = emptyMap(),
        isSynthetic = false,
        jvmModifiers = emptySet(),
        isOverride = false,
        exceptions = emptyList()
    )
  }
}
