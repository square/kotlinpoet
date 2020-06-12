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
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.SET
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.Collections
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore
import kotlin.test.Test

class FileSpecTest {
  @Test fun importStaticReadmeExample() {
    val hoverboard = ClassName("com.mattel", "Hoverboard")
    val namedBoards = ClassName("com.mattel", "Hoverboard", "Boards")
    val list = List::class.asClassName()
    val arrayList = ClassName("java.util", "ArrayList").parameterizedBy(hoverboard)
    val listOfHoverboards = list.parameterizedBy(hoverboard)
    val beyond = FunSpec.builder("beyond")
        .returns(listOfHoverboards)
        .addStatement("val result = %T()", arrayList)
        .addStatement("result.add(%T.createNimbus(2000))", hoverboard)
        .addStatement("result.add(%T.createNimbus(\"2001\"))", hoverboard)
        .addStatement("result.add(%T.createNimbus(%T.THUNDERBOLT))", hoverboard, namedBoards)
        .addStatement("%T.sort(result)", Collections::class)
        .addStatement("return if (result.isEmpty()) %T.emptyList() else result", Collections::class)
        .build()
    val hello = TypeSpec.classBuilder("HelloWorld")
        .addFunction(beyond)
        .build()
    val source = FileSpec.builder("com.example.helloworld", "HelloWorld")
        .addType(hello)
        .addImport(hoverboard, "createNimbus")
        .addImport(namedBoards, "THUNDERBOLT")
        .addImport(Collections::class, "sort", "emptyList")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.example.helloworld
        |
        |import com.mattel.Hoverboard
        |import com.mattel.Hoverboard.Boards.THUNDERBOLT
        |import com.mattel.Hoverboard.createNimbus
        |import java.util.ArrayList
        |import java.util.Collections.emptyList
        |import java.util.Collections.sort
        |import kotlin.collections.List
        |
        |public class HelloWorld {
        |  public fun beyond(): List<Hoverboard> {
        |    val result = ArrayList<Hoverboard>()
        |    result.add(createNimbus(2000))
        |    result.add(createNimbus("2001"))
        |    result.add(createNimbus(THUNDERBOLT))
        |    sort(result)
        |    return if (result.isEmpty()) emptyList() else result
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticMixed() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addInitializerBlock(CodeBlock.builder()
                .addStatement("assert %1T.valueOf(\"BLOCKED\") == %1T.BLOCKED", Thread.State::class)
                .addStatement("%T.gc()", System::class)
                .addStatement("%1T.out.println(%1T.nanoTime())", System::class)
                .build())
            .addFunction(FunSpec.constructorBuilder()
                .addParameter("states", Thread.State::class.asClassName(), VARARG)
                .build())
            .build())
        .addImport(Thread.State.BLOCKED)
        .addImport(System::class, "gc", "out", "nanoTime")
        .addImport(Thread.State::class, "valueOf")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.System.gc
        |import java.lang.System.nanoTime
        |import java.lang.System.out
        |import java.lang.Thread
        |import java.lang.Thread.State.BLOCKED
        |import java.lang.Thread.State.valueOf
        |
        |public class Taco {
        |  init {
        |    assert valueOf("BLOCKED") == BLOCKED
        |    gc()
        |    out.println(nanoTime())
        |  }
        |
        |  public constructor(vararg states: Thread.State)
        |}
        |""".trimMargin())
  }

  @Test fun importTopLevel() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addImport("com.squareup.tacos.internal", "INGREDIENTS", "wrap")
        .addFunction(FunSpec.builder("prepareTacos")
            .returns(List::class.asClassName()
                .parameterizedBy(ClassName("com.squareup.tacos", "Taco")))
            .addCode("return wrap(INGREDIENTS)\n")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.tacos.internal.INGREDIENTS
        |import com.squareup.tacos.internal.wrap
        |import kotlin.collections.List
        |
        |public fun prepareTacos(): List<Taco> = wrap(INGREDIENTS)
        |""".trimMargin())
  }

