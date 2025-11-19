/*
 * Copyright (C) 2019 Square, Inc.
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
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.junit.Before
import org.junit.Test

class MemberNameTest {
  @Test
  fun memberNames() {
    val randomTaco = MemberName("com.squareup.tacos", "randomTaco")
    val bestTacoEver = MemberName("com.squareup.tacos", "bestTacoEver")
    val funSpec =
      FunSpec.builder("makeTastyTacos")
        .addStatement("val randomTaco = %M()", randomTaco)
        .addStatement("val bestTaco = %M", bestTacoEver)
        .build()
    val file = FileSpec.builder("com.example", "Tacos").addFunction(funSpec).build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.bestTacoEver
        |import com.squareup.tacos.randomTaco
        |
        |public fun makeTastyTacos() {
        |  val randomTaco = randomTaco()
        |  val bestTaco = bestTacoEver
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberInsideCompanionObject() {
    val companion = ClassName("com.squareup.tacos", "Taco").nestedClass("Companion")
    val createTaco = MemberName(companion, "createTaco")
    val file =
      FileSpec.builder("com.example", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos").addStatement("%M()", createTaco).build())
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.Taco.Companion.createTaco
        |
        |public fun makeTastyTacos() {
        |  createTaco()
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberInsideSamePackage() {
    val createTaco = MemberName("com.squareup.tacos", "createTaco")
    val file =
      FileSpec.builder("com.squareup.tacos", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos").addStatement("%M()", createTaco).build())
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |public fun makeTastyTacos() {
        |  createTaco()
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberInsideClassInSamePackage() {
    val createTaco = MemberName(ClassName("com.squareup.tacos", "Town"), "createTaco")
    val file =
      FileSpec.builder("com.squareup.tacos", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos").addStatement("%M()", createTaco).build())
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import com.squareup.tacos.Town.createTaco
        |
        |public fun makeTastyTacos() {
        |  createTaco()
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberNamesClash() {
    val createSquareTaco = MemberName("com.squareup.tacos", "createTaco")
    val createTwitterTaco = MemberName("com.twitter.tacos", "createTaco")
    val file =
      FileSpec.builder("com.example", "Tacos")
        .addFunction(
          FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createSquareTaco)
            .addStatement("%M()", createTwitterTaco)
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.createTaco as squareupTacosCreateTaco
        |import com.twitter.tacos.createTaco as twitterTacosCreateTaco
        |
        |public fun makeTastyTacos() {
        |  squareupTacosCreateTaco()
        |  twitterTacosCreateTaco()
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberNamesInsideCompanionsClash() {
    val squareTacos = ClassName("com.squareup.tacos", "SquareTacos")
    val twitterTacos = ClassName("com.twitter.tacos", "TwitterTacos")
    val createSquareTaco = MemberName(squareTacos.nestedClass("Companion"), "createTaco")
    val createTwitterTaco = MemberName(twitterTacos.nestedClass("Companion"), "createTaco")
    val file =
      FileSpec.builder("com.example", "Tacos")
        .addFunction(
          FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createSquareTaco)
            .addStatement("%M()", createTwitterTaco)
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.SquareTacos.Companion.createTaco as squareTacosCreateTaco
        |import com.twitter.tacos.TwitterTacos.Companion.createTaco as twitterTacosCreateTaco
        |
        |public fun makeTastyTacos() {
        |  squareTacosCreateTaco()
        |  twitterTacosCreateTaco()
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberAndClassNamesClash() {
    val squareTacosClass = ClassName("com.squareup.tacos", "SquareTacos")
    val squareTacosFunction = MemberName("com.squareup.tacos.math", "SquareTacos")
    val file =
      FileSpec.builder("com.example", "Tacos")
        .addFunction(
          FunSpec.builder("makeTastyTacos")
            .addStatement("val tacos = %T()", squareTacosClass)
            .addStatement("%M(tacos)", squareTacosFunction)
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.SquareTacos
        |
        |public fun makeTastyTacos() {
        |  val tacos = SquareTacos()
        |  com.squareup.tacos.math.SquareTacos(tacos)
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun importedMemberAndClassFunctionNameClash() {
    val kotlinErrorMember = MemberName("kotlin", "error")
    val file =
      FileSpec.builder("com.squareup.tacos", "TacoTest")
        .addType(
          TypeSpec.classBuilder("TacoTest")
            .addFunction(
              FunSpec.builder("test").addStatement("%M(%S)", kotlinErrorMember, "errorText").build()
            )
            .addFunction(FunSpec.builder("error").build())
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |public class TacoTest {
        |  public fun test() {
        |    kotlin.error("errorText")
        |  }
        |
        |  public fun error() {
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun importedMemberAndSuperClassFunctionNameClashForInnerClass() {
    val kotlinErrorMember = MemberName("kotlin", "error")
    val file =
      FileSpec.builder("com.squareup.tacos", "TacoTest")
        .addType(
          TypeSpec.classBuilder("Test")
            .addFunction(FunSpec.builder("error").build())
            .addType(
              TypeSpec.classBuilder("TacoTest")
                .addModifiers(KModifier.INNER)
                .addFunction(
                  FunSpec.builder("test")
                    .addStatement("%M(%S)", kotlinErrorMember, "errorText")
                    .build()
                )
                .build()
            )
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |public class Test {
        |  public fun error() {
        |  }
        |
        |  public inner class TacoTest {
        |    public fun test() {
        |      kotlin.error("errorText")
        |    }
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun importedMemberAndSuperClassFunctionNameDontClashForNonInnerClass() {
    val kotlinErrorMember = MemberName("kotlin", "error")
    val file =
      FileSpec.builder("com.squareup.tacos", "TacoTest")
        .addType(
          TypeSpec.classBuilder("Test")
            .addFunction(FunSpec.builder("error").build())
            .addType(
              TypeSpec.classBuilder("TacoTest")
                .addFunction(
                  FunSpec.builder("test")
                    .addStatement("%M(%S)", kotlinErrorMember, "errorText")
                    .build()
                )
                .build()
            )
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.error
        |
        |public class Test {
        |  public fun error() {
        |  }
        |
        |  public class TacoTest {
        |    public fun test() {
        |      error("errorText")
        |    }
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun importedMemberClassFunctionNameDontClashForParameterValue() {
    val tacoName = ClassName("com.squareup.tacos", "Taco")
    val meatMember = ClassName("com.squareup", "Fridge").member("meat")
    val buildFun =
      FunSpec.builder("build")
        .returns(tacoName)
        .addStatement("return %T(%M { })", tacoName, meatMember)
        .build()
    val spec =
      FileSpec.builder(tacoName)
        .addType(
          TypeSpec.classBuilder("DeliciousTaco")
            .addFunction(buildFun)
            .addFunction(FunSpec.builder("deliciousMeat").build())
            .build()
        )
        .addType(
          TypeSpec.classBuilder("TastelessTaco")
            .addFunction(buildFun)
            .addFunction(FunSpec.builder("meat").build())
            .build()
        )
        .build()
    assertThat(spec.toString())
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import com.squareup.Fridge.meat
        |
        |public class DeliciousTaco {
        |  public fun build(): Taco = Taco(meat { })
        |
        |  public fun deliciousMeat() {
        |  }
        |}
        |
        |public class TastelessTaco {
        |  public fun build(): Taco = Taco(com.squareup.Fridge.meat { })
        |
        |  public fun meat() {
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberNameAliases() {
    val createSquareTaco = MemberName("com.squareup.tacos", "createTaco")
    val createTwitterTaco = MemberName("com.twitter.tacos", "createTaco")
    val file =
      FileSpec.builder("com.example", "Tacos")
        .addAliasedImport(createSquareTaco, "createSquareTaco")
        .addAliasedImport(createTwitterTaco, "createTwitterTaco")
        .addFunction(
          FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createSquareTaco)
            .addStatement("%M()", createTwitterTaco)
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.createTaco as createSquareTaco
        |import com.twitter.tacos.createTaco as createTwitterTaco
        |
        |public fun makeTastyTacos() {
        |  createSquareTaco()
        |  createTwitterTaco()
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun keywordsEscaping() {
    val `when` = MemberName("org.mockito", "when")
    val file =
      FileSpec.builder("com.squareup.tacos", "TacoTest")
        .addType(
          TypeSpec.classBuilder("TacoTest")
            .addFunction(
              FunSpec.builder("setUp")
                .addAnnotation(Before::class)
                .addStatement("%M(tacoService.createTaco()).thenReturn(tastyTaco())", `when`)
                .build()
            )
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import org.junit.Before
        |import org.mockito.`when`
        |
        |public class TacoTest {
        |  @Before
        |  public fun setUp() {
        |    `when`(tacoService.createTaco()).thenReturn(tastyTaco())
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun clashingNamesKeywordsEscaping() {
    val squareTacos = ClassName("com.squareup.tacos", "SquareTacos")
    val twitterTacos = ClassName("com.twitter.tacos", "TwitterTacos")
    val whenSquareTaco = MemberName(squareTacos.nestedClass("Companion"), "when")
    val whenTwitterTaco = MemberName(twitterTacos.nestedClass("Companion"), "when")
    val file =
      FileSpec.builder("com.example", "Tacos")
        .addFunction(
          FunSpec.builder("whenTastyTacos")
            .addStatement("%M()", whenSquareTaco)
            .addStatement("%M()", whenTwitterTaco)
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.SquareTacos.Companion.`when` as squareTacosWhen
        |import com.twitter.tacos.TwitterTacos.Companion.`when` as twitterTacosWhen
        |
        |public fun whenTastyTacos() {
        |  squareTacosWhen()
        |  twitterTacosWhen()
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberReferences() {
    val randomTaco = MemberName("com.squareup.tacos", "randomTaco")
    val bestTacoEver = ClassName("com.squareup.tacos", "TacoTruck").member("bestTacoEver")
    val funSpec =
      FunSpec.builder("makeTastyTacos")
        .addStatement("val randomTacoFactory = %L", randomTaco.reference())
        .addStatement("val bestTacoFactory = %L", bestTacoEver.reference())
        .build()
    val file = FileSpec.builder("com.example", "Tacos").addFunction(funSpec).build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.TacoTruck
        |import com.squareup.tacos.randomTaco
        |
        |public fun makeTastyTacos() {
        |  val randomTacoFactory = ::randomTaco
        |  val bestTacoFactory = TacoTruck::bestTacoEver
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun spacesEscaping() {
    val produceTacos = MemberName("com.squareup.taco factory", "produce tacos")
    val file =
      FileSpec.builder("com.squareup.tacos", "TacoTest")
        .addFunction(FunSpec.builder("main").addStatement("println(%M())", produceTacos).build())
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import com.squareup.`taco factory`.`produce tacos`
        |
        |public fun main() {
        |  println(`produce tacos`())
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun memberExtension_className() {
    val regex = ClassName("kotlin.text", "Regex")
    assertThat(regex.member("fromLiteral")).isEqualTo(MemberName(regex, "fromLiteral"))
  }

  @Test
  fun memberExtension_kclass() {
    assertThat(Regex::class.member("fromLiteral"))
      .isEqualTo(MemberName(ClassName("kotlin.text", "Regex"), "fromLiteral"))
  }

  @Test
  fun memberExtension_class() {
    assertThat(Regex::class.java.member("fromLiteral"))
      .isEqualTo(MemberName(ClassName("kotlin.text", "Regex"), "fromLiteral"))
  }

  @Test
  fun `N escapes MemberNames`() {
    val taco = ClassName("com.squareup.tacos", "Taco")
    val packager = ClassName("com.squareup.tacos", "TacoPackager")
    val file =
      FileSpec.builder("com.example", "Test")
        .addFunction(
          FunSpec.builder("packageTacos")
            .addParameter("tacos", LIST.parameterizedBy(taco))
            .addParameter("packager", packager)
            .addStatement("packager.%N(tacos)", packager.member("package"))
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.Taco
        |import com.squareup.tacos.TacoPackager
        |import kotlin.collections.List
        |
        |public fun packageTacos(tacos: List<Taco>, packager: TacoPackager) {
        |  packager.`package`(tacos)
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun importOperator() {
    val taco = ClassName("com.squareup.tacos", "Taco")
    val meat = ClassName("com.squareup.tacos.ingredient", "Meat")
    val iterator = MemberName("com.squareup.tacos.internal", KOperator.ITERATOR)
    val minusAssign = MemberName("com.squareup.tacos.internal", KOperator.MINUS_ASSIGN)
    val file =
      FileSpec.builder("com.example", "Test")
        .addFunction(
          FunSpec.builder("makeTacoHealthy")
            .addParameter("taco", taco)
            .beginControlFlow("for (ingredient %M taco)", iterator)
            .addStatement("if (ingredient is %T) taco %M ingredient", meat, minusAssign)
            .endControlFlow()
            .addStatement("return taco")
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import com.squareup.tacos.Taco
        |import com.squareup.tacos.`internal`.iterator
        |import com.squareup.tacos.`internal`.minusAssign
        |import com.squareup.tacos.ingredient.Meat
        |
        |public fun makeTacoHealthy(taco: Taco) {
        |  for (ingredient in taco) {
        |    if (ingredient is Meat) taco -= ingredient
        |  }
        |  return taco
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun importMemberWithoutPackage() {
    val createTaco = MemberName("", "createTaco")
    val file =
      FileSpec.builder("com.example", "Test")
        .addFunction(
          FunSpec.builder("makeTacoHealthy").addStatement("val taco = %M()", createTaco).build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        |package com.example
        |
        |import createTaco
        |
        |public fun makeTacoHealthy() {
        |  val taco = createTaco()
        |}
        |"""
          .trimMargin()
      )
  }

  // https://github.com/square/kotlinpoet/issues/1089
  @Test
  fun `extension MemberName imported if name clash`() {
    val hashCode = MemberName("kotlin", "hashCode", isExtension = true)
    val file =
      FileSpec.builder("com.squareup.tacos", "Message")
        .addType(
          TypeSpec.classBuilder("Message")
            .addFunction(
              FunSpec.builder("hashCode")
                .addModifiers(OVERRIDE)
                .returns(INT)
                .addCode(
                  buildCodeBlock {
                    addStatement("var result = super.hashCode")
                    beginControlFlow("if (result == 0)")
                    addStatement("result = result * 37 + embedded_message.%M()", hashCode)
                    addStatement("super.hashCode = result")
                    endControlFlow()
                    addStatement("return result")
                  }
                )
                .build()
            )
            .build()
        )
        .build()
    // language=kotlin
    assertThat(file.toString())
      .isEqualTo(
        """
        package com.squareup.tacos

        import kotlin.Int
        import kotlin.hashCode

        public class Message {
          override fun hashCode(): Int {
            var result = super.hashCode
            if (result == 0) {
              result = result * 37 + embedded_message.hashCode()
              super.hashCode = result
            }
            return result
          }
        }

        """
          .trimIndent()
      )
  }

  // https://github.com/square/kotlinpoet/issues/1907
  @Test
  fun `extension and non-extension MemberName clash`() {
    val file =
      FileSpec.builder("com.squareup.tacos", "Tacos")
        .addFunction(
          FunSpec.builder("main")
            .addStatement("println(%M(Taco()))", MemberName("com.squareup.wrappers", "wrap"))
            .addStatement(
              "println(Taco().%M())",
              MemberName("com.squareup.wrappers", "wrap", isExtension = true),
            )
            .build()
        )
        .build()
    assertThat(file.toString())
      .isEqualTo(
        """
        package com.squareup.tacos

        import com.squareup.wrappers.wrap

        public fun main() {
          println(wrap(Taco()))
          println(Taco().wrap())
        }

        """
          .trimIndent()
      )
  }
}
