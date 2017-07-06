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

import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.VARARG
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.io.Serializable
import java.math.BigDecimal
import java.util.AbstractSet
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.EventListener
import java.util.Locale
import java.util.Random
import java.util.concurrent.Callable
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

class TypeSpecTest {
  private val tacosPackage = "com.squareup.tacos"

  @Rule @JvmField val compilation = CompilationRule()

  private fun getElement(`class`: Class<*>): TypeElement {
    return compilation.elements.getTypeElement(`class`.canonicalName)
  }

  private fun getElement(`class`: KClass<*>): TypeElement {
    return getElement(`class`.java)
  }

  @Test fun basic() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("toString")
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL, KModifier.OVERRIDE)
            .returns(String::class)
            .addCode("return %S;\n", "taco")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  final override fun toString(): String {
        |    return "taco";
        |  }
        |}
        |""".trimMargin())
    assertEquals(2095734687, taco.hashCode().toLong()) // Update expected number if source changes.
  }

  @Test fun interestingTypes() {
    val listOfAny = ParameterizedTypeName.get(
        List::class.asClassName(), WildcardTypeName.subtypeOf(ANY))
    val listOfExtends = ParameterizedTypeName.get(
        List::class.asClassName(), WildcardTypeName.subtypeOf(Serializable::class))
    val listOfSuper = ParameterizedTypeName.get(List::class.asClassName(),
        WildcardTypeName.supertypeOf(String::class))
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty("star", listOfAny)
        .addProperty("outSerializable", listOfExtends)
        .addProperty("inString", listOfSuper)
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import kotlin.String
        |import kotlin.collections.List
        |
        |class Taco {
        |  val star: List<*>
        |
        |  val outSerializable: List<out Serializable>
        |
        |  val inString: List<in String>
        |}
        |""".trimMargin())
  }

  @Test fun anonymousInnerClass() {
    val foo = ClassName(tacosPackage, "Foo")
    val bar = ClassName(tacosPackage, "Bar")
    val thingThang = ClassName(tacosPackage, "Thing", "Thang")
    val thingThangOfFooBar = ParameterizedTypeName.get(thingThang, foo, bar)
    val thung = ClassName(tacosPackage, "Thung")
    val simpleThung = ClassName(tacosPackage, "SimpleThung")
    val thungOfSuperBar = ParameterizedTypeName.get(thung, WildcardTypeName.supertypeOf(bar))
    val thungOfSuperFoo = ParameterizedTypeName.get(thung, WildcardTypeName.supertypeOf(foo))
    val simpleThungOfBar = ParameterizedTypeName.get(simpleThung, bar)

    val thungParameter = ParameterSpec.builder("thung", thungOfSuperFoo)
        .addModifiers(KModifier.FINAL)
        .build()
    val aSimpleThung = TypeSpec.anonymousClassBuilder("%N", thungParameter)
        .superclass(simpleThungOfBar)
        .addFun(FunSpec.builder("doSomething")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addParameter("bar", bar)
            .addCode("/* code snippets */\n")
            .build())
        .build()
    val aThingThang = TypeSpec.anonymousClassBuilder("")
        .superclass(thingThangOfFooBar)
        .addFun(FunSpec.builder("call")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(thungOfSuperBar)
            .addParameter(thungParameter)
            .addCode("return %L;\n", aSimpleThung)
            .build())
        .build()
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("NAME", thingThangOfFooBar)
            .initializer("%L", aThingThang)
            .build())
        .build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  val NAME: Thing.Thang<Foo, Bar> = object : Thing.Thang<Foo, Bar>() {
        |    override fun call(final thung: Thung<in Foo>): Thung<in Bar> {
        |      return object : SimpleThung<Bar>(thung) {
        |        override fun doSomething(bar: Bar) {
        |          /* code snippets */
        |        }
        |      };
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun annotatedParameters() {
    val service = TypeSpec.classBuilder("Foo")
        .addFun(FunSpec.constructorBuilder()
            .addModifiers(KModifier.PUBLIC)
            .addParameter("id", Long::class)
            .addParameter(ParameterSpec.builder("one", String::class)
                .addAnnotation(ClassName(tacosPackage, "Ping"))
                .build())
            .addParameter(ParameterSpec.builder("two", String::class)
                .addAnnotation(ClassName(tacosPackage, "Ping"))
                .build())
            .addParameter(ParameterSpec.builder("three", String::class)
                .addAnnotation(AnnotationSpec.builder(ClassName(tacosPackage, "Pong"))
                    .addMember("value", "%S", "pong")
                    .build())
                .build())
            .addParameter(ParameterSpec.builder("four", String::class)
                .addAnnotation(ClassName(tacosPackage, "Ping"))
                .build())
            .addCode("/* code snippets */\n")
            .build())
        .build()

    assertThat(toString(service)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Long
        |import kotlin.String
        |
        |class Foo {
        |  constructor(id: Long, @Ping one: String, @Ping two: String, @Pong("pong") three: String,
        |      @Ping four: String) {
        |    /* code snippets */
        |  }
        |}
        |""".trimMargin())
  }

  /**
   * We had a bug where annotations were preventing us from doing the right thing when resolving
   * imports. https://github.com/square/javapoet/issues/422
   */
  @Test fun annotationsAndJavaLangTypes() {
    val freeRange = ClassName("javax.annotation", "FreeRange")
    val taco = TypeSpec.classBuilder("EthicalTaco")
        .addProperty("meat", String::class.asClassName()
            .annotated(AnnotationSpec.builder(freeRange).build()))
        .build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import javax.annotation.FreeRange
        |import kotlin.String
        |
        |class EthicalTaco {
        |  val meat: @FreeRange String
        |}
        |""".trimMargin())
  }

  @Test fun retrofitStyleInterface() {
    val observable = ClassName(tacosPackage, "Observable")
    val fooBar = ClassName(tacosPackage, "FooBar")
    val thing = ClassName(tacosPackage, "Thing")
    val things = ClassName(tacosPackage, "Things")
    val map = Map::class.asClassName()
    val string = String::class.asClassName()
    val headers = ClassName(tacosPackage, "Headers")
    val post = ClassName(tacosPackage, "POST")
    val body = ClassName(tacosPackage, "Body")
    val queryMap = ClassName(tacosPackage, "QueryMap")
    val header = ClassName(tacosPackage, "Header")
    val service = TypeSpec.interfaceBuilder("Service")
        .addFun(FunSpec.builder("fooBar")
            .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
            .addAnnotation(AnnotationSpec.builder(headers)
                .addMember("value", "%S", "Accept: application/json")
                .addMember("value", "%S", "User-Agent: foobar")
                .build())
            .addAnnotation(AnnotationSpec.builder(post)
                .addMember("value", "%S", "/foo/bar")
                .build())
            .returns(ParameterizedTypeName.get(observable, fooBar))
            .addParameter(ParameterSpec.builder("things", ParameterizedTypeName.get(things, thing))
                .addAnnotation(body)
                .build())
            .addParameter(ParameterSpec.builder("query", ParameterizedTypeName.get(map, string, string))
                .addAnnotation(AnnotationSpec.builder(queryMap)
                    .addMember("encodeValues", "false")
                    .build())
                .build())
            .addParameter(ParameterSpec.builder("authorization", string)
                .addAnnotation(AnnotationSpec.builder(header)
                    .addMember("value", "%S", "Authorization")
                    .build())
                .build())
            .build())
        .build()

    assertThat(toString(service)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |import kotlin.collections.Map
        |
        |interface Service {
        |  @Headers([
        |      "Accept: application/json",
        |      "User-Agent: foobar"
        |  ])
        |  @POST("/foo/bar")
        |  fun fooBar(@Body things: Things<Thing>,
        |      @QueryMap(encodeValues = false) query: Map<String, String>,
        |      @Header("Authorization") authorization: String): Observable<FooBar>
        |}
        |""".trimMargin())
  }

  @Test fun annotatedProperty() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("thing", String::class, KModifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(ClassName(tacosPackage, "JsonAdapter"))
                .addMember("value", "%T::class", ClassName(tacosPackage, "Foo"))
                .build())
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  @JsonAdapter(Foo::class)
        |  private val thing: String
        |}
        |""".trimMargin())
  }

  @Test fun annotatedPropertyUseSiteTarget() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("thing", String::class, KModifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(ClassName(tacosPackage, "JsonAdapter"))
                .addMember("value", "%T::class", ClassName(tacosPackage, "Foo"))
                .useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
                .build())
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  @field:JsonAdapter(Foo::class)
        |  private val thing: String
        |}
        |""".trimMargin())
  }

  @Test fun annotatedClass() {
    val someType = ClassName(tacosPackage, "SomeType")
    val taco = TypeSpec.classBuilder("Foo")
        .addAnnotation(AnnotationSpec.builder(ClassName(tacosPackage, "Something"))
            .addMember("hi", "%T.%N", someType, "PROPERTY")
            .addMember("hey", "%L", 12)
            .addMember("hello", "%S", "goodbye")
            .build())
        .addModifiers(KModifier.PUBLIC)
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |@Something(
        |    hi = SomeType.PROPERTY,
        |    hey = 12,
        |    hello = "goodbye"
        |)
        |class Foo
        |""".trimMargin())
  }

  @Test fun enumWithSubclassing() {
    val roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(KModifier.PUBLIC)
        .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder("")
            .addKdoc("Avalanche!\n")
            .build())
        .addEnumConstant("PAPER", TypeSpec.anonymousClassBuilder("%S", "flat")
            .addFun(FunSpec.builder("toString")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE, KModifier.OVERRIDE)
                .returns(String::class)
                .addCode("return %S\n", "paper airplane!")
                .build())
            .build())
        .addEnumConstant("SCISSORS", TypeSpec.anonymousClassBuilder("%S", "peace sign")
            .build())
        .addProperty(PropertySpec.builder("handPosition", String::class, KModifier.PRIVATE)
            .initializer("handPosition")
            .build())
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("handPosition", String::class)
            .build())
        .addFun(FunSpec.constructorBuilder()
            .addCode("this(%S)\n", "fist")
            .build())
        .build()
    assertThat(toString(roshambo)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |enum class Roshambo(private val handPosition: String) {
        |  /**
        |   * Avalanche!
        |   */
        |  ROCK,
        |
        |  PAPER("flat") {
        |    override fun toString(): String {
        |      return "paper airplane!"
        |    }
        |  },
        |
        |  SCISSORS("peace sign");
        |
        |  constructor() {
        |    this("fist")
        |  }
        |}
        |""".trimMargin())
  }

  /** https://github.com/square/javapoet/issues/193  */
  @Test fun enumsMayDefineAbstractFunctions() {
    val roshambo = TypeSpec.enumBuilder("Tortilla")
        .addModifiers(KModifier.PUBLIC)
        .addEnumConstant("CORN", TypeSpec.anonymousClassBuilder("")
            .addFun(FunSpec.builder("fold")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .build())
            .build())
        .addFun(FunSpec.builder("fold")
            .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
            .build())
        .build()
    assertThat(toString(roshambo)).isEqualTo("""
        |package com.squareup.tacos
        |
        |enum class Tortilla {
        |  CORN {
        |    override fun fold() {
        |    }
        |  };
        |
        |  abstract fun fold()
        |}
        |""".trimMargin())
  }

  @Test fun enumConstantsRequired() {
    try {
      TypeSpec.enumBuilder("Roshambo")
          .build()
      fail()
    } catch (expected: IllegalArgumentException) {
    }

  }

  @Test fun onlyEnumsMayHaveEnumConstants() {
    try {
      TypeSpec.classBuilder("Roshambo")
          .addEnumConstant("ROCK")
          .build()
      fail()
    } catch (expected: IllegalStateException) {
    }

  }

  @Test fun enumWithMembersButNoConstructorCall() {
    val roshambo = TypeSpec.enumBuilder("Roshambo")
        .addEnumConstant("SPOCK", TypeSpec.anonymousClassBuilder("")
            .addFun(FunSpec.builder("toString")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .returns(String::class)
                .addCode("return %S;\n", "west side")
                .build())
            .build())
        .build()
    assertThat(toString(roshambo)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |enum class Roshambo {
        |  SPOCK {
        |    override fun toString(): String {
        |      return "west side";
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  /** https://github.com/square/javapoet/issues/253  */
  @Test fun enumWithAnnotatedValues() {
    val roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(KModifier.PUBLIC)
        .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder("")
            .addAnnotation(java.lang.Deprecated::class)
            .build())
        .addEnumConstant("PAPER")
        .addEnumConstant("SCISSORS")
        .build()
    assertThat(toString(roshambo)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.Deprecated
        |
        |enum class Roshambo {
        |  @Deprecated
        |  ROCK,
        |
        |  PAPER,
        |
        |  SCISSORS
        |}
        |""".trimMargin())
  }

  @Test fun funThrows() {
    val taco = TypeSpec.classBuilder("Taco")
        .addModifiers(KModifier.ABSTRACT)
        .addFun(FunSpec.builder("throwOne")
            .addAnnotation(AnnotationSpec.builder(Throws::class)
                .addMember("value", "%T::class", IOException::class.asClassName())
                .build())
            .build())
        .addFun(FunSpec.builder("throwTwo")
            .addAnnotation(AnnotationSpec.builder(Throws::class)
                .addMember("value", "%T::class, %T::class",
                    IOException::class.asClassName(), ClassName(tacosPackage, "SourCreamException"))
                .build())
            .build())
        .addFun(FunSpec.builder("abstractThrow")
            .addModifiers(KModifier.ABSTRACT)
            .addAnnotation(AnnotationSpec.builder(Throws::class)
                .addMember("value", "%T::class", IOException::class.asClassName())
                .build())
            .build())
        .addFun(FunSpec.builder("nativeThrow")
            .addModifiers(KModifier.EXTERNAL)
            .addAnnotation(AnnotationSpec.builder(Throws::class)
                .addMember("value", "%T::class", IOException::class.asClassName())
                .build())
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.IOException
        |import kotlin.jvm.Throws
        |
        |abstract class Taco {
        |  @Throws(IOException::class)
        |  fun throwOne() {
        |  }
        |
        |  @Throws(IOException::class, SourCreamException::class)
        |  fun throwTwo() {
        |  }
        |
        |  @Throws(IOException::class)
        |  abstract fun abstractThrow()
        |
        |  @Throws(IOException::class)
        |  external fun nativeThrow()
        |}
        |""".trimMargin())
  }

  @Test fun typeVariables() {
    val t = TypeVariableName("T")
    val p = TypeVariableName("P", Number::class)
    val location = ClassName(tacosPackage, "Location")
    val typeSpec = TypeSpec.classBuilder("Location")
        .addTypeVariable(t)
        .addTypeVariable(p)
        .addSuperinterface(ParameterizedTypeName.get(Comparable::class.asClassName(), p))
        .addProperty("label", t)
        .addProperty("x", p)
        .addProperty("y", p)
        .addFun(FunSpec.builder("compareTo")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(Int::class)
            .addParameter("p", p)
            .addStatement("return 0")
            .build())
        .addFun(FunSpec.builder("of")
            .addModifiers(KModifier.PUBLIC)
            .addTypeVariable(t)
            .addTypeVariable(p)
            .returns(ParameterizedTypeName.get(location, t, p))
            .addParameter("label", t)
            .addParameter("x", p)
            .addParameter("y", p)
            .addStatement("throw new %T(%S)", UnsupportedOperationException::class, "TODO")
            .build())
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.UnsupportedOperationException
        |import kotlin.Comparable
        |import kotlin.Int
        |import kotlin.Number
        |
        |class Location<T, P : Number> : Comparable<P> {
        |  val label: T
        |
        |  val x: P
        |
        |  val y: P
        |
        |  override fun compareTo(p: P): Int = 0
        |
        |  fun <T, P : Number> of(label: T, x: P, y: P): Location<T, P> {
        |    throw new UnsupportedOperationException("TODO")
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun typeVariableWithBounds() {
    val a = AnnotationSpec.builder(ClassName("com.squareup.tacos", "A")).build()
    val p = TypeVariableName("P", Number::class)
    val q = TypeVariableName("Q", Number::class).annotated(a) as TypeVariableName
    val typeSpec = TypeSpec.classBuilder("Location")
        .addTypeVariable(p.withBounds(Comparable::class))
        .addTypeVariable(q.withBounds(Comparable::class))
        .addProperty("x", p)
        .addProperty("y", q)
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Comparable
        |import kotlin.Number
        |
        |class Location<P, Q> where P : Number, P : Comparable, Q : Number, Q : Comparable {
        |  val x: P
        |
        |  val y: @A Q
        |}
        |""".trimMargin())
  }

  @Test fun classImplementsExtends() {
    val taco = ClassName(tacosPackage, "Taco")
    val food = ClassName("com.squareup.tacos", "Food")
    val typeSpec = TypeSpec.classBuilder("Taco")
        .addModifiers(KModifier.ABSTRACT)
        .superclass(ParameterizedTypeName.get(AbstractSet::class.asClassName(), food))
        .addSuperinterface(Serializable::class)
        .addSuperinterface(ParameterizedTypeName.get(Comparable::class.asClassName(), taco))
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import java.util.AbstractSet
        |import kotlin.Comparable
        |
        |abstract class Taco : AbstractSet<Food>(), Serializable, Comparable<Taco>
        |""".trimMargin())
  }

  @Test fun classImplementsExtendsSameName() {
    val javapoetTaco = ClassName(tacosPackage, "Taco")
    val tacoBellTaco = ClassName("com.taco.bell", "Taco")
    val fishTaco = ClassName("org.fish.taco", "Taco")
    val typeSpec = TypeSpec.classBuilder("Taco")
        .superclass(fishTaco)
        .addSuperinterface(ParameterizedTypeName.get(Comparable::class.asClassName(), javapoetTaco))
        .addSuperinterface(tacoBellTaco)
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Comparable
        |
        |class Taco : org.fish.taco.Taco(), Comparable<Taco>, com.taco.bell.Taco
        |""".trimMargin())
  }

  @Test fun classImplementsInnerClass() {
    val outer = ClassName(tacosPackage, "Outer")
    val inner = outer.nestedClass("Inner")
    val callable = Callable::class.asClassName()
    val typeSpec = TypeSpec.classBuilder("Outer")
        .superclass(ParameterizedTypeName.get(callable,
            inner))
        .addType(TypeSpec.classBuilder("Inner")
            .addModifiers(KModifier.INNER)
            .build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.concurrent.Callable
        |
        |class Outer : Callable<Outer.Inner>() {
        |  inner class Inner
        |}
        |""".trimMargin())
  }

  @Test fun enumImplements() {
    val typeSpec = TypeSpec.enumBuilder("Food")
        .addSuperinterface(Serializable::class)
        .addSuperinterface(Cloneable::class)
        .addEnumConstant("LEAN_GROUND_BEEF")
        .addEnumConstant("SHREDDED_CHEESE")
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import kotlin.Cloneable
        |
        |enum class Food : Serializable, Cloneable {
        |  LEAN_GROUND_BEEF,
        |
        |  SHREDDED_CHEESE
        |}
        |""".trimMargin())
  }

  @Test fun interfaceExtends() {
    val taco = ClassName(tacosPackage, "Taco")
    val typeSpec = TypeSpec.interfaceBuilder("Taco")
        .addSuperinterface(Serializable::class)
        .addSuperinterface(ParameterizedTypeName.get(Comparable::class.asClassName(), taco))
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import kotlin.Comparable
        |
        |interface Taco : Serializable, Comparable<Taco>
        |""".trimMargin())
  }

  @Test fun nestedClasses() {
    val taco = ClassName(tacosPackage, "Combo", "Taco")
    val topping = ClassName(tacosPackage, "Combo", "Taco", "Topping")
    val chips = ClassName(tacosPackage, "Combo", "Chips")
    val sauce = ClassName(tacosPackage, "Combo", "Sauce")
    val typeSpec = TypeSpec.classBuilder("Combo")
        .addProperty("taco", taco)
        .addProperty("chips", chips)
        .addType(TypeSpec.classBuilder(taco.simpleName())
            .addProperty("toppings", ParameterizedTypeName.get(List::class.asClassName(), topping))
            .addProperty("sauce", sauce)
            .addType(TypeSpec.enumBuilder(topping.simpleName())
                .addEnumConstant("SHREDDED_CHEESE")
                .addEnumConstant("LEAN_GROUND_BEEF")
                .build())
            .build())
        .addType(TypeSpec.classBuilder(chips.simpleName())
            .addProperty("topping", topping)
            .addProperty("dippingSauce", sauce)
            .build())
        .addType(TypeSpec.enumBuilder(sauce.simpleName())
            .addEnumConstant("SOUR_CREAM")
            .addEnumConstant("SALSA")
            .addEnumConstant("QUESO")
            .addEnumConstant("MILD")
            .addEnumConstant("FIRE")
            .build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.collections.List
        |
        |class Combo {
        |  val taco: Taco
        |
        |  val chips: Chips
        |
        |  class Taco {
        |    val toppings: List<Topping>
        |
        |    val sauce: Sauce
        |
        |    enum class Topping {
        |      SHREDDED_CHEESE,
        |
        |      LEAN_GROUND_BEEF
        |    }
        |  }
        |
        |  class Chips {
        |    val topping: Taco.Topping
        |
        |    val dippingSauce: Sauce
        |  }
        |
        |  enum class Sauce {
        |    SOUR_CREAM,
        |
        |    SALSA,
        |
        |    QUESO,
        |
        |    MILD,
        |
        |    FIRE
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun annotation() {
    val annotation = TypeSpec.annotationBuilder("MyAnnotation")
        .addModifiers(KModifier.PUBLIC)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("test", Int::class)
                .build())
            .build())
        .addProperty(PropertySpec.builder("test", Int::class)
            .initializer("test")
            .build())
        .build()

    assertThat(toString(annotation)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |annotation class MyAnnotation(val test: Int)
        |""".trimMargin())
  }

  @Ignore @Test fun innerAnnotationInAnnotationDeclaration() {
    val bar = TypeSpec.annotationBuilder("Bar")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("value", java.lang.Deprecated::class)
                .build())
            .build())
        .addProperty(PropertySpec.builder("value", java.lang.Deprecated::class)
            .initializer("value")
            .build())
        .build()

    assertThat(toString(bar)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.Deprecated
        |
        |annotation class Bar {
        |  fun value(): Deprecated default @Deprecated
        |}
        |""".trimMargin())
  }

  @Test fun interfaceWithProperties() {
    val taco = TypeSpec.interfaceBuilder("Taco")
        .addProperty("v", Int::class)
        .build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |interface Taco {
        |  val v: Int
        |}
        |""".trimMargin())
  }

  @Test fun referencedAndDeclaredSimpleNamesConflict() {
    val internalTop = PropertySpec.builder(
        "internalTop", ClassName(tacosPackage, "Top")).build()
    val internalBottom = PropertySpec.builder(
        "internalBottom", ClassName(tacosPackage, "Top", "Middle", "Bottom")).build()
    val externalTop = PropertySpec.builder(
        "externalTop", ClassName(donutsPackage, "Top")).build()
    val externalBottom = PropertySpec.builder(
        "externalBottom", ClassName(donutsPackage, "Bottom")).build()
    val top = TypeSpec.classBuilder("Top")
        .addProperty(internalTop)
        .addProperty(internalBottom)
        .addProperty(externalTop)
        .addProperty(externalBottom)
        .addType(TypeSpec.classBuilder("Middle")
            .addProperty(internalTop)
            .addProperty(internalBottom)
            .addProperty(externalTop)
            .addProperty(externalBottom)
            .addType(TypeSpec.classBuilder("Bottom")
                .addProperty(internalTop)
                .addProperty(internalBottom)
                .addProperty(externalTop)
                .addProperty(externalBottom)
                .build())
            .build())
        .build()
    assertThat(toString(top)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.donuts.Bottom
        |
        |class Top {
        |  val internalTop: Top
        |
        |  val internalBottom: Middle.Bottom
        |
        |  val externalTop: com.squareup.donuts.Top
        |
        |  val externalBottom: Bottom
        |
        |  class Middle {
        |    val internalTop: Top
        |
        |    val internalBottom: Bottom
        |
        |    val externalTop: com.squareup.donuts.Top
        |
        |    val externalBottom: com.squareup.donuts.Bottom
        |
        |    class Bottom {
        |      val internalTop: Top
        |
        |      val internalBottom: Bottom
        |
        |      val externalTop: com.squareup.donuts.Top
        |
        |      val externalBottom: com.squareup.donuts.Bottom
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun simpleNamesConflictInThisAndOtherPackage() {
    val internalOther = PropertySpec.builder(
        "internalOther", ClassName(tacosPackage, "Other")).build()
    val externalOther = PropertySpec.builder(
        "externalOther", ClassName(donutsPackage, "Other")).build()
    val gen = TypeSpec.classBuilder("Gen")
        .addProperty(internalOther)
        .addProperty(externalOther)
        .build()
    assertThat(toString(gen)).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Gen {
        |  val internalOther: Other
        |
        |  val externalOther: com.squareup.donuts.Other
        |}
        |""".trimMargin())
  }

  @Test fun intersectionType() {
    val typeVariable = TypeVariableName("T", Comparator::class, Serializable::class)
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("getComparator")
            .addTypeVariable(typeVariable)
            .returns(typeVariable)
            .addCode("return null;\n")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import java.util.Comparator
        |
        |class Taco {
        |  fun <T> getComparator(): T where T : Comparator, T : Serializable {
        |    return null;
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun primitiveArrayType() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty("ints", IntArray::class)
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.IntArray
        |
        |class Taco {
        |  val ints: IntArray
        |}
        |""".trimMargin())
  }

  @Test fun kdoc() {
    val taco = TypeSpec.classBuilder("Taco")
        .addKdoc("A hard or soft tortilla, loosely folded and filled with whatever\n")
        .addKdoc("[random][%T] tex-mex stuff we could find in the pantry\n", Random::class)
        .addKdoc(CodeBlock.of("and some [%T] cheese.\n", String::class))
        .addProperty(PropertySpec.builder("soft", Boolean::class)
            .addKdoc("True for a soft flour tortilla; false for a crunchy corn tortilla.\n")
            .build())
        .addFun(FunSpec.builder("refold")
            .addKdoc("Folds the back of this taco to reduce sauce leakage.\n"
                + "\n"
                + "For [%T#KOREAN], the front may also be folded.\n", Locale::class)
            .addParameter("locale", Locale::class)
            .build())
        .build()
    // Mentioning a type in KDoc will not cause an import to be added (java.util.Random here), but
    // the short name will be used if it's already imported (java.util.Locale here).
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.Locale
        |import kotlin.Boolean
        |
        |/**
        | * A hard or soft tortilla, loosely folded and filled with whatever
        | * [random][java.util.Random] tex-mex stuff we could find in the pantry
        | * and some [kotlin.String] cheese.
        | */
        |class Taco {
        |  /**
        |   * True for a soft flour tortilla; false for a crunchy corn tortilla.
        |   */
        |  val soft: Boolean
        |
        |  /**
        |   * Folds the back of this taco to reduce sauce leakage.
        |   *
        |   * For [Locale#KOREAN], the front may also be folded.
        |   */
        |  fun refold(locale: Locale) {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun annotationsInAnnotations() {
    val beef = ClassName(tacosPackage, "Beef")
    val chicken = ClassName(tacosPackage, "Chicken")
    val option = ClassName(tacosPackage, "Option")
    val mealDeal = ClassName(tacosPackage, "MealDeal")
    val menu = TypeSpec.classBuilder("Menu")
        .addAnnotation(AnnotationSpec.builder(mealDeal)
            .addMember("price", "%L", 500)
            .addMember("options", "%L", AnnotationSpec.builder(option)
                .addMember("name", "%S", "taco")
                .addMember("meat", "%T::class", beef)
                .build())
            .addMember("options", "%L", AnnotationSpec.builder(option)
                .addMember("name", "%S", "quesadilla")
                .addMember("meat", "%T::class", chicken)
                .build())
            .build())
        .build()
    assertThat(toString(menu)).isEqualTo("""
        |package com.squareup.tacos
        |
        |@MealDeal(
        |    price = 500,
        |    options = [
        |        Option(name = "taco", meat = Beef::class),
        |        Option(name = "quesadilla", meat = Chicken::class)
        |    ]
        |)
        |class Menu
        |""".trimMargin())
  }

  @Test fun varargs() {
    val taqueria = TypeSpec.classBuilder("Taqueria")
        .addFun(FunSpec.builder("prepare")
            .addParameter("workers", Int::class)
            .addParameter("jobs", Runnable::class.asClassName(), VARARG)
            .build())
        .build()
    assertThat(toString(taqueria)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.Runnable
        |import kotlin.Int
        |
        |class Taqueria {
        |  fun prepare(workers: Int, vararg jobs: Runnable) {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun varargsNotLast() {
    val taqueria = TypeSpec.classBuilder("Taqueria")
        .addFun(FunSpec.builder("prepare")
            .addParameter("workers", Int::class)
            .addParameter("jobs", Runnable::class.asClassName(), VARARG)
            .addParameter("start", Boolean::class.asClassName())
            .build())
        .build()
    assertThat(toString(taqueria)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.Runnable
        |import kotlin.Boolean
        |import kotlin.Int
        |
        |class Taqueria {
        |  fun prepare(workers: Int, vararg jobs: Runnable, start: Boolean) {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun codeBlocks() {
    val ifBlock = CodeBlock.builder()
        .beginControlFlow("if (!a.equals(b))")
        .addStatement("return i")
        .endControlFlow()
        .build()
    val funBody = CodeBlock.builder()
        .addStatement("%T size = %T.min(listA.size(), listB.size())", Int::class.javaPrimitiveType, Math::class)
        .beginControlFlow("for (%T i = 0; i < size; i++)", Int::class.javaPrimitiveType)
        .addStatement("%T %N = %N.get(i)", String::class, "a", "listA")
        .addStatement("%T %N = %N.get(i)", String::class, "b", "listB")
        .add("%L", ifBlock)
        .endControlFlow()
        .addStatement("return size")
        .build()
    val propertyBlock = CodeBlock.builder()
        .add("%T.<%T, %T>builder()", ImmutableMap::class, String::class, String::class)
        .add("\n.add(%S, %S)", '\'', "&#39;")
        .add("\n.add(%S, %S)", '&', "&amp;")
        .add("\n.add(%S, %S)", '<', "&lt;")
        .add("\n.add(%S, %S)", '>', "&gt;")
        .add("\n.build()")
        .build()
    val escapeHtml = PropertySpec.builder("ESCAPE_HTML", ParameterizedTypeName.get(
        Map::class, String::class, String::class))
        .addModifiers(KModifier.PRIVATE)
        .initializer(propertyBlock)
        .build()
    val util = TypeSpec.classBuilder("Util")
        .addProperty(escapeHtml)
        .addFun(FunSpec.builder("commonPrefixLength")
            .returns(Int::class)
            .addParameter("listA", ParameterizedTypeName.get(List::class, String::class))
            .addParameter("listB", ParameterizedTypeName.get(List::class, String::class))
            .addCode(funBody)
            .build())
        .build()
    assertThat(toString(util)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.google.common.collect.ImmutableMap
        |import java.lang.Math
        |import kotlin.Int
        |import kotlin.String
        |import kotlin.collections.List
        |import kotlin.collections.Map
        |
        |class Util {
        |  private val ESCAPE_HTML: Map<String, String> = ImmutableMap.<String, String>builder()
        |      .add("'", "&#39;")
        |      .add("&", "&amp;")
        |      .add("<", "&lt;")
        |      .add(">", "&gt;")
        |      .build()
        |
        |  fun commonPrefixLength(listA: List<String>, listB: List<String>): Int {
        |    Int size = Math.min(listA.size(), listB.size())
        |    for (Int i = 0; i < size; i++) {
        |      String a = listA.get(i)
        |      String b = listB.get(i)
        |      if (!a.equals(b)) {
        |        return i
        |      }
        |    }
        |    return size
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun indexedElseIf() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("choices")
            .beginControlFlow("if (%1L != null || %1L == %2L)", "taco", "otherTaco")
            .addStatement("%T.out.println(%S)", System::class, "only one taco? NOO!")
            .nextControlFlow("else if (%1L.%3L && %2L.%3L)", "taco", "otherTaco", "isSupreme()")
            .addStatement("%T.out.println(%S)", System::class, "taco heaven")
            .endControlFlow()
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.System
        |
        |class Taco {
        |  fun choices() {
        |    if (taco != null || taco == otherTaco) {
        |      System.out.println("only one taco? NOO!")
        |    } else if (taco.isSupreme() && otherTaco.isSupreme()) {
        |      System.out.println("taco heaven")
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun elseIf() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("choices")
            .beginControlFlow("if (5 < 4) ")
            .addStatement("%T.out.println(%S)", System::class, "wat")
            .nextControlFlow("else if (5 < 6)")
            .addStatement("%T.out.println(%S)", System::class, "hello")
            .endControlFlow()
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.System
        |
        |class Taco {
        |  fun choices() {
        |    if (5 < 4)  {
        |      System.out.println("wat")
        |    } else if (5 < 6) {
        |      System.out.println("hello")
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun inlineIndent() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("inlineIndent")
            .addCode("if (3 < 4) {\n%>%T.out.println(%S);\n%<}\n", System::class, "hello")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.System
        |
        |class Taco {
        |  fun inlineIndent() {
        |    if (3 < 4) {
        |      System.out.println("hello");
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun defaultModifiersForMemberInterfacesAndEnums() {
    val taco = TypeSpec.classBuilder("Taco")
        .addType(TypeSpec.classBuilder("Meat")
            .build())
        .addType(TypeSpec.interfaceBuilder("Tortilla")
            .build())
        .addType(TypeSpec.enumBuilder("Topping")
            .addEnumConstant("SALSA")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  class Meat
        |
        |  interface Tortilla
        |
        |  enum class Topping {
        |    SALSA
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun membersOrdering() {
    // Hand out names in reverse-alphabetical order to defend against unexpected sorting.
    val taco = TypeSpec.classBuilder("Members")
        .addType(TypeSpec.classBuilder("Z").build())
        .addType(TypeSpec.classBuilder("Y").build())
        .addProperty("W", String::class)
        .addProperty("U", String::class)
        .addFun(FunSpec.builder("T").build())
        .addFun(FunSpec.builder("S").build())
        .addFun(FunSpec.builder("R").build())
        .addFun(FunSpec.builder("Q").build())
        .addFun(FunSpec.constructorBuilder()
            .addParameter("p", Int::class)
            .build())
        .addFun(FunSpec.constructorBuilder()
            .addParameter("o", Long::class)
            .build())
        .build()
    // Static properties, instance properties, constructors, functions, classes.
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.Long
        |import kotlin.String
        |
        |class Members {
        |  val W: String
        |
        |  val U: String
        |
        |  constructor(p: Int) {
        |  }
        |
        |  constructor(o: Long) {
        |  }
        |
        |  fun T() {
        |  }
        |
        |  fun S() {
        |  }
        |
        |  fun R() {
        |  }
        |
        |  fun Q() {
        |  }
        |
        |  class Z
        |
        |  class Y
        |}
        |""".trimMargin())
  }

  @Test fun nativeFunctions() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("nativeInt")
            .addModifiers(KModifier.EXTERNAL)
            .returns(Int::class)
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |class Taco {
        |  external fun nativeInt(): Int
        |}
        |""".trimMargin())
  }

  @Test fun nullStringLiteral() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("NULL", String::class)
            .initializer("%S", null)
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  val NULL: String = null
        |}
        |""".trimMargin())
  }

  @Test fun annotationToString() {
    val annotation = AnnotationSpec.builder(SuppressWarnings::class)
        .addMember("value", "%S", "unused")
        .build()
    assertThat(annotation.toString()).isEqualTo("@java.lang.SuppressWarnings(\"unused\")")
  }

  @Test fun codeBlockToString() {
    val codeBlock = CodeBlock.builder()
        .addStatement("%T %N = %S.substring(0, 3)", String::class, "s", "taco")
        .build()
    assertThat(codeBlock.toString()).isEqualTo("kotlin.String s = \"taco\".substring(0, 3)\n")
  }

  @Test fun propertyToString() {
    val property = PropertySpec.builder("s", String::class)
        .initializer("%S.substring(0, 3)", "taco")
        .build()
    assertThat(property.toString())
        .isEqualTo("val s: kotlin.String = \"taco\".substring(0, 3)\n")
  }

  @Test fun functionToString() {
    val funSpec = FunSpec.builder("toString")
        .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
        .returns(String::class)
        .addStatement("return %S", "taco")
        .build()
    assertThat(funSpec.toString())
        .isEqualTo("override fun toString(): kotlin.String = \"taco\"\n")
  }

  @Test fun constructorToString() {
    val constructor = FunSpec.constructorBuilder()
        .addModifiers(KModifier.PUBLIC)
        .addParameter("taco", ClassName(tacosPackage, "Taco"))
        .addStatement("this.%N = %N", "taco", "taco")
        .build()
    assertThat(constructor.toString()).isEqualTo(""
        + "constructor(taco: com.squareup.tacos.Taco) {\n"
        + "  this.taco = taco\n"
        + "}\n")
  }

  @Test fun parameterToString() {
    val parameter = ParameterSpec.builder("taco", ClassName(tacosPackage, "Taco"))
        .addModifiers(KModifier.FINAL)
        .addAnnotation(ClassName("javax.annotation", "Nullable"))
        .build()
    assertThat(parameter.toString())
        .isEqualTo("@javax.annotation.Nullable final taco: com.squareup.tacos.Taco")
  }

  @Test fun classToString() {
    val type = TypeSpec.classBuilder("Taco")
        .build()
    assertThat(type.toString()).isEqualTo(""
        + "class Taco\n")
  }

  @Test fun anonymousClassToString() {
    val type = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(Runnable::class)
        .addFun(FunSpec.builder("run")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .build())
        .build()
    assertThat(type.toString()).isEqualTo("""
        |object : java.lang.Runnable() {
        |  override fun run() {
        |  }
        |}""".trimMargin())
  }

  @Test fun interfaceClassToString() {
    val type = TypeSpec.interfaceBuilder("Taco")
        .build()
    assertThat(type.toString()).isEqualTo("""
        |interface Taco
        |""".trimMargin())
  }

  @Test fun annotationDeclarationToString() {
    val type = TypeSpec.annotationBuilder("Taco")
        .build()
    assertThat(type.toString()).isEqualTo("""
        |annotation class Taco
        |""".trimMargin())
  }

  private fun toString(typeSpec: TypeSpec): String {
    return KotlinFile.get(tacosPackage, typeSpec).toString()
  }

  @Test fun multilineStatement() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("toString")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("val result = %S\n+ %S\n+ %S\n+ %S\n+ %S",
                "Taco(", "beef,", "lettuce,", "cheese", ")")
            .addStatement("return result")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  override fun toString(): String {
        |    val result = "Taco("
        |        + "beef,"
        |        + "lettuce,"
        |        + "cheese"
        |        + ")"
        |    return result
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun multilineStatementWithAnonymousClass() {
    val stringComparator = ParameterizedTypeName.get(Comparator::class, String::class)
    val listOfString = ParameterizedTypeName.get(List::class, String::class)
    val prefixComparator = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(stringComparator)
        .addFun(FunSpec.builder("compare")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(Int::class)
            .addParameter("a", String::class)
            .addParameter("b", String::class)
            .addComment("Prefix the strings and compare them")
            .addStatement("return a.substring(0, length)\n" + ".compareTo(b.substring(0, length))")
            .build())
        .build()
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("comparePrefix")
            .returns(stringComparator)
            .addParameter("length", Int::class, KModifier.FINAL)
            .addComment("Return a new comparator for the target length.")
            .addStatement("return %L", prefixComparator)
            .build())
        .addFun(FunSpec.builder("sortPrefix")
            .addParameter("list", listOfString)
            .addParameter("length", Int::class, KModifier.FINAL)
            .addStatement("%T.sort(\nlist,\n%L)", Collections::class, prefixComparator)
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.Collections
        |import java.util.Comparator
        |import kotlin.Int
        |import kotlin.String
        |import kotlin.collections.List
        |
        |class Taco {
        |  fun comparePrefix(final length: Int): Comparator<String> {
        |    // Return a new comparator for the target length.
        |    return object : Comparator<String>() {
        |      override fun compare(a: String, b: String): Int {
        |        // Prefix the strings and compare them
        |        return a.substring(0, length)
        |            .compareTo(b.substring(0, length))
        |      }
        |    }
        |  }
        |
        |  fun sortPrefix(list: List<String>, final length: Int) {
        |    Collections.sort(
        |        list,
        |        object : Comparator<String>() {
        |          override fun compare(a: String, b: String): Int {
        |            // Prefix the strings and compare them
        |            return a.substring(0, length)
        |                .compareTo(b.substring(0, length))
        |          }
        |        })
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun multilineStrings() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("toppings", String::class)
            .initializer("%S", "shell\nbeef\nlettuce\ncheese\n")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  val toppings: String = ${"\"\"\""}
        |      |shell
        |      |beef
        |      |lettuce
        |      |cheese
        |      |${"\"\"\""}.trimMargin()
        |}
        |""".trimMargin())
  }

  @Test fun doublePropertyInitialization() {
    try {
      PropertySpec.builder("listA", String::class)
          .initializer("foo")
          .initializer("bar")
          .build()
      fail()
    } catch (expected: IllegalStateException) {
    }

    try {
      PropertySpec.builder("listA", String::class)
          .initializer(CodeBlock.builder().add("foo").build())
          .initializer(CodeBlock.builder().add("bar").build())
          .build()
      fail()
    } catch (expected: IllegalStateException) {
    }

  }

  @Test fun multipleAnnotationAddition() {
    val taco = TypeSpec.classBuilder("Taco")
        .addAnnotations(Arrays.asList(
            AnnotationSpec.builder(SuppressWarnings::class)
                .addMember("value", "%S", "unchecked")
                .build(),
            AnnotationSpec.builder(java.lang.Deprecated::class).build()))
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.Deprecated
        |import java.lang.SuppressWarnings
        |
        |@SuppressWarnings("unchecked")
        |@Deprecated
        |class Taco
        |""".trimMargin())
  }

  @Test fun multiplePropertyAddition() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperties(Arrays.asList(
            PropertySpec.builder("ANSWER", Int::class, KModifier.CONST).build(),
            PropertySpec.builder("price", BigDecimal::class, KModifier.PRIVATE).build()))
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.math.BigDecimal
        |import kotlin.Int
        |
        |class Taco {
        |  const val ANSWER: Int
        |
        |  private val price: BigDecimal
        |}
        |""".trimMargin())
  }

  @Test fun multipleFunctionAddition() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFunctions(Arrays.asList(
            FunSpec.builder("getAnswer")
                .addModifiers(KModifier.PUBLIC)
                .returns(Int::class)
                .addStatement("return %L", 42)
                .build(),
            FunSpec.builder("getRandomQuantity")
                .addModifiers(KModifier.PUBLIC)
                .returns(Int::class)
                .addKdoc("chosen by fair dice roll ;)\n")
                .addStatement("return %L", 4)
                .build()))
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |class Taco {
        |  fun getAnswer(): Int = 42
        |
        |  /**
        |   * chosen by fair dice roll ;)
        |   */
        |  fun getRandomQuantity(): Int = 4
        |}
        |""".trimMargin())
  }

  @Test fun multipleSuperinterfaceAddition() {
    val taco = TypeSpec.classBuilder("Taco")
        .addSuperinterfaces(Arrays.asList(
            Serializable::class.asTypeName(),
            EventListener::class.asTypeName()))
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import java.util.EventListener
        |
        |class Taco : Serializable, EventListener
        |""".trimMargin())
  }

  @Test fun multipleTypeVariableAddition() {
    val location = TypeSpec.classBuilder("Location")
        .addTypeVariables(Arrays.asList(
            TypeVariableName("T"),
            TypeVariableName("P", Number::class)))
        .build()
    assertThat(toString(location)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Number
        |
        |class Location<T, P : Number>
        |""".trimMargin())
  }

  @Test fun multipleTypeAddition() {
    val taco = TypeSpec.classBuilder("Taco")
        .addTypes(Arrays.asList(
            TypeSpec.classBuilder("Topping").build(),
            TypeSpec.classBuilder("Sauce").build()))
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  class Topping
        |
        |  class Sauce
        |}
        |""".trimMargin())
  }

  @Test fun tryCatch() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("addTopping")
            .addParameter("topping", ClassName("com.squareup.tacos", "Topping"))
            .beginControlFlow("try")
            .addCode("/* do something tricky with the topping */\n")
            .nextControlFlow("catch (e: %T)",
                ClassName("com.squareup.tacos", "IllegalToppingException"))
            .endControlFlow()
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  fun addTopping(topping: Topping) {
        |    try {
        |      /* do something tricky with the topping */
        |    } catch (e: IllegalToppingException) {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun ifElse() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFun(FunSpec.builder("isDelicious")
            .addParameter("count", INT)
            .returns(BOOLEAN)
            .beginControlFlow("if (count > 0)")
            .addStatement("return true")
            .nextControlFlow("else")
            .addStatement("return false")
            .endControlFlow()
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Boolean
        |import kotlin.Int
        |
        |class Taco {
        |  fun isDelicious(count: Int): Boolean {
        |    if (count > 0) {
        |      return true
        |    } else {
        |      return false
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun literalFromAnything() {
    val value = object : Any() {
      override fun toString(): String {
        return "foo"
      }
    }
    assertThat(CodeBlock.of("%L", value).toString()).isEqualTo("foo")
  }

  @Test fun nameFromCharSequence() {
    assertThat(CodeBlock.of("%N", "text").toString()).isEqualTo("text")
  }

  @Test fun nameFromProperty() {
    val property = PropertySpec.builder("property", String::class).build()
    assertThat(CodeBlock.of("%N", property).toString()).isEqualTo("property")
  }

  @Test fun nameFromParameter() {
    val parameter = ParameterSpec.builder("parameter", String::class).build()
    assertThat(CodeBlock.of("%N", parameter).toString()).isEqualTo("parameter")
  }

  @Test fun nameFromFunction() {
    val funSpec = FunSpec.builder("method")
        .addModifiers(KModifier.ABSTRACT)
        .returns(String::class)
        .build()
    assertThat(CodeBlock.of("%N", funSpec).toString()).isEqualTo("method")
  }

  @Test fun nameFromType() {
    val type = TypeSpec.classBuilder("Type").build()
    assertThat(CodeBlock.of("%N", type).toString()).isEqualTo("Type")
  }

  @Test fun nameFromUnsupportedType() {
    try {
      CodeBlock.builder().add("%N", String::class)
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("expected name but was " + String::class)
    }

  }

  @Test fun stringFromAnything() {
    val value = object : Any() {
      override fun toString(): String {
        return "foo"
      }
    }
    assertThat(CodeBlock.of("%S", value).toString()).isEqualTo("\"foo\"")
  }

  @Test fun stringFromNull() {
    assertThat(CodeBlock.of("%S", null).toString()).isEqualTo("null")
  }

  @Test fun typeFromTypeName() {
    val typeName = String::class.asTypeName()
    assertThat(CodeBlock.of("%T", typeName).toString()).isEqualTo("kotlin.String")
  }

  @Test fun typeFromTypeMirror() {
    val mirror = getElement(String::class).asType()
    assertThat(CodeBlock.of("%T", mirror).toString()).isEqualTo("java.lang.String")
  }

  @Test fun typeFromTypeElement() {
    val element = getElement(String::class)
    assertThat(CodeBlock.of("%T", element).toString()).isEqualTo("java.lang.String")
  }

  @Test fun typeFromReflectType() {
    assertThat(CodeBlock.of("%T", String::class).toString()).isEqualTo("kotlin.String")
  }

  @Test fun typeFromUnsupportedType() {
    try {
      CodeBlock.builder().add("%T", "kotlin.String")
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("expected type but was kotlin.String")
    }

  }

  @Test fun tooFewArguments() {
    try {
      CodeBlock.builder().add("%S")
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("index 1 for '%S' not in range (received 0 arguments)")
    }

  }

  @Test fun unusedArgumentsRelative() {
    try {
      CodeBlock.builder().add("%L %L", "a", "b", "c")
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("unused arguments: expected 2, received 3")
    }

  }

  @Test fun unusedArgumentsIndexed() {
    try {
      CodeBlock.builder().add("%1L %2L", "a", "b", "c")
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("unused argument: %3")
    }

    try {
      CodeBlock.builder().add("%1L %1L %1L", "a", "b", "c")
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("unused arguments: %2, %3")
    }

    try {
      CodeBlock.builder().add("%3L %1L %3L %1L %3L", "a", "b", "c", "d")
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("unused arguments: %2, %4")
    }

  }

  @Test fun superClassOnlyValidForClasses() {
    try {
      TypeSpec.annotationBuilder("A").superclass(Any::class.asClassName())
      fail()
    } catch (expected: IllegalStateException) {
    }

    try {
      TypeSpec.enumBuilder("E").superclass(Any::class.asClassName())
      fail()
    } catch (expected: IllegalStateException) {
    }

    try {
      TypeSpec.interfaceBuilder("I").superclass(Any::class.asClassName())
      fail()
    } catch (expected: IllegalStateException) {
    }

  }

  @Test fun invalidSuperClass() {
    try {
      TypeSpec.classBuilder("foo")
          .superclass(List::class)
          .superclass(Map::class)
      fail()
    } catch (expected: IllegalStateException) {
    }

  }

  @Test fun staticCodeBlock() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty("foo", String::class, KModifier.PRIVATE)
        .addProperty(PropertySpec.builder("FOO", String::class, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", "FOO")
            .build())
        .addFun(FunSpec.builder("toString")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return FOO")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  private val foo: String
        |
        |  private const val FOO: String = "FOO"
        |
        |  override fun toString(): String = FOO
        |}
        |""".trimMargin())
  }

  @Test fun initializerBlockInRightPlace() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty("foo", String::class, KModifier.PRIVATE)
        .addProperty(PropertySpec.builder("FOO", String::class, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", "FOO")
            .build())
        .addFun(FunSpec.constructorBuilder().build())
        .addFun(FunSpec.builder("toString")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return FOO")
            .build())
        .addInitializerBlock(CodeBlock.builder()
            .addStatement("foo = %S", "FOO")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  private val foo: String
        |
        |  private const val FOO: String = "FOO"
        |
        |  init {
        |    foo = "FOO"
        |  }
        |
        |  constructor() {
        |  }
        |
        |  override fun toString(): String = FOO
        |}
        |""".trimMargin())
  }

  @Test fun initializersToBuilder() {
    // Tests if toBuilder() contains instance initializers
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("foo", String::class, KModifier.PRIVATE).build())
        .addProperty(PropertySpec.builder("FOO", String::class, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", "FOO")
            .build())
        .addFun(FunSpec.constructorBuilder().build())
        .addFun(FunSpec.builder("toString")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return FOO")
            .build())
        .build()

    val recreatedTaco = taco.toBuilder().build()
    assertThat(toString(taco)).isEqualTo(toString(recreatedTaco))

    val initializersAdded = taco.toBuilder()
        .addInitializerBlock(CodeBlock.builder()
            .addStatement("foo = %S", "instanceFoo")
            .build())
        .build()

    assertThat(toString(initializersAdded)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  private val foo: String
        |
        |  private const val FOO: String = "FOO"
        |
        |  init {
        |    foo = "instanceFoo"
        |  }
        |
        |  constructor() {
        |  }
        |
        |  override fun toString(): String = FOO
        |}
        |""".trimMargin())
  }

  @Test fun initializerBlockUnsupportedExceptionOnInterface() {
    val interfaceBuilder = TypeSpec.interfaceBuilder("Taco")
    try {
      interfaceBuilder.addInitializerBlock(CodeBlock.builder().build())
      fail("Exception expected")
    } catch (expected: IllegalStateException) {
    }
  }

  @Test fun initializerBlockUnsupportedExceptionOnAnnotation() {
    val annotationBuilder = TypeSpec.annotationBuilder("Taco")
    try {
      annotationBuilder.addInitializerBlock(CodeBlock.builder().build())
      fail("Exception expected")
    } catch (expected: IllegalStateException) {
    }
  }

  @Test fun lineWrapping() {
    val funSpecBuilder = FunSpec.builder("call")
    funSpecBuilder.addCode("%[call(")
    for (i in 0..31) {
      funSpecBuilder.addParameter("s" + i, String::class)
      funSpecBuilder.addCode(if (i > 0) ",%W%S" else "%S", i)
    }
    funSpecBuilder.addCode(");%]\n")

    val taco = TypeSpec.classBuilder("Taco")
        .addFun(funSpecBuilder.build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  fun call(s0: String, s1: String, s2: String, s3: String, s4: String, s5: String, s6: String,
        |      s7: String, s8: String, s9: String, s10: String, s11: String, s12: String, s13: String,
        |      s14: String, s15: String, s16: String, s17: String, s18: String, s19: String, s20: String,
        |      s21: String, s22: String, s23: String, s24: String, s25: String, s26: String, s27: String,
        |      s28: String, s29: String, s30: String, s31: String) {
        |    call("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
        |        "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31");
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun equalsAndHashCode() {
    var a = TypeSpec.interfaceBuilder("taco").build()
    var b = TypeSpec.interfaceBuilder("taco").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = TypeSpec.classBuilder("taco").build()
    b = TypeSpec.classBuilder("taco").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = TypeSpec.enumBuilder("taco").addEnumConstant("SALSA").build()
    b = TypeSpec.enumBuilder("taco").addEnumConstant("SALSA").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = TypeSpec.annotationBuilder("taco").build()
    b = TypeSpec.annotationBuilder("taco").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun classNameFactories() {
    val className = ClassName("com.example", "Example")
    assertThat(TypeSpec.classBuilder(className).build().name).isEqualTo("Example")
    assertThat(TypeSpec.interfaceBuilder(className).build().name).isEqualTo("Example")
    assertThat(TypeSpec.enumBuilder(className).addEnumConstant("A").build().name).isEqualTo("Example")
    assertThat(TypeSpec.annotationBuilder(className).build().name).isEqualTo("Example")
  }

  @Test fun objectType() {
    val type = TypeSpec.objectBuilder("MyObject")
        .addModifiers(KModifier.PUBLIC)
        .addProperty("tacos", Int::class)
        .addInitializerBlock(CodeBlock.builder().build())
        .addFun(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |object MyObject {
        |  val tacos: Int
        |
        |  init {
        |  }
        |
        |  fun test() {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun objectClassWithSupertype() {
    val superclass = ClassName("com.squareup.wire", "Message")
    val type = TypeSpec.objectBuilder("MyObject")
        .addModifiers(KModifier.PUBLIC)
        .superclass(superclass)
        .addInitializerBlock(CodeBlock.builder().build())
        .addFun(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.wire.Message
        |
        |object MyObject : Message() {
        |  init {
        |  }
        |
        |  fun test() {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObject() {
    val companion = TypeSpec.companionObjectBuilder()
        .addProperty(PropertySpec.builder("tacos", Int::class)
            .initializer("%L", 42)
            .build())
        .addFun(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val type = TypeSpec.classBuilder("MyClass")
        .addModifiers(KModifier.PUBLIC)
        .companionObject(companion)
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |class MyClass {
        |  companion object {
        |    val tacos: Int = 42
        |
        |    fun test() {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObjectOnInterface() {
    val companion = TypeSpec.companionObjectBuilder()
        .addFun(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val type = TypeSpec.interfaceBuilder("MyInterface")
        .addModifiers(KModifier.PUBLIC)
        .companionObject(companion)
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |interface MyInterface {
        |  companion object {
        |    fun test() {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObjectOnEnumNotAlowed() {
    val companion = TypeSpec.companionObjectBuilder()
        .addFun(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val enumBuilder = TypeSpec.enumBuilder("MyEnum")
        .addModifiers(KModifier.PUBLIC)

    try {
      enumBuilder.companionObject(companion)
      fail("Exception expected")
    } catch (expected: IllegalStateException) {
    }
  }

  @Test fun companionObjectOnObjectNotAlowed() {
    val companion = TypeSpec.companionObjectBuilder()
        .addFun(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val objectBuilder = TypeSpec.objectBuilder("MyObject")
        .addModifiers(KModifier.PUBLIC)

    try {
      objectBuilder.companionObject(companion)
      fail("Exception expected")
    } catch (expected: IllegalStateException) {
    }
  }

  @Test fun companionObjectIsCompanionObjectKind() {
    val companion = TypeSpec.objectBuilder("Companion")
        .addFun(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val typeBuilder = TypeSpec.classBuilder("MyClass")
        .addModifiers(KModifier.PUBLIC)

    try {
      typeBuilder.companionObject(companion)
      fail("Exception expected")
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test fun companionObjectSuper() {
    val superclass = ClassName("com.squareup.wire", "Message")
    val companion = TypeSpec.companionObjectBuilder()
        .superclass(superclass)
        .addFun(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val type = TypeSpec.classBuilder("MyClass")
        .addModifiers(KModifier.PUBLIC)
        .companionObject(companion)
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.wire.Message
        |
        |class MyClass {
        |  companion object : Message() {
        |    fun test() {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun propertyInPrimaryConstructor() {
    val type = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("a", Int::class)
            .addParameter("b", String::class)
            .build())
        .addProperty(PropertySpec.builder("a", Int::class)
            .initializer("a")
            .build())
        .addProperty(PropertySpec.builder("b", String::class)
            .initializer("b")
            .build())
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.String
        |
        |class Taco(val a: Int, val b: String)
        |""".trimMargin())
  }

  @Test fun annotatedConstructor() {
    val injectAnnotation = ClassName("javax.inject", "Inject")
    val taco = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addAnnotation(AnnotationSpec.builder(injectAnnotation).build())
            .build())
        .build()

    assertThat(toString(taco)).isEqualTo("""
    |package com.squareup.tacos
    |
    |import javax.inject.Inject
    |
    |class Taco @Inject constructor()
    |
    """.trimMargin())
  }

  @Test fun internalConstructor() {
    val taco = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addModifiers(INTERNAL)
            .build())
        .build()

    assertThat(toString(taco)).isEqualTo("""
    |package com.squareup.tacos
    |
    |class Taco internal constructor()
    |
    """.trimMargin())
  }

  @Test fun annotatedInternalConstructor() {
    val injectAnnotation = ClassName("javax.inject", "Inject")
    val taco = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addAnnotation(AnnotationSpec.builder(injectAnnotation).build())
            .addModifiers(INTERNAL)
            .build())
        .build()

    assertThat(toString(taco)).isEqualTo("""
    |package com.squareup.tacos
    |
    |import javax.inject.Inject
    |
    |class Taco @Inject internal constructor()
    |
    """.trimMargin())
  }

  @Test fun multipleAnnotationsInternalConstructor() {
    val injectAnnotation = ClassName("javax.inject", "Inject")
    val namedAnnotation = ClassName("javax.inject", "Named")
    val taco = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addAnnotation(AnnotationSpec.builder(injectAnnotation).build())
            .addAnnotation(AnnotationSpec.builder(namedAnnotation).build())
            .addModifiers(INTERNAL)
            .build())
        .build()

    assertThat(toString(taco)).isEqualTo("""
    |package com.squareup.tacos
    |
    |import javax.inject.Inject
    |import javax.inject.Named
    |
    |class Taco @Inject @Named internal constructor()
    |
    """.trimMargin())
  }

  @Test fun importNonNullableProperty() {
    val type = String::class.asTypeName()
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("taco", type.asNonNullable())
            .initializer("%S", "taco")
            .build())
        .addProperty(PropertySpec.builder("nullTaco", type.asNullable())
            .initializer("null")
            .build())
        .build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |class Taco {
        |  val taco: String = "taco"
        |
        |  val nullTaco: String? = null
        |}
        |""".trimMargin())
  }

  @Test fun superclassConstructorParams() {
    val taco = TypeSpec.classBuilder("Foo")
        .superclass(ClassName(tacosPackage, "Bar"))
        .addSuperclassConstructorParameter("%S", "foo")
        .addSuperclassConstructorParameter(CodeBlock.of("%L", 42))
        .build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Foo : Bar("foo", 42)
        |""".trimMargin())
  }

  @Test fun superclassConstructorParamsForbiddenForAnnotation() {
    try {
      TypeSpec.annotationBuilder("Taco")
          .addSuperclassConstructorParameter("%S", "foo")
      fail("Exception expected")
    } catch (expected: IllegalStateException) {
    }
  }

  @Test fun constructorWithDefaultParamValue() {
    val type = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("a", Int::class)
                .defaultValue("1")
                .build())
            .addParameter(ParameterSpec.builder("b", String::class.asTypeName().asNullable())
                .defaultValue("null")
                .build())
            .build())
        .addProperty(PropertySpec.builder("a", Int::class)
            .initializer("a")
            .build())
        .addProperty(PropertySpec.builder("b", String::class.asTypeName().asNullable())
            .initializer("b")
            .build())
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.String
        |
        |class Taco(val a: Int = 1, val b: String? = null)
        |""".trimMargin())
  }

  companion object {
    private val donutsPackage = "com.squareup.donuts"
  }
}
