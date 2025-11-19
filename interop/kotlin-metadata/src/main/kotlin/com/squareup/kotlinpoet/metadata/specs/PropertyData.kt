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
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.GET
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.SET
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil

/**
 * Represents relevant information on a property used for [ClassInspector]. Should only be
 * associated with properties of a [ClassData].
 *
 * @param annotations declared annotations on this property.
 * @property fieldData associated [FieldData] with this property, if any.
 * @property getterData associated getter (as [MethodData]) with this property, if any.
 * @property setterData associated setter (as [MethodData]) with this property, if any.
 * @property isJvmField indicates if this property should be treated as a jvm field.
 */
public data class PropertyData(
  private val annotations: List<AnnotationSpec>,
  val fieldData: FieldData?,
  val getterData: MethodData?,
  val setterData: MethodData?,
  val isJvmField: Boolean,
) {
  /** Indicates if this property overrides another from a supertype. */
  val isOverride: Boolean = (getterData?.isOverride ?: false) || (setterData?.isOverride ?: false)

  /**
   * A collection of all annotations on this property including declared ones and any derived from
   * [fieldData], [getterData], [setterData], and [isJvmField].
   */
  val allAnnotations: Collection<AnnotationSpec> =
    ClassInspectorUtil.createAnnotations {
      // Don't add annotations that are already defined on the parent
      val higherScopedAnnotations = annotations.associateBy { it.typeName }
      val fieldAnnotations =
        fieldData
          ?.allAnnotations
          .orEmpty()
          .filterNot { it.typeName in higherScopedAnnotations }
          .associateByTo(LinkedHashMap()) { it.typeName }
      val getterAnnotations =
        getterData
          ?.allAnnotations(GET)
          .orEmpty()
          .filterNot { it.typeName in higherScopedAnnotations }
          .associateByTo(LinkedHashMap()) { it.typeName }

      val finalTopAnnotations = annotations.toMutableList()

      // If this is a val, and annotation is on both getter and field, we can move it to just the
      // regular annotations
      if (setterData == null && !isJvmField) {
        val sharedAnnotations = getterAnnotations.keys.intersect(fieldAnnotations.keys)
        for (sharedAnnotation in sharedAnnotations) {
          // Add it to the top-level annotations without a site-target
          finalTopAnnotations +=
            getterAnnotations.getValue(sharedAnnotation).toBuilder().useSiteTarget(null).build()

          // Remove from field and getter
          fieldAnnotations.remove(sharedAnnotation)
          getterAnnotations.remove(sharedAnnotation)
        }
      }

      addAll(finalTopAnnotations)
      addAll(fieldAnnotations.values)
      addAll(getterAnnotations.values)
      addAll(
        setterData?.allAnnotations(SET).orEmpty().filterNot {
          it.typeName in higherScopedAnnotations
        }
      )
      if (isJvmField) {
        add(ClassInspectorUtil.JVM_FIELD_SPEC)
      }
    }
}
