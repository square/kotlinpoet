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

  @Test fun topLevelMembersRetainOrder() {
    val source = KotlinFile.builder(tacosPackage, "Taco")
        .addFun(FunSpec.builder("a").addModifiers(KModifier.PUBLIC).build())
        .addType(TypeSpec.classBuilder("B").build())
        .addProperty(PropertySpec.builder(String::class, "c", KModifier.PUBLIC)
            .initializer("%S", "C")
            .build())
        .addFun(FunSpec.builder("d").build())
        .addType(TypeSpec.classBuilder("E").build())
        .addProperty(PropertySpec.builder(String::class, "f", KModifier.PUBLIC)
            .initializer("%S", "F")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.String
        |
        |fun a() {
        |}
        |
        |class B {
        |}
        |
        |val c: String = "C"
        |
        |fun d() {
        |}
        |
        |class E {
        |}
        |
        |val f: String = "F"
        |""".trimMargin())
  }

  @Test fun noTopLevelConstructor() {
    try {
      KotlinFile.builder(tacosPackage, "Taco")
          .addFun(FunSpec.constructorBuilder().build())
      fail()
    } catch(expected: IllegalArgumentException) {
    }
  }

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

  @Test fun nullable() {
    val type = TypeName.get(String::class).nullable()
    assertThat(type.toString()).isEqualTo("java.lang.String?")

    val parameters = ParameterizedTypeName.get(MutableMap::class, String::class, Int::class).nullable()
    assertThat(parameters.toString()).isEqualTo("java.util.Map<java.lang.String, kotlin.Int>?")

    val array = ArrayTypeName.of(String::class).nullable()
    assertThat(array.toString()).isEqualTo("kotlin.Array<java.lang.String>?")
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

  @Test fun strings() {
    val source = KotlinFile.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("strings")
            .addStatement("val a = %S", "basic string")
            .addStatement("val b = %S", "string with a \$ dollar sign")
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("" +
        "package com.squareup.tacos\n" +
        "\n" +
        "class Taco {\n" +
        "  fun strings() {\n" +
        "    val a = \"basic string\"\n" +
        "    val b = \"string with a \$ dollar sign\"\n" +
        "  }\n" +
        "}\n")
  }

  /** When emitting a triple quote, KotlinPoet escapes the 3rd quote in the triplet. */
  @Test fun rawStrings() {
    val source = KotlinFile.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("strings")
            .addStatement("val a = %S", "\"\n")
            .addStatement("val b = %S", "a\"\"\"b\"\"\"\"\"\"c\n")
            .addStatement("val c = %S", """
            |whoa
            |"raw"
            |string
            """.trimMargin())
            .addStatement("val d = %S", """
            |"raw"
            |string
            |with
            |${'$'}a interpolated value
            """.trimMargin())
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("" +
        "package com.squareup.tacos\n" +
        "\n" +
        "class Taco {\n" +
        "  fun strings() {\n" +
        "    val a = \"\"\"\n" +
        "        |\"\n" +
        "        |\"\"\".trimMargin()\n" +
        "    val b = \"\"\"\n" +
        "        |a\"\"\${'\"'}b\"\"\${'\"'}\"\"\${'\"'}c\n" +
        "        |\"\"\".trimMargin()\n" +
        "    val c = \"\"\"\n" +
        "        |whoa\n" +
        "        |\"raw\"\n" +
        "        |string\n" +
        "        \"\"\".trimMargin()\n" +
        "    val d = \"\"\"\n" +
        "        |\"raw\"\n" +
        "        |string\n" +
        "        |with\n" +
        "        |\$a interpolated value\n" +
        "        \"\"\".trimMargin()\n" +
        "  }\n" +
        "}\n")
  }

  /**
   * When a string literal ends in a newline, there's a pipe `|` immediately preceding the closing
   * triple quote. Otherwise the closing triple quote has no preceding `|`.
   */
  @Test fun edgeCaseStrings() {
    val source = KotlinFile.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("strings")
            .addStatement("val a = %S", "\n")
            .addStatement("val b = %S", " \n ")
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("" +
        "package com.squareup.tacos\n" +
        "\n" +
        "class Taco {\n" +
        "  fun strings() {\n" +
        "    val a = \"\"\"\n" +
        "        |\n" +
        "        |\"\"\".trimMargin()\n" +
        "    val b = \"\"\"\n" +
        "        | \n" +
        "        | \n" +
        "        \"\"\".trimMargin()\n" +
        "  }\n" +
        "}\n")
  }

  @Test fun parameterDefaultValue() {
    val source = KotlinFile.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("addCheese")
            .addParameter(ParameterSpec.builder(String::class, "kind")
                .defaultValue("%S", "monterey jack")
                .build())
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.String
        |
        |class Taco {
        |  fun addCheese(kind: String = "monterey jack") {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun extensionFunction() {
    val source = KotlinFile.builder(tacosPackage, "Taco")
        .addFun(FunSpec.builder("shrink")
            .returns(String::class)
            .receiver(String::class)
            .addStatement("return substring(0, length - 1)")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.String
        |
        |fun String.shrink(): String {
        |  return substring(0, length - 1)
        |}
        |""".trimMargin())
  }
}
