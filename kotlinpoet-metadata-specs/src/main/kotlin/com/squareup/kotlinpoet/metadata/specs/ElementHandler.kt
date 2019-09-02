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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmMethodSignature

/**
 * A basic interface for looking up information about JVM elements.
 */
@KotlinPoetMetadataPreview
interface ElementHandler {

  interface JvmModifier {
    fun annotationSpec(): AnnotationSpec
  }

  /** Modifiers that are annotations in Kotlin but modifier keywords in bytecode. */
  enum class JvmFieldModifier : JvmModifier {
    STATIC {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(JvmStatic::class.asClassName()).build()
    },
    TRANSIENT {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(Transient::class.asClassName()).build()
    },
    VOLATILE {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(Volatile::class.asClassName()).build()
    };
  }

  /** Modifiers that are annotations in Kotlin but modifier keywords in bytecode. */
  enum class JvmMethodModifier : JvmModifier {
    STATIC {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(JvmStatic::class.asClassName()).build()
    },
    SYNCHRONIZED {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(Synchronized::class.asClassName()).build()
    }
  }

  /**
   * Indicates if this element handler supports [AnnotationRetention.RUNTIME]-retained annotations.
   * This is used to indicate if manual inference of certain non-RUNTIME-retained annotations should
   * be done, such as [JvmName].
   */
  val supportsNonRuntimeRetainedAnnotations: Boolean

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
   * Looks up a given class field given its [JvmFieldSignature] and returns any [JvmModifier]s
   * found on it.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param fieldSignature The field to look up.
   * @param isJvmField Indicates if this field is a JvmField, in which case it should not be marked
   *                   as JvmStatic.
   * @return the set of found modifiers.
   */
  fun fieldJvmModifiers(
    classJvmName: String,
    fieldSignature: JvmFieldSignature,
    isJvmField: Boolean
  ): Set<JvmFieldModifier>

  /**
   * Looks up the annotations on a given class field given its [JvmFieldSignature].
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param fieldSignature The field with annotations to look up.
   * @return the [AnnotationSpec] representations of the annotations on the target field.
   */
  fun fieldAnnotations(classJvmName: String, fieldSignature: JvmFieldSignature): List<AnnotationSpec>

  /**
   * Looks up a [JvmFieldSignature] and returns whether or not it is synthetic.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param fieldSignature The field to look up.
   * @return whether or not the field is synthetic.
   */
  fun isFieldSynthetic(classJvmName: String, fieldSignature: JvmFieldSignature): Boolean

  /**
   * Looks up a given class method given its [JvmMethodSignature] and returns any [JvmMethodModifier]s
   * found on it.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method with annotations to look up.
   * @return the set of found modifiers.
   */
  fun methodJvmModifiers(classJvmName: String, methodSignature: JvmMethodSignature): Set<JvmMethodModifier>

  /**
   * Looks up the annotations on a given class constructor given its [JvmMethodSignature].
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param constructorSignature The constructor with annotations to look up.
   * @return the [AnnotationSpec] representations of the annotations on the target method.
   */
  fun constructorAnnotations(classJvmName: String, constructorSignature: JvmMethodSignature): List<AnnotationSpec>

  /**
   * Looks up the annotations on a given class method given its [JvmMethodSignature].
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method with annotations to look up.
   * @return the [AnnotationSpec] representations of the annotations on the target method.
   */
  fun methodAnnotations(classJvmName: String, methodSignature: JvmMethodSignature): List<AnnotationSpec>

  /**
   * Looks up a [JvmMethodSignature] and returns whether or not it is synthetic.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method to look up.
   * @return whether or not the method is synthetic.
   */
  fun isMethodSynthetic(classJvmName: String, methodSignature: JvmMethodSignature): Boolean

  /**
   * Looks up a given class method given its [JvmMethodSignature] and returns any thrown types
   * found on it. Used for [Throws][@Throws]
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method with annotations to look up.
   * @param isConstructor Indicates if [methodSignature] is a constructor.
   * @return the set of found thrown types, or empty.
   */
  fun methodExceptions(
    classJvmName: String,
    methodSignature: JvmMethodSignature,
    isConstructor: Boolean
  ): Set<TypeName>

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
   * Looks up the constant value on a given class field given its [JvmFieldSignature].
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param fieldSignature The field with annotations to look up.
   * @return the [CodeBlock] representation of the constant on the target field. Null if the value
   *         cannot be resolved.
   */
  fun fieldConstant(classJvmName: String, fieldSignature: JvmFieldSignature): CodeBlock?

  /**
   * Looks up if a given [methodSignature] within [classJvmName] is an override of another method.
   * Implementers should search for a matching method signature in the supertypes/classes of
   * [classJvmName] to see if there are matches.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method signature to check.
   * @return whether or not the method is an override.
   */
  fun isMethodOverride(classJvmName: String, methodSignature: JvmMethodSignature): Boolean

  /**
   * Looks up if a given [methodSignature] within [classJvmName] exists.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method signature to check.
   * @return whether or not the method exists.
   */
  fun methodExists(classJvmName: String, methodSignature: JvmMethodSignature): Boolean
}
