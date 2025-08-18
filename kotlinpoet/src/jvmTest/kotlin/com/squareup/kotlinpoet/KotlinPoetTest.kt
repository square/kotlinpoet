/*
 * Copyright (C) 2017 Square, Inc.
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

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.kotlinpoet.jvm.jvmSuppressWildcards
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class KotlinPoetTest {
  private val tacosPackage = "com.squareup.tacos"

  @Test fun topLevelMembersRetainOrder() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addFunction(FunSpec.builder("a").addModifiers(KModifier.PUBLIC).build())
      .addType(TypeSpec.classBuilder("B").build())
      .addProperty(
        PropertySpec.builder("c", String::class, KModifier.PUBLIC)
          .initializer("%S", "C")
          .build(),
      )
      .addFunction(FunSpec.builder("d").build())
      .addType(TypeSpec.classBuilder("E").build())
      .addProperty(
        PropertySpec.builder("f", String::class, KModifier.PUBLIC)
          .initializer("%S", "F")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun a() {
        |}
        |
        |public class B
        |
        |public val c: String = "C"
        |
        |public fun d() {
        |}
        |
        |public class E
        |
        |public val f: String = "F"
        |
      """.trimMargin(),
    )
  }

  @Test fun noTopLevelConstructor() {
    assertThrows<IllegalArgumentException> {
      FileSpec.builder(tacosPackage, "Taco")
        .addFunction(FunSpec.constructorBuilder().build())
    }
  }

  @Test fun primaryConstructor() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("Taco")
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("cheese", String::class)
            .beginControlFlow("require(cheese.isNotEmpty())")
            .addStatement("%S", "cheese cannot be empty")
            .endControlFlow()
            .build(),
        )
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco(
        |  cheese: String,
        |) {
        |  init {
        |    require(cheese.isNotEmpty()) {
        |      "cheese cannot be empty"
        |    }
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun primaryConstructorProperties() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("Taco")
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("cheese", String::class)
            .addParameter("cilantro", String::class)
            .addParameter("lettuce", String::class)
            .beginControlFlow("require(!cheese.isEmpty())")
            .addStatement("%S", "cheese cannot be empty")
            .endControlFlow()
            .build(),
        )
        .addProperty(
          PropertySpec.builder("cheese", String::class)
            .initializer("cheese")
            .build(),
        )
        .addProperty(
          PropertySpec.builder("cilantro", String::class.asTypeName())
            .mutable()
            .initializer("cilantro")
            .build(),
        )
        .addProperty(
          PropertySpec.builder("lettuce", String::class)
            .initializer("lettuce.trim()")
            .build(),
        )
        .addProperty(
          PropertySpec.builder("onion", Boolean::class)
            .initializer("true")
            .build(),
        )
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.Boolean
        |import kotlin.String
        |
        |public class Taco(
        |  public val cheese: String,
        |  public var cilantro: String,
        |  lettuce: String,
        |) {
        |  public val lettuce: String = lettuce.trim()
        |
        |  public val onion: Boolean = true
        |  init {
        |    require(!cheese.isEmpty()) {
        |      "cheese cannot be empty"
        |    }
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun propertyModifiers() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("Taco")
        .addProperty(
          PropertySpec.builder("CHEESE", String::class, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", "monterey jack")
            .build(),
        )
        .addProperty(
          PropertySpec.builder("sauce", String::class.asTypeName(), KModifier.PUBLIC)
            .mutable()
            .initializer("%S", "chipotle mayo")
            .build(),
        )
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco {
        |  private const val CHEESE: String = "monterey jack"
        |
        |  public var sauce: String = "chipotle mayo"
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun mistargetedModifier() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("CHEESE", String::class, KModifier.DATA).build()
    }
  }

  @Test fun visibilityModifiers() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("a").addModifiers(KModifier.PUBLIC).build())
        .addFunction(FunSpec.builder("b").addModifiers(KModifier.PROTECTED).build())
        .addFunction(FunSpec.builder("c").addModifiers(KModifier.INTERNAL).build())
        .addFunction(FunSpec.builder("d").addModifiers(KModifier.PRIVATE).build())
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |public class Taco {
        |  public fun a() {
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
        |
      """.trimMargin(),
    )
  }

  @Test fun strings() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("Taco")
        .addFunction(
          FunSpec.builder("strings")
            .addStatement("val a = %S", "basic string")
            .addStatement("val b = %S", "string with a \$ dollar sign")
            .build(),
        )
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      "" +
        "package com.squareup.tacos\n" +
        "\n" +
        "public class Taco {\n" +
        "  public fun strings() {\n" +
        "    val a = \"basic string\"\n" +
        "    val b = \"string with a \${\'\$\'} dollar sign\"\n" +
        "  }\n" +
        "}\n",
    )
  }

  /** When emitting a triple quote, KotlinPoet escapes the 3rd quote in the triplet. */
  @Test fun rawStrings() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("Taco")
        .addFunction(
          FunSpec.builder("strings")
            .addStatement("val a = %S", "\"\n")
            .addStatement("val b = %S", "a\"\"\"b\"\"\"\"\"\"c\n")
            .addStatement(
              "val c = %S",
              """
            |whoa
            |"raw"
            |string
              """.trimMargin(),
            )
            .addStatement(
              "val d = %S",
              """
            |"raw"
            |string
            |with
            |${'$'}a interpolated value
              """.trimMargin(),
            )
            .build(),
        )
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      "" +
        "package com.squareup.tacos\n" +
        "\n" +
        "public class Taco {\n" +
        "  public fun strings() {\n" +
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
        "        |\${\'\$\'}a interpolated value\n" +
        "        \"\"\".trimMargin()\n" +
        "  }\n" +
        "}\n",
    )
  }

  /**
   * When a string literal ends in a newline, there's a pipe `|` immediately preceding the closing
   * triple quote. Otherwise the closing triple quote has no preceding `|`.
   */
  @Test fun edgeCaseStrings() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("Taco")
        .addFunction(
          FunSpec.builder("strings")
            .addStatement("val a = %S", "\n")
            .addStatement("val b = %S", " \n ")
            .build(),
        )
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      "" +
        "package com.squareup.tacos\n" +
        "\n" +
        "public class Taco {\n" +
        "  public fun strings() {\n" +
        "    val a = \"\"\"\n" +
        "        |\n" +
        "        |\"\"\".trimMargin()\n" +
        "    val b = \"\"\"\n" +
        "        | \n" +
        "        | \n" +
        "        \"\"\".trimMargin()\n" +
        "  }\n" +
        "}\n",
    )
  }

  @Test fun parameterDefaultValue() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("Taco")
        .addFunction(
          FunSpec.builder("addCheese")
            .addParameter(
              ParameterSpec.builder("kind", String::class)
                .defaultValue("%S", "monterey jack")
                .build(),
            )
            .build(),
        )
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco {
        |  public fun addCheese(kind: String = "monterey jack") {
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun extensionFunction() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addFunction(
        FunSpec.builder("shrink")
          .returns(String::class)
          .receiver(String::class)
          .addStatement("return substring(0, length - 1)")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun String.shrink(): String = substring(0, length - 1)
        |
      """.trimMargin(),
    )
  }

  @Test fun extensionFunctionLambda() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addFunction(
        FunSpec.builder("shrink")
          .returns(String::class)
          .receiver(
            LambdaTypeName.get(
              parameters = arrayOf(String::class.asClassName()),
              returnType = String::class.asTypeName(),
            ),
          )
          .addStatement("return substring(0, length - 1)")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun ((String) -> String).shrink(): String = substring(0, length - 1)
        |
      """.trimMargin(),
    )
  }

  @Test fun extensionFunctionLambdaWithParamName() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addFunction(
        FunSpec.builder("whatever")
          .returns(Unit::class)
          .receiver(
            LambdaTypeName.get(
              parameters = arrayOf(ParameterSpec.builder("name", String::class).build()),
              returnType = Unit::class.asClassName(),
            ),
          )
          .addStatement("return Unit")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.Unit
      |
      |public fun ((name: String) -> Unit).whatever(): Unit = Unit
      |
      """.trimMargin(),
    )
  }

  @Test fun extensionFunctionLambdaWithMultipleParams() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addFunction(
        FunSpec.builder("whatever")
          .returns(Unit::class)
          .receiver(
            LambdaTypeName.get(
              parameters = listOf(
                ParameterSpec.builder("name", String::class).build(),
                ParameterSpec.unnamed(Int::class),
                ParameterSpec.builder("age", Long::class).build(),
              ),
              returnType = Unit::class.asClassName(),
            ),
          )
          .addStatement("return Unit")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.Unit
      |
      |public fun ((
      |  name: String,
      |  Int,
      |  age: Long,
      |) -> Unit).whatever(): Unit = Unit
      |
      """.trimMargin(),
    )
  }

  @Test fun extensionProperty() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addProperty(
        PropertySpec.builder("extensionProperty", Int::class)
          .receiver(String::class)
          .getter(
            FunSpec.getterBuilder()
              .addStatement("return length")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.String
        |
        |public val String.extensionProperty: Int
        |  get() = length
        |
      """.trimMargin(),
    )
  }

  @Test fun extensionPropertyLambda() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addProperty(
        PropertySpec.builder("extensionProperty", Int::class)
          .receiver(
            LambdaTypeName.get(
              parameters = arrayOf(String::class.asClassName()),
              returnType = String::class.asClassName(),
            ),
          )
          .getter(
            FunSpec.getterBuilder()
              .addStatement("return length")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.String
        |
        |public val ((String) -> String).extensionProperty: Int
        |  get() = length
        |
      """.trimMargin(),
    )
  }

  @Test fun nullableTypes() {
    val list = (List::class.asClassName().copy(nullable = true) as ClassName)
      .parameterizedBy(Int::class.asClassName().copy(nullable = true))
      .copy(nullable = true)
    assertThat(list.toString()).isEqualTo("kotlin.collections.List<kotlin.Int?>?")
  }

  @Test
  fun typeVariableWithOutVariance() {
    System.setProperty("kotlinpoet.emit.variance", "true")

    try {
      val funspec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T"))
        .returns(
          ClassName.bestGuess("kotlin.Array")
            .parameterizedBy(TypeVariableName("T", variance = KModifier.OUT)),
        )
        .addStatement("TODO()")
        .build()

      val result = FileSpec.builder("foo.bar", "Baz")
        .addFunction(funspec)
        .build()
        .toString()

      assertThat(result).contains("Array<out T>")
    } finally {
      System.clearProperty("kotlinpoet.emit.variance")
    }
  }

  @Test
  fun typeVariableWithInVariance() {
    System.setProperty("kotlinpoet.emit.variance", "true")

    try {
      val funspec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T"))
        .returns(
          ClassName.bestGuess("kotlin.Array")
            .parameterizedBy(TypeVariableName("T", variance = KModifier.IN)),
        )
        .addStatement("TODO()")
        .build()

      val result = FileSpec.builder("foo.bar", "Baz")
        .addFunction(funspec)
        .build()
        .toString()

      assertThat(result).contains("Array<in T>")
    } finally {
      System.clearProperty("kotlinpoet.emit.variance")
    }
  }

  @Test
  fun typeVariableWithoutVariance() {
    val funspec = FunSpec.builder("foo")
      .addTypeVariable(TypeVariableName("T"))
      .returns(
        ClassName.bestGuess("kotlin.Array")
          .parameterizedBy(TypeVariableName("T")),
      )
      .addStatement("TODO()")
      .build()

    val result = FileSpec.builder("foo.bar", "Baz")
      .addFunction(funspec)
      .build()
      .toString()

    assertThat(result).contains("Array<T>")
    assertThat(result).doesNotContain("Array<out T>")
    assertThat(result).doesNotContain("Array<in T>")
  }

  @Test fun getAndSet() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addProperty(
        PropertySpec.builder("propertyWithCustomAccessors", Int::class.asTypeName())
          .mutable()
          .initializer("%L", 1)
          .getter(
            FunSpec.getterBuilder()
              .addStatement("println(%S)", "getter")
              .addStatement("return field")
              .build(),
          )
          .setter(
            FunSpec.setterBuilder()
              .addParameter("value", Int::class)
              .addStatement("println(%S)", "setter")
              .addStatement("field = value")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |public var propertyWithCustomAccessors: Int = 1
        |  get() {
        |    println("getter")
        |    return field
        |  }
        |  set(`value`) {
        |    println("setter")
        |    field = value
        |  }
        |
      """.trimMargin(),
    )
  }

  @Test fun propertyWithLongInitializerWrapping() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addProperty(
        PropertySpec
          .builder("foo", ClassName(tacosPackage, "Foo").copy(nullable = true))
          .addModifiers(KModifier.PRIVATE)
          .initializer("DefaultFooRegistry.getInstance().getDefaultFooInstanceForPropertiesFiles(file)")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |private val foo: Foo? =
      |    DefaultFooRegistry.getInstance().getDefaultFooInstanceForPropertiesFiles(file)
      |
      """.trimMargin(),
    )
  }

  @Test fun stackedPropertyModifiers() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addType(
        TypeSpec.classBuilder("A")
          .addModifiers(KModifier.ABSTRACT)
          .addProperty(
            PropertySpec.builder("q", String::class.asTypeName())
              .mutable()
              .addModifiers(KModifier.ABSTRACT, KModifier.PROTECTED)
              .build(),
          )
          .build(),
      )
      .addProperty(
        PropertySpec.builder("p", String::class)
          .addModifiers(KModifier.CONST, KModifier.INTERNAL)
          .initializer("%S", "a")
          .build(),
      )
      .addType(
        TypeSpec.classBuilder("B")
          .superclass(ClassName(tacosPackage, "A"))
          .addModifiers(KModifier.ABSTRACT)
          .addProperty(
            PropertySpec.builder("q", String::class.asTypeName())
              .mutable()
              .addModifiers(
                KModifier.FINAL,
                KModifier.LATEINIT,
                KModifier.OVERRIDE,
                KModifier.PUBLIC,
              )
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public abstract class A {
        |  protected abstract var q: String
        |}
        |
        |internal const val p: String = "a"
        |
        |public abstract class B : A() {
        |  public final override lateinit var q: String
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun stackedFunModifiers() {
    val source = FileSpec.get(
      tacosPackage,
      TypeSpec.classBuilder("A")
        .addModifiers(KModifier.OPEN)
        .addFunction(
          FunSpec.builder("get")
            .addModifiers(
              KModifier.EXTERNAL,
              KModifier.INFIX,
              KModifier.OPEN,
              KModifier.OPERATOR,
              KModifier.PROTECTED,
            )
            .addParameter("v", String::class)
            .returns(String::class)
            .build(),
        )
        .addFunction(
          FunSpec.builder("loop")
            .addModifiers(KModifier.FINAL, KModifier.INLINE, KModifier.INTERNAL, KModifier.TAILREC)
            .returns(String::class)
            .addStatement("return %S", "a")
            .build(),
        )
        .build(),
    )
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public open class A {
        |  protected open external infix operator fun `get`(v: String): String
        |
        |  internal final tailrec inline fun loop(): String = "a"
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun basicExpressionBody() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addFunction(
        FunSpec.builder("addA")
          .addParameter("s", String::class)
          .returns(String::class)
          .addStatement("return s + %S", "a")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun addA(s: String): String = s + "a"
        |
      """.trimMargin(),
    )
  }

  @Test fun suspendingLambdas() {
    val barType = ClassName(tacosPackage, "Bar")
    val suspendingLambda = LambdaTypeName
      .get(parameters = arrayOf(ClassName(tacosPackage, "Foo")), returnType = barType)
      .copy(suspending = true)
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addProperty(
        PropertySpec.builder("bar", suspendingLambda)
          .mutable()
          .initializer("{ %T() }", barType)
          .build(),
      )
      .addProperty(
        PropertySpec.builder("nullBar", suspendingLambda.copy(nullable = true))
          .mutable()
          .initializer("null")
          .build(),
      )
      .addFunction(
        FunSpec.builder("foo")
          .addParameter("bar", suspendingLambda)
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |public var bar: suspend (Foo) -> Bar = { Bar() }
      |
      |public var nullBar: (suspend (Foo) -> Bar)? = null
      |
      |public fun foo(bar: suspend (Foo) -> Bar) {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun enumAsDefaultArgument() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addFunction(
        FunSpec.builder("timeout")
          .addParameter("duration", Long::class)
          .addParameter(
            ParameterSpec.builder("timeUnit", TimeUnit::class)
              .defaultValue("%T.%L", TimeUnit::class, TimeUnit.MILLISECONDS.name)
              .build(),
          )
          .addStatement("this.timeout = timeUnit.toMillis(duration)")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import java.util.concurrent.TimeUnit
      |import kotlin.Long
      |
      |public fun timeout(duration: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
      |  this.timeout = timeUnit.toMillis(duration)
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun dynamicType() {
    val source = FileSpec.builder(tacosPackage, "Taco")
      .addFunction(
        FunSpec.builder("dynamicTest")
          .addCode(
            CodeBlock.of(
              "%L",
              PropertySpec.builder("d1", DYNAMIC)
                .initializer("%S", "Taco")
                .build(),
            ),
          )
          .addCode(
            CodeBlock.of(
              "%L",
              PropertySpec.builder("d2", DYNAMIC)
                .initializer("1f")
                .build(),
            ),
          )
          .addStatement("// dynamics are dangerous!")
          .addStatement("println(d1 - d2)")
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |public fun dynamicTest() {
      |  val d1: dynamic = "Taco"
      |  val d2: dynamic = 1f
      |  // dynamics are dangerous!
      |  println(d1 - d2)
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun primaryConstructorParameterAnnotation() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("foo", String::class)
              .build(),
          )
          .addProperty(
            PropertySpec.builder("foo", String::class)
              .jvmField()
              .initializer("foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmField
      |
      |public class Taco(
      |  @JvmField
      |  public val foo: String,
      |)
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/346
  @Test fun importTypeArgumentInParameterizedTypeName() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .addParameter(
            "a",
            List::class.asTypeName()
              .parameterizedBy(Int::class.asTypeName().jvmSuppressWildcards()),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.collections.List
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |public fun foo(a: List<@JvmSuppressWildcards Int>) {
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/462
  @Test fun foldingPropertyWithLambdaInitializer() {
    val param = ParameterSpec.builder("arg", ANY).build()
    val initializer = CodeBlock.builder()
      .beginControlFlow("{ %L ->", param)
      .addStatement("println(\"arg=\$%N\")", param)
      .endControlFlow()
      .build()
    val lambdaTypeName = ClassName.bestGuess("com.example.SomeTypeAlias")
    val property = PropertySpec.builder("foo", lambdaTypeName)
      .initializer("foo")
      .build()
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter(
                ParameterSpec.builder("foo", lambdaTypeName)
                  .defaultValue(initializer)
                  .build(),
              )
              .build(),
          )
          .addProperty(property)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import com.example.SomeTypeAlias
      |
      |public class Taco(
      |  public val foo: SomeTypeAlias = { arg: kotlin.Any ->
      |    println("arg=${'$'}arg")
      |  }
      |  ,
      |)
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/483
  @Test fun foldingPropertyWithEscapedName() {
    val file = FileSpec.builder("com.squareup.tacos", "AlarmInfo")
      .addType(
        TypeSpec.classBuilder("AlarmInfo")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("when", Float::class)
              .build(),
          )
          .addProperty(
            PropertySpec.builder("when", Float::class)
              .initializer("when")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Float
      |
      |public class AlarmInfo(
      |  public val `when`: Float,
      |)
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/577
  @Test fun noWrappingBetweenParamNameAndType() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("functionWithAPrettyLongNameThatWouldCauseWrapping")
          .addParameter("parameterWithALongNameThatWouldAlsoCauseWrapping", String::class)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |
      |public fun functionWithAPrettyLongNameThatWouldCauseWrapping(parameterWithALongNameThatWouldAlsoCauseWrapping: String) {
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/576
  @Test fun noWrappingBetweenValAndPropertyName() {
    val wireField = ClassName("com.squareup.wire", "WireField")
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addModifiers(KModifier.DATA)
          .addProperty(
            PropertySpec.builder("name", String::class)
              .addAnnotation(
                AnnotationSpec.builder(wireField)
                  .addMember("tag = %L", 1)
                  .addMember("adapter = %S", "CustomStringAdapterWithALongNameThatCauses")
                  .build(),
              )
              .initializer("name")
              .build(),
          )
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter(
                ParameterSpec.builder("name", String::class)
                  .build(),
              )
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import com.squareup.wire.WireField
      |import kotlin.String
      |
      |public data class Taco(
      |  @WireField(
      |    tag = 1,
      |    adapter = "CustomStringAdapterWithALongNameThatCauses",
      |  )
      |  public val name: String,
      |)
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/578
  @Test fun wrappingInsideKdocKeepsKdocFormatting() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Builder")
          .addKdoc(
            "Builder class for Foo. Allows creating instances of Foo by initializing " +
              "a subset of their fields, following the Builder pattern.\n",
          )
          .addFunction(
            FunSpec.builder("summary_text")
              .addKdoc(
                "The description for the choice, e.g. \"Currently unavailable due to " +
                  "high demand. Please try later.\" May be null.",
              )
              .addParameter("summary_text", String::class.asClassName().copy(nullable = true))
              .returns(ClassName("com.squareup.tacos", "Builder"))
              .addStatement("this.summary_text = summary_text")
              .addStatement("return this")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |
      |/**
      | * Builder class for Foo. Allows creating instances of Foo by initializing a subset of their fields, following the Builder pattern.
      | */
      |public class Builder {
      |  /**
      |   * The description for the choice, e.g. "Currently unavailable due to high demand. Please try later." May be null.
      |   */
      |  public fun summary_text(summary_text: String?): Builder {
      |    this.summary_text = summary_text
      |    return this
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/606
  @Test fun typeNamesInsideTemplateStringsGetImported() {
    val taco = ClassName("com.squareup.tacos", "Taco")
    val file = FileSpec.builder("com.squareup.example", "Tacos")
      .addFunction(
        FunSpec.builder("main")
          .addStatement("println(%P)", CodeBlock.of("Here's a taco: \${%T()}", taco))
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.example
      |
      |import com.squareup.tacos.Taco
      |
      |public fun main() {
      |  println(${'"'}""Here's a taco: ${'$'}{Taco()}""${'"'})
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/606
  @Test fun memberNamesInsideTemplateStringsGetImported() {
    val contentToString = MemberName("kotlin.collections", "contentToString")
    val file = FileSpec.builder("com.squareup.example", "Tacos")
      .addFunction(
        FunSpec.builder("main")
          .addStatement("val ints = arrayOf(1, 2, 3)")
          .addStatement("println(%P)", CodeBlock.of("\${ints.%M()}", contentToString))
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.example
      |
      |import kotlin.collections.contentToString
      |
      |public fun main() {
      |  val ints = arrayOf(1, 2, 3)
      |  println(${'"'}""${'$'}{ints.contentToString()}""${'"'})
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/701
  @Test fun noIllegalCharacterInIdentifier() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.enumBuilder("MyEnum")
        .addEnumConstant("with.dots") // dots are illegal, so this should fail
        .build().toString()
    }.hasMessageThat().isEqualTo("Can't escape identifier `with.dots` because it contains illegal characters: .")
  }

  // https://github.com/square/kotlinpoet/issues/814
  @Test fun percentAtTheEndOfKdoc() {
    val paramSpec1 = ParameterSpec.builder("a", Int::class)
      .addKdoc("Progress in %%")
      .build()
    val paramSpec2 = ParameterSpec.builder("b", Int::class)
      .addKdoc("Some other parameter with %%")
      .build()
    val funSpec = FunSpec.builder("test")
      .addParameters(listOf(paramSpec1, paramSpec2))
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * @param a Progress in %
      | * @param b Some other parameter with %
      | */
      |public fun test(a: kotlin.Int, b: kotlin.Int) {
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/1031
  @Test fun superClassGetsFullyQualifiedOnConflict() {
    val namespace = "test"

    val kotlinExceptionName = ClassName("kotlin", "Exception")
    val customExceptionName = ClassName(namespace, "Exception")
    val customException = TypeSpec
      .classBuilder("Exception")
      .superclass(kotlinExceptionName)
      .addFunction(
        FunSpec
          .builder("test")
          .addParameter("e", customExceptionName)
          .build(),
      )
      .build()

    val file = FileSpec.builder(namespace, "Exception")
      .addType(customException)
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package test
      |
      |public class Exception : kotlin.Exception() {
      |  public fun test(e: Exception) {
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun allStringsAreUnderscore() {
    val file = FileSpec.builder("com.squareup.tacos", "SourceWithUnderscores")
      .addType(
        TypeSpec.classBuilder("SourceWithUnderscores")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("_", Float::class)
              .addParameter("____", Float::class)
              .build(),
          )
          .addProperty(
            PropertySpec.builder("_", Float::class)
              .initializer("_")
              .build(),
          )
          .addProperty(
            PropertySpec.builder("____", Float::class)
              .initializer("____")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Float
      |
      |public class SourceWithUnderscores(
      |  public val `_`: Float,
      |  public val `____`: Float,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun generatedImportAliases() {
    val squareTaco = ClassName("com.squareup.tacos", "Taco")
    val blockTaco = ClassName("xyz.block.tacos", "Taco")
    val kotlinIsNullOrEmpty = MemberName("kotlin.text", "isNullOrEmpty")
    val cashIsNullOrEmpty = MemberName("com.squareup.cash.util", "isNullOrEmpty")
    val file = FileSpec.builder("com.example", "Test")
      .addFunction(
        FunSpec.builder("main")
          .addStatement("val squareTaco = %L", squareTaco.constructorReference())
          .addStatement("val blockTaco = %L", blockTaco.constructorReference())
          .addStatement("val isSquareTacoNull = %S.%M()", "Taco", kotlinIsNullOrEmpty)
          .addStatement("val isBlockTacoNull = %S.%M()", "Taco", cashIsNullOrEmpty)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.example
      |
      |import com.squareup.cash.util.isNullOrEmpty as utilIsNullOrEmpty
      |import com.squareup.tacos.Taco as SquareupTacosTaco
      |import kotlin.text.isNullOrEmpty as textIsNullOrEmpty
      |import xyz.block.tacos.Taco as BlockTacosTaco
      |
      |public fun main() {
      |  val squareTaco = ::SquareupTacosTaco
      |  val blockTaco = ::BlockTacosTaco
      |  val isSquareTacoNull = "Taco".textIsNullOrEmpty()
      |  val isBlockTacoNull = "Taco".utilIsNullOrEmpty()
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/1518
  @Test fun generatedImportAliasesSamePackageDifferentContainingClasses() {
    val strokeCapRound = MemberName(
      enclosingClassName = ClassName("androidx.compose.ui.graphics", "StrokeCap").nestedClass("Companion"),
      simpleName = "Round",
    )
    val strokeJoinRound = MemberName(
      enclosingClassName = ClassName("androidx.compose.ui.graphics", "StrokeJoin").nestedClass("Companion"),
      simpleName = "Round",
    )
    val file = FileSpec.builder("com.example", "Test")
      .addFunction(
        FunSpec.builder("main")
          .addStatement("val strokeCapRound = %M()", strokeCapRound)
          .addStatement("val strokeJoinRound = %M()", strokeJoinRound)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.example
      |
      |import androidx.compose.ui.graphics.StrokeCap.Companion.Round as strokeCapRound
      |import androidx.compose.ui.graphics.StrokeJoin.Companion.Round as strokeJoinRound
      |
      |public fun main() {
      |  val strokeCapRound = strokeCapRound()
      |  val strokeJoinRound = strokeJoinRound()
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun memberImportsOverGeneratedImportAliases() {
    val squareTaco = ClassName("com.squareup.tacos", "Taco")
    val blockTaco = ClassName("xyz.block.tacos", "Taco")
    val kotlinIsNullOrEmpty = MemberName("kotlin.text", "isNullOrEmpty")
    val cashIsNullOrEmpty = MemberName("com.squareup.cash.util", "isNullOrEmpty")
    val file = FileSpec.builder("com.example", "Test")
      .addAliasedImport(squareTaco, "SquareTaco")
      .addAliasedImport(blockTaco, "BlockTaco")
      .addAliasedImport(kotlinIsNullOrEmpty, "kotlinIsNullOrEmpty")
      .addAliasedImport(cashIsNullOrEmpty, "cashIsNullOrEmpty")
      .addFunction(
        FunSpec.builder("main")
          .addStatement("val squareTaco = %L", squareTaco.constructorReference())
          .addStatement("val blockTaco = %L", blockTaco.constructorReference())
          .addStatement("val isSquareTacoNull = %S.%M()", "Taco", kotlinIsNullOrEmpty)
          .addStatement("val isBlockTacoNull = %S.%M()", "Taco", cashIsNullOrEmpty)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.example
      |
      |import com.squareup.cash.util.isNullOrEmpty as cashIsNullOrEmpty
      |import com.squareup.tacos.Taco as SquareTaco
      |import kotlin.text.isNullOrEmpty as kotlinIsNullOrEmpty
      |import xyz.block.tacos.Taco as BlockTaco
      |
      |public fun main() {
      |  val squareTaco = ::SquareTaco
      |  val blockTaco = ::BlockTaco
      |  val isSquareTacoNull = "Taco".kotlinIsNullOrEmpty()
      |  val isBlockTacoNull = "Taco".cashIsNullOrEmpty()
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun extensionFunctionIsImportedEvenIfTheSameIsUsedAlsoFromTheCurrentPackage() {
    val kotlinIsNullOrEmpty = MemberName(packageName = "kotlin.text", simpleName = "isNullOrEmpty", isExtension = true)
    val samePackageIsNullOrEmpty = MemberName(packageName = "com.example", simpleName = "isNullOrEmpty", isExtension = true)
    val file = FileSpec.builder("com.example", "Test")
      .addFunction(
        FunSpec.builder("main")
          .addStatement("val isFirstNull = null.%M()", kotlinIsNullOrEmpty)
          .addStatement("val isSecondNull = null.%M()", samePackageIsNullOrEmpty)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.text.isNullOrEmpty as textIsNullOrEmpty
      |
      |public fun main() {
      |  val isFirstNull = null.textIsNullOrEmpty()
      |  val isSecondNull = null.isNullOrEmpty()
      |}
      |
      """.trimMargin(),
    )
  }

  // not a good idea to do that, but still valid syntax
  @Test fun extensionFunctionIsImportedEvenIfTheSameTypeIsAlreadyImported() {
    val subpkgIsNullOrEmpty = ClassName(packageName = "com.example.subpkg", simpleNames = listOf("isNullOrEmpty"))
    val kotlinIsNullOrEmpty = MemberName(packageName = "kotlin.text", simpleName = "isNullOrEmpty", isExtension = true)
    val file = FileSpec.builder("com.example", "Test")
      .addFunction(
        FunSpec.builder("main")
          .addStatement("val instance = %T()", subpkgIsNullOrEmpty)
          .addStatement("val extensionFunctionResult = null.%M()", kotlinIsNullOrEmpty)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.example
      |
      |import com.example.subpkg.isNullOrEmpty
      |import kotlin.text.isNullOrEmpty
      |
      |public fun main() {
      |  val instance = isNullOrEmpty()
      |  val extensionFunctionResult = null.isNullOrEmpty()
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/1563
  @Test fun nestedClassesWithConflictingAutoGeneratedImports() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addProperty("madeFreshDate", ClassName("java.util", "Date", "Builder"))
          .addProperty("madeFreshDatabaseDate", ClassName("java.sql", "Date", "Builder"))
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import java.sql.Date as SqlDate
        |import java.util.Date as UtilDate
        |
        |public class Taco {
        |  public val madeFreshDate: UtilDate.Builder
        |
        |  public val madeFreshDatabaseDate: SqlDate.Builder
        |}
        |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/1563
  @Test fun nestedClassesWithConflictingManuallySuppliedImports() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
      .addAliasedImport(ClassName("java.util", "Date"), "UtilDate")
      .addAliasedImport(ClassName("java.sql", "Date"), "SqlDate")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addProperty("madeFreshDate", ClassName("java.util", "Date", "Builder"))
          .addProperty("madeFreshDatabaseDate", ClassName("java.sql", "Date", "Builder"))
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import java.sql.Date as SqlDate
        |import java.util.Date as UtilDate
        |
        |public class Taco {
        |  public val madeFreshDate: UtilDate.Builder
        |
        |  public val madeFreshDatabaseDate: SqlDate.Builder
        |}
        |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/2046
  @Test fun importAliasCollisionWithRegularImportWhenAliasesComputedFirst() {
    val class1 = ClassName("com.squareup.taco", "Meal")
    val class2 = ClassName("com.squareup.burrito", "Meal")
    val class3 = ClassName("com.squareup.meal", "TacoMeal")
    val source = FileSpec.builder("com.squareup.food", "Food")
      .addType(
        TypeSpec.classBuilder("Food")
          .addProperty("tacoPackageMeal", class1)
          .addProperty("burritoPackageMeal", class2)
          .addProperty("mealPackageTacoMeal", class3)
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.food
        |
        |import com.squareup.meal.TacoMeal
        |import com.squareup.burrito.Meal as SquareupBurritoMeal
        |import com.squareup.taco.Meal as SquareupTacoMeal
        |
        |public class Food {
        |  public val tacoPackageMeal: SquareupTacoMeal
        |
        |  public val burritoPackageMeal: SquareupBurritoMeal
        |
        |  public val mealPackageTacoMeal: TacoMeal
        |}
        |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/2046
  @Test fun importAliasCollisionWithImportWhenRegularImportComputedFirst() {
    val class1 = ClassName("com.squareup.taco", "Meal")
    val class2 = ClassName("com.squareup.burrito", "Meal")
    val class3 = ClassName("com.squareup.meal", "TacoMeal")
    val source = FileSpec.builder("com.squareup.food", "Food")
      .addType(
        TypeSpec.classBuilder("Food")
          .addProperty("mealPackageTacoMeal", class3)
          .addProperty("tacoPackageMeal", class1)
          .addProperty("burritoPackageMeal", class2)
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package com.squareup.food
        |
        |import com.squareup.meal.TacoMeal
        |import com.squareup.burrito.Meal as SquareupBurritoMeal
        |import com.squareup.taco.Meal as SquareupTacoMeal
        |
        |public class Food {
        |  public val mealPackageTacoMeal: TacoMeal
        |
        |  public val tacoPackageMeal: SquareupTacoMeal
        |
        |  public val burritoPackageMeal: SquareupBurritoMeal
        |}
        |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/2046
  @Test fun importAliasCollisionWithImportWhenWeRunOutOfSegments() {
    val class0 = ClassName("squareup", "SquareupTacoMeal")
    val class1 = ClassName("squareup.taco", "Meal")
    val class2 = ClassName("squareup.burrito", "Meal")
    val class3 = ClassName("squareup", "TacoMeal")
    val source = FileSpec.builder("squareup.food", "Food")
      .addType(
        TypeSpec.classBuilder("Food")
          .addProperty("noMoreSegments", class0)
          .addProperty("mealPackageTacoMeal", class3)
          .addProperty("tacoPackageMeal", class1)
          .addProperty("burritoPackageMeal", class2)
          .build(),
      )
      .build()
    assertThat(source.toString()).isEqualTo(
      """
        |package squareup.food
        |
        |import squareup.SquareupTacoMeal
        |import squareup.TacoMeal
        |import squareup.burrito.Meal as SquareupBurritoMeal_
        |import squareup.taco.Meal as SquareupTacoMeal_
        |
        |public class Food {
        |  public val noMoreSegments: SquareupTacoMeal
        |
        |  public val mealPackageTacoMeal: TacoMeal
        |
        |  public val tacoPackageMeal: SquareupTacoMeal_
        |
        |  public val burritoPackageMeal: SquareupBurritoMeal_
        |}
        |
      """.trimMargin(),
    )
  }
}
