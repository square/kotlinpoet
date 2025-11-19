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
package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil

/**
 * Represents relevant information on a method used for [ClassInspector]. Should only be associated
 * with methods of a [ClassData] or [PropertyData].
 *
 * @param annotations declared annotations on this method.
 * @property parameterAnnotations a mapping of parameter indices to annotations on them.
 * @property isSynthetic indicates if this method is synthetic or not.
 * @property jvmModifiers set of [JvmMethodModifiers][JvmMethodModifier] on this method.
 * @property isOverride indicates if this method overrides one in a supertype.
 * @property exceptions list of exceptions thrown by this method.
 */
public data class MethodData(
  private val annotations: List<AnnotationSpec>,
  val parameterAnnotations: Map<Int, Collection<AnnotationSpec>>,
  val isSynthetic: Boolean,
  val jvmModifiers: Set<JvmMethodModifier>,
  val isOverride: Boolean,
  val exceptions: List<TypeName>,
) {

  /**
   * A collection of all annotations on this method, including any derived from [jvmModifiers],
   * [isSynthetic], and [exceptions].
   *
   * @param useSiteTarget an optional [UseSiteTarget] that all annotations on this method should
   *   use.
   * @param containsReifiedTypeParameter an optional boolean indicating if any type parameters on
   *   this function are `reified`, which are implicitly synthetic.
   */
  public fun allAnnotations(
    useSiteTarget: UseSiteTarget? = null,
    containsReifiedTypeParameter: Boolean = false,
  ): Collection<AnnotationSpec> {
    return ClassInspectorUtil.createAnnotations(useSiteTarget) {
      addAll(annotations)
      if (isSynthetic && !containsReifiedTypeParameter) {
        add(ClassInspectorUtil.JVM_SYNTHETIC_SPEC)
      }
      addAll(jvmModifiers.mapNotNull(JvmMethodModifier::annotationSpec))
      exceptions
        .takeIf { it.isNotEmpty() }
        ?.let { add(ClassInspectorUtil.createThrowsSpec(it, useSiteTarget)) }
    }
  }

  public companion object {
    public val SYNTHETIC: MethodData =
      MethodData(
        annotations = emptyList(),
        parameterAnnotations = emptyMap(),
        isSynthetic = true,
        jvmModifiers = emptySet(),
        isOverride = false,
        exceptions = emptyList(),
      )
    public val EMPTY: MethodData =
      MethodData(
        annotations = emptyList(),
        parameterAnnotations = emptyMap(),
        isSynthetic = false,
        jvmModifiers = emptySet(),
        isOverride = false,
        exceptions = emptyList(),
      )
  }
}
