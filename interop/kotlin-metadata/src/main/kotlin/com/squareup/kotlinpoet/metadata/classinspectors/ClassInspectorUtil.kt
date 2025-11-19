/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet.metadata.classinspectors

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FIELD
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.COLLECTION
import com.squareup.kotlinpoet.COMPARABLE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ITERABLE
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MAP_ENTRY
import com.squareup.kotlinpoet.MUTABLE_COLLECTION
import com.squareup.kotlinpoet.MUTABLE_ITERABLE
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_MAP_ENTRY
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import java.util.Collections
import java.util.TreeSet
import kotlin.metadata.KmProperty
import kotlin.metadata.isConst
import kotlin.metadata.isLocalClassName
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

internal object ClassInspectorUtil {
  val JVM_NAME: ClassName = JvmName::class.asClassName()
  private val JVM_FIELD = JvmField::class.asClassName()
  internal val JVM_FIELD_SPEC = AnnotationSpec.builder(JVM_FIELD).build()
  internal val JVM_SYNTHETIC = JvmSynthetic::class.asClassName()
  internal val JVM_SYNTHETIC_SPEC = AnnotationSpec.builder(JVM_SYNTHETIC).build()
  internal val JAVA_DEPRECATED = java.lang.Deprecated::class.asClassName()
  private val JVM_TRANSIENT = Transient::class.asClassName()
  private val JVM_VOLATILE = Volatile::class.asClassName()
  private val IMPLICIT_FIELD_ANNOTATIONS = setOf(JVM_FIELD, JVM_TRANSIENT, JVM_VOLATILE)
  private val NOT_NULL = NotNull::class.asClassName()
  private val NULLABLE = Nullable::class.asClassName()
  private val EXTENSION_FUNCTION_TYPE = ExtensionFunctionType::class.asClassName()
  private val KOTLIN_INTRINSIC_ANNOTATIONS = setOf(NOT_NULL, NULLABLE, EXTENSION_FUNCTION_TYPE)

  val KOTLIN_INTRINSIC_INTERFACES: Set<ClassName> =
    setOf(
      CHAR_SEQUENCE,
      COMPARABLE,
      ITERABLE,
      COLLECTION,
      LIST,
      SET,
      MAP,
      MAP_ENTRY,
      MUTABLE_ITERABLE,
      MUTABLE_COLLECTION,
      MUTABLE_LIST,
      MUTABLE_SET,
      MUTABLE_MAP,
      MUTABLE_MAP_ENTRY,
    )

  private val KOTLIN_NULLABILITY_ANNOTATIONS =
    setOf("org.jetbrains.annotations.NotNull", "org.jetbrains.annotations.Nullable")

  fun filterOutNullabilityAnnotations(annotations: List<AnnotationSpec>): List<AnnotationSpec> {
    return annotations.filterNot {
      val typeName = it.typeName
      return@filterNot typeName is ClassName &&
        typeName.canonicalName in KOTLIN_NULLABILITY_ANNOTATIONS
    }
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
   * Infers if [property] is a jvm field and should be annotated as such given the input parameters.
   */
  fun computeIsJvmField(
    property: KmProperty,
    classInspector: ClassInspector,
    isCompanionObject: Boolean,
    hasGetter: Boolean,
    hasSetter: Boolean,
    hasField: Boolean,
  ): Boolean {
    return if (!hasGetter && !hasSetter && hasField && !property.isConst) {
      !(classInspector.supportsNonRuntimeRetainedAnnotations && !isCompanionObject)
    } else {
      false
    }
  }

  /**
   * @return a new collection of [AnnotationSpecs][AnnotationSpec] with sorting and de-duping input
   *   annotations from [body].
   */
  fun createAnnotations(
    siteTarget: UseSiteTarget? = null,
    body: MutableCollection<AnnotationSpec>.() -> Unit,
  ): Collection<AnnotationSpec> {
    val result =
      mutableSetOf<AnnotationSpec>().apply(body).filterNot { spec ->
        spec.typeName in KOTLIN_INTRINSIC_ANNOTATIONS
      }
    val withUseSiteTarget =
      if (siteTarget != null) {
        result.map {
          if (!(siteTarget == FIELD && it.typeName in IMPLICIT_FIELD_ANNOTATIONS)) {
            // Some annotations are implicitly only for FIELD, so don't emit those site targets
            it.toBuilder().useSiteTarget(siteTarget).build()
          } else {
            it
          }
        }
      } else {
        result
      }

    val sorted = withUseSiteTarget.toTreeSet()

    return Collections.unmodifiableCollection(sorted)
  }

  /**
   * @return a [@Throws][Throws] [AnnotationSpec] representation of a given collection of
   *   [exceptions].
   */
  fun createThrowsSpec(
    exceptions: Collection<TypeName>,
    useSiteTarget: UseSiteTarget? = null,
  ): AnnotationSpec {
    return AnnotationSpec.builder(Throws::class)
      .addMember(
        "exceptionClasses = %L",
        exceptions.map { CodeBlock.of("%T::class", it) }.joinToCode(prefix = "[", suffix = "]"),
      )
      .useSiteTarget(useSiteTarget)
      .build()
  }

  /**
   * Best guesses a [ClassName] as represented in Metadata's [kotlin.metadata.ClassName], where
   * package names in this name are separated by '/' and class names are separated by '.'.
   *
   * For example: `"org/foo/bar/Baz.Nested"`.
   *
   * Local classes are prefixed with ".", but for KotlinPoetMetadataSpecs' use case we don't deal
   * with those.
   */
  fun createClassName(kotlinMetadataName: String): ClassName {
    require(!kotlinMetadataName.isLocalClassName()) { "Local/anonymous classes are not supported!" }
    // Top-level: package/of/class/MyClass
    // Nested A:  package/of/class/MyClass.NestedClass
    val simpleName =
      kotlinMetadataName.substringAfterLast(
        '/', // Drop the package name, e.g. "package/of/class/"
        '.', // Drop any enclosing classes, e.g. "MyClass."
      )
    val packageName =
      kotlinMetadataName.substringBeforeLast(delimiter = "/", missingDelimiterValue = "")
    val simpleNames =
      kotlinMetadataName
        .removeSuffix(simpleName)
        .removeSuffix(".") // Trailing "." if any
        .removePrefix(packageName)
        .removePrefix("/")
        .let {
          if (it.isNotEmpty()) {
            it.split(".")
          } else {
            // Don't split, otherwise we end up with an empty string as the first element!
            emptyList()
          }
        }
        .plus(simpleName)

    return ClassName(packageName = packageName.replace("/", "."), simpleNames = simpleNames)
  }

  fun Iterable<AnnotationSpec>.toTreeSet(): TreeSet<AnnotationSpec> {
    return TreeSet<AnnotationSpec>(compareBy { it.toString() }).apply { addAll(this@toTreeSet) }
  }

  private fun String.substringAfterLast(vararg delimiters: Char): String {
    val index = lastIndexOfAny(delimiters)
    return if (index == -1) this else substring(index + 1, length)
  }
}
