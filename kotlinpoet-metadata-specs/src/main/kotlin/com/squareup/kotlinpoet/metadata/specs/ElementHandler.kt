/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FIELD
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isConst
import kotlinx.metadata.jvm.JvmMethodSignature
import java.util.Collections
import java.util.TreeSet

/**
 * A basic interface for looking up information about JVM elements.
 */
@KotlinPoetMetadataPreview
interface ElementHandler {

  /**
   * Indicates if this element handler supports [AnnotationRetention.RUNTIME]-retained annotations.
   * This is used to indicate if manual inference of certain non-RUNTIME-retained annotations should
   * be done, such as [JvmName].
   */
  val supportsNonRuntimeRetainedAnnotations: Boolean

  fun classData(classJvmName: String, parentName: String?, simpleName: String): ClassData {
    return classData(classFor(classJvmName), parentName, simpleName)
  }

  fun classData(
    kmClass: ImmutableKmClass,
    parentName: String?,
    simpleName: String = kmClass.bestGuessSimpleName()
  ): ClassData

  /**
   * Looks up other classes, such as for nested members. Note that this class would always be
   * Kotlin, so Metadata can be relied on for this.
   *
   * @param jvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @return the read [ImmutableKmClass] from its metadata. If no class was found, this should throw
   *         an exception.
   */
  fun classFor(jvmName: String): ImmutableKmClass

  /**
   * Looks up a class and returns whether or not it is an interface. Note that this class can be
   * Java or Kotlin, so Metadata should not be relied on for this.
   *
   * @param jvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @return whether or not it is an interface.
   */
  fun isInterface(jvmName: String): Boolean

  /**
   * Looks up the enum entry on a given enum given its member name.
   *
   * @param enumClassJvmName The JVM name of the enum class (example: `"org/foo/bar/Baz$Nested"`).
   * @param memberName The simple member name.
   * @return the read [ImmutableKmClass] from its metadata, if any. For simple enum members with no
   *         class bodies, this should always be null.
   */
  fun enumEntry(enumClassJvmName: String, memberName: String): ImmutableKmClass?

  /**
   * Looks up if a given [methodSignature] within [classJvmName] exists.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method signature to check.
   * @return whether or not the method exists.
   */
  fun methodExists(classJvmName: String, methodSignature: JvmMethodSignature): Boolean

  companion object {

    // Top-level: package/of/class/MyClass
    // Nested A:  package/of/class/MyClass.InnerClass
    // Nested B:  package/of/class/MyClass$InnerClass
    private fun ImmutableKmClass.bestGuessSimpleName(): String {
      return name.substringAfterLast(
          '/', // Drop the package name, e.g. "package/of/class/"
          '.', // Drop any enclosing classes, e.g. "MyClass."
          '$' // Drop any enclosing classes, e.g. "MyClass$"
      )
    }

    private fun String.substringAfterLast(vararg delimiters: Char): String {
      val index = lastIndexOfAny(delimiters)
      return if (index == -1) this else substring(index + 1, length)
    }

    fun ImmutableKmProperty.computeIsJvmField(
      elementHandler: ElementHandler,
      isCompanionObject: Boolean,
      hasGetter: Boolean,
      hasSetter: Boolean,
      hasField: Boolean
    ): Boolean {
      return if (!hasGetter &&
          !hasSetter &&
          hasField &&
          !isConst) {
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
      val result = TreeSet<AnnotationSpec>(compareBy { it.toString() }).apply {
        body()
      }
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
}
