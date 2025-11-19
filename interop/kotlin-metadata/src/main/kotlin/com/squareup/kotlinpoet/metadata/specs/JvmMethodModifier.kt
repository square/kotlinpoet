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
import com.squareup.kotlinpoet.asClassName

/** Modifiers that are annotations or implicit in Kotlin but modifier keywords in bytecode. */
public enum class JvmMethodModifier : JvmModifier {
  STATIC {
    override fun annotationSpec(): AnnotationSpec =
      AnnotationSpec.builder(JvmStatic::class.asClassName()).build()
  },
  SYNCHRONIZED {
    override fun annotationSpec(): AnnotationSpec =
      AnnotationSpec.builder(Synchronized::class.asClassName()).build()
  },
  DEFAULT,
}
