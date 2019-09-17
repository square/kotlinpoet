package com.squareup.kotlinpoet.metadata.specs.internal

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FIELD
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import java.util.Collections
import java.util.TreeSet

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
   * @return a new collection of [AnnotationSpecs][AnnotationSpec] with sorting and de-duping
   *         input annotations from [body].
   */
  fun createAnnotations(
      siteTarget: UseSiteTarget? = null,
      body: MutableCollection<AnnotationSpec>.() -> Unit
  ): Collection<AnnotationSpec> {
    val result = TreeSet<AnnotationSpec>(compareBy { it.toString() })
        .apply(body)
    val withUseSiteTarget = if (siteTarget != null) {
      result.map {
        if (!(siteTarget == FIELD && it.className in IMPLICIT_FIELD_ANNOTATIONS)) {
          // Some annotations are implicitly only for FIELD, so don't emit those site targets
          it.toBuilder().useSiteTarget(siteTarget).build()
        } else {
          it
        }
      }
    } else {
      result
    }

    return Collections.unmodifiableCollection(withUseSiteTarget)
  }

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

