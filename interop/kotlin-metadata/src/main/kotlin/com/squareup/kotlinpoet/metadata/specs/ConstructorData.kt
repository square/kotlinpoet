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
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil

/**
 * Represents relevant information on a constructor used for [ClassInspector]. Should only be
 * associated with constructors of a [ClassData].
 *
 * @param annotations declared annotations on this constructor.
 * @property parameterAnnotations a mapping of parameter indices to annotations on them.
 * @property isSynthetic indicates if this constructor is synthetic or not.
 * @property jvmModifiers set of [JvmMethodModifiers][JvmMethodModifier] on this constructor.
 * @property exceptions list of exceptions thrown by this constructor.
 */
public data class ConstructorData(
  private val annotations: List<AnnotationSpec>,
  val parameterAnnotations: Map<Int, Collection<AnnotationSpec>>,
  val isSynthetic: Boolean,
  val jvmModifiers: Set<JvmMethodModifier>,
  val exceptions: List<TypeName>,
) {

  /**
   * A collection of all annotations on this constructor, including any derived from [jvmModifiers],
   * [isSynthetic], and [exceptions].
   */
  val allAnnotations: Collection<AnnotationSpec> =
    ClassInspectorUtil.createAnnotations {
      addAll(annotations)
      if (isSynthetic) {
        add(ClassInspectorUtil.JVM_SYNTHETIC_SPEC)
      }
      addAll(jvmModifiers.mapNotNull { it.annotationSpec() })
      exceptions.takeIf { it.isNotEmpty() }?.let { add(ClassInspectorUtil.createThrowsSpec(it)) }
    }

  public companion object {
    public val EMPTY: ConstructorData =
      ConstructorData(
        annotations = emptyList(),
        parameterAnnotations = emptyMap(),
        isSynthetic = false,
        jvmModifiers = emptySet(),
        exceptions = emptyList(),
      )
  }
}
