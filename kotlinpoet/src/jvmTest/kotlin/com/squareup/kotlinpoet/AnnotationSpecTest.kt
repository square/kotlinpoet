/*
 * Copyright (C) 2015 Square, Inc.
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
import assertk.assertions.isTrue
import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import java.lang.annotation.Inherited
import kotlin.reflect.KClass
import kotlin.test.Test
import org.junit.Rule

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
      return "$name with cherries!"
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
    val g: CharArray = ['\u0000', '\uCAFE', 'z', '€', 'ℕ', '"', '\'', '\t', '\n'],
    val h: Boolean = true,
    val i: Breakfast = Breakfast.WAFFLES,
    val j: AnnotationA = AnnotationA(),
    val k: String = "maple",
    val l: KClass<out Annotation> = AnnotationB::class,
    val m: IntArray = [1, 2, 3],
    val n: Array<Breakfast> = [Breakfast.WAFFLES, Breakfast.PANCAKES],
    val o: Breakfast,
    val p: Int,
    val q: AnnotationC = AnnotationC("foo"),
    val r: Array<KClass<out Number>> = [Byte::class, Short::class, Int::class, Long::class],
    val s: Array<AnnotationC> = [AnnotationC("foo"), AnnotationC("bar")],
  )

  @HasDefaultsAnnotation(
    o = Breakfast.PANCAKES,
    p = 1701,
    f = 11.1,
    m = [9, 8, 1],
    l = Override::class,
    j = AnnotationA(),
    q = AnnotationC("bar"),
    r = [Float::class, Double::class],
    s = [AnnotationC("bar")],
  )
  inner class IsAnnotated

  @Rule @JvmField
  val compilation = CompilationRule()

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

    assertThat(toString(annotation)).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.AnnotationSpecTest
        |import java.lang.Override
        |import kotlin.Double
        |import kotlin.Float
        |
        |@AnnotationSpecTest.HasDefaultsAnnotation(
        |  f = 11.1,
        |  j = AnnotationSpecTest.AnnotationA(),
        |  l = Override::class,
        |  m = arrayOf(9, 8, 1),
        |  o = AnnotationSpecTest.Breakfast.PANCAKES,
        |  p = 1_701,
        |  q = AnnotationSpecTest.AnnotationC(value = "bar"),
        |  r = arrayOf(Float::class, Double::class),
        |  s = arrayOf(AnnotationSpecTest.AnnotationC(value = "bar")),
        |)
        |public class Taco
        |
      """.trimMargin(),
    )
  }

  @Test fun defaultAnnotationWithImport() {
    val name = IsAnnotated::class.java.canonicalName
    val element = compilation.elements.getTypeElement(name)
    val annotation = AnnotationSpec.get(element.annotationMirrors[0])
    val typeBuilder = TypeSpec.classBuilder(IsAnnotated::class.java.simpleName)
    typeBuilder.addAnnotation(annotation)
    val file = FileSpec.get("com.squareup.kotlinpoet", typeBuilder.build())
    assertThat(file.toString()).isEqualTo(
      """
        |package com.squareup.kotlinpoet
        |
        |import java.lang.Override
        |import kotlin.Double
        |import kotlin.Float
        |
        |@AnnotationSpecTest.HasDefaultsAnnotation(
        |  f = 11.1,
        |  j = AnnotationSpecTest.AnnotationA(),
        |  l = Override::class,
        |  m = arrayOf(9, 8, 1),
        |  o = AnnotationSpecTest.Breakfast.PANCAKES,
        |  p = 1_701,
        |  q = AnnotationSpecTest.AnnotationC(value = "bar"),
        |  r = arrayOf(Float::class, Double::class),
        |  s = arrayOf(AnnotationSpecTest.AnnotationC(value = "bar")),
        |)
        |public class IsAnnotated
        |
      """.trimMargin(),
    )
  }

  @Test fun emptyArray() {
    val builder = AnnotationSpec.builder(HasDefaultsAnnotation::class.java)
    builder.addMember("%L = %L", "n", "[]")
    assertThat(builder.build().toString()).isEqualTo(
      "" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "n = []" +
        ")",
    )
    builder.addMember("%L = %L", "m", "[]")
    assertThat(builder.build().toString()).isEqualTo(
      "" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.HasDefaultsAnnotation(" +
        "n = [], " +
        "m = []" +
        ")",
    )
  }

  @Test fun reflectAnnotation() {
    val annotation = IsAnnotated::class.java.getAnnotation(HasDefaultsAnnotation::class.java)
    val spec = AnnotationSpec.get(annotation)

    assertThat(toString(spec)).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.AnnotationSpecTest
        |import java.lang.Override
        |import kotlin.Double
        |import kotlin.Float
        |
        |@AnnotationSpecTest.HasDefaultsAnnotation(
        |  f = 11.1,
        |  l = Override::class,
        |  m = arrayOf(9, 8, 1),
        |  o = AnnotationSpecTest.Breakfast.PANCAKES,
        |  p = 1_701,
        |  q = AnnotationSpecTest.AnnotationC(value = "bar"),
        |  r = arrayOf(Float::class, Double::class),
        |  s = arrayOf(AnnotationSpecTest.AnnotationC(value = "bar")),
        |)
        |public class Taco
        |
      """.trimMargin(),
    )
  }

  @Test fun reflectAnnotationWithDefaults() {
    val annotation = IsAnnotated::class.java.getAnnotation(HasDefaultsAnnotation::class.java)
    val spec = AnnotationSpec.get(annotation, true)

    assertThat(toString(spec)).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.AnnotationSpecTest
        |import java.lang.Override
        |import kotlin.Double
        |import kotlin.Float
        |
        |@AnnotationSpecTest.HasDefaultsAnnotation(
        |  a = 5,
        |  b = 6,
        |  c = 7,
        |  d = 8,
        |  e = 9.0f,
        |  f = 11.1,
        |  g = arrayOf('\u0000', '쫾', 'z', '€', 'ℕ', '"', '\'', '\t', '\n'),
        |  h = true,
        |  i = AnnotationSpecTest.Breakfast.WAFFLES,
        |  j = AnnotationSpecTest.AnnotationA(),
        |  k = "maple",
        |  l = Override::class,
        |  m = arrayOf(9, 8, 1),
        |  n = arrayOf(AnnotationSpecTest.Breakfast.WAFFLES, AnnotationSpecTest.Breakfast.PANCAKES),
        |  o = AnnotationSpecTest.Breakfast.PANCAKES,
        |  p = 1_701,
        |  q = AnnotationSpecTest.AnnotationC(value = "bar"),
        |  r = arrayOf(Float::class, Double::class),
        |  s = arrayOf(AnnotationSpecTest.AnnotationC(value = "bar")),
        |)
        |public class Taco
        |
      """.trimMargin(),
    )
  }

  @Test fun useSiteTarget() {
    val builder = AnnotationSpec.builder(AnnotationA::class)
    assertThat(builder.build().toString()).isEqualTo(
      "" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.AnnotationA",
    )
    builder.useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
    assertThat(builder.build().toString()).isEqualTo(
      "" +
        "@field:com.squareup.kotlinpoet.AnnotationSpecTest.AnnotationA",
    )
    builder.useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
    assertThat(builder.build().toString()).isEqualTo(
      "" +
        "@get:com.squareup.kotlinpoet.AnnotationSpecTest.AnnotationA",
    )
    builder.useSiteTarget(null)
    assertThat(builder.build().toString()).isEqualTo(
      "" +
        "@com.squareup.kotlinpoet.AnnotationSpecTest.AnnotationA",
    )
  }

  @Test fun deprecatedTest() {
    val annotation = AnnotationSpec.builder(Deprecated::class)
      .addMember("%S", "Nope")
      .addMember("%T(%S)", ReplaceWith::class, "Yep")
      .build()

    assertThat(annotation.toString()).isEqualTo(
      "" +
        "@kotlin.Deprecated(\"Nope\", kotlin.ReplaceWith(\"Yep\"))",
    )
  }

  @Test fun modifyMembers() {
    val builder = AnnotationSpec.builder(Deprecated::class)
      .addMember("%S", "Nope")
      .addMember("%T(%S)", ReplaceWith::class, "Yep")

    builder.members.removeAt(1)
    builder.members.add(CodeBlock.of("%T(%S)", ReplaceWith::class, "Nope"))

    assertThat(builder.build().toString()).isEqualTo(
      "" +
        "@kotlin.Deprecated(\"Nope\", kotlin.ReplaceWith(\"Nope\"))",
    )
  }

  @Test fun annotationStringsAreConstant() {
    val text = "This is a long string with a newline\nin the middle."
    val builder = AnnotationSpec.builder(Deprecated::class)
      .addMember("%S", text)

    assertThat(builder.build().toString()).isEqualTo(
      "" +
        "@kotlin.Deprecated(\"This is a long string with a newline\\nin the middle.\")",
    )
  }

  @Test fun literalAnnotation() {
    val annotationSpec = AnnotationSpec.builder(Suppress::class)
      .addMember("%S", "Things")
      .build()

    val file = FileSpec.builder("test", "Test")
      .addFunction(
        FunSpec.builder("test")
          .addStatement("%L", annotationSpec)
          .addStatement("val annotatedString = %S", "AnnotatedString")
          .build(),
      )
      .build()
    assertThat(file.toString().trim()).isEqualTo(
      """
      |package test
      |
      |import kotlin.Suppress
      |
      |public fun test() {
      |  @Suppress("Things")
      |  val annotatedString = "AnnotatedString"
      |}
      """.trimMargin(),
    )
  }

  @Test fun functionOnlyLiteralAnnotation() {
    val annotation = AnnotationSpec
      .builder(ClassName.bestGuess("Suppress"))
      .addMember("%S", "UNCHECKED_CAST")
      .build()
    val funSpec = FunSpec.builder("operation")
      .addStatement("%L", annotation)
      .build()

    assertThat(funSpec.toString().trim()).isEqualTo(
      """
      |public fun operation() {
      |  @Suppress("UNCHECKED_CAST")
      |}
      """.trimMargin(),
    )
  }

  @Test fun getOnVarargMirrorShouldNameValueArg() {
    val myClazz = compilation.elements
      .getTypeElement(KotlinClassWithVarargAnnotation::class.java.canonicalName)
    val classBuilder = TypeSpec.classBuilder("Result")

    myClazz.annotationMirrors.map { AnnotationSpec.get(it) }
      .filter {
        val typeName = it.typeName
        return@filter typeName is ClassName && typeName.simpleName == "AnnotationWithArrayValue"
      }
      .forEach {
        classBuilder.addAnnotation(it)
      }

    assertThat(toString(classBuilder.build()).trim()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.AnnotationSpecTest
        |import java.lang.Object
        |import kotlin.Boolean
        |
        |@AnnotationSpecTest.AnnotationWithArrayValue(value = arrayOf(Object::class, Boolean::class))
        |public class Result
      """.trimMargin(),
    )
  }

  @Test fun getOnVarargAnnotationShouldNameValueArg() {
    val annotation = KotlinClassWithVarargAnnotation::class.java
      .getAnnotation(AnnotationWithArrayValue::class.java)
    val classBuilder = TypeSpec.classBuilder("Result")
      .addAnnotation(AnnotationSpec.get(annotation))

    assertThat(toString(classBuilder.build()).trim()).isEqualTo(
      """
        |package com.squareup.tacos
        |
        |import com.squareup.kotlinpoet.AnnotationSpecTest
        |import java.lang.Object
        |import kotlin.Boolean
        |
        |@AnnotationSpecTest.AnnotationWithArrayValue(value = arrayOf(Object::class, Boolean::class))
        |public class Result
      """.trimMargin(),
    )
  }

  @AnnotationWithArrayValue(Any::class, Boolean::class)
  class KotlinClassWithVarargAnnotation

  @Retention(AnnotationRetention.RUNTIME)
  internal annotation class AnnotationWithArrayValue(vararg val value: KClass<*>)

  @Test fun annotationsWithTypeParameters() {
    // Example from https://kotlinlang.org/docs/tutorials/android-plugin.html
    val externalClass = ClassName("com.squareup.parceler", "ExternalClass")
    val externalClassSpec = TypeSpec.classBuilder(externalClass)
      .addProperty(
        PropertySpec.builder("value", Int::class)
          .initializer("value")
          .build(),
      )
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("value", Int::class)
          .build(),
      )
      .build()
    val externalClassParceler = ClassName("com.squareup.parceler", "ExternalClassParceler")
    val parcel = ClassName("com.squareup.parceler", "Parcel")
    val externalClassParcelerSpec = TypeSpec.objectBuilder(externalClassParceler)
      .addSuperinterface(
        ClassName("com.squareup.parceler", "Parceler")
          .parameterizedBy(externalClass),
      )
      .addFunction(
        FunSpec.builder("create")
          .addModifiers(OVERRIDE)
          .addParameter("parcel", parcel)
          .returns(externalClass)
          .addStatement("return %T(parcel.readInt())", externalClass)
          .build(),
      )
      .addFunction(
        FunSpec.builder("write")
          .addModifiers(OVERRIDE)
          .receiver(externalClass)
          .addParameter("parcel", parcel)
          .addParameter("flags", Int::class)
          .addStatement("parcel.writeInt(value)")
          .build(),
      )
      .build()
    val parcelize = ClassName("com.squareup.parceler", "Parcelize")
    val typeParceler = ClassName("com.squareup.parceler", "TypeParceler")
    val typeParcelerAnnotation = AnnotationSpec.builder(
      typeParceler
        .plusParameter(externalClass)
        .plusParameter(externalClassParceler),
    )
      .build()
    val classLocalParceler = TypeSpec.classBuilder("MyClass")
      .addAnnotation(parcelize)
      .addAnnotation(typeParcelerAnnotation)
      .addProperty(
        PropertySpec.builder("external", externalClass)
          .initializer("external")
          .build(),
      )
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("external", externalClass)
          .build(),
      )
      .build()
    val propertyLocalParceler = TypeSpec.classBuilder("MyClass")
      .addAnnotation(parcelize)
      .addProperty(
        PropertySpec.builder("external", externalClass)
          .addAnnotation(typeParcelerAnnotation)
          .initializer("external")
          .build(),
      )
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("external", externalClass)
          .build(),
      )
      .build()
    val writeWith = ClassName("com.squareup.parceler", "WriteWith")
    val writeWithExternalClass = externalClass
      .copy(
        annotations = listOf(
          AnnotationSpec
            .builder(writeWith.plusParameter(externalClassParceler))
            .build(),
        ),
      )
    val typeLocalParceler = TypeSpec.classBuilder("MyClass")
      .addAnnotation(parcelize)
      .addProperty(
        PropertySpec.builder("external", writeWithExternalClass)
          .initializer("external")
          .build(),
      )
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("external", writeWithExternalClass)
          .build(),
      )
      .build()
    val file = FileSpec.builder("com.squareup.parceler", "Test")
      .addType(externalClassSpec)
      .addType(externalClassParcelerSpec)
      .addType(classLocalParceler)
      .addType(propertyLocalParceler)
      .addType(typeLocalParceler)
      .build()
    //language=kotlin
    assertThat(file.toString()).isEqualTo(
      """
      package com.squareup.parceler

      import kotlin.Int

      public class ExternalClass(
        public val `value`: Int,
      )

      public object ExternalClassParceler : Parceler<ExternalClass> {
        override fun create(parcel: Parcel): ExternalClass = ExternalClass(parcel.readInt())

        override fun ExternalClass.write(parcel: Parcel, flags: Int) {
          parcel.writeInt(value)
        }
      }

      @Parcelize
      @TypeParceler<ExternalClass, ExternalClassParceler>
      public class MyClass(
        public val `external`: ExternalClass,
      )

      @Parcelize
      public class MyClass(
        @TypeParceler<ExternalClass, ExternalClassParceler>
        public val `external`: ExternalClass,
      )

      @Parcelize
      public class MyClass(
        public val `external`: @WriteWith<ExternalClassParceler> ExternalClass,
      )

      """.trimIndent(),
    )
  }

  private fun toString(annotationSpec: AnnotationSpec) =
    toString(TypeSpec.classBuilder("Taco").addAnnotation(annotationSpec).build())

  private fun toString(typeSpec: TypeSpec) =
    FileSpec.get("com.squareup.tacos", typeSpec).toString()
}
