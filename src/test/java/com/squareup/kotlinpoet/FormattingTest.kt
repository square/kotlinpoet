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

class FormattingTest {

  @Test fun ifExpression() {
    val fileSpec = FileSpec.builder("", "Test")
        .addProperty(PropertySpec.builder("lunchMenu", String::class)
            .initializer("""
              |if (2 == 2) {
              |  "taco"
              |} else {
              |  "no taco"
              |}
              |""".trimMargin())
            .build())
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import kotlin.String
      |
      |val lunchMenu: String = if (2 == 2) {
      |  "taco"
      |} else {
      |  "no taco"
      |}
      |
      |""".trimMargin())
  }

  @Test fun lambdaExpression() {
    val fileSpec = FileSpec.builder("", "Test")
        .addProperty(PropertySpec.builder("lunchMenu", String::class)
            .initializer("""
              |buildString {
              |  append("taco, ")
              |  append("quesadilla, ")
              |  append("gelato")
              |}
              |""".trimMargin())
            .build())
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import kotlin.String
      |
      |val lunchMenu: String = buildString {
      |  append("taco, ")
      |  append("quesadilla, ")
      |  append("gelato")
      |}
      |
      |""".trimMargin())
  }

  @Test fun lambdaExpressionWithArgs() {
    val fileSpec = FileSpec.builder("", "Test")
        .addFunction(FunSpec.builder("printLunchMenu")
            .addStatement("""
              |listOf("taco", "quesadilla", "gelato").forEach { meal ->
              |  println(meal)
              |}
              |""".trimMargin())
            .build())
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |fun printLunchMenu() {
      |  listOf("taco", "quesadilla", "gelato").forEach { meal ->
      |    println(meal)
      |  }
      |
      |}
      |""".trimMargin())
  }

  @Test fun multilineString() {
    val fileSpec = FileSpec.builder("", "Test")
        .addProperty(PropertySpec.builder("lunchMenu", String::class)
            .initializer("%S", "taco\nquesadilla\ngelato")
            .build())
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import kotlin.String
      |
      |val lunchMenu: String = ${"\"\"\""}
      |  |taco
      |  |quesadilla
      |  |gelato
      |  ${"\"\"\""}.trimMargin()
      |""".trimMargin())
  }

  @Test fun multipleStatements() {
    val fileSpec = FileSpec.builder("", "Test")
        .addProperty(PropertySpec.builder("foo", Unit::class)
            .initializer("""
              |listOf(1, 2, 3).forEachIndexed { index, n ->
              |  when (n) {
              |    1 -> if (index == 1) {
              |      println("foo")
              |    } else {
              |      println("bar")
              |    }
              |    2 -> println(${"\"\"\""}
              |      |foo
              |      |bar
              |      |${"\"\"\""}.trimMargin())
              |    3 -> while (true) {
              |      if (2 == 2) {
              |        println("one")
              |      } else {
              |        println("two")
              |      }
              |    }
              |  }
              |}
              |""".trimMargin())
            .build())
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import kotlin.Unit
      |
      |val foo: Unit = listOf(1, 2, 3).forEachIndexed { index, n ->
      |  when (n) {
      |    1 -> if (index == 1) {
      |      println("foo")
      |    } else {
      |      println("bar")
      |    }
      |    2 -> println(${"\"\"\""}
      |      |foo
      |      |bar
      |      |${"\"\"\""}.trimMargin())
      |    3 -> while (true) {
      |      if (2 == 2) {
      |        println("one")
      |      } else {
      |        println("two")
      |      }
      |    }
      |  }
      |}
      |
      |""".trimMargin())
  }
}
