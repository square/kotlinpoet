/*
 * Copyright (C) 2019 Square, Inc.
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
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.junit.Before
import org.junit.Test

class MemberNameTest {
  @Test fun memberNames() {
    val randomTaco = MemberName("com.squareup.tacos", "randomTaco")
    val bestTacoEver = MemberName("com.squareup.tacos", "bestTacoEver")
    val funSpec = FunSpec.builder("makeTastyTacos")
        .addStatement("val randomTaco = %M()", randomTaco)
        .addStatement("val bestTaco = %M", bestTacoEver)
        .build()
    val file = FileSpec.builder("com.example", "Tacos")
        .addFunction(funSpec)
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.bestTacoEver
      |import com.squareup.tacos.randomTaco
      |
      |fun makeTastyTacos() {
      |  val randomTaco = randomTaco()
      |  val bestTaco = bestTacoEver
      |}
      |""".trimMargin())
  }

  @Test fun memberInsideCompanionObject() {
    val companion = ClassName("com.squareup.tacos", "Taco").nestedClass("Companion")
    val createTaco = MemberName(companion, "createTaco")
    val file = FileSpec.builder("com.example", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createTaco)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.Taco.Companion.createTaco
      |
      |fun makeTastyTacos() {
      |  createTaco()
      |}
      |""".trimMargin())
  }

  @Test fun memberInsideSamePackage() {
    val createTaco = MemberName("com.squareup.tacos", "createTaco")
    val file = FileSpec.builder("com.squareup.tacos", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createTaco)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |fun makeTastyTacos() {
      |  createTaco()
      |}
      |""".trimMargin())
  }

  @Test fun memberInsideClassInSamePackage() {
    val createTaco = MemberName(
        ClassName("com.squareup.tacos", "Town"),
        "createTaco"
    )
    val file = FileSpec.builder("com.squareup.tacos", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createTaco)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import com.squareup.tacos.Town.createTaco
      |
      |fun makeTastyTacos() {
      |  createTaco()
      |}
      |""".trimMargin())
  }

  @Test fun memberNamesClash() {
    val createSquareTaco = MemberName("com.squareup.tacos", "createTaco")
    val createTwitterTaco = MemberName("com.twitter.tacos", "createTaco")
    val file = FileSpec.builder("com.example", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createSquareTaco)
            .addStatement("%M()", createTwitterTaco)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.createTaco
      |
      |fun makeTastyTacos() {
      |  createTaco()
      |  com.twitter.tacos.createTaco()
      |}
      |""".trimMargin())
  }

  @Test fun memberNamesInsideCompanionsClash() {
    val squareTacos = ClassName("com.squareup.tacos", "SquareTacos")
    val twitterTacos = ClassName("com.twitter.tacos", "TwitterTacos")
    val createSquareTaco = MemberName(squareTacos.nestedClass("Companion"), "createTaco")
    val createTwitterTaco = MemberName(twitterTacos.nestedClass("Companion"), "createTaco")
    val file = FileSpec.builder("com.example", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createSquareTaco)
            .addStatement("%M()", createTwitterTaco)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.SquareTacos.Companion.createTaco
      |import com.twitter.tacos.TwitterTacos
      |
      |fun makeTastyTacos() {
      |  createTaco()
      |  TwitterTacos.Companion.createTaco()
      |}
      |""".trimMargin())
  }

  @Test fun memberAndClassNamesClash() {
    val squareTacosClass = ClassName("com.squareup.tacos", "SquareTacos")
    val squareTacosFunction = MemberName("com.squareup.tacos.math", "SquareTacos")
    val file = FileSpec.builder("com.example", "Tacos")
        .addFunction(FunSpec.builder("makeTastyTacos")
            .addStatement("val tacos = %T()", squareTacosClass)
            .addStatement("%M(tacos)", squareTacosFunction)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.SquareTacos
      |
      |fun makeTastyTacos() {
      |  val tacos = SquareTacos()
      |  com.squareup.tacos.math.SquareTacos(tacos)
      |}
      |""".trimMargin())
  }

  @Test fun memberNameAliases() {
    val createSquareTaco = MemberName("com.squareup.tacos", "createTaco")
    val createTwitterTaco = MemberName("com.twitter.tacos", "createTaco")
    val file = FileSpec.builder("com.example", "Tacos")
        .addAliasedImport(createSquareTaco, "createSquareTaco")
        .addAliasedImport(createTwitterTaco, "createTwitterTaco")
        .addFunction(FunSpec.builder("makeTastyTacos")
            .addStatement("%M()", createSquareTaco)
            .addStatement("%M()", createTwitterTaco)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.createTaco as createSquareTaco
      |import com.twitter.tacos.createTaco as createTwitterTaco
      |
      |fun makeTastyTacos() {
      |  createSquareTaco()
      |  createTwitterTaco()
      |}
      |""".trimMargin())
  }

  @Test fun keywordsEscaping() {
    val `when` = MemberName("org.mockito", "when")
    val file = FileSpec.builder("com.squareup.tacos", "TacoTest")
        .addType(TypeSpec.classBuilder("TacoTest")
            .addFunction(FunSpec.builder("setUp")
                .addAnnotation(Before::class)
                .addStatement("%M(tacoService.createTaco()).thenReturn(tastyTaco())", `when`)
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import org.junit.Before
      |import org.mockito.`when`
      |
      |class TacoTest {
      |  @Before
      |  fun setUp() {
      |    `when`(tacoService.createTaco()).thenReturn(tastyTaco())
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun clashingNamesKeywordsEscaping() {
    val squareTacos = ClassName("com.squareup.tacos", "SquareTacos")
    val twitterTacos = ClassName("com.twitter.tacos", "TwitterTacos")
    val whenSquareTaco = MemberName(squareTacos.nestedClass("Companion"), "when")
    val whenTwitterTaco = MemberName(twitterTacos.nestedClass("Companion"), "when")
    val file = FileSpec.builder("com.example", "Tacos")
        .addFunction(FunSpec.builder("whenTastyTacos")
            .addStatement("%M()", whenSquareTaco)
            .addStatement("%M()", whenTwitterTaco)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.SquareTacos.Companion.`when`
      |import com.twitter.tacos.TwitterTacos
      |
      |fun whenTastyTacos() {
      |  `when`()
      |  TwitterTacos.Companion.`when`()
      |}
      |""".trimMargin())
  }

  @Test fun memberReferences() {
    val randomTaco = MemberName("com.squareup.tacos", "randomTaco")
    val bestTacoEver = ClassName("com.squareup.tacos", "TacoTruck")
        .member("bestTacoEver")
    val funSpec = FunSpec.builder("makeTastyTacos")
        .addStatement("val randomTacoFactory = %L", randomTaco.reference())
        .addStatement("val bestTacoFactory = %L", bestTacoEver.reference())
        .build()
    val file = FileSpec.builder("com.example", "Tacos")
        .addFunction(funSpec)
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.TacoTruck
      |import com.squareup.tacos.randomTaco
      |
      |fun makeTastyTacos() {
      |  val randomTacoFactory = ::randomTaco
      |  val bestTacoFactory = TacoTruck::bestTacoEver
      |}
      |""".trimMargin())
  }

  @Test fun spacesEscaping() {
    val produceTacos = MemberName("com.squareup.taco factory", "produce tacos")
    val file = FileSpec.builder("com.squareup.tacos", "TacoTest")
        .addFunction(FunSpec.builder("main")
            .addStatement("println(%M())", produceTacos)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import com.squareup.`taco factory`.`produce tacos`
      |
      |fun main() {
      |  println(`produce tacos`())
      |}
      |""".trimMargin())
  }

  @Test fun memberExtension_className() {
    val regex = ClassName("kotlin.text", "Regex")
    assertThat(regex.member("fromLiteral"))
        .isEqualTo(MemberName(regex, "fromLiteral"))
  }

  @Test fun memberExtension_kclass() {
    assertThat(Regex::class.member("fromLiteral"))
        .isEqualTo(MemberName(ClassName("kotlin.text", "Regex"), "fromLiteral"))
  }

  @Test fun memberExtension_class() {
    assertThat(Regex::class.java.member("fromLiteral"))
        .isEqualTo(MemberName(ClassName("kotlin.text", "Regex"), "fromLiteral"))
  }

  @Test fun `%N escapes MemberNames`() {
    val taco = ClassName("com.squareup.tacos", "Taco")
    val packager = ClassName("com.squareup.tacos", "TacoPackager")
    val file = FileSpec.builder("com.example", "Test")
        .addFunction(FunSpec.builder("packageTacos")
            .addParameter("tacos", LIST.parameterizedBy(taco))
            .addParameter("packager", packager)
            .addStatement("packager.%N(tacos)", packager.member("package"))
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.tacos.Taco
      |import com.squareup.tacos.TacoPackager
      |import kotlin.collections.List
      |
      |fun packageTacos(tacos: List<Taco>, packager: TacoPackager) {
      |  packager.`package`(tacos)
      |}
      |""".trimMargin())
  }
}
