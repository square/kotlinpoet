package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.internal.ElementHandlerUtil

/**
 * Represents relevant information on a method used for [ElementHandler]. Should only be
 * associated with methods of a [ClassData] or [PropertyData].
 *
 * @param annotations declared annotations on this method.
 * @property parameterAnnotations a mapping of parameter indices to annotations on them.
 * @property isSynthetic indicates if this method is synthetic or not.
 * @property jvmModifiers set of [JvmMethodModifiers][JvmMethodModifier] on this method.
 * @property isOverride indicates if this method overrides one in a supertype.
 * @property exceptions list of exceptions thrown by this method.
 */
@KotlinPoetMetadataPreview
data class MethodData(
  private val annotations: List<AnnotationSpec>,
  val parameterAnnotations: Map<Int, Collection<AnnotationSpec>>,
  val isSynthetic: Boolean,
  val jvmModifiers: Set<JvmMethodModifier>,
  val isOverride: Boolean,
  val exceptions: List<TypeName>
) {

  /**
   * A collection of all annotations on this method, including any derived from [jvmModifiers],
   * [isSynthetic], and [exceptions].
   *
   * @param useSiteTarget an optional [UseSiteTarget] that all annotations on this method should
   *        use.
   */
  fun allAnnotations(useSiteTarget: UseSiteTarget? = null): Collection<AnnotationSpec> {
    return ElementHandlerUtil.createAnnotations(
        useSiteTarget) {
      addAll(annotations)
      if (isSynthetic) {
        add(ElementHandlerUtil.JVM_SYNTHETIC_SPEC)
      }
      addAll(jvmModifiers.map { it.annotationSpec() })
      exceptions.takeIf { it.isNotEmpty() }
          ?.let {
            add(ElementHandlerUtil.createThrowsSpec(it, useSiteTarget))
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
