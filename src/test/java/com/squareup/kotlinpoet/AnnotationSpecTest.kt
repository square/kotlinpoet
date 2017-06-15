/*
 * Copyright (C) 2015 Square, Inc.
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
import org.junit.Rule
import org.junit.Test
import java.lang.annotation.Inherited
import kotlin.reflect.KClass

class AnnotationSpecTest {

  @Retention(AnnotationRetention.RUNTIME)
  annotation class AnnotationA

  @Inherited
  @Retention(AnnotationRetention.RUNTIME)
  annotation class AnnotationB

  @Retention(AnnotationRetention.RUNTIME)
  annotation class AnnotationC(val value: String)

  enum class Breakfast {
    WAFFLES, PANCAKES;

    override fun toString(): String {
      return name + " with cherries!"
    }
  }

  @Retention(AnnotationRetention.RUNTIME)
  annotation class HasDefaultsAnnotation(
      val a: Byte = 5,
      val b: Short = 6,
      val c: Int = 7,
      val d: Long = 8,
      val e: Float = 9.0f,
      val f: Double = 10.0,
      val g: CharArray = charArrayOf('\u0000', '\uCAFE', 'z', '€', 'ℕ', '"', '\'', '\t', '\n'),
      val h: Boolean = true,
      val i: Breakfast = Breakfast.WAFFLES,
      val j: AnnotationA = AnnotationA(),
      val k: String = "maple",
      val l: KClass<out Annotation> = AnnotationB::class,
      val m: IntArray = intArrayOf(1, 2, 3),
      val n: Array<Breakfast> = arrayOf(Breakfast.WAFFLES, Breakfast.PANCAKES),
      val o: Breakfast,
      val p: Int,
      val q: AnnotationC = AnnotationC("foo"),
      val r: Array<KClass<out Number>> = arrayOf(
          Byte::class, Short::class, Int::class, Long::class))

  @HasDefaultsAnnotation(
      o = Breakfast.PANCAKES,
      p = 1701,
      f = 11.1,
      m = intArrayOf(9, 8, 1),
      l = Override::class,
      j = AnnotationA(),
      q = AnnotationC("bar"),
      r = arrayOf(Float::class, Double::class))
  inner class IsAnnotated

  @Rule @JvmField val compilation = CompilationRule()

  @Test fun equalsAndHashCode() {
    var a = AnnotationSpec.builder(AnnotationC::class.java).build()
    var b = AnnotationSpec.builder(AnnotationC::class.java).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = AnnotationSpec.builder(AnnotationC::class.java).addMember("value", "%S", "123").build()
    b = AnnotationSpec.builder(AnnotationC::class.java).addMember("value", "%S", "123").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun defaultAnnotation() {
    val name = IsAnnotated::class.java.canonicalName
    val element = compilation.elements.getTypeElement(name)
    val annotation = AnnotationSpec.get(element.annotationMirrors[0])

    val taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(annotation)
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.AnnotationSpecTest
        |import java.lang.Override
        |import kotlin.Double
        |import kotlin.Float
        |
        |@AnnotationSpecTest.HasDefaultsAnnotation(
        |    o = AnnotationSpecTest.Breakfast.PANCAKES,
        |    p = 1701,
        |    f = 11.1,
        |    m = {
        |        9,
        |        8,
        |        1
        |    },
        |    l = Override::class,
        |    j = @AnnotationSpecTest.AnnotationA,
        |    q = @AnnotationSpecTest.AnnotationC("bar"),
        |    r = {
        |        Float::class,
        |        Double::class
        |    }
        |)
        |class Taco
        |""".trimMargin())
  }

  @Test fun defaultAnnotationWithImport() {
    val name = IsAnnotated::class.java.canonicalName
    val element = compilation.elements.getTypeElement(name)
    val annotation = AnnotationSpec.get(element.annotationMirrors[0])
    val typeBuilder = TypeSpec.classBuilder(IsAnnotated::class.java.simpleName)
    typeBuilder.addAnnotation(annotation)
    val file = KotlinFile.get("com.squareup.kotlinpoet", typeBuilder.build())
    assertThat(file.toString()).isEqualTo("""
        |package com.squareup.kotlinpoet
        |
        |import java.lang.Override
        |import kotlin.Double
        |import kotlin.Float
        |
        |@AnnotationSpecTest.HasDefaultsAnnotation(
        |    o = AnnotationSpecTest.Breakfast.PANCAKES,
        |    p = 1701,
        |    f = 11.1,
        |    m = {
        |        9,
        |        8,
        |        1
        |    },
        |    l = Override::class,
        |    j = @AnnotationSpecTest.AnnotationA,
        |    q = @AnnotationSpecTest.AnnotationC("bar"),
        |    r = {
        |        Float::class,
        |        Double::class
        |    }
        |)
        |class IsAnnotated
        |""".trimMargin())
  }

  @Test fun emptyArray() {
    val builder = AnnotationSpec.builder(HasDefaultsAnnotation::class.java)
    builder.addMember("n", "%L", "{}")
    assertThat(builder.build().toString()).isEqualTo("" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "n = {}" +
        ")")
    builder.addMember("m", "%L", "{}")
    assertThat(builder.build().toString()).isEqualTo("" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "n = {}, " +
        "m = {}" +
        ")")
  }

  @Test fun dynamicArrayOfEnumConstants() {
    var builder: AnnotationSpec.Builder = AnnotationSpec.builder(HasDefaultsAnnotation::class.java)
    builder.addMember("n", "%T.%L", Breakfast::class.java, Breakfast.PANCAKES.name)
    assertThat(builder.build().toString()).isEqualTo("" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "n = com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.PANCAKES" +
        ")")

    // builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "%T.%L", Breakfast::class.java, Breakfast.WAFFLES.name)
    builder.addMember("n", "%T.%L", Breakfast::class.java, Breakfast.PANCAKES.name)
    assertThat(builder.build().toString()).isEqualTo("" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "n = {" +
        "com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.PANCAKES, " +
        "com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.WAFFLES, " +
        "com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.PANCAKES" +
        "})")

    builder = builder.build().toBuilder() // idempotent
    assertThat(builder.build().toString()).isEqualTo("" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "n = {" +
        "com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.PANCAKES" +
        ", com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.WAFFLES" +
        ", com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.PANCAKES" +
        "})")

    builder.addMember("n", "%T.%L", Breakfast::class.java, Breakfast.WAFFLES.name)
    assertThat(builder.build().toString()).isEqualTo("" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "n = {" +
        "com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.PANCAKES" +
        ", com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.WAFFLES" +
        ", com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.PANCAKES" +
        ", com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.WAFFLES" +
        "})")
  }

  @Test fun defaultAnnotationToBuilder() {
    val name = IsAnnotated::class.java.canonicalName
    val element = compilation.elements.getTypeElement(name)
    val builder = AnnotationSpec.get(element.annotationMirrors[0])
        .toBuilder()
    builder.addMember("m", "%L", 123)
    assertThat(builder.build().toString()).isEqualTo("" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "o = com.squareup.kotlinpoet.AnnotationSpecTest.Breakfast.PANCAKES" +
        ", p = 1701" +
        ", f = 11.1" +
        ", m = {9, 8, 1, 123}" +
        ", l = java.lang.Override::class" +
        ", j = @com.squareup.kotlinpoet.AnnotationSpecTest.AnnotationA" +
        ", q = @com.squareup.kotlinpoet.AnnotationSpecTest.AnnotationC(\"bar\")" +
        ", r = {kotlin.Float::class, kotlin.Double::class}" +
        ")")
  }

  @Test fun reflectAnnotation() {
    val annotation = IsAnnotated::class.java.getAnnotation(HasDefaultsAnnotation::class.java)
    val spec = AnnotationSpec.get(annotation)
    val taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(spec)
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.AnnotationSpecTest
        |import java.lang.Override
        |import kotlin.Double
        |import kotlin.Float
        |
        |@AnnotationSpecTest.HasDefaultsAnnotation(
        |    f = 11.1,
        |    l = Override::class,
        |    m = {
        |        9,
        |        8,
        |        1
        |    },
        |    o = AnnotationSpecTest.Breakfast.PANCAKES,
        |    p = 1701,
        |    q = @AnnotationSpecTest.AnnotationC("bar"),
        |    r = {
        |        Float::class,
        |        Double::class
        |    }
        |)
        |class Taco
        |""".trimMargin())
  }

  @Test fun reflectAnnotationWithDefaults() {
    val annotation = IsAnnotated::class.java.getAnnotation(HasDefaultsAnnotation::class.java)
    val spec = AnnotationSpec.get(annotation, true)
    val taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(spec)
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.AnnotationSpecTest
        |import java.lang.Override
        |import kotlin.Double
        |import kotlin.Float
        |
        |@AnnotationSpecTest.HasDefaultsAnnotation(
        |    a = 5,
        |    b = 6,
        |    c = 7,
        |    d = 8,
        |    e = 9.0f,
        |    f = 11.1,
        |    g = {
        |        '\u0000',
        |        '쫾',
        |        'z',
        |        '€',
        |        'ℕ',
        |        '"',
        |        '\'',
        |        '\t',
        |        '\n'
        |    },
        |    h = true,
        |    i = AnnotationSpecTest.Breakfast.WAFFLES,
        |    j = @AnnotationSpecTest.AnnotationA,
        |    k = "maple",
        |    l = Override::class,
        |    m = {
        |        9,
        |        8,
        |        1
        |    },
        |    n = {
        |        AnnotationSpecTest.Breakfast.WAFFLES,
        |        AnnotationSpecTest.Breakfast.PANCAKES
        |    },
        |    o = AnnotationSpecTest.Breakfast.PANCAKES,
        |    p = 1701,
        |    q = @AnnotationSpecTest.AnnotationC("bar"),
        |    r = {
        |        Float::class,
        |        Double::class
        |    }
        |)
        |class Taco
        |""".trimMargin())
  }

  private fun toString(typeSpec: TypeSpec): String {
    return KotlinFile.get("com.squareup.tacos", typeSpec).toString()
  }
}
