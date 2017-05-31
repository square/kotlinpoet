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
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class ClassNameTest {
  @Rule @JvmField var compilationRule = CompilationRule()

  @Test fun bestGuessForString_simpleClass() {
    assertThat(ClassName.bestGuess(String::class.java.name))
        .isEqualTo(ClassName.get("java.lang", "String"))
  }

  @Test fun bestGuessNonAscii() {
    val className = ClassName.bestGuess(
        "com.\ud835\udc1andro\ud835\udc22d.\ud835\udc00ctiv\ud835\udc22ty")
    assertEquals("com.\ud835\udc1andro\ud835\udc22d", className.packageName())
    assertEquals("\ud835\udc00ctiv\ud835\udc22ty", className.simpleName())
  }

  internal class OuterClass {
    internal class InnerClass
  }

  @Test fun bestGuessForString_nestedClass() {
    assertThat(ClassName.bestGuess(Map.Entry::class.java.canonicalName))
        .isEqualTo(ClassName.get("java.util", "Map", "Entry"))
    assertThat(ClassName.bestGuess(OuterClass.InnerClass::class.java.canonicalName))
        .isEqualTo(ClassName.get("com.squareup.kotlinpoet",
            "ClassNameTest", "OuterClass", "InnerClass"))
  }

  @Test fun bestGuessForString_defaultPackage() {
    assertThat(ClassName.bestGuess("SomeClass"))
        .isEqualTo(ClassName.get("", "SomeClass"))
    assertThat(ClassName.bestGuess("SomeClass.Nested"))
        .isEqualTo(ClassName.get("", "SomeClass", "Nested"))
    assertThat(ClassName.bestGuess("SomeClass.Nested.EvenMore"))
        .isEqualTo(ClassName.get("", "SomeClass", "Nested", "EvenMore"))
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
    try {
      ClassName.bestGuess(s)
      fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test fun createNestedClass() {
    val foo = ClassName.get("com.example", "Foo")
    val bar = foo.nestedClass("Bar")
    assertThat(bar).isEqualTo(ClassName.get("com.example", "Foo", "Bar"))
    val baz = bar.nestedClass("Baz")
    assertThat(baz).isEqualTo(ClassName.get("com.example", "Foo", "Bar", "Baz"))
  }

  @Test fun classNameFromTypeElement() {
    val elements = compilationRule.elements
    val element = elements.getTypeElement(Any::class.java.canonicalName)
    assertThat(ClassName.get(element).toString()).isEqualTo("java.lang.Object")
  }

  @Test fun classNameFromClass() {
    assertThat(ClassName.get(Any::class.java).toString())
        .isEqualTo("java.lang.Object")
    assertThat(ClassName.get(OuterClass.InnerClass::class.java).toString())
        .isEqualTo("com.squareup.kotlinpoet.ClassNameTest.OuterClass.InnerClass")
  }

  @Test fun classNameFromKClass() {
    assertThat(ClassName.get(Any::class).toString())
        .isEqualTo("kotlin.Any")
    assertThat(ClassName.get(OuterClass.InnerClass::class).toString())
        .isEqualTo("com.squareup.kotlinpoet.ClassNameTest.OuterClass.InnerClass")
  }

  @Test fun peerClass() {
    assertThat(ClassName.get(java.lang.Double::class).peerClass("Short"))
        .isEqualTo(ClassName.get(java.lang.Short::class))
    assertThat(ClassName.get("", "Double").peerClass("Short"))
        .isEqualTo(ClassName.get("", "Short"))
    assertThat(ClassName.get("a.b", "Combo", "Taco").peerClass("Burrito"))
        .isEqualTo(ClassName.get("a.b", "Combo", "Burrito"))
  }

  @Test fun fromClassRejectionTypes() {
    try {
      ClassName.get(java.lang.Integer.TYPE)
      fail()
    } catch (expected: IllegalArgumentException) {
    }

    try {
      ClassName.get(Void.TYPE)
      fail()
    } catch (expected: IllegalArgumentException) {
    }

    try {
      ClassName.get(Array<Any>::class.java)
      fail()
    } catch (expected: IllegalArgumentException) {
    }

    // TODO
    //try {
    //  ClassName.get(Array<Int>::class)
    //  fail()
    //} catch (expected: IllegalArgumentException) {
    //}
  }

  @Test fun reflectionName() {
    assertThat(ANY.reflectionName())
        .isEqualTo("kotlin.Any")
    assertThat(ClassName.get(Thread.State::class).reflectionName())
        .isEqualTo("java.lang.Thread\$State")
    assertThat(ClassName.get(Map.Entry::class).reflectionName())
        .isEqualTo("kotlin.collections.Map\$Entry")
    assertThat(ClassName.get("", "Foo").reflectionName())
        .isEqualTo("Foo")
    assertThat(ClassName.get("", "Foo", "Bar", "Baz").reflectionName())
        .isEqualTo("Foo\$Bar\$Baz")
    assertThat(ClassName.get("a.b.c", "Foo", "Bar", "Baz").reflectionName())
        .isEqualTo("a.b.c.Foo\$Bar\$Baz")
  }
}