  @Ignore("addImport doesn't support members with %L")
  @Test
  fun importStaticDynamic() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addFunction(FunSpec.builder("main")
                .addStatement("%T.%L.println(%S)", System::class, "out", "hello")
                .build())
            .build())
        .addImport(System::class, "out")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos;
        |
        |import static java.lang.System.out;
        |
        |class Taco {
        |  void main() {
        |    out.println("hello");
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticNone() {
    val source = FileSpec.builder("readme", "Util")
        .addType(importStaticTypeSpec("Util"))
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import java.lang.System
        |import java.util.concurrent.TimeUnit
        |import kotlin.Long
        |
        |public class Util {
        |  public fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return TimeUnit.SECONDS.convert(minutes, TimeUnit.MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticOnce() {
    val source = FileSpec.builder("readme", "Util")
        .addType(importStaticTypeSpec("Util"))
        .addImport(TimeUnit.SECONDS).build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import java.lang.System
        |import java.util.concurrent.TimeUnit
        |import java.util.concurrent.TimeUnit.SECONDS
        |import kotlin.Long
        |
        |public class Util {
        |  public fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return SECONDS.convert(minutes, TimeUnit.MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticTwice() {
    val source = FileSpec.builder("readme", "Util")
        .addType(importStaticTypeSpec("Util"))
        .addImport(TimeUnit.SECONDS)
        .addImport(TimeUnit.MINUTES)
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import java.lang.System
        |import java.util.concurrent.TimeUnit.MINUTES
        |import java.util.concurrent.TimeUnit.SECONDS
        |import kotlin.Long
        |
        |public class Util {
        |  public fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return SECONDS.convert(minutes, MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticWildcardsForbidden() {
    assertThrows<IllegalArgumentException> {
      FileSpec.builder("readme", "Util")
          .addType(importStaticTypeSpec("Util"))
          .addImport(TimeUnit::class, "*")
    }.hasMessageThat().isEqualTo("Wildcard imports are not allowed")
  }

  private fun importStaticTypeSpec(name: String): TypeSpec {
    val funSpec = FunSpec.builder("minutesToSeconds")
        .addModifiers(KModifier.PUBLIC)
        .returns(Long::class)
        .addParameter("minutes", Long::class)
        .addStatement("%T.gc()", System::class)
        .addStatement("return %1T.SECONDS.convert(minutes, %1T.MINUTES)", TimeUnit::class)
        .build()
    return TypeSpec.classBuilder(name).addFunction(funSpec).build()
  }

  @Test fun noImports() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco").build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |public class Taco
        |""".trimMargin())
  }

  @Test fun singleImport() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("madeFreshDate", Date::class)
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.Date
        |
        |public class Taco {
        |  public val madeFreshDate: Date
        |}
        |""".trimMargin())
  }

  @Test fun singleImportEscapeKeywords() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("madeFreshDate", ClassName("com.squareup.is.fun.in", "Date"))
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.`is`.`fun`.`in`.Date
        |
        |public class Taco {
        |  public val madeFreshDate: Date
        |}
        |""".trimMargin())
  }

  @Test fun escapeSpacesInPackageName() {
    val file = FileSpec.builder("com.squareup.taco factory", "TacoFactory")
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.`taco factory`
      |
      |""".trimMargin())
  }

  @Test fun conflictingImports() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("madeFreshDate", Date::class)
            .addProperty("madeFreshDatabaseDate", ClassName("java.sql", "Date"))
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.Date
        |
        |public class Taco {
        |  public val madeFreshDate: Date
        |
        |  public val madeFreshDatabaseDate: java.sql.Date
        |}
        |""".trimMargin())
  }

  @Test fun conflictingImportsEscapeKeywords() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("madeFreshDate1", ClassName("com.squareup.is.fun.in", "Date"))
            .addProperty("madeFreshDate2", ClassName("com.squareup.do.val.var", "Date"))
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.`is`.`fun`.`in`.Date
        |
        |public class Taco {
        |  public val madeFreshDate1: Date
        |
        |  public val madeFreshDate2: com.squareup.`do`.`val`.`var`.Date
        |}
        |""".trimMargin())
  }

  @Test fun escapeSpacesInImports() {
    val tacoFactory = ClassName("com.squareup.taco factory", "TacoFactory")
    val file = FileSpec.builder("com.example", "TacoFactoryDemo")
        .addFunction(FunSpec.builder("main")
            .addStatement("println(%T.produceTacos())", tacoFactory)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.`taco factory`.TacoFactory
      |import kotlin.Unit
      |
      |public fun main(): Unit {
      |  println(TacoFactory.produceTacos())
      |}
      |""".trimMargin())
  }

  @Test fun escapeSpacesInAliasedImports() {
    val tacoFactory = ClassName("com.squareup.taco factory", "TacoFactory")
    val file = FileSpec.builder("com.example", "TacoFactoryDemo")
        .addAliasedImport(tacoFactory, "La Taqueria")
        .addFunction(FunSpec.builder("main")
            .addStatement("println(%T.produceTacos())", tacoFactory)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.Unit
      |import com.squareup.`taco factory`.TacoFactory as `La Taqueria`
      |
      |public fun main(): Unit {
      |  println(`La Taqueria`.produceTacos())
      |}
      |""".trimMargin())
  }

  @Test fun aliasedImports() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addAliasedImport(java.lang.String::class.java, "JString")
        .addAliasedImport(String::class, "KString")
        .addProperty(PropertySpec.builder("a", java.lang.String::class.java)
            .initializer("%T(%S)", java.lang.String::class.java, "a")
            .build())
        .addProperty(PropertySpec.builder("b", String::class)
            .initializer("%S", "b")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import java.lang.String as JString
      |import kotlin.String as KString
      |
      |public val a: JString = JString("a")
      |
      |public val b: KString = "b"
      |""".trimMargin())
  }

  @Test fun enumAliasedImport() {
    val minsAlias = "MINS"
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addAliasedImport(TimeUnit::class.asClassName(), "MINUTES", minsAlias)
        .addFunction(FunSpec.builder("sleepForFiveMins")
            .addStatement("%T.MINUTES.sleep(5)", TimeUnit::class)
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Unit
      |import java.util.concurrent.TimeUnit.MINUTES as MINS
      |
      |public fun sleepForFiveMins(): Unit {
      |  MINS.sleep(5)
      |}
      |""".trimMargin())
  }

  @Test fun conflictingParentName() {
    val source = FileSpec.builder("com.squareup.tacos", "A")
        .addType(TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("Twin").build())
                .addType(TypeSpec.classBuilder("C")
                    .addProperty("d", ClassName("com.squareup.tacos", "A", "Twin", "D"))
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |public class A {
        |  public class B {
        |    public class Twin
        |
        |    public class C {
        |      public val d: A.Twin.D
        |    }
        |  }
        |
        |  public class Twin {
        |    public class D
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun conflictingChildName() {
    val source = FileSpec.builder("com.squareup.tacos", "A")
        .addType(TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addProperty("d", ClassName("com.squareup.tacos", "A", "Twin", "D"))
                    .addType(TypeSpec.classBuilder("Twin").build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |public class A {
        |  public class B {
        |    public class C {
        |      public val d: A.Twin.D
        |
        |      public class Twin
        |    }
        |  }
        |
        |  public class Twin {
        |    public class D
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun conflictingNameOutOfScope() {
    val source = FileSpec.builder("com.squareup.tacos", "A")
        .addType(TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addProperty("d", ClassName("com.squareup.tacos", "A", "Twin", "D"))
                    .addType(TypeSpec.classBuilder("Nested")
                        .addType(TypeSpec.classBuilder("Twin").build())
                        .build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |public class A {
        |  public class B {
        |    public class C {
        |      public val d: Twin.D
        |
        |      public class Nested {
        |        public class Twin
        |      }
        |    }
        |  }
        |
        |  public class Twin {
        |    public class D
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun nestedClassAndSuperclassShareName() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .superclass(ClassName("com.squareup.wire", "Message"))
            .addType(TypeSpec.classBuilder("Builder")
                .superclass(ClassName("com.squareup.wire", "Message", "Builder"))
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.wire.Message
        |
        |public class Taco : Message() {
        |  public class Builder : Message.Builder()
        |}
        |""".trimMargin())
  }

  /** https://github.com/square/javapoet/issues/366  */
  @Test fun annotationIsNestedClass() {
    val source = FileSpec.builder("com.squareup.tacos", "TestComponent")
        .addType(TypeSpec.classBuilder("TestComponent")
            .addAnnotation(ClassName("dagger", "Component"))
            .addType(TypeSpec.classBuilder("Builder")
                .addAnnotation(ClassName("dagger", "Component", "Builder"))
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import dagger.Component
        |
        |@Component
        |public class TestComponent {
        |  @Component.Builder
        |  public class Builder
        |}
        |""".trimMargin())
  }

  @Test fun defaultPackage() {
    val source = FileSpec.builder("", "HelloWorld")
        .addType(TypeSpec.classBuilder("HelloWorld")
            .addFunction(FunSpec.builder("main")
                .addModifiers(KModifier.PUBLIC)
                .addParameter("args", ARRAY.parameterizedBy(String::class.asClassName()))
                .addCode("%T.out.println(%S);\n", System::class, "Hello World!")
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |import java.lang.System
        |import kotlin.Array
        |import kotlin.String
        |import kotlin.Unit
        |
        |public class HelloWorld {
        |  public fun main(args: Array<String>): Unit {
        |    System.out.println("Hello World!");
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun defaultPackageTypesAreImported() {
    val source = FileSpec.builder("hello", "World")
        .addType(TypeSpec.classBuilder("World")
            .addSuperinterface(ClassName("", "Test"))
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package hello
        |
        |import Test
        |
        |public class World : Test
        |""".trimMargin())
  }

  @Test fun topOfFileComment() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco").build())
        .addComment("Generated %L by KotlinPoet. DO NOT EDIT!", "2015-01-13")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |// Generated 2015-01-13 by KotlinPoet. DO NOT EDIT!
        |package com.squareup.tacos
        |
        |public class Taco
        |""".trimMargin())
  }

  @Test fun emptyLinesInTopOfFileComment() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco").build())
        .addComment("\nGENERATED FILE:\n\nDO NOT EDIT!\n")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |//
        |// GENERATED FILE:
        |//
        |// DO NOT EDIT!
        |//
        |package com.squareup.tacos
        |
        |public class Taco
        |""".trimMargin())
  }

  @Test fun packageClassConflictsWithNestedClass() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("a", ClassName("com.squareup.tacos", "A"))
            .addType(TypeSpec.classBuilder("A").build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |public class Taco {
        |  public val a: com.squareup.tacos.A
        |
        |  public class A
        |}
        |""".trimMargin())
  }

  @Test fun multipleTypesInOneFile() {
    val source = FileSpec.builder("com.squareup.tacos", "AB")
        .addType(TypeSpec.classBuilder("A").build())
        .addType(TypeSpec.classBuilder("B").build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |public class A
        |
        |public class B
        |""".trimMargin())
  }

  @Test fun simpleTypeAliases() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addTypeAlias(TypeAliasSpec.builder("Int8", Byte::class).build())
        .addTypeAlias(TypeAliasSpec.builder("FileTable",
            Map::class.parameterizedBy(String::class, Int::class)).build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Byte
        |import kotlin.Int
        |import kotlin.String
        |import kotlin.collections.Map
        |
        |public typealias Int8 = Byte
        |
        |public typealias FileTable = Map<String, Int>
        |""".trimMargin())
  }

  @Test fun fileAnnotations() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addAnnotation(AnnotationSpec.builder(JvmName::class)
            .useSiteTarget(FILE)
            .addMember("%S", "TacoUtils")
            .build())
        .addAnnotation(JvmMultifileClass::class)
        .build()
    assertThat(source.toString()).isEqualTo("""
        |@file:JvmName("TacoUtils")
        |@file:JvmMultifileClass
        |
        |package com.squareup.tacos
        |
        |import kotlin.jvm.JvmMultifileClass
        |import kotlin.jvm.JvmName
        |
        |""".trimMargin())
  }

  @Test fun fileAnnotationMustHaveCorrectUseSiteTarget() {
    val builder = FileSpec.builder("com.squareup.tacos", "Taco")
    val annotation = AnnotationSpec.builder(JvmName::class)
        .useSiteTarget(SET)
        .addMember("value", "%S", "TacoUtils")
        .build()
    assertThrows<IllegalStateException> {
      builder.addAnnotation(annotation)
    }.hasMessageThat().isEqualTo("Use-site target SET not supported for file annotations.")
  }

  @Test fun escapeKeywordInPackageName() {
    val source = FileSpec.builder("com.squareup.is.fun.in", "California")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.`is`.`fun`.`in`
        |
        |""".trimMargin())
  }

  @Test fun generalBuilderEqualityTest() {
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addAnnotation(JvmMultifileClass::class)
        .addComment("Generated 2015-01-13 by KotlinPoet. DO NOT EDIT!")
        .addImport("com.squareup.tacos.internal", "INGREDIENTS")
        .addTypeAlias(TypeAliasSpec.builder("Int8", Byte::class).build())
        .indent("  ")
        .addFunction(FunSpec.builder("defaultIngredients")
            .addCode("println(INGREDIENTS)\n")
            .build())
        .build()

    assertThat(source.toBuilder().build()).isEqualTo(source)
  }

  @Test fun modifyAnnotations() {
    val builder = FileSpec.builder("com.taco", "Taco")
        .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
            .useSiteTarget(FILE)
            .addMember("name = %S", "JvmTaco")
            .build())

    val javaWord = AnnotationSpec.builder(JvmName::class.asClassName())
        .useSiteTarget(FILE)
        .addMember("name = %S", "JavaTaco")
        .build()
    builder.annotations.clear()
    builder.annotations.add(javaWord)

    assertThat(builder.build().annotations).containsExactly(javaWord)
  }

  @Test fun modifyImports() {
    val builder = FileSpec.builder("com.taco", "Taco")
        .addImport("com.foo", "Foo")

    val currentImports = builder.imports
    builder.clearImports()
    builder.addImport("com.foo", "Foo2")
        .apply {
          for (current in currentImports) {
            addImport(current)
          }
        }
        .indent("")

    assertThat(builder.build().toString()).isEqualTo("""
      package com.taco

      import com.foo.Foo
      import com.foo.Foo2


    """.trimIndent())
  }

  @Test fun modifyMembers() {
    val builder = FileSpec.builder("com.taco", "Taco")
        .addFunction(FunSpec.builder("aFunction").build())
        .addProperty(PropertySpec.builder("aProperty", INT).initializer("1").build())
        .addTypeAlias(TypeAliasSpec.builder("ATypeAlias", INT).build())
        .addType(TypeSpec.classBuilder("AClass").build())

    builder.members.removeAll { it !is TypeSpec }

    check(builder.build().members.all { it is TypeSpec })
  }

  @Test fun clearComment() {
    val builder = FileSpec.builder("com.taco", "Taco")
        .addFunction(FunSpec.builder("aFunction").build())
        .addComment("Hello!")

    builder.clearComment()
        .addComment("Goodbye!")

    assertThat(builder.build().comment.toString()).isEqualTo("Goodbye!")
  }

  // https://github.com/square/kotlinpoet/issues/480
  @Test fun defaultPackageMemberImport() {
    val bigInteger = ClassName.bestGuess("bigInt.BigInteger")
    val spec = FileSpec.builder("testsrc", "Test")
        .addImport("", "bigInt")
        .addFunction(FunSpec.builder("add5ToInput")
            .addParameter("input", Int::class)
            .returns(bigInteger)
            .addCode("""
               |val inputBigInt = bigInt(input)
               |return inputBigInt.add(5)
               |""".trimMargin())
            .build())
        .build()
    assertThat(spec.toString()).isEqualTo("""
      |package testsrc
      |
      |import bigInt
      |import bigInt.BigInteger
      |import kotlin.Int
      |
      |public fun add5ToInput(input: Int): BigInteger {
      |  val inputBigInt = bigInt(input)
      |  return inputBigInt.add(5)
      |}
      |""".trimMargin())
  }

  @Test fun longFilePackageName() {
    val spec = FileSpec.builder("com.squareup.taco.enchilada.quesadillas.tamales.burritos.super.burritos.trying.to.get.a.really.large.packagename", "Test")
        .addFunction(FunSpec.builder("foo")
            .build())
        .build()
    assertThat(spec.toString()).isEqualTo("""
      |package com.squareup.taco.enchilada.quesadillas.tamales.burritos.`super`.burritos.trying.to.get.a.really.large.packagename
      |
      |import kotlin.Unit
      |
      |public fun foo(): Unit {
      |}
      |""".trimMargin())
  }

  @Test fun importLongPackageName() {
    val spec = FileSpec.builder("testsrc", "Test")
        .addImport("a.really.veryveryveryveryveryveryvery.long.pkgname.that.will.definitely.cause.a.wrap.duetoitslength", "MyClass")
        .build()
    assertThat(spec.toString()).isEqualTo("""
      |package testsrc
      |
      |import a.really.veryveryveryveryveryveryvery.long.pkgname.that.will.definitely.cause.a.wrap.duetoitslength.MyClass
      |
      |""".trimMargin())
  }

  @Test fun longComment() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addComment("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua.")
        .build()
    assertThat(file.toString()).isEqualTo("""
      |// Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
      |package com.squareup.tacos
      |
      |""".trimMargin())
  }
}
