/*
 * Copyright (C) 2014 Google, Inc.
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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.Cased.Weird.Sup
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
      "com.\ud835\udc1andro\ud835\udc22d.\ud835\udc00ctiv\ud835\udc22ty"
    )
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
      .isEqualTo(
        ClassName(
          "com.squareup.kotlinpoet",
          "ClassNameTest", "OuterClass", "InnerClass"
        )
      )
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
    assertFailure {
      ClassName.bestGuess(s)
    }.isInstanceOf<IllegalArgumentException>()
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

    // Note: Do NOT rewrite this assertion to something more clever since behaviors
    //  like TypeName.equals may subvert the correct partitioning of package and names.
    val hi = Sup.Hi::class.asClassName()
    assertThat(hi.packageName).isEqualTo("com.squareup.kotlinpoet.Cased.Weird")
    assertThat(hi.simpleNames).containsExactly("Sup", "Hi")
  }

  @Test fun classNameFromKClassSpecialCases() {
    assertEquals(ClassName(listOf("kotlin", "Boolean", "Companion")), Boolean.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "Byte", "Companion")), Byte.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "Char", "Companion")), Char.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "Double", "Companion")), Double.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "Enum", "Companion")), Enum.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "Float", "Companion")), Float.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "Int", "Companion")), Int.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "Long", "Companion")), Long.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "Short", "Companion")), Short.Companion::class.asClassName())
    assertEquals(ClassName(listOf("kotlin", "String", "Companion")), String.Companion::class.asClassName())
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
    assertFailure {
      java.lang.Integer.TYPE.asClassName()
    }.isInstanceOf<IllegalArgumentException>()

    assertFailure {
      Void.TYPE.asClassName()
    }.isInstanceOf<IllegalArgumentException>()

    assertFailure {
      Array<Any>::class.java.asClassName()
    }.isInstanceOf<IllegalArgumentException>()

    // TODO
    // assertFailure {
    //  Array<Int>::class.asClassName()
    // }.isInstanceOf<IllegalArgumentException>()
  }

  @Suppress("DEPRECATION_ERROR") // Ensure still throws in case called from Java.
  @Test fun fromEmptySimpleName() {
    assertFailure {
      ClassName("foo" /* no simple name */)
    }.isInstanceOf<IllegalArgumentException>()
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
      .addFunction(
        FunSpec.builder("main")
          .addStatement("println(%T.produceTacos())", tacoFactory)
          .build()
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import com.squareup.`taco factory`.`Taco Factory`
      |
      |public fun main() {
      |  println(`Taco Factory`.produceTacos())
      |}
      |""".trimMargin()
    )
  }

  @Test fun emptySimpleNamesForbidden() {
    assertFailure {
      ClassName(packageName = "", simpleNames = emptyArray())
    }.isInstanceOf<IllegalArgumentException>()
      .hasMessage("simpleNames must not be empty")

    assertFailure {
      ClassName(packageName = "", simpleNames = arrayOf("Foo", "Bar", ""))
    }.isInstanceOf<IllegalArgumentException>()
      .hasMessage(
      "simpleNames must not contain empty items: " +
        "[Foo, Bar, ]"
    )

    assertFailure {
      ClassName(packageName = "", simpleNames = emptyList())
    }.isInstanceOf<IllegalArgumentException>()
      .hasMessage("simpleNames must not be empty")

    assertFailure {
      ClassName(packageName = "", simpleNames = listOf("Foo", "Bar", ""))
    }.isInstanceOf<IllegalArgumentException>()
      .hasMessage(
      "simpleNames must not contain empty items: " +
        "[Foo, Bar, ]"
    )
  }

  @Test fun equalsAndHashCode() {
    val foo1 = ClassName(names = listOf("com.example", "Foo"))
    val foo2 = ClassName(names = listOf("com.example", "Foo"))
    assertThat(foo1).isEqualTo(foo2)
    assertThat(foo1.hashCode()).isEqualTo(foo2.hashCode())
  }

  @Test fun equalsDifferentiatesPackagesFromSimpleNames() {
    val outerFoo = ClassName("com.example.Foo", "Bar")
    val packageFoo = ClassName("com.example", "Foo", "Bar")

    assertThat(outerFoo).isNotEqualTo(packageFoo)
  }

  @Test fun equalsDifferentiatesNullabilityAndAnnotations() {
    val foo = ClassName(names = listOf("com.example", "Foo"))
    assertThat(foo.copy(annotations = listOf(AnnotationSpec.Builder(Suppress::class.asClassName()).build()))).isNotEqualTo(foo)
    assertThat(foo.copy(nullable = true)).isNotEqualTo(foo)
  }

  @Test fun equalsAndHashCodeIgnoreTags() {
    val foo = ClassName(names = listOf("com.example", "Foo"))
    val taggedFoo = foo.copy(tags = mapOf(String::class to "test"))

    assertThat(foo).isEqualTo(taggedFoo)
    assertThat(foo.hashCode()).isEqualTo(taggedFoo.hashCode())
  }

  @Test fun compareTo() {
    val robot = ClassName("com.example", "Robot")
    val robotMotor = ClassName("com.example", "Robot", "Motor")
    val roboticVacuum = ClassName("com.example", "RoboticVacuum")

    val list = listOf(robot, robotMotor, roboticVacuum)

    assertThat(list.sorted()).isEqualTo(listOf(robot, robotMotor, roboticVacuum))
  }

  @Test fun compareToConsistentWithEquals() {
    val foo1 = ClassName(names = listOf("com.example", "Foo"))
    val foo2 = ClassName(names = listOf("com.example", "Foo"))
    assertThat(foo1.compareTo(foo2)).isEqualTo(0)
  }

  @Test fun compareToDifferentiatesPackagesFromSimpleNames() {
    val parentFooNestedBar = ClassName("com.example", "Foo", "Bar")
    val packageFooClassBar = ClassName("com.example.Foo", "Bar")
    val parentFooNestedBaz = ClassName("com.example", "Foo", "Baz")
    val packageFooClassBaz = ClassName("com.example.Foo", "Baz")
    val parentGooNestedBar = ClassName("com.example", "Goo", "Bar")
    val packageGooClassBar = ClassName("com.example.Goo", "Bar")

    val list = listOf(
      parentFooNestedBar,
      packageFooClassBar,
      parentFooNestedBaz,
      packageFooClassBaz,
      parentGooNestedBar,
      packageGooClassBar,
    )

    assertThat(list.sorted()).isEqualTo(
      listOf(
        parentFooNestedBar,
        parentFooNestedBaz,
        parentGooNestedBar,
        packageFooClassBar,
        packageFooClassBaz,
        packageGooClassBar,
      ),
    )
  }

  @Test fun compareToDifferentiatesNullabilityAndAnnotations() {
    val plain = ClassName(
      listOf("com.example", "Foo")
    )
    val nullable = ClassName(
      listOf("com.example", "Foo"),
      nullable = true,
    )
    val annotated = ClassName(
      listOf("com.example", "Foo"),
      nullable = true,
      annotations = listOf(
        AnnotationSpec.Builder(Suppress::class.asClassName()).build(),
      ),
    )

    val list = listOf(plain, nullable, annotated)

    assertThat(list.sorted()).isEqualTo(
      listOf(plain, nullable, annotated),
    )
  }

  @Test fun compareToDifferentiatesByAnnotation() {
    val noAnnotations = ClassName(listOf("com.example", "Foo"))

    val oneAnnotation = ClassName(
      listOf("com.example", "Foo"),
      annotations = listOf(AnnotationSpec.Builder(Suppress::class.asClassName()).build()),
    )
    val twoAnnotations = ClassName(
      listOf("com.example", "Foo"),
      annotations = listOf(
        AnnotationSpec.Builder(Suppress::class.asClassName()).build(),
        AnnotationSpec.Builder(Test::class.asClassName()).build(),
      ),
    )
    val secondAnnotationOnly = ClassName(
      listOf("com.example", "Foo"),
      annotations = listOf(
        AnnotationSpec.Builder(Test::class.asClassName()).build(),
      ),
    )

    val list = listOf(noAnnotations, oneAnnotation, twoAnnotations, secondAnnotationOnly)

    assertThat(list.sorted()).isEqualTo(
      listOf(noAnnotations, oneAnnotation, twoAnnotations, secondAnnotationOnly),
    )
  }

  @Test fun compareToDoesNotDifferentiateByTag() {
    val noTags = ClassName(listOf("com.example", "Foo"))

    val oneTag = ClassName(
      listOf("com.example", "Foo"),
      tags = mapOf(String::class to "test"),
    )
    val twoTags = ClassName(
      listOf("com.example", "Foo"),
      tags = mapOf(String::class to "test", Int::class to 1),
    )

    assertThat(noTags.compareTo(oneTag)).isEqualTo(0)
    assertThat(oneTag.compareTo(twoTags)).isEqualTo(0)
  }
}
