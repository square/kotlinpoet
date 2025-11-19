/*
 * Copyright (C) 2018 Square, Inc.
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
import org.junit.Test

class LineWrappingTest {
  @Test
  fun codeSpacesDoNotWrap() {
    val wrapMe =
      FunSpec.builder("wrapMe")
        .returns(STRING)
        .addStatement(
          "return %L * %L * %L * %L * %L * %L * %L * %L * %L * %L * %L * %L",
          10000000000,
          20000000000,
          30000000000,
          40000000000,
          50000000000,
          60000000000,
          70000000000,
          80000000000,
          90000000000,
          10000000000,
          20000000000,
          30000000000,
        )
        .build()
    assertThat(toString(wrapMe))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun wrapMe(): String = 10_000_000_000 * 20_000_000_000 * 30_000_000_000 * 40_000_000_000 * 50_000_000_000 * 60_000_000_000 * 70_000_000_000 * 80_000_000_000 * 90_000_000_000 * 10_000_000_000 * 20_000_000_000 * 30_000_000_000
        |"""
          .trimMargin()
      )
  }

  @Test
  fun stringSpacesDoNotWrap() {
    val wrapMe =
      FunSpec.builder("wrapMe")
        .returns(STRING)
        .addStatement(
          "return %S+%S+%S+%S+%S+%S+%S+%S+%S+%S+%S+%S",
          "Aaaa Aaaa",
          "Bbbb Bbbb",
          "Cccc Cccc",
          "Dddd Dddd",
          "Eeee Eeee",
          "Ffff Ffff",
          "Gggg Gggg",
          "Hhhh Hhhh",
          "Iiii Iiii",
          "Jjjj Jjjj",
          "Kkkk Kkkk",
          "Llll Llll",
        )
        .build()
    assertThat(toString(wrapMe))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun wrapMe(): String = "Aaaa Aaaa"+"Bbbb Bbbb"+"Cccc Cccc"+"Dddd Dddd"+"Eeee Eeee"+"Ffff Ffff"+"Gggg Gggg"+"Hhhh Hhhh"+"Iiii Iiii"+"Jjjj Jjjj"+"Kkkk Kkkk"+"Llll Llll"
        |"""
          .trimMargin()
      )
  }

  @Test
  fun nonwrappingWhitespaceDoesNotWrap() {
    val wrapMe =
      FunSpec.builder("wrapMe")
        .returns(STRING)
        .addStatement(
          "return %L·*·%L·*·%L·*·%L·*·%L·*·%L·*·%L·*·%L·*·%L·*·%L·*·%L·*·%L",
          10000000000,
          20000000000,
          30000000000,
          40000000000,
          50000000000,
          60000000000,
          70000000000,
          80000000000,
          90000000000,
          10000000000,
          20000000000,
          30000000000,
        )
        .build()
    assertThat(toString(wrapMe))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun wrapMe(): String = 10_000_000_000 * 20_000_000_000 * 30_000_000_000 * 40_000_000_000 * 50_000_000_000 * 60_000_000_000 * 70_000_000_000 * 80_000_000_000 * 90_000_000_000 * 10_000_000_000 * 20_000_000_000 * 30_000_000_000
        |"""
          .trimMargin()
      )
  }

  @Test
  fun wrappingWhitespaceWraps() {
    val wrapMe =
      FunSpec.builder("wrapMe")
        .returns(STRING)
        .addStatement(
          "return %L♢*♢%L♢*♢%L♢*♢%L♢*♢%L♢*♢%L♢*♢%L♢*♢%L♢*♢%L♢*♢%L♢*♢%L♢*♢%L",
          10000000000,
          20000000000,
          30000000000,
          40000000000,
          50000000000,
          60000000000,
          70000000000,
          80000000000,
          90000000000,
          10000000000,
          20000000000,
          30000000000,
        )
        .build()
    assertThat(toString(wrapMe))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun wrapMe(): String = 10_000_000_000 * 20_000_000_000 * 30_000_000_000 * 40_000_000_000 *
        |    50_000_000_000 * 60_000_000_000 * 70_000_000_000 * 80_000_000_000 * 90_000_000_000 *
        |    10_000_000_000 * 20_000_000_000 * 30_000_000_000
        |"""
          .trimMargin()
      )
  }

  @Test
  fun nonwrappingWhitespaceIsRetainedInStrings() {
    val wrapMe = FunSpec.builder("wrapMe").returns(STRING).addStatement("return %S", "a·b").build()
    assertThat(toString(wrapMe))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public fun wrapMe(): String = "a·b"
        |"""
          .trimMargin()
      )
  }

  @Test
  fun insignificantWhitespaceRetained() {
    val wrapMe =
      FunSpec.builder("wrapMe")
        .addStatement("val a =    8")
        .addStatement("val b =   64")
        .addStatement("val c =  512")
        .addStatement("val d = 4096")
        .build()
    assertThat(toString(wrapMe))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |public fun wrapMe() {
        |  val a =    8
        |  val b =   64
        |  val c =  512
        |  val d = 4096
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun parameterWrapping() {
    val funSpecBuilder = FunSpec.builder("call")
    funSpecBuilder.addCode("«call(")
    for (i in 0..31) {
      funSpecBuilder.addParameter("s$i", String::class)
      funSpecBuilder.addCode(if (i > 0) ",♢%S" else "%S", i)
    }
    funSpecBuilder.addCode(")»\n")

    val taco = TypeSpec.classBuilder("Taco").addFunction(funSpecBuilder.build()).build()
    assertThat(toString(taco))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco {
        |  public fun call(
        |    s0: String,
        |    s1: String,
        |    s2: String,
        |    s3: String,
        |    s4: String,
        |    s5: String,
        |    s6: String,
        |    s7: String,
        |    s8: String,
        |    s9: String,
        |    s10: String,
        |    s11: String,
        |    s12: String,
        |    s13: String,
        |    s14: String,
        |    s15: String,
        |    s16: String,
        |    s17: String,
        |    s18: String,
        |    s19: String,
        |    s20: String,
        |    s21: String,
        |    s22: String,
        |    s23: String,
        |    s24: String,
        |    s25: String,
        |    s26: String,
        |    s27: String,
        |    s28: String,
        |    s29: String,
        |    s30: String,
        |    s31: String,
        |  ) {
        |    call("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
        |        "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31")
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  private fun toString(typeSpec: TypeSpec): String {
    return FileSpec.get("com.squareup.tacos", typeSpec).toString()
  }

  private fun toString(funSpec: FunSpec): String {
    val fileSpec =
      FileSpec.builder("com.squareup.tacos", "${funSpec.name}.kt").addFunction(funSpec).build()
    return fileSpec.toString()
  }
}
