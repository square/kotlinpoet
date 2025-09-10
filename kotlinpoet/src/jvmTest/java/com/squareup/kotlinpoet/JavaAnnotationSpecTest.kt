/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.kotlinpoet

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.google.testing.compile.CompilationRule
import kotlin.test.Test
import org.junit.Rule

class JavaAnnotationSpecTest {

  @Rule @JvmField
  val compilation = CompilationRule()

  @Test fun getOnValueArrayTypeMirrorShouldNameValueArg() {
    val myClazz = compilation.elements
      .getTypeElement(JavaClassWithArrayValueAnnotation::class.java.canonicalName)
    val classBuilder = TypeSpec.classBuilder("Result")

    myClazz.annotationMirrors.map { AnnotationSpec.get(it) }
      .forEach {
        classBuilder.addAnnotation(it)
      }

    assertThat(toString(classBuilder.build())).isEqualTo(
      """
            |package com.squareup.tacos
            |
            |import com.squareup.kotlinpoet.JavaClassWithArrayValueAnnotation
            |import java.lang.Boolean
            |import java.lang.Object
            |
            |@JavaClassWithArrayValueAnnotation.AnnotationWithArrayValue(value = arrayOf(Object::class, Boolean::class))
            |public class Result
            |
      """.trimMargin(),
    )
  }

  @Test fun getOnValueArrayTypeAnnotationShouldNameValueArg() {
    val annotation = JavaClassWithArrayValueAnnotation::class.java.getAnnotation(
      JavaClassWithArrayValueAnnotation.AnnotationWithArrayValue::class.java,
    )
    val classBuilder = TypeSpec.classBuilder("Result")
      .addAnnotation(AnnotationSpec.get(annotation))

    assertThat(toString(classBuilder.build()).trim()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.JavaClassWithArrayValueAnnotation
        |import java.lang.Boolean
        |import java.lang.Object
        |
        |@JavaClassWithArrayValueAnnotation.AnnotationWithArrayValue(value = arrayOf(Object::class, Boolean::class))
        |public class Result
      """.trimMargin(),
    )
  }

  private fun toString(typeSpec: TypeSpec) =
    FileSpec.get("com.squareup.tacos", typeSpec).toString()
}
