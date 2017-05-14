/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.fail

class KotlinPoetTest {
  private val tacosPackage = "com.squareup.tacos"

  @Test fun primaryConstructor() {
    val source = KotlinFile.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(String::class, "cheese")
            .beginControlFlow("require (!cheese.isEmpty())")
            .addStatement("%S", "cheese cannot be empty")
            .endControlFlow()
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.String
        |
        |class Taco(cheese: String) {
        |  init {
        |    require (!cheese.isEmpty()) {
        |      "cheese cannot be empty"
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun propertyModifiers() {
    val source = KotlinFile.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder(
            String::class, "CHEESE", KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", "monterey jack")
            .build())
        .addProperty(PropertySpec.varBuilder(String::class, "sauce", KModifier.PUBLIC)
            .initializer("%S", "chipotle mayo")
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.String
        |
        |class Taco {
        |  private const val CHEESE: String = "monterey jack"
        |
        |  var sauce: String = "chipotle mayo"
        |}
        |""".trimMargin())
  }

  @Test fun mistargetedModifier() {
    try {
      PropertySpec.builder(String::class, "CHEESE", KModifier.DATA)
      fail()
    } catch(expected: IllegalArgumentException) {
    }
  }

  @Test fun visibilityModifiers() {
    val source = KotlinFile.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("a").addModifiers(KModifier.PUBLIC).build())
        .addFun(FunSpec.builder("b").addModifiers(KModifier.PROTECTED).build())
        .addFun(FunSpec.builder("c").addModifiers(KModifier.INTERNAL).build())
        .addFun(FunSpec.builder("d").addModifiers(KModifier.PRIVATE).build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  fun a() {
        |  }
        |
        |  protected fun b() {
        |  }
        |
        |  internal fun c() {
        |  }
        |
        |  private fun d() {
        |  }
        |}
        |""".trimMargin())
  }
}
