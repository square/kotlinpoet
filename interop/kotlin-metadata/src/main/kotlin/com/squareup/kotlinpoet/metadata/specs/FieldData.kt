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
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FIELD
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil

/**
 * Represents relevant information on a field used for [ClassInspector]. Should only be associated
 * with a [PropertyData].
 *
 * @param annotations declared annotations on this field.
 * @property isSynthetic indicates if this field is synthetic or not.
 * @property jvmModifiers set of [JvmMethodModifiers][JvmMethodModifier] on this field.
 * @property constant the constant value of this field, if available. Note that this is does not
 *   strictly imply that the associated property is `const`.
 */
public data class FieldData(
  private val annotations: List<AnnotationSpec>,
  val isSynthetic: Boolean,
  val jvmModifiers: Set<JvmFieldModifier>,
  val constant: CodeBlock?,
) {

  /**
   * A collection of all annotations on this method, including any derived from [jvmModifiers] and
   * [isSynthetic].
   */
  val allAnnotations: Collection<AnnotationSpec> =
    ClassInspectorUtil.createAnnotations(FIELD) {
      addAll(annotations)
      if (isSynthetic) {
        add(ClassInspectorUtil.JVM_SYNTHETIC_SPEC)
      }
      addAll(jvmModifiers.mapNotNull(JvmFieldModifier::annotationSpec))
    }

  public companion object {
    public val SYNTHETIC: FieldData =
      FieldData(
        annotations = emptyList(),
        isSynthetic = true,
        jvmModifiers = emptySet(),
        constant = null,
      )
  }
}
