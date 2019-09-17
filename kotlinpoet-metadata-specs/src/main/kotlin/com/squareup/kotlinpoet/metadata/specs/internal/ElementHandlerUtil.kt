package com.squareup.kotlinpoet.metadata.specs.internal

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode

object ElementHandlerUtil {
  internal val JVM_FIELD = JvmField::class.asClassName()
  internal val JVM_FIELD_SPEC = AnnotationSpec.builder(JVM_FIELD).build()
  internal val JVM_SYNTHETIC_SPEC =
      AnnotationSpec.builder(JvmSynthetic::class).build()
  internal val JVM_TRANSIENT = Transient::class.asClassName()
  internal val JVM_VOLATILE = Volatile::class.asClassName()
  internal val IMPLICIT_FIELD_ANNOTATIONS = setOf(
      JVM_FIELD,
      JVM_TRANSIENT,
      JVM_VOLATILE
  )

  /**
   * @return a [@Throws][Throws] [AnnotationSpec] representation of a given collection of
   *         [exceptions].
   */
  fun createThrowsSpec(
      exceptions: Collection<TypeName>,
      useSiteTarget: UseSiteTarget? = null
  ): AnnotationSpec {
    return AnnotationSpec.builder(Throws::class)
        .addMember(
            "exceptionClasses = %L",
            exceptions.map { CodeBlock.of("%T::class", it) }
                .joinToCode(prefix = "[", suffix = "]")
        )
        .useSiteTarget(useSiteTarget)
        .build()
  }
}

