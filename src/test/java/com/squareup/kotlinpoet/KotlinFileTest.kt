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
import org.junit.Ignore
import org.junit.Test
import java.util.Collections
import java.util.Date
import java.util.concurrent.TimeUnit

class KotlinFileTest {
  @Test fun importStaticReadmeExample() {
    val hoverboard = ClassName.get("com.mattel", "Hoverboard")
    val namedBoards = ClassName.get("com.mattel", "Hoverboard", "Boards")
    val list = ClassName.get(List::class)
    val arrayList = ClassName.get("java.util", "ArrayList")
    val listOfHoverboards = ParameterizedTypeName.get(list, hoverboard)
    val beyond = FunSpec.builder("beyond")
        .returns(listOfHoverboards)
        .addStatement("%T result = new %T<>()", listOfHoverboards, arrayList)
        .addStatement("result.add(%T.createNimbus(2000))", hoverboard)
        .addStatement("result.add(%T.createNimbus(\"2001\"))", hoverboard)
        .addStatement("result.add(%T.createNimbus(%T.THUNDERBOLT))", hoverboard, namedBoards)
        .addStatement("%T.sort(result)", Collections::class)
        .addStatement("return result.isEmpty() ? %T.emptyList() : result", Collections::class)
        .build()
    val hello = TypeSpec.classBuilder("HelloWorld")
        .addFun(beyond)
        .build()
    val source = KotlinFile.builder("com.example.helloworld", "HelloWorld")
        .addType(hello)
        .addStaticImport(hoverboard, "createNimbus")
        .addStaticImport(namedBoards, "*")
        .addStaticImport(Collections::class, "*")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.example.helloworld
        |
        |import com.mattel.Hoverboard
        |import com.mattel.Hoverboard.Boards.*
        |import com.mattel.Hoverboard.createNimbus
        |import java.util.ArrayList
        |import java.util.Collections.*
        |import kotlin.collections.List
        |
        |class HelloWorld {
        |  fun beyond(): List<Hoverboard> {
        |    List<Hoverboard> result = new ArrayList<>()
        |    result.add(createNimbus(2000))
        |    result.add(createNimbus("2001"))
        |    result.add(createNimbus(THUNDERBOLT))
        |    sort(result)
        |    return result.isEmpty() ? emptyList() : result
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticForCrazyFormatsWorks() {
    val method = FunSpec.builder("method").build()
    KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addInitializerBlock(CodeBlock.builder()
                .addStatement("%T", Runtime::class)
                .addStatement("%T.a()", Runtime::class)
                .addStatement("%T.X", Runtime::class)
                .addStatement("%T%T", Runtime::class, Runtime::class)
                .addStatement("%T.%T", Runtime::class, Runtime::class)
                .addStatement("%1T%1T", Runtime::class)
                .addStatement("%1T%2L%1T", Runtime::class, "?")
                .addStatement("%1T%2L%2S%1T", Runtime::class, "?")
                .addStatement("%1T%2L%2S%1T%3N%1T", Runtime::class, "?", method)
                .addStatement("%T%L", Runtime::class, "?")
                .addStatement("%T%S", Runtime::class, "?")
                .addStatement("%T%N", Runtime::class, method)
                .build())
            .build())
        .addStaticImport(Runtime::class, "*")
        .build()
        .toString() // don't look at the generated code...
  }

  @Test fun importStaticMixed() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addInitializerBlock(CodeBlock.builder()
                .addStatement("assert %1T.valueOf(\"BLOCKED\") == %1T.BLOCKED", Thread.State::class)
                .addStatement("%T.gc()", System::class)
                .addStatement("%1T.out.println(%1T.nanoTime())", System::class)
                .build())
            .addFun(FunSpec.constructorBuilder()
                .addParameter("states", ParameterizedTypeName.get(ARRAY, ClassName.get(Thread.State::class)))
                .varargs(true)
                .build())
            .build())
        .addStaticImport(Thread.State.BLOCKED)
        .addStaticImport(System::class, "*")
        .addStaticImport(Thread.State::class, "valueOf")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.System.*
        |import java.lang.Thread
        |import java.lang.Thread.State.BLOCKED
        |import java.lang.Thread.State.valueOf
        |
        |class Taco {
        |  init {
        |    assert valueOf("BLOCKED") == BLOCKED
        |    gc()
        |    out.println(nanoTime())
        |  }
        |
        |  constructor(vararg states: Thread.State) {
        |  }
        |}
        |""".trimMargin())
  }

  @Ignore("addStaticImport doesn't support members with %L")
  @Test
  fun importStaticDynamic() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addFun(FunSpec.builder("main")
                .addStatement("%T.%L.println(%S)", System::class, "out", "hello")
                .build())
            .build())
        .addStaticImport(System::class, "out")
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
    val source = KotlinFile.builder("readme", "Util")
        .addType(importStaticTypeSpec("Util"))
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import java.lang.System
        |import java.util.concurrent.TimeUnit
        |import kotlin.Long
        |
        |class Util {
        |  fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return TimeUnit.SECONDS.convert(minutes, TimeUnit.MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticOnce() {
    val source = KotlinFile.builder("readme", "Util")
        .addType(importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS).build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import java.lang.System
        |import java.util.concurrent.TimeUnit
        |import java.util.concurrent.TimeUnit.SECONDS
        |import kotlin.Long
        |
        |class Util {
        |  fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return SECONDS.convert(minutes, TimeUnit.MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticTwice() {
    val source = KotlinFile.builder("readme", "Util")
        .addType(importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS)
        .addStaticImport(TimeUnit.MINUTES)
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import java.lang.System
        |import java.util.concurrent.TimeUnit.MINUTES
        |import java.util.concurrent.TimeUnit.SECONDS
        |import kotlin.Long
        |
        |class Util {
        |  fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return SECONDS.convert(minutes, MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticUsingWildcards() {
    val source = KotlinFile.builder("readme", "Util")
        .addType(importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit::class, "*")
        .addStaticImport(System::class, "*")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import java.lang.System.*
        |import java.util.concurrent.TimeUnit.*
        |import kotlin.Long
        |
        |class Util {
        |  fun minutesToSeconds(minutes: Long): Long {
        |    gc()
        |    return SECONDS.convert(minutes, MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  private fun importStaticTypeSpec(name: String): TypeSpec {
    val funSpec = FunSpec.builder("minutesToSeconds")
        .addModifiers(KModifier.PUBLIC)
        .returns(Long::class)
        .addParameter("minutes", Long::class)
        .addStatement("%T.gc()", System::class)
        .addStatement("return %1T.SECONDS.convert(minutes, %1T.MINUTES)", TimeUnit::class)
        .build()
    return TypeSpec.classBuilder(name).addFun(funSpec).build()

  }

  @Test fun noImports() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco").build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |}
        |""".trimMargin())
  }

  @Test fun singleImport() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("madeFreshDate", Date::class)
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.Date
        |
        |class Taco {
        |  val madeFreshDate: Date
        |}
        |""".trimMargin())
  }

  @Test fun conflictingImports() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("madeFreshDate", Date::class)
            .addProperty("madeFreshDatabaseDate", ClassName.get("java.sql", "Date"))
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.Date
        |
        |class Taco {
        |  val madeFreshDate: Date
        |
        |  val madeFreshDatabaseDate: java.sql.Date
        |}
        |""".trimMargin())
  }

  @Test fun skipJavaLangImportsWithConflictingClassLast() {
    // Whatever is used first wins! In this case the Float in java.lang is imported.
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("litres", ClassName.get("java.lang", "Float"))
            .addProperty("beverage", ClassName.get("com.squareup.soda", "Float"))
            .build())
        .skipJavaLangImports(true)
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  val litres: Float
        |
        |  val beverage: com.squareup.soda.Float
        |}
        |""".trimMargin())
  }

  @Test fun skipJavaLangImportsWithConflictingClassFirst() {
    // Whatever is used first wins! In this case the Float in com.squareup.soda is imported.
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("beverage", ClassName.get("com.squareup.soda", "Float"))
            .addProperty("litres", ClassName.get("java.lang", "Float"))
            .build())
        .skipJavaLangImports(true)
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.soda.Float
        |
        |class Taco {
        |  val beverage: Float
        |
        |  val litres: java.lang.Float
        |}
        |""".trimMargin())
  }

  @Test fun conflictingParentName() {
    val source = KotlinFile.builder("com.squareup.tacos", "A")
        .addType(TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("Twin").build())
                .addType(TypeSpec.classBuilder("C")
                    .addProperty("d", ClassName.get("com.squareup.tacos", "A", "Twin", "D"))
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
        |class A {
        |  class B {
        |    class Twin {
        |    }
        |
        |    class C {
        |      val d: A.Twin.D
        |    }
        |  }
        |
        |  class Twin {
        |    class D {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun conflictingChildName() {
    val source = KotlinFile.builder("com.squareup.tacos", "A")
        .addType(TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addProperty("d", ClassName.get("com.squareup.tacos", "A", "Twin", "D"))
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
        |class A {
        |  class B {
        |    class C {
        |      val d: A.Twin.D
        |
        |      class Twin {
        |      }
        |    }
        |  }
        |
        |  class Twin {
        |    class D {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun conflictingNameOutOfScope() {
    val source = KotlinFile.builder("com.squareup.tacos", "A")
        .addType(TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addProperty("d", ClassName.get("com.squareup.tacos", "A", "Twin", "D"))
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
        |class A {
        |  class B {
        |    class C {
        |      val d: Twin.D
        |
        |      class Nested {
        |        class Twin {
        |        }
        |      }
        |    }
        |  }
        |
        |  class Twin {
        |    class D {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun nestedClassAndSuperclassShareName() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.squareup.wire", "Message"))
            .addType(TypeSpec.classBuilder("Builder")
                .superclass(ClassName.get("com.squareup.wire", "Message", "Builder"))
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.wire.Message
        |
        |class Taco : Message {
        |  class Builder : Message.Builder {
        |  }
        |}
        |""".trimMargin())
  }

  /** https://github.com/square/javapoet/issues/366  */
  @Test fun annotationIsNestedClass() {
    val source = KotlinFile.builder("com.squareup.tacos", "TestComponent")
        .addType(TypeSpec.classBuilder("TestComponent")
            .addAnnotation(ClassName.get("dagger", "Component"))
            .addType(TypeSpec.classBuilder("Builder")
                .addAnnotation(ClassName.get("dagger", "Component", "Builder"))
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import dagger.Component
        |
        |@Component
        |class TestComponent {
        |  @Component.Builder
        |  class Builder {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun defaultPackage() {
    val source = KotlinFile.builder("", "HelloWorld")
        .addType(TypeSpec.classBuilder("HelloWorld")
            .addFun(FunSpec.builder("main")
                .addModifiers(KModifier.PUBLIC)
                .addParameter("args", ParameterizedTypeName.get(ARRAY, ClassName.get(String::class)))
                .addCode("%T.out.println(%S);\n", System::class, "Hello World!")
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |import java.lang.System
        |import kotlin.Array
        |import kotlin.String
        |
        |class HelloWorld {
        |  fun main(args: Array<String>) {
        |    System.out.println("Hello World!");
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun defaultPackageTypesAreNotImported() {
    val source = KotlinFile.builder("hello", "World")
        .addType(TypeSpec.classBuilder("World")
            .addSuperinterface(ClassName.get("", "Test"))
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package hello
        |
        |class World : Test {
        |}
        |""".trimMargin())
  }

  @Test fun topOfFileComment() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco").build())
        .addFileComment("Generated %L by KotlinPoet. DO NOT EDIT!", "2015-01-13")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |// Generated 2015-01-13 by KotlinPoet. DO NOT EDIT!
        |package com.squareup.tacos
        |
        |class Taco {
        |}
        |""".trimMargin())
  }

  @Test fun emptyLinesInTopOfFileComment() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco").build())
        .addFileComment("\nGENERATED FILE:\n\nDO NOT EDIT!\n")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |//
        |// GENERATED FILE:
        |//
        |// DO NOT EDIT!
        |//
        |package com.squareup.tacos
        |
        |class Taco {
        |}
        |""".trimMargin())
  }

  @Test fun packageClassConflictsWithNestedClass() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty("a", ClassName.get("com.squareup.tacos", "A"))
            .addType(TypeSpec.classBuilder("A").build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  val a: com.squareup.tacos.A
        |
        |  class A {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun multipleTypesInOneFile() {
    val source = KotlinFile.builder("com.squareup.tacos", "AB")
        .addType(TypeSpec.classBuilder("A").build())
        .addType(TypeSpec.classBuilder("B").build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class A {
        |}
        |
        |class B {
        |}
        |""".trimMargin())
  }

  @Test fun simpleTypeAliases() {
    val source = KotlinFile.builder("com.squareup.tacos", "Taco")
        .addTypeAlias(TypeAliasSpec.builder("Int8", Byte::class).build())
        .addTypeAlias(TypeAliasSpec.builder("FileTable",
            ParameterizedTypeName.get(Map::class, String::class, Int::class)).build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Byte
        |import kotlin.Int
        |import kotlin.String
        |import kotlin.collections.Map
        |
        |typealias Int8 = Byte
        |
        |typealias FileTable = Map<String, Int>
        |""".trimMargin())
  }
}
