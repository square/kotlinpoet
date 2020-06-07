/*
 * Copyright (C) 2014 Google, Inc.
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
import com.google.testing.compile.CompilationRule
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class ClassNameTest {
  @Rule @JvmField var compilationRule = CompilationRule()

  @Test fun bestGuessForString_simpleClass() {
    assertThat(ClassName.bestGuess(String::class.java.name))
        .isEqualTo(ClassName("java.lang", "String"))
  }

  @Test fun bestGuessNonAscii() {
    val className = ClassName.bestGuess(
        "com.\ud835\udc1andro\ud835\udc22d.\ud835\udc00ctiv\ud835\udc22ty")
    assertEquals("com.\ud835\udc1andro\ud835\udc22d", className.packageName)
    assertEquals("\ud835\udc00ctiv\ud835\udc22ty", className.simpleName)
  }

  internal class OuterClass {
    internal class InnerClass
  }

  @Test fun bestGuessForString_nestedClass() {
    assertThat(ClassName.bestGuess(Map.Entry::class.java.canonicalName))
        .isEqualTo(ClassName("java.util", "Map", "Entry"))
    assertThat(ClassName.bestGuess(OuterClass.InnerClass::class.java.canonicalName))
        .isEqualTo(ClassName("com.squareup.kotlinpoet",
            "ClassNameTest", "OuterClass", "InnerClass"))
  }

  @Test fun bestGuessForString_defaultPackage() {
    assertThat(ClassName.bestGuess("SomeClass"))
        .isEqualTo(ClassName("", "SomeClass"))
    assertThat(ClassName.bestGuess("SomeClass.Nested"))
        .isEqualTo(ClassName("", "SomeClass", "Nested"))
    assertThat(ClassName.bestGuess("SomeClass.Nested.EvenMore"))
        .isEqualTo(ClassName("", "SomeClass", "Nested", "EvenMore"))
  }

  @Test fun bestGuessForString_confusingInput() {
    assertBestGuessThrows("")
    assertBestGuessThrows(".")
    assertBestGuessThrows(".Map")
    assertBestGuessThrows("java")
    assertBestGuessThrows("java.util")
    assertBestGuessThrows("java.util.")
    assertBestGuessThrows("java..util.Map.Entry")
    assertBestGuessThrows("java.util..Map.Entry")
    assertBestGuessThrows("kotlin.collections.Map..Entry")
    assertBestGuessThrows("com.test.$")
    assertBestGuessThrows("com.test.LooksLikeAClass.pkg")
    assertBestGuessThrows("!@#\$gibberish%^&*")
  }

  private fun assertBestGuessThrows(s: String) {
    assertThrows<IllegalArgumentException> {
      ClassName.bestGuess(s)
    }
  }

  @Test fun createNestedClass() {
    val foo = ClassName("com.example", "Foo")
    val bar = foo.nestedClass("Bar")
    assertThat(bar).isEqualTo(ClassName("com.example", "Foo", "Bar"))
    val baz = bar.nestedClass("Baz")
    assertThat(baz).isEqualTo(ClassName("com.example", "Foo", "Bar", "Baz"))
  }

  @Test fun classNameFromTypeElement() {
    val elements = compilationRule.elements
    val element = elements.getTypeElement(Any::class.java.canonicalName)
    assertThat(element.asClassName().toString()).isEqualTo("java.lang.Object")
  }

  @Test fun classNameFromClass() {
    assertThat(Any::class.java.asClassName().toString())
        .isEqualTo("java.lang.Object")
    assertThat(OuterClass.InnerClass::class.java.asClassName().toString())
        .isEqualTo("com.squareup.kotlinpoet.ClassNameTest.OuterClass.InnerClass")
  }

  @Test fun classNameFromKClass() {
    assertThat(Any::class.asClassName().toString())
        .isEqualTo("kotlin.Any")
    assertThat(OuterClass.InnerClass::class.asClassName().toString())
        .isEqualTo("com.squareup.kotlinpoet.ClassNameTest.OuterClass.InnerClass")
  }

  @Test fun peerClass() {
    assertThat(java.lang.Double::class.asClassName().peerClass("Short"))
        .isEqualTo(java.lang.Short::class.asClassName())
    assertThat(ClassName("", "Double").peerClass("Short"))
        .isEqualTo(ClassName("", "Short"))
    assertThat(ClassName("a.b", "Combo", "Taco").peerClass("Burrito"))
        .isEqualTo(ClassName("a.b", "Combo", "Burrito"))
  }

  @Test fun fromClassRejectionTypes() {
    assertThrows<IllegalArgumentException> {
      java.lang.Integer.TYPE.asClassName()
    }

    assertThrows<IllegalArgumentException> {
      Void.TYPE.asClassName()
    }

    assertThrows<IllegalArgumentException> {
      Array<Any>::class.java.asClassName()
    }

    // TODO
    // assertThrows<IllegalArgumentException> {
    //  Array<Int>::class.asClassName()
    // }
  }

  @Test fun fromEmptySimpleName() {
    assertThrows<IllegalArgumentException> {
      ClassName("foo" /* no simple name */)
    }
  }

  @Test fun reflectionName() {
    assertThat(ANY.reflectionName())
        .isEqualTo("kotlin.Any")
    assertThat(Thread.State::class.asClassName().reflectionName())
        .isEqualTo("java.lang.Thread\$State")
    assertThat(Map.Entry::class.asClassName().reflectionName())
        .isEqualTo("kotlin.collections.Map\$Entry")
    assertThat(ClassName("", "Foo").reflectionName())
        .isEqualTo("Foo")
    assertThat(ClassName("", "Foo", "Bar", "Baz").reflectionName())
        .isEqualTo("Foo\$Bar\$Baz")
    assertThat(ClassName("a.b.c", "Foo", "Bar", "Baz").reflectionName())
        .isEqualTo("a.b.c.Foo\$Bar\$Baz")
  }

  @Test fun constructorReferences() {
    assertThat(String::class.asClassName().constructorReference().toString())
        .isEqualTo("::kotlin.String")
    assertThat(Thread.State::class.asClassName().constructorReference().toString())
        .isEqualTo("java.lang.Thread::State")
    assertThat(ClassName("", "Foo").constructorReference().toString())
        .isEqualTo("::Foo")
    assertThat(ClassName("", "Foo", "Bar", "Baz").constructorReference().toString())
        .isEqualTo("Foo.Bar::Baz")
    assertThat(ClassName("a.b.c", "Foo", "Bar", "Baz").constructorReference().toString())
        .isEqualTo("a.b.c.Foo.Bar::Baz")
  }

  @Test fun spacesEscaping() {
    val tacoFactory = ClassName("com.squareup.taco factory", "Taco Factory")
    val file = FileSpec.builder("com.squareup.tacos", "TacoTest")
        .addFunction(FunSpec.builder("main")
            .addStatement("println(%T.produceTacos())", tacoFactory)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import com.squareup.`taco factory`.`Taco Factory`
      |import kotlin.Unit
      |
      |public fun main(): Unit {
      |  println(`Taco Factory`.produceTacos())
      |}
      |""".trimMargin())
  }

  @Test fun emptySimpleNamesForbidden() {
    assertThrows<IllegalArgumentException> {
      ClassName(packageName = "", simpleNames = *emptyArray())
    }.hasMessageThat().isEqualTo("simpleNames must not be empty")

    assertThrows<IllegalArgumentException> {
      ClassName(packageName = "", simpleNames = *arrayOf("Foo", "Bar", ""))
    }.hasMessageThat().isEqualTo("simpleNames must not contain empty items: " +
        "[Foo, Bar, ]")

    assertThrows<IllegalArgumentException> {
      ClassName(packageName = "", simpleNames = emptyList())
    }.hasMessageThat().isEqualTo("simpleNames must not be empty")

    assertThrows<IllegalArgumentException> {
      ClassName(packageName = "", simpleNames = listOf("Foo", "Bar", ""))
    }.hasMessageThat().isEqualTo("simpleNames must not contain empty items: " +
        "[Foo, Bar, ]")
  }
}
