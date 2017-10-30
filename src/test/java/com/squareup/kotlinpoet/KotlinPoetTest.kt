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

class KotlinPoetTest {
  private val tacosPackage = "com.squareup.tacos"

  @Test fun topLevelMembersRetainOrder() {
    val source = FileSpec.builder(tacosPackage, "Taco")
        .addFunction(FunSpec.builder("a").addModifiers(KModifier.PUBLIC).build())
        .addType(TypeSpec.classBuilder("B").build())
        .addProperty(PropertySpec.builder("c", String::class, KModifier.PUBLIC)
            .initializer("%S", "C")
            .build())
        .addFunction(FunSpec.builder("d").build())
        .addType(TypeSpec.classBuilder("E").build())
        .addProperty(PropertySpec.builder("f", String::class, KModifier.PUBLIC)
            .initializer("%S", "F")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |fun a() {
        |}
        |
        |class B
        |
        |val c: String = "C"
        |
        |fun d() {
        |}
        |
        |class E
        |
        |val f: String = "F"
        |""".trimMargin())
  }

  @Test fun noTopLevelConstructor() {
    assertThrows<IllegalArgumentException> {
      FileSpec.builder(tacosPackage, "Taco")
          .addFunction(FunSpec.constructorBuilder().build())
    }
  }

  @Test fun primaryConstructor() {
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("cheese", String::class)
            .beginControlFlow("require(cheese.isNotEmpty())")
            .addStatement("%S", "cheese cannot be empty")
            .endControlFlow()
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco(cheese: String) {
        |  init {
        |    require(cheese.isNotEmpty()) {
        |      "cheese cannot be empty"
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun primaryConstructorProperties() {
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("cheese", String::class)
            .addParameter("cilantro", String::class)
            .addParameter("lettuce", String::class)
            .beginControlFlow("require(!cheese.isEmpty())")
            .addStatement("%S", "cheese cannot be empty")
            .endControlFlow()
            .build())
        .addProperty(PropertySpec.builder("cheese", String::class).initializer("cheese").build())
        .addProperty(PropertySpec.varBuilder("cilantro", String::class).initializer("cilantro").build())
        .addProperty(PropertySpec.builder("lettuce", String::class).initializer("lettuce.trim()").build())
        .addProperty(PropertySpec.builder("onion", Boolean::class).initializer("true").build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Boolean
        |import kotlin.String
        |
        |class Taco(
        |    val cheese: String,
        |    var cilantro: String,
        |    lettuce: String
        |) {
        |  val lettuce: String = lettuce.trim()
        |
        |  val onion: Boolean = true
        |  init {
        |    require(!cheese.isEmpty()) {
        |      "cheese cannot be empty"
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun propertyModifiers() {
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("CHEESE", String::class, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", "monterey jack")
            .build())
        .addProperty(PropertySpec.varBuilder("sauce", String::class, KModifier.PUBLIC)
            .initializer("%S", "chipotle mayo")
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  private const val CHEESE: String = "monterey jack"
        |
        |  var sauce: String = "chipotle mayo"
        |}
        |""".trimMargin())
  }

