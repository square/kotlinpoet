package com.squareup.kotlinpoet.metadata.specs.internal

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FIELD
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isConst
import com.squareup.kotlinpoet.metadata.specs.ElementHandler
import java.util.Collections
import java.util.TreeSet

@KotlinPoetMetadataPreview
object ElementHandlerUtil {
  private val JVM_FIELD = JvmField::class.asClassName()
  internal val JVM_FIELD_SPEC = AnnotationSpec.builder(JVM_FIELD).build()
  internal val JVM_SYNTHETIC_SPEC =
      AnnotationSpec.builder(JvmSynthetic::class).build()
  private val JVM_TRANSIENT = Transient::class.asClassName()
  private val JVM_VOLATILE = Volatile::class.asClassName()
  private val IMPLICIT_FIELD_ANNOTATIONS = setOf(
      JVM_FIELD,
      JVM_TRANSIENT,
      JVM_VOLATILE
  )

  val KOTLIN_INTRINSIC_INTERFACES = setOf(
      "kotlin.CharSequence",
      "kotlin.Comparable",
      "kotlin.collections.Iterable",
      "kotlin.collections.Collection",
      "kotlin.collections.List",
      "kotlin.collections.Set",
      "kotlin.collections.Map",
      "kotlin.collections.Map.Entry",
      "kotlin.collections.MutableIterable",
      "kotlin.collections.MutableCollection",
      "kotlin.collections.MutableList",
      "kotlin.collections.MutableSet",
      "kotlin.collections.MutableMap",
      "kotlin.collections.MutableMap.Entry"
  )

  private val KOTLIN_NULLABILITY_ANNOTATIONS = setOf(
      "org.jetbrains.annotations.NotNull",
      "org.jetbrains.annotations.Nullable"
  )

  fun filterOutNullabilityAnnotations(
    annotations: List<AnnotationSpec>
  ): List<AnnotationSpec> {
    return annotations.filterNot { it.className.canonicalName in KOTLIN_NULLABILITY_ANNOTATIONS }
  }

  /** @return a [CodeBlock] representation of a [literal] value. */
  fun codeLiteralOf(literal: Any): CodeBlock {
    return when (literal) {
      is String -> CodeBlock.of("%S", literal)
      is Long -> CodeBlock.of("%LL", literal)
      is Float -> CodeBlock.of("%LF", literal)
      else -> CodeBlock.of("%L", literal)
    }
  }

  /**
   * Infers if [property] is a jvm field and should be annotated as such given the input
   * parameters.
   */
  fun computeIsJvmField(
    property: ImmutableKmProperty,
    elementHandler: ElementHandler,
    isCompanionObject: Boolean,
    hasGetter: Boolean,
    hasSetter: Boolean,
    hasField: Boolean
  ): Boolean {
    return if (!hasGetter &&
        !hasSetter &&
        hasField &&
        !property.isConst) {
      !(elementHandler.supportsNonRuntimeRetainedAnnotations && !isCompanionObject)
    } else {
      false
    }
  }

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
