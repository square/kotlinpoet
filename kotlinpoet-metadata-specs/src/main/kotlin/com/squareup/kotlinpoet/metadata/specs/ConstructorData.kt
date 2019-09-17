package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.internal.ElementHandlerUtil
import com.squareup.kotlinpoet.metadata.specs.internal.JVM_SYNTHETIC_SPEC

/**
 * Represents relevant information on a constructor used for [ElementHandler]. Should only be
 * associated with constructors of a [ClassData].
 *
 * @param annotations declared annotations on this constructor.
 * @property parameterAnnotations a mapping of parameter indices to annotations on them.
 * @property isSynthetic indicates if this constructor is synthetic or not.
 * @property jvmModifiers set of [JvmMethodModifiers][JvmMethodModifier] on this constructor.
 * @property exceptions list of exceptions thrown by this constructor.
 */
@KotlinPoetMetadataPreview
data class ConstructorData(
  private val annotations: List<AnnotationSpec>,
  val parameterAnnotations: Map<Int, Collection<AnnotationSpec>>,
  val isSynthetic: Boolean,
  val jvmModifiers: Set<JvmMethodModifier>,
  val exceptions: List<TypeName>
) {

  /**
   * A collection of all annotations on this constructor, including any derived from [jvmModifiers],
   * [isSynthetic], and [exceptions].
   */
  val allAnnotations: Collection<AnnotationSpec> = ElementHandlerUtil.createAnnotations {
    addAll(annotations)
    if (isSynthetic) {
      add(ElementHandlerUtil.JVM_SYNTHETIC_SPEC)
    }
    addAll(jvmModifiers.map { it.annotationSpec() })
    exceptions.takeIf { it.isNotEmpty() }
        ?.let {
          add(ElementHandlerUtil.createThrowsSpec(it))
        }
  }

  companion object {
    val EMPTY = ConstructorData(
        annotations = emptyList(),
        parameterAnnotations = emptyMap(),
        isSynthetic = false,
        jvmModifiers = emptySet(),
        exceptions = emptyList()
    )
  }
}