  @Test fun mistargetedModifier() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("CHEESE", String::class, KModifier.DATA)
    }
  }

  @Test fun visibilityModifiers() {
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("a").addModifiers(KModifier.PUBLIC).build())
        .addFunction(FunSpec.builder("b").addModifiers(KModifier.PROTECTED).build())
        .addFunction(FunSpec.builder("c").addModifiers(KModifier.INTERNAL).build())
        .addFunction(FunSpec.builder("d").addModifiers(KModifier.PRIVATE).build())
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
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("strings")
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
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("strings")
            .addCode("val a = %S\n", "\"\n")
            .addCode("val b = %S\n", "a\"\"\"b\"\"\"\"\"\"c\n")
            .addCode("val c = %S\n", """
            |whoa
            |"raw"
            |string
            """.trimMargin())
            .addCode("val d = %S\n", """
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
        "      |\"\n" +
        "      |\"\"\".trimMargin()\n" +
        "    val b = \"\"\"\n" +
        "      |a\"\"\${'\"'}b\"\"\${'\"'}\"\"\${'\"'}c\n" +
        "      |\"\"\".trimMargin()\n" +
        "    val c = \"\"\"\n" +
        "      |whoa\n" +
        "      |\"raw\"\n" +
        "      |string\n" +
        "      \"\"\".trimMargin()\n" +
        "    val d = \"\"\"\n" +
        "      |\"raw\"\n" +
        "      |string\n" +
        "      |with\n" +
        "      |\$a interpolated value\n" +
        "      \"\"\".trimMargin()\n" +
        "  }\n" +
        "}\n")
  }

  /**
   * When a string literal ends in a newline, there's a pipe `|` immediately preceding the closing
   * triple quote. Otherwise the closing triple quote has no preceding `|`.
   */
  @Test fun edgeCaseStrings() {
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("strings")
            .addCode("val a = %S\n", "\n")
            .addCode("val b = %S\n", " \n ")
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("" +
        "package com.squareup.tacos\n" +
        "\n" +
        "class Taco {\n" +
        "  fun strings() {\n" +
        "    val a = \"\"\"\n" +
        "      |\n" +
        "      |\"\"\".trimMargin()\n" +
        "    val b = \"\"\"\n" +
        "      | \n" +
        "      | \n" +
        "      \"\"\".trimMargin()\n" +
        "  }\n" +
        "}\n")
  }

  @Test fun parameterDefaultValue() {
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("addCheese")
            .addParameter(ParameterSpec.builder("kind", String::class)
                .defaultValue("%S", "monterey jack")
                .build())
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  fun addCheese(kind: String = "monterey jack") {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun extensionFunction() {
    val source = FileSpec.builder(tacosPackage, "Taco")
        .addFunction(FunSpec.builder("shrink")
            .returns(String::class)
            .receiver(String::class)
            .addStatement("return substring(0, length - 1)")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |fun String.shrink(): String = substring(0, length - 1)
        |""".trimMargin())
  }

  @Test fun extensionProperty() {
    val source = FileSpec.builder(tacosPackage, "Taco")
        .addProperty(PropertySpec.builder("extensionProperty", Int::class)
            .receiver(String::class)
            .getter(FunSpec.getterBuilder()
                .addStatement("return length")
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.String
        |
        |val String.extensionProperty: Int
        |  get() = length
        |
        """.trimMargin())
  }

  @Test fun nullableTypes() {
    val list = ParameterizedTypeName.get(List::class.asClassName().asNullable(),
        Int::class.asClassName().asNullable()).asNullable()
    assertThat(list.toString()).isEqualTo("kotlin.collections.List<kotlin.Int?>?")
  }

  @Test fun getAndSet() {
    val source = FileSpec.builder(tacosPackage, "Taco")
        .addProperty(PropertySpec.varBuilder("propertyWithCustomAccessors", Int::class)
            .initializer("%L", 1)
            .getter(FunSpec.getterBuilder()
                .addStatement("println(%S)", "getter")
                .addStatement("return field")
                .build())
            .setter(FunSpec.setterBuilder()
                .addParameter("value", Int::class)
                .addStatement("println(%S)", "setter")
                .addStatement("field = value")
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |var propertyWithCustomAccessors: Int = 1
        |  get() {
        |    println("getter")
        |    return field
        |  }
        |  set(value) {
        |    println("setter")
        |    field = value
        |  }
        |""".trimMargin())
  }

  @Test fun stackedPropertyModifiers() {
    val source = FileSpec.builder(tacosPackage, "Taco")
        .addType(TypeSpec.classBuilder("A")
            .addModifiers(KModifier.ABSTRACT)
            .addProperty(PropertySpec.varBuilder("q", String::class)
                .addModifiers(KModifier.ABSTRACT, KModifier.PROTECTED)
                .build())
            .build())
        .addProperty(PropertySpec.builder("p", String::class)
            .addModifiers(KModifier.CONST, KModifier.INTERNAL)
            .initializer("%S", "a")
            .build())
        .addType(TypeSpec.classBuilder("B")
            .superclass(ClassName("", "A"))
            .addModifiers(KModifier.ABSTRACT)
            .addProperty(PropertySpec.varBuilder("q", String::class)
                .addModifiers(
                    KModifier.FINAL, KModifier.LATEINIT, KModifier.OVERRIDE, KModifier.PUBLIC)
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |abstract class A {
        |  protected abstract var q: String
        |}
        |
        |internal const val p: String = "a"
        |
        |abstract class B : A() {
        |  final override lateinit var q: String
        |}
        |""".trimMargin())
  }

  @Test fun stackedFunModifiers() {
    val source = FileSpec.get(tacosPackage, TypeSpec.classBuilder("A")
        .addModifiers(KModifier.OPEN)
        .addFunction(FunSpec.builder("get")
            .addModifiers(KModifier.EXTERNAL, KModifier.INFIX, KModifier.OPEN, KModifier.OPERATOR,
                KModifier.PROTECTED)
            .addParameter("v", String::class)
            .returns(String::class)
            .build())
        .addFunction(FunSpec.builder("loop")
            .addModifiers(KModifier.FINAL, KModifier.INLINE, KModifier.INTERNAL, KModifier.TAILREC)
            .returns(String::class)
            .addStatement("return %S", "a")
            .build())
        .build())
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |open class A {
        |  protected open infix operator external fun get(v: String): String
        |
        |  internal final inline tailrec fun loop(): String = "a"
        |}
        |""".trimMargin())
  }

  @Test fun basicExpressionBody() {
    val source = FileSpec.builder(tacosPackage, "Taco")
        .addFunction(FunSpec.builder("addA")
            .addParameter("s", String::class)
            .returns(String::class)
            .addStatement("return s + %S", "a")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |fun addA(s: String): String = s + "a"
        |""".trimMargin())
  }
}
