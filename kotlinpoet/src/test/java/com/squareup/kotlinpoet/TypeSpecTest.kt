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
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.IN
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.INNER
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.throws
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
import java.util.function.Consumer
import java.util.logging.Logger
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.Rule

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
        .addFunction(FunSpec.builder("toString")
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL, KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", "taco")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco {
        |  public final override fun toString(): String = "taco"
        |}
        |""".trimMargin())
    assertEquals(1906837485, taco.hashCode().toLong()) // Update expected number if source changes.
  }

  @Test fun interestingTypes() {
    val listOfAny = List::class.asClassName().parameterizedBy(STAR)
    val listOfExtends = List::class.asClassName()
        .parameterizedBy(WildcardTypeName.producerOf(Serializable::class))
    val listOfSuper = List::class.asClassName()
        .parameterizedBy(WildcardTypeName.consumerOf(String::class))
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
        |public class Taco {
        |  public val star: List<*>
        |
        |  public val outSerializable: List<out Serializable>
        |
        |  public val inString: List<in String>
        |}
        |""".trimMargin())
  }

  @Test fun anonymousInnerClass() {
    val foo = ClassName(tacosPackage, "Foo")
    val bar = ClassName(tacosPackage, "Bar")
    val thingThang = ClassName(tacosPackage, "Thing", "Thang")
    val thingThangOfFooBar = thingThang.parameterizedBy(foo, bar)
    val thung = ClassName(tacosPackage, "Thung")
    val simpleThung = ClassName(tacosPackage, "SimpleThung")
    val thungOfSuperBar = thung.parameterizedBy(WildcardTypeName.consumerOf(bar))
    val thungOfSuperFoo = thung.parameterizedBy(WildcardTypeName.consumerOf(foo))
    val simpleThungOfBar = simpleThung.parameterizedBy(bar)

    val thungParameter = ParameterSpec.builder("thung", thungOfSuperFoo)
        .addModifiers(KModifier.FINAL)
        .build()
    val aSimpleThung = TypeSpec.anonymousClassBuilder()
        .superclass(simpleThungOfBar)
        .addSuperclassConstructorParameter("%N", thungParameter)
        .addFunction(FunSpec.builder("doSomething")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addParameter("bar", bar)
            .addCode("/* code snippets */\n")
            .build())
        .build()
    val aThingThang = TypeSpec.anonymousClassBuilder()
        .superclass(thingThangOfFooBar)
        .addFunction(FunSpec.builder("call")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(thungOfSuperBar)
            .addParameter(thungParameter)
            .addStatement("return %L", aSimpleThung)
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
        |import kotlin.Unit
        |
        |public class Taco {
        |  public val NAME: Thing.Thang<Foo, Bar> = object : Thing.Thang<Foo, Bar>() {
        |    public override fun call(final thung: Thung<in Foo>): Thung<in Bar> = object :
        |        SimpleThung<Bar>(thung) {
        |      public override fun doSomething(bar: Bar): Unit {
        |        /* code snippets */
        |      }
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun anonymousClassWithSuperClassConstructorCall() {
    val superclass = ArrayList::class.parameterizedBy(String::class)
    val anonymousClass = TypeSpec.anonymousClassBuilder()
        .addSuperclassConstructorParameter("%L", "4")
        .superclass(superclass)
        .build()
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("names", superclass)
            .initializer("%L", anonymousClass)
            .build()
        ).build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.ArrayList
        |import kotlin.String
        |
        |public class Taco {
        |  public val names: ArrayList<String> = object : ArrayList<String>(4) {
        |  }
        |}
        |""".trimMargin())
  }

  // https://github.com/square/kotlinpoet/issues/315
  @Test fun anonymousClassWithMultipleSuperTypes() {
    val superclass = ClassName("com.squareup.wire", "Message")
    val anonymousClass = TypeSpec.anonymousClassBuilder()
        .superclass(superclass)
        .addSuperinterface(Runnable::class)
        .addFunction(FunSpec.builder("run")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addCode("/* code snippets */\n")
            .build()
        ).build()
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("NAME", Runnable::class)
            .initializer("%L", anonymousClass)
            .build()
        ).build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.wire.Message
        |import java.lang.Runnable
        |import kotlin.Unit
        |
        |public class Taco {
        |  public val NAME: Runnable = object : Message(), Runnable {
        |    public override fun run(): Unit {
        |      /* code snippets */
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun anonymousClassWithoutSuperType() {
    val anonymousClass = TypeSpec.anonymousClassBuilder().build()
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("NAME", Any::class)
            .initializer("%L", anonymousClass)
            .build()
        ).build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Any
        |
        |public class Taco {
        |  public val NAME: Any = object {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun annotatedParameters() {
    val service = TypeSpec.classBuilder("Foo")
        .addFunction(FunSpec.constructorBuilder()
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
                    .addMember("%S", "pong")
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
        |public class Foo {
        |  public constructor(
        |    id: Long,
        |    @Ping one: String,
        |    @Ping two: String,
        |    @Pong("pong") three: String,
        |    @Ping four: String
        |  ) {
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
            .copy(annotations = listOf(AnnotationSpec.builder(freeRange).build())))
        .build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import javax.annotation.FreeRange
        |import kotlin.String
        |
        |public class EthicalTaco {
        |  public val meat: @FreeRange String
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
        .addFunction(FunSpec.builder("fooBar")
            .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
            .addAnnotation(AnnotationSpec.builder(headers)
                .addMember("%S", "Accept: application/json")
                .addMember("%S", "User-Agent: foobar")
                .build())
            .addAnnotation(AnnotationSpec.builder(post)
                .addMember("%S", "/foo/bar")
                .build())
            .returns(observable.parameterizedBy(fooBar))
            .addParameter(ParameterSpec.builder("things", things.parameterizedBy(thing))
                .addAnnotation(body)
                .build())
            .addParameter(ParameterSpec.builder("query", map.parameterizedBy(string, string))
                .addAnnotation(AnnotationSpec.builder(queryMap)
                    .addMember("encodeValues = %L", "false")
                    .build())
                .build())
            .addParameter(ParameterSpec.builder("authorization", string)
                .addAnnotation(AnnotationSpec.builder(header)
                    .addMember("%S", "Authorization")
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
        |public interface Service {
        |  @Headers(
        |    "Accept: application/json",
        |    "User-Agent: foobar"
        |  )
        |  @POST("/foo/bar")
        |  public fun fooBar(
        |    @Body things: Things<Thing>,
        |    @QueryMap(encodeValues = false) query: Map<String, String>,
        |    @Header("Authorization") authorization: String
        |  ): Observable<FooBar>
        |}
        |""".trimMargin())
  }

  @Test fun annotatedProperty() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("thing", String::class, KModifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(ClassName(tacosPackage, "JsonAdapter"))
                .addMember("%T::class", ClassName(tacosPackage, "Foo"))
                .build())
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco {
        |  @JsonAdapter(Foo::class)
        |  private val thing: String
        |}
        |""".trimMargin())
  }

  @Test fun annotatedPropertyUseSiteTarget() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("thing", String::class, KModifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(ClassName(tacosPackage, "JsonAdapter"))
                .addMember("%T::class", ClassName(tacosPackage, "Foo"))
                .useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
                .build())
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco {
        |  @field:JsonAdapter(Foo::class)
        |  private val thing: String
        |}
        |""".trimMargin())
  }

  @Test fun annotatedClass() {
    val someType = ClassName(tacosPackage, "SomeType")
    val taco = TypeSpec.classBuilder("Foo")
        .addAnnotation(AnnotationSpec.builder(ClassName(tacosPackage, "Something"))
            .addMember("%T.%N", someType, "PROPERTY")
            .addMember("%L", 12)
            .addMember("%S", "goodbye")
            .build())
        .addModifiers(KModifier.PUBLIC)
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |@Something(
        |  SomeType.PROPERTY,
        |  12,
        |  "goodbye"
        |)
        |public class Foo
        |""".trimMargin())
  }

  @Test fun enumWithSubclassing() {
    val roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(KModifier.PUBLIC)
        .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder()
            .addKdoc("Avalanche!\n")
            .build())
        .addEnumConstant("PAPER", TypeSpec.anonymousClassBuilder()
            .addSuperclassConstructorParameter("%S", "flat")
            .addFunction(FunSpec.builder("toString")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE, KModifier.OVERRIDE)
                .returns(String::class)
                .addCode("return %S\n", "paper airplane!")
                .build())
            .build())
        .addEnumConstant("SCISSORS", TypeSpec.anonymousClassBuilder()
            .addSuperclassConstructorParameter("%S", "peace sign")
            .build())
        .addProperty(PropertySpec.builder("handPosition", String::class, KModifier.PRIVATE)
            .initializer("handPosition")
            .build())
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("handPosition", String::class)
            .build())
        .addFunction(FunSpec.constructorBuilder()
            .addCode("this(%S)\n", "fist")
            .build())
        .build()
    assertThat(toString(roshambo)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public enum class Roshambo(
        |  private val handPosition: String
        |) {
        |  /**
        |   * Avalanche!
        |   */
        |  ROCK,
        |  PAPER("flat") {
        |    public override fun toString(): String = "paper airplane!"
        |  },
        |  SCISSORS("peace sign"),
        |  ;
        |
        |  public constructor() {
        |    this("fist")
        |  }
        |}
        |""".trimMargin())
  }

  /** https://github.com/square/javapoet/issues/193  */
  @Test fun enumsMayDefineAbstractFunctions() {
    val roshambo = TypeSpec.enumBuilder("Tortilla")
        .addModifiers(KModifier.PUBLIC)
        .addEnumConstant("CORN", TypeSpec.anonymousClassBuilder()
            .addFunction(FunSpec.builder("fold")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .build())
            .build())
        .addFunction(FunSpec.builder("fold")
            .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
            .build())
        .build()
    assertThat(toString(roshambo)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Unit
        |
        |public enum class Tortilla {
        |  CORN {
        |    public override fun fold(): Unit {
        |    }
        |  },
        |  ;
        |
        |  public abstract fun fold(): Unit
        |}
        |""".trimMargin())
  }

  @Test fun sealedClassesMayDefineAbstractMembers() {
    val sealedClass = TypeSpec.classBuilder("Sealed")
        .addModifiers(KModifier.SEALED)
        .addProperty(PropertySpec.builder("name", String::class).addModifiers(ABSTRACT).build())
        .addFunction(FunSpec.builder("fold").addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT).build())
        .build()
    assertThat(toString(sealedClass)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |import kotlin.Unit
        |
        |public sealed class Sealed {
        |  public abstract val name: String
        |
        |  public abstract fun fold(): Unit
        |}
        |""".trimMargin())
  }

  @Test fun classesMayHaveVarargConstructorProperties() {
    val variable = TypeSpec.classBuilder("Variable")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("name", String::class, VARARG).build())
            .build())
        .addProperty(PropertySpec.builder("name", String::class).initializer("name").build())
        .build()
    assertThat(toString(variable)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Variable(
        |  public vararg val name: String
        |)
        |""".trimMargin())
  }

  /** https://github.com/square/kotlinpoet/issues/942  */
  @Test fun noConstructorPropertiesWithCustomGetter() {
    val taco = TypeSpec.classBuilder("ObservantTaco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("contents", String::class).build())
            .build())
        .addProperty(
            PropertySpec.builder("contents", String::class).initializer("contents")
                .getter(FunSpec.getterBuilder().addCode("println(%S)\nreturn field", "contents observed!").build())
                .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class ObservantTaco(
        |  contents: String
        |) {
        |  public val contents: String = contents
        |    public get() {
        |      println("contents observed!")
        |      return field
        |    }
        |}
        |""".trimMargin())
  }

  @Test fun noConstructorPropertiesWithCustomSetter() {
    val taco = TypeSpec.classBuilder("ObservantTaco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("contents", String::class).build())
            .build())
        .addProperty(
            PropertySpec.builder("contents", String::class).initializer("contents")
                .mutable()
                .setter(FunSpec.setterBuilder()
                    .addParameter("value", String::class)
                    .addCode("println(%S)\nfield = value", "contents changed!").build())
                .build())
        .build()
    println(toString(taco))
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class ObservantTaco(
        |  contents: String
        |) {
        |  public var contents: String = contents
        |    public set(value) {
        |      println("contents changed!")
        |      field = value
        |    }
        |}
        |""".trimMargin())
  }

  @Test fun onlyEnumsMayHaveEnumConstants() {
    assertThrows<IllegalStateException> {
      TypeSpec.classBuilder("Roshambo")
          .addEnumConstant("ROCK")
          .build()
    }
  }

  /** https://github.com/square/kotlinpoet/issues/621  */
  @Test fun enumWithMembersButNoConstansts() {
    val roshambo = TypeSpec.enumBuilder("RenderPassCreate")
        .addType(TypeSpec.companionObjectBuilder().build())
        .build()
    assertThat(toString(roshambo)).isEqualTo("""
        |package com.squareup.tacos
        |
        |public enum class RenderPassCreate {
        |  ;
        |  public companion object
        |}
        |""".trimMargin())
  }

  @Test fun enumWithMembersButNoConstructorCall() {
    val roshambo = TypeSpec.enumBuilder("Roshambo")
        .addEnumConstant("SPOCK", TypeSpec.anonymousClassBuilder()
            .addFunction(FunSpec.builder("toString")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .returns(String::class)
                .addStatement("return %S", "west side")
                .build())
            .build())
        .build()
    assertThat(toString(roshambo)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public enum class Roshambo {
        |  SPOCK {
        |    public override fun toString(): String = "west side"
        |  },
        |}
        |""".trimMargin())
  }

  /** https://github.com/square/javapoet/issues/253  */
  @Test fun enumWithAnnotatedValues() {
    val roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(KModifier.PUBLIC)
        .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder()
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
        |public enum class Roshambo {
        |  @Deprecated
        |  ROCK,
        |  PAPER,
        |  SCISSORS,
        |}
        |""".trimMargin())
  }

  @Test fun funThrows() {
    val taco = TypeSpec.classBuilder("Taco")
        .addModifiers(KModifier.ABSTRACT)
        .addFunction(FunSpec.builder("throwOne")
            .throws(IOException::class)
            .build())
        .addFunction(FunSpec.builder("throwTwo")
            .throws(IOException::class.asClassName(), ClassName(tacosPackage, "SourCreamException"))
            .build())
        .addFunction(FunSpec.builder("abstractThrow")
            .addModifiers(KModifier.ABSTRACT)
            .throws(IOException::class)
            .build())
        .addFunction(FunSpec.builder("nativeThrow")
            .addModifiers(KModifier.EXTERNAL)
            .throws(IOException::class)
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.IOException
        |import kotlin.Unit
        |import kotlin.jvm.Throws
        |
        |public abstract class Taco {
        |  @Throws(IOException::class)
        |  public fun throwOne(): Unit {
        |  }
        |
        |  @Throws(
        |    IOException::class,
        |    SourCreamException::class
        |  )
        |  public fun throwTwo(): Unit {
        |  }
        |
        |  @Throws(IOException::class)
        |  public abstract fun abstractThrow(): Unit
        |
        |  @Throws(IOException::class)
        |  public external fun nativeThrow(): Unit
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
        .addSuperinterface(Comparable::class.asClassName().parameterizedBy(p))
        .addProperty("label", t)
        .addProperty("x", p)
        .addProperty("y", p)
        .addFunction(FunSpec.builder("compareTo")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(Int::class)
            .addParameter("p", p)
            .addStatement("return 0")
            .build())
        .addFunction(FunSpec.builder("of")
            .addModifiers(KModifier.PUBLIC)
            .addTypeVariable(t)
            .addTypeVariable(p)
            .returns(location.parameterizedBy(t, p))
            .addParameter("label", t)
            .addParameter("x", p)
            .addParameter("y", p)
            .addStatement("throw %T(%S)", UnsupportedOperationException::class, "TODO")
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
        |public class Location<T, P : Number> : Comparable<P> {
        |  public val label: T
        |
        |  public val x: P
        |
        |  public val y: P
        |
        |  public override fun compareTo(p: P): Int = 0
        |
        |  public fun <T, P : Number> of(
        |    label: T,
        |    x: P,
        |    y: P
        |  ): Location<T, P> = throw UnsupportedOperationException("TODO")
        |}
        |""".trimMargin())
  }

  @Test fun typeVariableWithBounds() {
    val a = AnnotationSpec.builder(ClassName("com.squareup.tacos", "A")).build()
    val p = TypeVariableName("P", Number::class)
    val q = TypeVariableName("Q", Number::class).copy(annotations = listOf(a)) as TypeVariableName
    val typeSpec = TypeSpec.classBuilder("Location")
        .addTypeVariable(p.copy(bounds = p.bounds + listOf(Comparable::class.asTypeName())))
        .addTypeVariable(q.copy(bounds = q.bounds + listOf(Comparable::class.asTypeName())))
        .addProperty("x", p)
        .addProperty("y", q)
        .primaryConstructor(FunSpec.constructorBuilder().build())
        .superclass(Number::class)
        .addSuperinterface(Comparable::class)
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Comparable
        |import kotlin.Number
        |
        |public class Location<P, Q> : Number(), Comparable where P : Number, P : Comparable, Q : Number, Q :
        |    Comparable {
        |  public val x: P
        |
        |  public val y: @A Q
        |}
        |""".trimMargin())
  }

  @Test fun classImplementsExtends() {
    val taco = ClassName(tacosPackage, "Taco")
    val food = ClassName("com.squareup.tacos", "Food")
    val typeSpec = TypeSpec.classBuilder("Taco")
        .addModifiers(KModifier.ABSTRACT)
        .superclass(AbstractSet::class.asClassName().parameterizedBy(food))
        .addSuperinterface(Serializable::class)
        .addSuperinterface(Comparable::class.asClassName().parameterizedBy(taco))
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import java.util.AbstractSet
        |import kotlin.Comparable
        |
        |public abstract class Taco : AbstractSet<Food>(), Serializable, Comparable<Taco>
        |""".trimMargin())
  }

  @Test fun classImplementsExtendsSameName() {
    val javapoetTaco = ClassName(tacosPackage, "Taco")
    val tacoBellTaco = ClassName("com.taco.bell", "Taco")
    val fishTaco = ClassName("org.fish.taco", "Taco")
    val typeSpec = TypeSpec.classBuilder("Taco")
        .superclass(fishTaco)
        .addSuperinterface(Comparable::class.asClassName().parameterizedBy(javapoetTaco))
        .addSuperinterface(tacoBellTaco)
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Comparable
        |
        |public class Taco : org.fish.taco.Taco(), Comparable<Taco>, com.taco.bell.Taco
        |""".trimMargin())
  }

  @Test fun classImplementsInnerClass() {
    val outer = ClassName(tacosPackage, "Outer")
    val inner = outer.nestedClass("Inner")
    val callable = Callable::class.asClassName()
    val typeSpec = TypeSpec.classBuilder("Outer")
        .superclass(callable.parameterizedBy(inner))
        .addType(TypeSpec.classBuilder("Inner")
            .addModifiers(KModifier.INNER)
            .build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.concurrent.Callable
        |
        |public class Outer : Callable<Outer.Inner>() {
        |  public inner class Inner
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
        |public enum class Food : Serializable, Cloneable {
        |  LEAN_GROUND_BEEF,
        |  SHREDDED_CHEESE,
        |}
        |""".trimMargin())
  }

  @Test fun interfaceExtends() {
    val taco = ClassName(tacosPackage, "Taco")
    val typeSpec = TypeSpec.interfaceBuilder("Taco")
        .addSuperinterface(Serializable::class)
        .addSuperinterface(Comparable::class.asClassName().parameterizedBy(taco))
        .build()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import kotlin.Comparable
        |
        |public interface Taco : Serializable, Comparable<Taco>
        |""".trimMargin())
  }

  @Test fun funInterface() {
    val taco = ClassName(tacosPackage, "Taco")
    val typeSpec = TypeSpec.funInterfaceBuilder(taco)
        .addFunction(FunSpec.builder("sam")
            .addModifiers(ABSTRACT)
            .build())
        .addFunction(FunSpec.builder("notSam").build())
        .build()
    assertThat(typeSpec.isFunctionalInterface).isTrue()
    assertThat(toString(typeSpec)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Unit
        |
        |public fun interface Taco {
        |  public fun sam(): Unit
        |
        |  public fun notSam(): Unit {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun funInterface_empty_shouldError() {
    assertThrows<IllegalStateException> {
      TypeSpec.funInterfaceBuilder("Taco")
          .build()
    }.hasMessageThat()
        .contains("Functional interfaces must have exactly one abstract function. Contained 0")
  }

  @Test fun funInterface_multipleAbstract_shouldError() {
    assertThrows<IllegalStateException> {
      TypeSpec.funInterfaceBuilder("Taco")
          .addFunction(FunSpec.builder("fun1")
              .addModifiers(ABSTRACT)
              .build())
          .addFunction(FunSpec.builder("fun2")
              .addModifiers(ABSTRACT)
              .build())
          .build()
    }.hasMessageThat()
        .contains("Functional interfaces must have exactly one abstract function. Contained 2")
  }

  @Test fun nestedClasses() {
    val taco = ClassName(tacosPackage, "Combo", "Taco")
    val topping = ClassName(tacosPackage, "Combo", "Taco", "Topping")
    val chips = ClassName(tacosPackage, "Combo", "Chips")
    val sauce = ClassName(tacosPackage, "Combo", "Sauce")
    val typeSpec = TypeSpec.classBuilder("Combo")
        .addProperty("taco", taco)
        .addProperty("chips", chips)
        .addType(TypeSpec.classBuilder(taco.simpleName)
            .addProperty("toppings", List::class.asClassName().parameterizedBy(topping))
            .addProperty("sauce", sauce)
            .addType(TypeSpec.enumBuilder(topping.simpleName)
                .addEnumConstant("SHREDDED_CHEESE")
                .addEnumConstant("LEAN_GROUND_BEEF")
                .build())
            .build())
        .addType(TypeSpec.classBuilder(chips.simpleName)
            .addProperty("topping", topping)
            .addProperty("dippingSauce", sauce)
            .build())
        .addType(TypeSpec.enumBuilder(sauce.simpleName)
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
        |public class Combo {
        |  public val taco: Taco
        |
        |  public val chips: Chips
        |
        |  public class Taco {
        |    public val toppings: List<Topping>
        |
        |    public val sauce: Sauce
        |
        |    public enum class Topping {
        |      SHREDDED_CHEESE,
        |      LEAN_GROUND_BEEF,
        |    }
        |  }
        |
        |  public class Chips {
        |    public val topping: Taco.Topping
        |
        |    public val dippingSauce: Sauce
        |  }
        |
        |  public enum class Sauce {
        |    SOUR_CREAM,
        |    SALSA,
        |    QUESO,
        |    MILD,
        |    FIRE,
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
        |public annotation class MyAnnotation(
        |  public val test: Int
        |)
        |""".trimMargin())
  }

  @Test fun annotationWithNestedTypes() {
    val annotationName = ClassName(tacosPackage, "TacoDelivery")
    val kindName = annotationName.nestedClass("Kind")
    val annotation = TypeSpec.annotationBuilder(annotationName)
            .addModifiers(PUBLIC)
            .primaryConstructor(FunSpec.constructorBuilder()
                    .addParameter(ParameterSpec.builder("kind", kindName)
                            .build())
                    .addParameter(ParameterSpec.builder("quantity", Int::class)
                            .defaultValue("QUANTITY_DEFAULT")
                            .build())
                    .build())
            .addProperty(PropertySpec.builder("kind", kindName)
                    .initializer("kind")
                    .build())
            .addProperty(PropertySpec.builder("quantity", Int::class)
                    .initializer("quantity")
                    .build())
            .addType(TypeSpec.enumBuilder("Kind")
                    .addEnumConstant("SOFT")
                    .addEnumConstant("HARD")
                    .build())
            .addType(TypeSpec.companionObjectBuilder()
                    .addProperty(PropertySpec
                            .builder("QUANTITY_DEFAULT", Int::class, KModifier.CONST)
                            .initializer("%L", 10_000)
                            .build())
                    .build())
            .build()

    assertThat(toString(annotation)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |public annotation class TacoDelivery(
        |  public val kind: Kind,
        |  public val quantity: Int = QUANTITY_DEFAULT
        |) {
        |  public enum class Kind {
        |    SOFT,
        |    HARD,
        |  }
        |
        |  public companion object {
        |    public const val QUANTITY_DEFAULT: Int = 10000
        |  }
        |}
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
        |public interface Taco {
        |  public val v: Int
        |}
        |""".trimMargin())
  }

  @Test fun expectClass() {
    val classA = TypeSpec.expectClassBuilder("ClassA")
        .addFunction(FunSpec.builder("test")
            .build())
        .build()

    assertThat(classA.toString()).isEqualTo("""
      |public expect class ClassA {
      |  public fun test(): kotlin.Unit
      |}
      |""".trimMargin())
  }

  @Test fun nestedExpectCompanionObjectWithFunction() {
    val classA = TypeSpec.expectClassBuilder("ClassA")
        .addType(TypeSpec.companionObjectBuilder()
            .addFunction(FunSpec.builder("test")
                .build())
            .build())
        .build()

    assertThat(classA.toString()).isEqualTo("""
      |public expect class ClassA {
      |  public companion object {
      |    public fun test(): kotlin.Unit
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun nestedExpectClassWithFunction() {
    val classA = TypeSpec.expectClassBuilder("ClassA")
        .addType(TypeSpec.classBuilder("ClassB")
            .addFunction(FunSpec.builder("test")
                .build())
            .build())
        .build()

    assertThat(classA.toString()).isEqualTo("""
      |public expect class ClassA {
      |  public class ClassB {
      |    public fun test(): kotlin.Unit
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun deeplyNestedExpectClassWithFunction() {
    val classA = TypeSpec.expectClassBuilder("ClassA")
        .addType(TypeSpec.classBuilder("ClassB")
            .addType(TypeSpec.classBuilder("ClassC")
                .addFunction(FunSpec.builder("test")
                    .build())
                .build())
            .build())
        .build()

    assertThat(classA.toString()).isEqualTo("""
      |public expect class ClassA {
      |  public class ClassB {
      |    public class ClassC {
      |      public fun test(): kotlin.Unit
      |    }
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun veryDeeplyNestedExpectClassWithFunction() {
    val classA = TypeSpec.expectClassBuilder("ClassA")
        .addType(TypeSpec.classBuilder("ClassB")
            .addType(TypeSpec.classBuilder("ClassC")
                .addType(TypeSpec.classBuilder("ClassD")
                    .addFunction(FunSpec.builder("test")
                        .build())
                    .build())
                .build())
            .build())
        .build()

    assertThat(classA.toString()).isEqualTo("""
      |public expect class ClassA {
      |  public class ClassB {
      |    public class ClassC {
      |      public class ClassD {
      |        public fun test(): kotlin.Unit
      |      }
      |    }
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun deeplyNestedExpectClassWithConstructor() {
    val classA = TypeSpec.expectClassBuilder("ClassA")
        .addType(TypeSpec.classBuilder("ClassB")
            .addType(TypeSpec.classBuilder("ClassC")
                .addFunction(FunSpec.constructorBuilder()
                    .addStatement("Unit")
                    .build())
                .build())
            .build())
        .build()

    assertThat(classA.toString()).isEqualTo("""
      |public expect class ClassA {
      |  public class ClassB {
      |    public class ClassC {
      |      public constructor()
      |    }
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun veryDeeplyNestedExpectClassWithConstructor() {
    val classA = TypeSpec.expectClassBuilder("ClassA")
        .addType(TypeSpec.classBuilder("ClassB")
            .addType(TypeSpec.classBuilder("ClassC")
                .addType(TypeSpec.classBuilder("ClassD")
                    .addFunction(FunSpec.constructorBuilder()
                        .addStatement("Unit")
                        .build())
                    .build())
                .build())
            .build())
        .build()

    assertThat(classA.toString()).isEqualTo("""
      |public expect class ClassA {
      |  public class ClassB {
      |    public class ClassC {
      |      public class ClassD {
      |        public constructor()
      |      }
      |    }
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun interfaceWithMethods() {
    val taco = TypeSpec.interfaceBuilder("Taco")
        .addFunction(FunSpec.builder("aMethod")
            .addModifiers(KModifier.ABSTRACT)
            .build())
        .addFunction(FunSpec.builder("aDefaultMethod").build())
        .addFunction(FunSpec.builder("aPrivateMethod")
            .addModifiers(KModifier.PRIVATE)
            .build())
        .build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Unit
        |
        |public interface Taco {
        |  public fun aMethod(): Unit
        |
        |  public fun aDefaultMethod(): Unit {
        |  }
        |
        |  private fun aPrivateMethod(): Unit {
        |  }
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
        |public class Top {
        |  public val internalTop: Top
        |
        |  public val internalBottom: Middle.Bottom
        |
        |  public val externalTop: com.squareup.donuts.Top
        |
        |  public val externalBottom: Bottom
        |
        |  public class Middle {
        |    public val internalTop: Top
        |
        |    public val internalBottom: Bottom
        |
        |    public val externalTop: com.squareup.donuts.Top
        |
        |    public val externalBottom: com.squareup.donuts.Bottom
        |
        |    public class Bottom {
        |      public val internalTop: Top
        |
        |      public val internalBottom: Bottom
        |
        |      public val externalTop: com.squareup.donuts.Top
        |
        |      public val externalBottom: com.squareup.donuts.Bottom
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
        |public class Gen {
        |  public val internalOther: Other
        |
        |  public val externalOther: com.squareup.donuts.Other
        |}
        |""".trimMargin())
  }

  @Test fun intersectionType() {
    val typeVariable = TypeVariableName("T", Comparator::class, Serializable::class)
    val taco = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("getComparator")
            .addTypeVariable(typeVariable)
            .returns(typeVariable)
            .addStatement("return null")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.io.Serializable
        |import java.util.Comparator
        |
        |public class Taco {
        |  public fun <T> getComparator(): T where T : Comparator, T : Serializable = null
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
        |public class Taco {
        |  public val ints: IntArray
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
        .addFunction(FunSpec.builder("refold")
            .addKdoc("Folds the back of this taco to reduce sauce leakage.\n" +
                "\n" +
                "For [%T#KOREAN], the front may also be folded.\n", Locale::class)
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
        |import kotlin.Unit
        |
        |/**
        | * A hard or soft tortilla, loosely folded and filled with whatever
        | * [random][java.util.Random] tex-mex stuff we could find in the pantry
        | * and some [kotlin.String] cheese.
        | */
        |public class Taco {
        |  /**
        |   * True for a soft flour tortilla; false for a crunchy corn tortilla.
        |   */
        |  public val soft: Boolean
        |
        |  /**
        |   * Folds the back of this taco to reduce sauce leakage.
        |   *
        |   * For [Locale#KOREAN], the front may also be folded.
        |   */
        |  public fun refold(locale: Locale): Unit {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun kdocWithParameters() {
    val taco = TypeSpec.classBuilder("Taco")
        .addKdoc("A hard or soft tortilla, loosely folded and filled with whatever\n")
        .addKdoc("[random][%T] tex-mex stuff we could find in the pantry\n", Random::class)
        .addKdoc(CodeBlock.of("and some [%T] cheese.\n", String::class))
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("temperature", Double::class)
                .addKdoc(CodeBlock.of("%L", """
                    |Taco temperature. Can be as cold as the famous ice tacos from
                    |the Andes, or hot with lava-like cheese from the depths of
                    |the Ninth Circle.
                    |""".trimMargin()))
                .build())
            .addParameter("soft", Boolean::class)
            .addParameter(ParameterSpec.builder("mild", Boolean::class)
                .addKdoc(CodeBlock.of("%L", "Whether the taco is mild (ew) or crunchy (ye).\n"))
                .build())
            .addParameter("nodoc", Int::class)
            .build())
        .addProperty(PropertySpec.builder("soft", Boolean::class)
            .addKdoc("True for a soft flour tortilla; false for a crunchy corn tortilla.\n")
            .initializer("soft")
            .build())
        .addProperty(PropertySpec.builder("mild", Boolean::class)
            .addKdoc("No one likes mild tacos.")
            .initializer("mild")
            .build())
        .addProperty(PropertySpec.builder("nodoc", Int::class, KModifier.PRIVATE)
            .initializer("nodoc")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Boolean
        |import kotlin.Double
        |import kotlin.Int
        |
        |/**
        | * A hard or soft tortilla, loosely folded and filled with whatever
        | * [random][java.util.Random] tex-mex stuff we could find in the pantry
        | * and some [kotlin.String] cheese.
        | *
        | * @param mild Whether the taco is mild (ew) or crunchy (ye).
        | */
        |public class Taco(
        |  /**
        |   * Taco temperature. Can be as cold as the famous ice tacos from
        |   * the Andes, or hot with lava-like cheese from the depths of
        |   * the Ninth Circle.
        |   */
        |  temperature: Double,
        |  /**
        |   * True for a soft flour tortilla; false for a crunchy corn tortilla.
        |   */
        |  public val soft: Boolean,
        |  /**
        |   * No one likes mild tacos.
        |   */
        |  public val mild: Boolean,
        |  private val nodoc: Int
        |)
        |""".trimMargin())
  }

  @Test fun annotationsInAnnotations() {
    val beef = ClassName(tacosPackage, "Beef")
    val chicken = ClassName(tacosPackage, "Chicken")
    val option = ClassName(tacosPackage, "Option")
    val mealDeal = ClassName(tacosPackage, "MealDeal")
    val menu = TypeSpec.classBuilder("Menu")
        .addAnnotation(AnnotationSpec.builder(mealDeal)
            .addMember("%L = %L", "price", 500)
            .addMember("%L = [%L, %L]", "options",
                AnnotationSpec.builder(option)
                    .addMember("%S", "taco")
                    .addMember("%T::class", beef)
                    .build(),
                AnnotationSpec.builder(option)
                    .addMember("%S", "quesadilla")
                    .addMember("%T::class", chicken)
                    .build())
            .build())
        .build()
    assertThat(toString(menu)).isEqualTo("""
        |package com.squareup.tacos
        |
        |@MealDeal(
        |  price = 500,
        |  options = [Option("taco", Beef::class), Option("quesadilla", Chicken::class)]
        |)
        |public class Menu
        |""".trimMargin())
  }

  @Test fun varargs() {
    val taqueria = TypeSpec.classBuilder("Taqueria")
        .addFunction(FunSpec.builder("prepare")
            .addParameter("workers", Int::class)
            .addParameter("jobs", Runnable::class.asClassName(), VARARG)
            .build())
        .build()
    assertThat(toString(taqueria)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.Runnable
        |import kotlin.Int
        |import kotlin.Unit
        |
        |public class Taqueria {
        |  public fun prepare(workers: Int, vararg jobs: Runnable): Unit {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun varargsNotLast() {
    val taqueria = TypeSpec.classBuilder("Taqueria")
        .addFunction(FunSpec.builder("prepare")
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
        |import kotlin.Unit
        |
        |public class Taqueria {
        |  public fun prepare(
        |    workers: Int,
        |    vararg jobs: Runnable,
        |    start: Boolean
        |  ): Unit {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun codeBlocks() {
    val ifBlock = CodeBlock.builder()
        .beginControlFlow("if (a != b)")
        .addStatement("return i")
        .endControlFlow()
        .build()
    val funBody = CodeBlock.builder()
        .addStatement("val size = %T.min(listA.size, listB.size)", Math::class)
        .beginControlFlow("for (i in 0 until size)")
        .addStatement("val %N = %N[i]", "a", "listA")
        .addStatement("val %N = %N[i]", "b", "listB")
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
    val escapeHtml = PropertySpec.builder("ESCAPE_HTML",
        Map::class.parameterizedBy(String::class, String::class))
        .addModifiers(KModifier.PRIVATE)
        .initializer(propertyBlock)
        .build()
    val util = TypeSpec.classBuilder("Util")
        .addProperty(escapeHtml)
        .addFunction(FunSpec.builder("commonPrefixLength")
            .returns(Int::class)
            .addParameter("listA", List::class.parameterizedBy(String::class))
            .addParameter("listB", List::class.parameterizedBy(String::class))
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
        |public class Util {
        |  private val ESCAPE_HTML: Map<String, String> = ImmutableMap.<String, String>builder()
        |      .add("'", "&#39;")
        |      .add("&", "&amp;")
        |      .add("<", "&lt;")
        |      .add(">", "&gt;")
        |      .build()
        |
        |  public fun commonPrefixLength(listA: List<String>, listB: List<String>): Int {
        |    val size = Math.min(listA.size, listB.size)
        |    for (i in 0 until size) {
        |      val a = listA[i]
        |      val b = listB[i]
        |      if (a != b) {
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
        .addFunction(FunSpec.builder("choices")
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
        |import kotlin.Unit
        |
        |public class Taco {
        |  public fun choices(): Unit {
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
        .addFunction(FunSpec.builder("choices")
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
        |import kotlin.Unit
        |
        |public class Taco {
        |  public fun choices(): Unit {
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
        .addFunction(FunSpec.builder("inlineIndent")
            .addCode("if (3 < 4) {\n⇥%T.out.println(%S);\n⇤}\n", System::class, "hello")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.lang.System
        |import kotlin.Unit
        |
        |public class Taco {
        |  public fun inlineIndent(): Unit {
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
        |public class Taco {
        |  public class Meat
        |
        |  public interface Tortilla
        |
        |  public enum class Topping {
        |    SALSA,
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
        .addFunction(FunSpec.builder("T").build())
        .addFunction(FunSpec.builder("S").build())
        .addFunction(FunSpec.builder("R").build())
        .addFunction(FunSpec.builder("Q").build())
        .addFunction(FunSpec.constructorBuilder()
            .addParameter("p", Int::class)
            .build())
        .addFunction(FunSpec.constructorBuilder()
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
        |import kotlin.Unit
        |
        |public class Members {
        |  public val W: String
        |
        |  public val U: String
        |
        |  public constructor(p: Int)
        |
        |  public constructor(o: Long)
        |
        |  public fun T(): Unit {
        |  }
        |
        |  public fun S(): Unit {
        |  }
        |
        |  public fun R(): Unit {
        |  }
        |
        |  public fun Q(): Unit {
        |  }
        |
        |  public class Z
        |
        |  public class Y
        |}
        |""".trimMargin())
  }

  @Test fun nativeFunctions() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("nativeInt")
            .addModifiers(KModifier.EXTERNAL)
            .returns(Int::class)
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |public class Taco {
        |  public external fun nativeInt(): Int
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
        |public class Taco {
        |  public val NULL: String = null
        |}
        |""".trimMargin())
  }

  @Test fun annotationToString() {
    val annotation = AnnotationSpec.builder(SuppressWarnings::class)
        .addMember("%S", "unused")
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
        .isEqualTo("public override fun toString(): kotlin.String = \"taco\"\n")
  }

  @Test fun constructorToString() {
    val constructor = FunSpec.constructorBuilder()
        .addModifiers(KModifier.PUBLIC)
        .addParameter("taco", ClassName(tacosPackage, "Taco"))
        .addStatement("this.%N = %N", "taco", "taco")
        .build()
    assertThat(constructor.toString()).isEqualTo("" +
        "public constructor(taco: com.squareup.tacos.Taco) {\n" +
        "  this.taco = taco\n" +
        "}\n")
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
    assertThat(type.toString()).isEqualTo("" +
        "public class Taco\n")
  }

  @Test fun anonymousClassToString() {
    val type = TypeSpec.anonymousClassBuilder()
        .addSuperinterface(Runnable::class)
        .addFunction(FunSpec.builder("run")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .build())
        .build()
    assertThat(type.toString()).isEqualTo("""
        |object : java.lang.Runnable {
        |  public override fun run(): kotlin.Unit {
        |  }
        |}""".trimMargin())
  }

  @Test fun interfaceClassToString() {
    val type = TypeSpec.interfaceBuilder("Taco")
        .build()
    assertThat(type.toString()).isEqualTo("""
        |public interface Taco
        |""".trimMargin())
  }

  @Test fun annotationDeclarationToString() {
    val type = TypeSpec.annotationBuilder("Taco")
        .build()
    assertThat(type.toString()).isEqualTo("""
        |public annotation class Taco
        |""".trimMargin())
  }

  private fun toString(typeSpec: TypeSpec): String {
    return FileSpec.get(tacosPackage, typeSpec).toString()
  }

  @Test fun multilineStatement() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("toString")
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
        |public class Taco {
        |  public override fun toString(): String {
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
    val stringComparator = Comparator::class.parameterizedBy(String::class)
    val listOfString = List::class.parameterizedBy(String::class)
    val prefixComparator = TypeSpec.anonymousClassBuilder()
        .addSuperinterface(stringComparator)
        .addFunction(FunSpec.builder("compare")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(Int::class)
            .addParameter("a", String::class)
            .addParameter("b", String::class)
            .addComment("Prefix the strings and compare them")
            .addStatement("return a.substring(0, length)\n" + ".compareTo(b.substring(0, length))")
            .build())
        .build()
    val taco = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("comparePrefix")
            .returns(stringComparator)
            .addParameter("length", Int::class, KModifier.FINAL)
            .addComment("Return a new comparator for the target length.")
            .addStatement("return %L", prefixComparator)
            .build())
        .addFunction(FunSpec.builder("sortPrefix")
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
        |import kotlin.Unit
        |import kotlin.collections.List
        |
        |public class Taco {
        |  public fun comparePrefix(final length: Int): Comparator<String> {
        |    // Return a new comparator for the target length.
        |    return object : Comparator<String> {
        |      public override fun compare(a: String, b: String): Int {
        |        // Prefix the strings and compare them
        |        return a.substring(0, length)
        |            .compareTo(b.substring(0, length))
        |      }
        |    }
        |  }
        |
        |  public fun sortPrefix(list: List<String>, final length: Int): Unit {
        |    Collections.sort(
        |        list,
        |        object : Comparator<String> {
        |          public override fun compare(a: String, b: String): Int {
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
        |public class Taco {
        |  public val toppings: String = ${"\"\"\""}
        |      |shell
        |      |beef
        |      |lettuce
        |      |cheese
        |      |${"\"\"\""}.trimMargin()
        |}
        |""".trimMargin())
  }

  @Test fun validInlineClass() {
    val guacamole = TypeSpec.classBuilder("Guacamole")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("avacado", String::class)
            .build())
        .addProperty(PropertySpec.builder("avacado", String::class)
            .initializer("avacado")
            .build())
        .addModifiers(INLINE)
        .build()

    assertThat(guacamole.toString()).isEqualTo("""
      |public inline class Guacamole(
      |  public val avacado: kotlin.String
      |)
      |""".trimMargin())
  }

  @Test fun inlineClassWithInitBlock() {
    assertThrows<IllegalStateException> {
      TypeSpec.classBuilder("Guacamole")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("avacado", String::class)
              .build())
          .addProperty(PropertySpec.builder("avacado", String::class)
              .initializer("avacado")
              .build())
          .addInitializerBlock(CodeBlock.EMPTY)
          .addModifiers(INLINE)
          .build()
    }.hasMessageThat().isEqualTo("Inline classes can't have initializer blocks")
  }

  class InlineSuperClass

  @Test fun inlineClassWithSuperClass() {
    assertThrows<IllegalStateException> {
      TypeSpec.classBuilder("Guacamole")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("avocado", String::class)
              .build())
          .addProperty(PropertySpec.builder("avocado", String::class)
              .initializer("avocado")
              .build())
          .superclass(InlineSuperClass::class)
          .addModifiers(INLINE)
          .build()
    }.hasMessageThat().isEqualTo("Inline classes cannot have super classes")
  }

  interface InlineSuperInterface

  @Test fun inlineClassInheritsFromInterface() {
    val guacamole = TypeSpec.classBuilder("Guacamole")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("avocado", String::class)
            .build())
        .addProperty(PropertySpec.builder("avocado", String::class)
            .initializer("avocado")
            .build())
        .addSuperinterface(InlineSuperInterface::class)
        .addModifiers(INLINE)
        .build()

    assertThat(guacamole.toString()).isEqualTo("""
      |public inline class Guacamole(
      |  public val avocado: kotlin.String
      |) : com.squareup.kotlinpoet.TypeSpecTest.InlineSuperInterface
      |""".trimMargin())
  }

  @Test fun inlineClassWithoutBackingProperty() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.classBuilder("Guacamole")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("avocado", String::class)
              .build())
          .addProperty("garlic", String::class)
          .addModifiers(INLINE)
          .build()
    }.hasMessageThat().isEqualTo("Inline classes must have a single read-only (val) property parameter.")
  }

  @Test fun inlineClassWithoutProperties() {
    assertThrows<IllegalStateException> {
      TypeSpec.classBuilder("Guacamole")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("avocado", String::class)
              .build())
          .addModifiers(INLINE)
          .build()
    }.hasMessageThat().isEqualTo("Inline classes must have at least 1 property")
  }

  @Test fun inlineClassWithMutableProperties() {
    assertThrows<IllegalStateException> {
      TypeSpec.classBuilder("Guacamole")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("avocado", String::class)
              .build())
          .addProperty(PropertySpec.builder("avocado", String::class)
              .initializer("avocado")
              .mutable()
              .build())
          .addModifiers(INLINE)
          .build()
    }.hasMessageThat().isEqualTo("Inline classes must have a single read-only (val) property parameter.")
  }

  @Test fun inlineClassWithPrivateConstructor() {
    assertThrows<IllegalStateException> {
      TypeSpec.classBuilder("Guacamole")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("avocado", String::class)
              .addModifiers(PRIVATE)
              .build())
          .addProperty(PropertySpec.builder("avocado", String::class)
              .initializer("avocado")
              .build())
          .addModifiers(INLINE)
          .build()
    }.hasMessageThat().isEqualTo("Inline classes must have a public primary constructor")
  }

  @Test fun inlineEnumClass() {
    val guacamole = TypeSpec.enumBuilder("Foo")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("x", Int::class)
            .build())
        .addEnumConstant("A", TypeSpec.anonymousClassBuilder()
            .addSuperclassConstructorParameter("%L", 1)
            .build())
        .addEnumConstant("B", TypeSpec.anonymousClassBuilder()
            .addSuperclassConstructorParameter("%L", 2)
            .build())
        .addProperty(PropertySpec.builder("x", Int::class)
            .initializer("x")
            .build())
        .addModifiers(INLINE)
        .build()
    assertThat(guacamole.toString()).isEqualTo("""
      |public enum inline class Foo(
      |  public val x: kotlin.Int
      |) {
      |  A(1),
      |  B(2),
      |  ;
      |}
      |""".trimMargin())
  }

  @Test fun doublePropertyInitialization() {
    assertThrows<IllegalStateException> {
      PropertySpec.builder("listA", String::class)
          .initializer("foo")
          .initializer("bar")
          .build()
    }

    assertThrows<IllegalStateException> {
      PropertySpec.builder("listA", String::class)
          .initializer(CodeBlock.builder().add("foo").build())
          .initializer(CodeBlock.builder().add("bar").build())
          .build()
    }
  }

  @Test fun multipleAnnotationAddition() {
    val taco = TypeSpec.classBuilder("Taco")
        .addAnnotations(Arrays.asList(
            AnnotationSpec.builder(SuppressWarnings::class)
                .addMember("%S", "unchecked")
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
        |public class Taco
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
        |public class Taco {
        |  public const val ANSWER: Int
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
        |public class Taco {
        |  public fun getAnswer(): Int = 42
        |
        |  /**
        |   * chosen by fair dice roll ;)
        |   */
        |  public fun getRandomQuantity(): Int = 4
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
        |public class Taco : Serializable, EventListener
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
        |public class Location<T, P : Number>
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
        |public class Taco {
        |  public class Topping
        |
        |  public class Sauce
        |}
        |""".trimMargin())
  }

  @Test fun tryCatch() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("addTopping")
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
        |import kotlin.Unit
        |
        |public class Taco {
        |  public fun addTopping(topping: Topping): Unit {
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
        .addFunction(FunSpec.builder("isDelicious")
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
        |public class Taco {
        |  public fun isDelicious(count: Int): Boolean {
        |    if (count > 0) {
        |      return true
        |    } else {
        |      return false
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun whenReturn() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("toppingPrice")
            .addParameter("topping", String::class)
            .beginControlFlow("return when(topping)")
            .addStatement("%S -> 1", "beef")
            .addStatement("%S -> 2", "lettuce")
            .addStatement("%S -> 3", "cheese")
            .addStatement("else -> throw IllegalToppingException(topping)")
            .endControlFlow()
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco {
        |  public fun toppingPrice(topping: String) = when(topping) {
        |    "beef" -> 1
        |    "lettuce" -> 2
        |    "cheese" -> 3
        |    else -> throw IllegalToppingException(topping)
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
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%N", String::class)
    }.hasMessageThat().isEqualTo("expected name but was " + String::class)
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
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%T", "kotlin.String")
    }.hasMessageThat().isEqualTo("expected type but was kotlin.String")
  }

  @Test fun tooFewArguments() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%S")
    }.hasMessageThat().isEqualTo("index 1 for '%S' not in range (received 0 arguments)")
  }

  @Test fun unusedArgumentsRelative() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%L %L", "a", "b", "c")
    }.hasMessageThat().isEqualTo("unused arguments: expected 2, received 3")
  }

  @Test fun unusedArgumentsIndexed() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1L %2L", "a", "b", "c")
    }.hasMessageThat().isEqualTo("unused argument: %3")

    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1L %1L %1L", "a", "b", "c")
    }.hasMessageThat().isEqualTo("unused arguments: %2, %3")

    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%3L %1L %3L %1L %3L", "a", "b", "c", "d")
    }.hasMessageThat().isEqualTo("unused arguments: %2, %4")
  }

  @Test fun superClassOnlyValidForClasses() {
    assertThrows<IllegalStateException> {
      TypeSpec.annotationBuilder("A").superclass(Any::class.asClassName())
    }

    assertThrows<IllegalStateException> {
      TypeSpec.enumBuilder("E").superclass(Any::class.asClassName())
    }

    assertThrows<IllegalStateException> {
      TypeSpec.interfaceBuilder("I").superclass(Any::class.asClassName())
    }
  }

  @Test fun superClassConstructorParametersOnlyValidForClasses() {
    assertThrows<IllegalStateException> {
      TypeSpec.annotationBuilder("A").addSuperclassConstructorParameter("")
    }

    assertThrows<IllegalStateException> {
      TypeSpec.enumBuilder("E").addSuperclassConstructorParameter("")
    }

    assertThrows<IllegalStateException> {
      TypeSpec.interfaceBuilder("I").addSuperclassConstructorParameter("")
    }
  }

  @Test fun anonymousClassesCannotHaveModifiersOrTypeVariable() {
    assertThrows<IllegalStateException> {
      TypeSpec.anonymousClassBuilder().addModifiers(PUBLIC)
    }

    assertThrows<IllegalStateException> {
      TypeSpec.anonymousClassBuilder().addTypeVariable(TypeVariableName("T")).build()
    }

    assertThrows<IllegalStateException> {
      TypeSpec.anonymousClassBuilder().addTypeVariables(listOf(TypeVariableName("T"))).build()
    }
  }

  @Test fun invalidSuperClass() {
    assertThrows<IllegalStateException> {
      TypeSpec.classBuilder("foo")
          .superclass(List::class)
          .superclass(Map::class)
    }
  }

  @Test fun staticCodeBlock() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty("foo", String::class, KModifier.PRIVATE)
        .addProperty(PropertySpec.builder("FOO", String::class, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", "FOO")
            .build())
        .addFunction(FunSpec.builder("toString")
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
        |public class Taco {
        |  private val foo: String
        |
        |  private const val FOO: String = "FOO"
        |
        |  public override fun toString(): String = FOO
        |}
        |""".trimMargin())
  }

  @Test fun initializerBlockInRightPlace() {
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty("foo", String::class, KModifier.PRIVATE)
        .addProperty(PropertySpec.builder("FOO", String::class, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", "FOO")
            .build())
        .addFunction(FunSpec.constructorBuilder().build())
        .addFunction(FunSpec.builder("toString")
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
        |public class Taco {
        |  private val foo: String
        |
        |  private const val FOO: String = "FOO"
        |
        |  init {
        |    foo = "FOO"
        |  }
        |
        |  public constructor()
        |
        |  public override fun toString(): String = FOO
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
        .addFunction(FunSpec.constructorBuilder().build())
        .addFunction(FunSpec.builder("toString")
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
        |public class Taco {
        |  private val foo: String
        |
        |  private const val FOO: String = "FOO"
        |
        |  init {
        |    foo = "instanceFoo"
        |  }
        |
        |  public constructor()
        |
        |  public override fun toString(): String = FOO
        |}
        |""".trimMargin())
  }

  @Test fun generalToBuilderEqualityTest() {
    val originatingElement = FakeElement()
    val comprehensiveTaco = TypeSpec.classBuilder("Taco")
        .addKdoc("SuperTaco")
        .addAnnotation(SuppressWarnings::class)
        .addModifiers(DATA)
        .addTypeVariable(TypeVariableName.of("State", listOf(ANY), IN).copy(reified = true))
        .addType(TypeSpec.companionObjectBuilder()
            .build())
        .addType(TypeSpec.classBuilder("InnerTaco")
            .addModifiers(INNER)
            .build())
        .primaryConstructor(FunSpec.constructorBuilder()
            .build())
        .superclass(ClassName("texmexfood", "TortillaBased"))
        .addSuperclassConstructorParameter("true")
        .addProperty(PropertySpec.builder("meat", ClassName("texmexfood", "Meat"))
            .build())
        .addFunction(FunSpec.builder("fold")
            .build())
        .addSuperinterface(ClassName("texmexfood", "Consumable"))
        .addOriginatingElement(originatingElement)
        .build()

    val newTaco = comprehensiveTaco.toBuilder().build()
    assertThat(newTaco).isEqualTo(comprehensiveTaco)
    assertThat(newTaco.originatingElements).containsExactly(originatingElement)
  }

  @Test fun generalEnumToBuilderEqualityTest() {
    val bestTexMexEnum = TypeSpec.enumBuilder("BestTexMex")
        .addEnumConstant("TACO")
        .addEnumConstant("BREAKFAST_TACO")
        .build()

    assertThat(bestTexMexEnum.toBuilder().build()).isEqualTo(bestTexMexEnum)
  }

  @Test fun generalInterfaceBuilderEqualityTest() {
    val taco = TypeSpec.interfaceBuilder("Taco")
        .addProperty("isVegan", Boolean::class)
        .addSuperinterface(Runnable::class)
        .build()
    assertThat(taco.toBuilder().build()).isEqualTo(taco)
  }

  @Test fun generalAnnotationBuilderEqualityTest() {
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
    assertThat(annotation.toBuilder().build()).isEqualTo(annotation)
  }

  @Test fun generalExpectClassBuilderEqualityTest() {
    val expectSpec = TypeSpec.expectClassBuilder("AtmoicRef")
        .addModifiers(KModifier.INTERNAL)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("value", Int::class)
            .build())
        .addProperty(PropertySpec.builder("value", Int::class).build())
        .addFunction(FunSpec.builder("get")
            .returns(Int::class)
            .build())
        .build()
    assertThat(expectSpec.toBuilder().build()).isEqualTo(expectSpec)
  }

  @Test fun generalObjectBuilderEqualityTest() {
    val objectSpec = TypeSpec.objectBuilder("MyObject")
        .addModifiers(KModifier.PUBLIC)
        .addProperty("tacos", Int::class)
        .addInitializerBlock(CodeBlock.builder().build())
        .addFunction(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()
    assertThat(objectSpec.toBuilder().build()).isEqualTo(objectSpec)
  }

  @Test fun generalAnonymousClassBuilderEqualityTest() {
    val anonObjectSpec = TypeSpec.anonymousClassBuilder()
        .addSuperinterface(Runnable::class)
        .addFunction(FunSpec.builder("run")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .build())
        .build()
    assertThat(anonObjectSpec.toBuilder().build()).isEqualTo(anonObjectSpec)
  }

  @Test fun initializerBlockUnsupportedExceptionOnInterface() {
    val interfaceBuilder = TypeSpec.interfaceBuilder("Taco")
    assertThrows<IllegalStateException> {
      interfaceBuilder.addInitializerBlock(CodeBlock.builder().build())
    }
  }

  @Test fun initializerBlockUnsupportedExceptionOnAnnotation() {
    val annotationBuilder = TypeSpec.annotationBuilder("Taco")
    assertThrows<IllegalStateException> {
      annotationBuilder.addInitializerBlock(CodeBlock.builder().build())
    }
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
        .addFunction(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.Unit
        |
        |public object MyObject {
        |  public val tacos: Int
        |
        |  init {
        |  }
        |
        |  public fun test(): Unit {
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
        .addFunction(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.wire.Message
        |import kotlin.Unit
        |
        |public object MyObject : Message() {
        |  init {
        |  }
        |
        |  public fun test(): Unit {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObject() {
    val companion = TypeSpec.companionObjectBuilder()
        .addProperty(PropertySpec.builder("tacos", Int::class)
            .initializer("%L", 42)
            .build())
        .addFunction(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val type = TypeSpec.classBuilder("MyClass")
        .addModifiers(KModifier.PUBLIC)
        .addType(companion)
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.Unit
        |
        |public class MyClass {
        |  public companion object {
        |    public val tacos: Int = 42
        |
        |    public fun test(): Unit {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObjectWithInitializer() {
    val companion = TypeSpec.companionObjectBuilder()
            .addProperty(PropertySpec.builder("tacos", Int::class)
                    .mutable()
                    .initializer("%L", 24)
                    .build())
            .addInitializerBlock(CodeBlock.builder()
                    .addStatement("tacos = %L", 42)
                    .build())
            .build()

    val type = TypeSpec.classBuilder("MyClass")
            .addModifiers(KModifier.PUBLIC)
            .addType(companion)
            .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |public class MyClass {
        |  public companion object {
        |    public var tacos: Int = 24
        |
        |    init {
        |      tacos = 42
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObjectWithName() {
    val companion = TypeSpec.companionObjectBuilder("Factory")
        .addFunction(FunSpec.builder("tacos").build())
        .build()

    val type = TypeSpec.classBuilder("MyClass")
        .addType(companion)
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Unit
        |
        |public class MyClass {
        |  public companion object Factory {
        |    public fun tacos(): Unit {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObjectOnInterface() {
    val companion = TypeSpec.companionObjectBuilder()
        .addFunction(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val type = TypeSpec.interfaceBuilder("MyInterface")
        .addModifiers(KModifier.PUBLIC)
        .addType(companion)
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Unit
        |
        |public interface MyInterface {
        |  public companion object {
        |    public fun test(): Unit {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObjectOnEnum() {
    val companion = TypeSpec.companionObjectBuilder()
        .addFunction(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val enumBuilder = TypeSpec.enumBuilder("MyEnum")
        .addEnumConstant("FOO")
        .addEnumConstant("BAR")
        .addModifiers(KModifier.PUBLIC)
        .addType(companion)
        .build()

    assertThat(toString(enumBuilder)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Unit
        |
        |public enum class MyEnum {
        |  FOO,
        |  BAR,
        |  ;
        |
        |  public companion object {
        |    public fun test(): Unit {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun companionObjectOnObjectNotAllowed() {
    val companion = TypeSpec.companionObjectBuilder()
        .addFunction(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val objectBuilder = TypeSpec.objectBuilder("MyObject")
        .addModifiers(KModifier.PUBLIC)
        .addType(companion)

    assertThrows<IllegalArgumentException> {
      objectBuilder.build()
    }
  }

  @Test fun companionObjectSuper() {
    val superclass = ClassName("com.squareup.wire", "Message")
    val companion = TypeSpec.companionObjectBuilder()
        .superclass(superclass)
        .addFunction(FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .build())
        .build()

    val type = TypeSpec.classBuilder("MyClass")
        .addModifiers(KModifier.PUBLIC)
        .addType(companion)
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.wire.Message
        |import kotlin.Unit
        |
        |public class MyClass {
        |  public companion object : Message() {
        |    public fun test(): Unit {
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
        |public class Taco(
        |  public val a: Int,
        |  public val b: String
        |)
        |""".trimMargin())
  }

  @Test fun propertyWithKdocInPrimaryConstructor() {
    val type = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("a", Int::class)
            .addParameter("b", String::class)
            .build())
        .addProperty(PropertySpec.builder("a", Int::class)
            .initializer("a")
            .addKdoc("KDoc\n")
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
        |public class Taco(
        |  /**
        |   * KDoc
        |   */
        |  public val a: Int,
        |  public val b: String
        |)
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
    |public class Taco @Inject constructor()
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
    |public class Taco internal constructor()
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
    |public class Taco @Inject internal constructor()
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
    |public class Taco @Inject @Named internal constructor()
    |
    """.trimMargin())
  }

  @Test fun importNonNullableProperty() {
    val type = String::class.asTypeName()
    val taco = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("taco", type.copy(nullable = false))
            .initializer("%S", "taco")
            .build())
        .addProperty(PropertySpec.builder("nullTaco", type.copy(nullable = true))
            .initializer("null")
            .build())
        .build()

    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |
        |public class Taco {
        |  public val taco: String = "taco"
        |
        |  public val nullTaco: String? = null
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
        |public class Foo : Bar("foo", 42)
        |""".trimMargin())
  }

  @Test fun superclassConstructorParamsForbiddenForAnnotation() {
    assertThrows<IllegalStateException> {
      TypeSpec.annotationBuilder("Taco")
          .addSuperclassConstructorParameter("%S", "foo")
    }
  }

  @Test fun classExtendsNoPrimaryConstructor() {
    val typeSpec = TypeSpec.classBuilder("IoException")
        .superclass(Exception::class)
        .addFunction(FunSpec.constructorBuilder().build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
      |package com.squareup.tacos
      |
      |import java.lang.Exception
      |
      |public class IoException : Exception {
      |  public constructor()
      |}
      |""".trimMargin())
  }

  @Test fun classExtendsNoPrimaryOrSecondaryConstructor() {
    val typeSpec = TypeSpec.classBuilder("IoException")
        .superclass(Exception::class)
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
      |package com.squareup.tacos
      |
      |import java.lang.Exception
      |
      |public class IoException : Exception()
      |""".trimMargin())
  }

  @Test fun classExtendsNoPrimaryConstructorButSuperclassParams() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.classBuilder("IoException")
          .superclass(Exception::class)
          .addSuperclassConstructorParameter("%S", "hey")
          .addFunction(FunSpec.constructorBuilder().build())
          .build()
    }.hasMessageThat().isEqualTo(
        "types without a primary constructor cannot specify secondary constructors and superclass constructor parameters")
  }

  @Test fun constructorWithDefaultParamValue() {
    val type = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("a", Int::class)
                .defaultValue("1")
                .build())
            .addParameter(ParameterSpec
                .builder("b", String::class.asTypeName().copy(nullable = true))
                .defaultValue("null")
                .build())
            .build())
        .addProperty(PropertySpec.builder("a", Int::class)
            .initializer("a")
            .build())
        .addProperty(PropertySpec.builder("b", String::class.asTypeName().copy(nullable = true))
            .initializer("b")
            .build())
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |import kotlin.String
        |
        |public class Taco(
        |  public val a: Int = 1,
        |  public val b: String? = null
        |)
        |""".trimMargin())
  }

  @Test fun constructorDelegation() {
    val type = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("a", String::class.asTypeName().copy(nullable = true))
            .addParameter("b", String::class.asTypeName().copy(nullable = true))
            .addParameter("c", String::class.asTypeName().copy(nullable = true))
            .build())
        .addProperty(PropertySpec.builder("a", String::class.asTypeName().copy(nullable = true))
            .initializer("a")
            .build())
        .addProperty(PropertySpec.builder("b", String::class.asTypeName().copy(nullable = true))
            .initializer("b")
            .build())
        .addProperty(PropertySpec.builder("c", String::class.asTypeName().copy(nullable = true))
            .initializer("c")
            .build())
        .addFunction(FunSpec.constructorBuilder()
            .addParameter("map", Map::class.parameterizedBy(String::class, String::class))
            .callThisConstructor(
                CodeBlock.of("map[%S]", "a"),
                CodeBlock.of("map[%S]", "b"),
                CodeBlock.of("map[%S]", "c"))
            .build())
        .build()

    assertThat(toString(type)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |import kotlin.collections.Map
        |
        |public class Taco(
        |  public val a: String?,
        |  public val b: String?,
        |  public val c: String?
        |) {
        |  public constructor(map: Map<String, String>) : this(map["a"], map["b"], map["c"])
        |}
        |""".trimMargin())
  }

  @Test fun internalFunForbiddenInInterface() {
    val type = TypeSpec.interfaceBuilder("ITaco")

    assertThrows<IllegalArgumentException> {
      type.addFunction(FunSpec.builder("eat")
          .addModifiers(ABSTRACT, INTERNAL)
          .build())
          .build()
    }.hasMessageThat().isEqualTo("modifiers [ABSTRACT, INTERNAL] must contain none of [INTERNAL, PROTECTED]")

    assertThrows<IllegalArgumentException> {
      type.addFunctions(listOf(FunSpec.builder("eat")
          .addModifiers(ABSTRACT, INTERNAL)
          .build()))
          .build()
    }.hasMessageThat().isEqualTo("modifiers [ABSTRACT, INTERNAL] must contain none of [INTERNAL, PROTECTED]")
  }

  @Test fun privateAbstractFunForbiddenInInterface() {
    val type = TypeSpec.interfaceBuilder("ITaco")

    assertThrows<IllegalArgumentException> {
      type.addFunction(FunSpec.builder("eat")
          .addModifiers(ABSTRACT, PRIVATE)
          .build())
          .build()
    }.hasMessageThat().isEqualTo("modifiers [ABSTRACT, PRIVATE] must contain none or only one of [ABSTRACT, PRIVATE]")

    assertThrows<IllegalArgumentException> {
      type.addFunctions(listOf(FunSpec.builder("eat")
          .addModifiers(ABSTRACT, PRIVATE)
          .build()))
          .build()
    }.hasMessageThat().isEqualTo("modifiers [ABSTRACT, PRIVATE] must contain none or only one of [ABSTRACT, PRIVATE]")
  }

  @Test fun internalFunForbiddenInAnnotation() {
    val type = TypeSpec.annotationBuilder("Taco")

    assertThrows<IllegalArgumentException> {
      type.addFunction(FunSpec.builder("eat")
          .addModifiers(INTERNAL)
          .build())
          .build()
    }.hasMessageThat().isEqualTo("annotation class Taco.eat requires modifiers [PUBLIC, ABSTRACT]")

    assertThrows<IllegalArgumentException> {
      type.addFunctions(listOf(FunSpec.builder("eat")
          .addModifiers(INTERNAL)
          .build()))
          .build()
    }.hasMessageThat().isEqualTo("annotation class Taco.eat requires modifiers [PUBLIC, ABSTRACT]")
  }

  @Test fun classHeaderFormatting() {
    val typeSpec = TypeSpec.classBuilder("Person")
        .addModifiers(KModifier.DATA)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("id", Int::class)
            .addParameter("name", String::class)
            .addParameter("surname", String::class)
            .build())
        .addProperty(PropertySpec.builder("id", Int::class, KModifier.OVERRIDE)
            .initializer("id")
            .build())
        .addProperty(PropertySpec.builder("name", String::class, KModifier.OVERRIDE)
            .initializer("name")
            .build())
        .addProperty(PropertySpec.builder("surname", String::class, KModifier.OVERRIDE)
            .initializer("surname")
            .build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.String
      |
      |public data class Person(
      |  public override val id: Int,
      |  public override val name: String,
      |  public override val surname: String
      |)
      |""".trimMargin())
  }

  @Test
  fun classHeaderAnnotations() {
    val idParameterSpec = ParameterSpec.builder("id", Int::class)
        .addAnnotation(ClassName("com.squareup.kotlinpoet", "Id"))
        .addModifiers(PRIVATE)
        .defaultValue("1")
        .build()

    val typeSpec = TypeSpec.classBuilder("Person")
        .addModifiers(KModifier.DATA)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(idParameterSpec)
            .build())
        .addProperty(PropertySpec.builder("id", Int::class)
            .initializer("id")
            .addAnnotation(ClassName("com.squareup.kotlinpoet", "OrderBy"))
            .build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
      |package com.squareup.tacos
      |
      |import com.squareup.kotlinpoet.Id
      |import com.squareup.kotlinpoet.OrderBy
      |import kotlin.Int
      |
      |public data class Person(
      |  @OrderBy
      |  @Id
      |  private val id: Int = 1
      |)
      |""".trimMargin())
  }

  @Test fun literalPropertySpec() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("shell")
            .addCode(CodeBlock.of("%L", PropertySpec.builder("taco1", String::class.asTypeName())
                .initializer("%S", "Taco!").build()))
            .addCode(CodeBlock.of("%L",
                PropertySpec.builder("taco2", String::class.asTypeName().copy(nullable = true))
                    .initializer("null")
                    .build()))
            .addCode(CodeBlock.of("%L",
                PropertySpec.builder("taco3", String::class.asTypeName(), KModifier.LATEINIT)
                    .mutable()
                    .build()))
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.String
        |import kotlin.Unit
        |
        |public class Taco {
        |  public fun shell(): Unit {
        |    val taco1: String = "Taco!"
        |    val taco2: String? = null
        |    lateinit var taco3: String
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun basicDelegateTest() {
    val type = TypeSpec.classBuilder("Guac")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("somethingElse", String::class)
            .build())
        .addSuperinterface(
            Consumer::class.parameterizedBy(String::class),
            CodeBlock.of("({ println(it) })"))
        .build()

    val expect = """
        |package com.squareup.tacos
        |
        |import java.util.function.Consumer
        |import kotlin.String
        |
        |public class Guac(
        |  somethingElse: String
        |) : Consumer<String> by ({ println(it) })
        |""".trimMargin()

    assertThat(toString(type)).isEqualTo(expect)
  }

  @Test fun testDelegateOnObject() {
    val type = TypeSpec.objectBuilder("Guac")
        .addSuperinterface(
            Consumer::class.parameterizedBy(String::class),
            CodeBlock.of("({ println(it) })"))
        .build()

    val expect = """
        |package com.squareup.tacos
        |
        |import java.util.function.Consumer
        |import kotlin.String
        |
        |public object Guac : Consumer<String> by ({ println(it) })
        |""".trimMargin()

    assertThat(toString(type)).isEqualTo(expect)
  }

  @Test fun testMultipleDelegates() {
    val type = TypeSpec.classBuilder("StringToInteger")
        .primaryConstructor(FunSpec.constructorBuilder()
            .build())
        .addSuperinterface(Function::class.parameterizedBy(String::class, Int::class),
            CodeBlock.of("Function ({ text -> text.toIntOrNull() ?: 0 })"))
        .addSuperinterface(
            Runnable::class,
            CodeBlock.of("Runnable ({ %T.debug(\"Hello world\") })", Logger::class.asTypeName()))
        .build()

    val expect = """
        |package com.squareup.tacos
        |
        |import java.lang.Runnable
        |import kotlin.Function
        |import kotlin.Int
        |import kotlin.String
        |
        |public class StringToInteger : Function<String, Int> by Function ({ text -> text.toIntOrNull() ?: 0
        |    }), Runnable by Runnable ({ java.util.logging.Logger.debug("Hello world") })
        |""".trimMargin()

    assertThat(toString(type)).isEqualTo(expect)
  }

  @Test fun testNoSuchParameterDelegate() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.classBuilder("Taco")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("other", String::class)
              .build())
          .addSuperinterface(KFunction::class, "notOther")
          .build()
    }.hasMessageThat().isEqualTo("no such constructor parameter 'notOther' to delegate to for type 'Taco'")
  }

  @Test fun failAddParamDelegateWhenNullConstructor() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.classBuilder("Taco")
          .addSuperinterface(Runnable::class, "etc")
          .build()
    }.hasMessageThat().isEqualTo("delegating to constructor parameter requires not-null constructor")
  }

  @Test fun testAddedDelegateByParamName() {
    val type = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("superString", Function::class)
            .build())
        .addSuperinterface(Function::class, "superString")
        .build()

    assertThat(toString(type)).isEqualTo("""
          |package com.squareup.tacos
          |
          |import kotlin.Function
          |
          |public class Taco(
          |  superString: Function
          |) : Function by superString
          |""".trimMargin())
  }

  @Test fun failOnAddExistingDelegateType() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.classBuilder("Taco")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("superString", Function::class)
              .build())
          .addSuperinterface(Function::class, CodeBlock.of("{ print(Hello) }"))
          .addSuperinterface(Function::class, "superString")
          .build()
      fail()
    }.hasMessageThat().isEqualTo("'Taco' can not delegate to kotlin.Function " +
        "by superString with existing declaration by { print(Hello) }")
  }

  @Test fun testDelegateIfaceWithOtherParamTypeName() {
    val entity = ClassName(tacosPackage, "Entity")
    val entityBuilder = ClassName(tacosPackage, "EntityBuilder")
    val type = TypeSpec.classBuilder("EntityBuilder")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("argBuilder", ClassName(tacosPackage, "Payload")
                .parameterizedBy(entityBuilder, entity))
                .defaultValue("Payload.create()")
                .build())
            .build())
        .addSuperinterface(ClassName(tacosPackage, "TypeBuilder")
            .parameterizedBy(entityBuilder, entity),
            "argBuilder")
        .build()

    assertThat(toString(type)).isEqualTo("""
          |package com.squareup.tacos
          |
          |public class EntityBuilder(
          |  argBuilder: Payload<EntityBuilder, Entity> = Payload.create()
          |) : TypeBuilder<EntityBuilder, Entity> by argBuilder
          |""".trimMargin())
  }

  @Test fun externalClassFunctionHasNoBody() {
    val typeSpec = TypeSpec.classBuilder("Foo")
        .addModifiers(KModifier.EXTERNAL)
        .addFunction(FunSpec.builder("bar").addModifiers(KModifier.EXTERNAL).build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Unit
      |
      |public external class Foo {
      |  public fun bar(): Unit
      |}
      |""".trimMargin())
  }

  @Test fun externalInterfaceWithMembers() {
    val typeSpec = TypeSpec.interfaceBuilder("Foo")
        .addModifiers(KModifier.EXTERNAL)
        .addProperty(PropertySpec.builder("baz", String::class).addModifiers(KModifier.EXTERNAL).build())
        .addFunction(FunSpec.builder("bar").addModifiers(KModifier.EXTERNAL).build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.Unit
      |
      |public external interface Foo {
      |  public val baz: String
      |
      |  public fun bar(): Unit
      |}
      |""".trimMargin())
  }

  @Test fun externalObjectWithMembers() {
    val typeSpec = TypeSpec.objectBuilder("Foo")
        .addModifiers(KModifier.EXTERNAL)
        .addProperty(PropertySpec.builder("baz", String::class).addModifiers(KModifier.EXTERNAL).build())
        .addFunction(FunSpec.builder("bar").addModifiers(KModifier.EXTERNAL).build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.Unit
      |
      |public external object Foo {
      |  public val baz: String
      |
      |  public fun bar(): Unit
      |}
      |""".trimMargin())
  }

  @Test fun externalClassWithNestedTypes() {
    val typeSpec = TypeSpec.classBuilder("Foo")
        .addModifiers(KModifier.EXTERNAL)
        .addType(TypeSpec.classBuilder("Nested1")
            .addModifiers(KModifier.EXTERNAL)
            .addType(TypeSpec.objectBuilder("Nested2")
                .addModifiers(KModifier.EXTERNAL)
                .addFunction(FunSpec.builder("bar").addModifiers(KModifier.EXTERNAL).build())
                .build())
            .addFunction(FunSpec.builder("baz").addModifiers(KModifier.EXTERNAL).build())
            .build())
        .addType(TypeSpec.companionObjectBuilder()
            .addModifiers(KModifier.EXTERNAL)
            .addFunction(FunSpec.builder("qux").addModifiers(KModifier.EXTERNAL).build())
            .build())
        .build()

    assertThat(toString(typeSpec)).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Unit
      |
      |public external class Foo {
      |  public class Nested1 {
      |    public fun baz(): Unit
      |
      |    public object Nested2 {
      |      public fun bar(): Unit
      |    }
      |  }
      |
      |  public companion object {
      |    public fun qux(): Unit
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun isEnum() {
    val enum = TypeSpec.enumBuilder("Topping")
        .addEnumConstant("CHEESE")
        .build()
    assertThat(enum.isEnum).isTrue()
  }

  @Test fun isAnnotation() {
    val annotation = TypeSpec.annotationBuilder("Taco")
        .build()
    assertThat(annotation.isAnnotation).isTrue()
  }

  @Test fun escapePunctuationInTypeName() {
    assertThat(TypeSpec.classBuilder("With-Hyphen").build().toString()).isEqualTo("""
      |public class `With-Hyphen`
      |
      """.trimMargin())
  }

  @Test fun multipleCompanionObjects() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.classBuilder("Taco")
          .addTypes(listOf(
              TypeSpec.companionObjectBuilder()
                  .build(),
              TypeSpec.companionObjectBuilder()
                  .build()
          ))
          .build()
    }
  }

  @Test fun objectKindIsCompanion() {
    val companionObject = TypeSpec.companionObjectBuilder()
        .build()
    assertThat(companionObject.isCompanion).isTrue()
  }

  @Test fun typeNamesCollision() {
    val sqlTaco = ClassName("java.sql", "Taco")
    val source = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addModifiers(KModifier.DATA)
            .addProperty(PropertySpec.builder("madeFreshDatabaseDate", sqlTaco)
                .initializer("madeFreshDatabaseDate")
                .build())
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("madeFreshDatabaseDate", sqlTaco)
                .addParameter("fooNt", INT)
                .build())
            .addFunction(FunSpec.constructorBuilder()
                .addParameter("anotherTaco", ClassName("com.squareup.tacos", "Taco"))
                .callThisConstructor(CodeBlock.of("%T.defaultInstance(), 0", sqlTaco))
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |
      |public data class Taco(
      |  public val madeFreshDatabaseDate: java.sql.Taco,
      |  fooNt: Int
      |) {
      |  public constructor(anotherTaco: Taco) : this(java.sql.Taco.defaultInstance(), 0)
      |}
      |""".trimMargin())
  }

  @Test fun modifyAnnotations() {
    val builder = TypeSpec.classBuilder("Taco")
        .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
            .addMember("name = %S", "jvmWord")
            .build())

    val javaWord = AnnotationSpec.builder(JvmName::class.asClassName())
        .addMember("name = %S", "javaWord")
        .build()
    builder.annotationSpecs.clear()
    builder.annotationSpecs.add(javaWord)

    assertThat(builder.build().annotationSpecs).containsExactly(javaWord)
  }

  @Test fun modifyTypeVariableNames() {
    val builder = TypeSpec.classBuilder("Taco")
        .addTypeVariable(TypeVariableName("V"))

    val tVar = TypeVariableName("T")
    builder.typeVariables.clear()
    builder.typeVariables.add(tVar)

    assertThat(builder.build().typeVariables).containsExactly(tVar)
  }

  @Test fun modifyFunctions() {
    val builder = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("topping").build())

    val seasoning = FunSpec.builder("seasoning").build()
    builder.funSpecs.clear()
    builder.funSpecs.add(seasoning)

    assertThat(builder.build().funSpecs).containsExactly(seasoning)
  }

  @Test fun modifyTypeSpecs() {
    val builder = TypeSpec.classBuilder("Taco")
        .addType(TypeSpec.classBuilder("Topping").build())

    val seasoning = TypeSpec.classBuilder("Seasoning").build()
    builder.typeSpecs.clear()
    builder.typeSpecs.add(seasoning)

    assertThat(builder.build().typeSpecs).containsExactly(seasoning)
  }

  @Test fun modifySuperinterfaces() {
    val builder = TypeSpec.classBuilder("Taco")
        .addSuperinterface(List::class)

    builder.superinterfaces.clear()
    builder.superinterfaces[Set::class.asTypeName()] = CodeBlock.EMPTY

    assertThat(builder.build().superinterfaces)
        .containsExactlyEntriesIn(mapOf(Set::class.asTypeName() to CodeBlock.EMPTY))
  }

  @Test fun modifyProperties() {
    val builder = TypeSpec.classBuilder("Taco")
        .addProperty(PropertySpec.builder("topping", String::class.asClassName()).build())

    val seasoning = PropertySpec.builder("seasoning", String::class.asClassName()).build()
    builder.propertySpecs.clear()
    builder.propertySpecs.add(seasoning)

    assertThat(builder.build().propertySpecs).containsExactly(seasoning)
  }

  @Test fun modifyEnumConstants() {
    val builder = TypeSpec.enumBuilder("Taco")
        .addEnumConstant("TOPPING")

    builder.enumConstants.clear()
    builder.enumConstants["SEASONING"] = TypeSpec.anonymousClassBuilder().build()

    assertThat(builder.build().enumConstants)
        .containsExactlyEntriesIn(mapOf("SEASONING" to TypeSpec.anonymousClassBuilder().build()))
  }

  @Test fun modifySuperclassConstructorParams() {
    val builder = TypeSpec.classBuilder("Taco")
        .addSuperclassConstructorParameter(CodeBlock.of("seasoning = %S", "mild"))

    val seasoning = CodeBlock.of("seasoning = %S", "spicy")
    builder.superclassConstructorParameters.clear()
    builder.superclassConstructorParameters.add(seasoning)

    assertThat(builder.build().superclassConstructorParameters).containsExactly(seasoning)
  }

  // https://github.com/square/kotlinpoet/issues/565
  @Test fun markerEnum() {
    val spec = TypeSpec.enumBuilder("Topping")
        .build()
    assertThat(spec.toString()).isEqualTo("""
      |public enum class Topping
      |""".trimMargin())
  }

  // https://github.com/square/kotlinpoet/issues/586
  @Test fun classKdocWithoutTags() {
    val typeSpec = TypeSpec.classBuilder("Foo")
        .addKdoc("blah blah")
        .build()
    assertThat(typeSpec.toString()).isEqualTo("""
    |/**
    | * blah blah
    | */
    |public class Foo
    |""".trimMargin())
  }

  @Test fun classWithPropertyKdoc() {
    val typeSpec = TypeSpec.classBuilder("Foo")
        .addProperty(PropertySpec.builder("bar", String::class)
            .addKdoc("The bar for your foo")
            .build())
        .build()
    assertThat(typeSpec.toString()).isEqualTo("""
    |public class Foo {
    |  /**
    |   * The bar for your foo
    |   */
    |  public val bar: kotlin.String
    |}
    |""".trimMargin())
  }

  // https://github.com/square/kotlinpoet/issues/563
  @Test fun kdocFormatting() {
    val typeSpec = TypeSpec.classBuilder("MyType")
        .addKdoc("This is a thing for stuff.")
        .addProperty(PropertySpec.builder("first", INT)
            .initializer("first")
            .build())
        .addProperty(PropertySpec.builder("second", INT)
            .initializer("second")
            .build())
        .addProperty(PropertySpec.builder("third", INT)
            .initializer("third")
            .build())
        .primaryConstructor(FunSpec.constructorBuilder()
            .addKdoc("Construct a thing!")
            .addParameter(ParameterSpec.builder("first", INT)
                .addKdoc("the first thing")
                .build())
            .addParameter(ParameterSpec.builder("second", INT)
                .addKdoc("the second thing")
                .build())
            .addParameter(ParameterSpec.builder("third", INT)
                .addKdoc("the third thing")
                .build())
            .build())
        .build()
    assertThat(typeSpec.toString()).isEqualTo("""
      |/**
      | * This is a thing for stuff.
      | */
      |public class MyType(
      |  /**
      |   * the first thing
      |   */
      |  public val first: kotlin.Int,
      |  /**
      |   * the second thing
      |   */
      |  public val second: kotlin.Int,
      |  /**
      |   * the third thing
      |   */
      |  public val third: kotlin.Int
      |)
      |""".trimMargin())
  }

  @Test fun primaryConstructorWithOneParameterKdocFormatting() {
    val typeSpec = TypeSpec.classBuilder("MyType")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("first", INT)
                .addKdoc("the first thing")
                .build())
            .build())
        .build()
    assertThat(typeSpec.toString()).isEqualTo("""
      |public class MyType(
      |  /**
      |   * the first thing
      |   */
      |  first: kotlin.Int
      |)
      |""".trimMargin())
  }

  // https://github.com/square/kotlinpoet/issues/594
  @Test fun longComment() {
    val taco = TypeSpec.classBuilder("Taco")
        .addFunction(FunSpec.builder("getAnswer")
            .returns(Int::class)
            .addComment("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
                "eiusmod tempor incididunt ut labore et dolore magna aliqua.")
            .addStatement("return 42")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Int
        |
        |public class Taco {
        |  public fun getAnswer(): Int {
        |    // Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        |    return 42
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun originatingElementsIncludesThoseOfNestedTypes() {
    val outerElement = FakeElement()
    val innerElement = FakeElement()
    val outer = TypeSpec.classBuilder("Outer")
        .addOriginatingElement(outerElement)
        .addType(TypeSpec.classBuilder("Inner")
            .addOriginatingElement(innerElement)
            .build())
        .build()
    assertThat(outer.originatingElements).containsExactly(outerElement, innerElement)
  }

  // https://github.com/square/kotlinpoet/issues/698
  @Test fun escapeEnumConstants() {
    val enum = TypeSpec.enumBuilder("MyEnum")
        .addEnumConstant("test test")
        .addEnumConstant("0constants")
        .build()
    assertThat(enum.toString()).isEqualTo("""
      |public enum class MyEnum {
      |  `test test`,
      |  `0constants`,
      |}
      |""".trimMargin())
  }

  @Test fun initOrdering_first() {
    val type = TypeSpec.classBuilder("MyClass")
        .addInitializerBlock(CodeBlock.builder().build())
        .addProperty("tacos", Int::class)
        .build()

    //language=kotlin
    assertThat(toString(type)).isEqualTo("""
        package com.squareup.tacos

        import kotlin.Int

        public class MyClass {
          init {
          }

          public val tacos: Int
        }

        """.trimIndent())
  }

  @Test fun initOrdering_middle() {
    val type = TypeSpec.classBuilder("MyClass")
        .addProperty("tacos1", Int::class)
        .addInitializerBlock(CodeBlock.builder().build())
        .addProperty("tacos2", Int::class)
        .build()

    //language=kotlin
    assertThat(toString(type)).isEqualTo("""
        package com.squareup.tacos

        import kotlin.Int

        public class MyClass {
          public val tacos1: Int

          init {
          }

          public val tacos2: Int
        }

        """.trimIndent())
  }

  @Test fun initOrdering_last() {
    val type = TypeSpec.classBuilder("MyClass")
        .addProperty("tacos", Int::class)
        .addInitializerBlock(CodeBlock.builder().build())
        .build()

    //language=kotlin
    assertThat(toString(type)).isEqualTo("""
        package com.squareup.tacos

        import kotlin.Int

        public class MyClass {
          public val tacos: Int

          init {
          }
        }

        """.trimIndent())
  }

  @Test fun initOrdering_constructorParamsExludedAfterIndex() {
    val type = TypeSpec.classBuilder("MyClass")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("tacos1", Int::class)
            .addParameter("tacos2", Int::class)
            .build())
        .addProperty(PropertySpec.builder("tacos1", Int::class)
            .initializer("tacos1")
            .build())
        .addInitializerBlock(CodeBlock.builder().build())
        .addProperty(PropertySpec.builder("tacos2", Int::class)
            .initializer("tacos2")
            .build())
        .build()

    //language=kotlin
    assertThat(toString(type)).isEqualTo("""
        package com.squareup.tacos

        import kotlin.Int

        public class MyClass(
          public val tacos1: Int,
          tacos2: Int
        ) {
          init {
          }

          public val tacos2: Int = tacos2
        }

        """.trimIndent())
  }

  // https://github.com/square/kotlinpoet/issues/843
  @Test fun kdocWithParametersWithoutClassKdoc() {
    val taco = TypeSpec.classBuilder("Taco")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("mild", Boolean::class)
                .addKdoc(CodeBlock.of("%L", "Whether the taco is mild (ew) or crunchy (ye).\n"))
                .build())
            .build())
        .addProperty(PropertySpec.builder("mild", Boolean::class)
            .addKdoc("No one likes mild tacos.")
            .initializer("mild")
            .build())
        .build()
    assertThat(toString(taco)).isEqualTo("""
        |package com.squareup.tacos
        |
        |import kotlin.Boolean
        |
        |/**
        | * @param mild Whether the taco is mild (ew) or crunchy (ye).
        | */
        |public class Taco(
        |  /**
        |   * No one likes mild tacos.
        |   */
        |  public val mild: Boolean
        |)
        |""".trimMargin())
  }

  // https://github.com/square/kotlinpoet/issues/848
  @Test fun escapeEnumConstantNames() {
    val enum = TypeSpec
        .enumBuilder("MyEnum")
        .addEnumConstant("object")
        .build()
    assertThat(toString(enum)).isEqualTo("""
      |package com.squareup.tacos
      |
      |public enum class MyEnum {
      |  `object`,
      |}
      |""".trimMargin())
  }

  @Test fun escapeClassNames() {
    val type = TypeSpec.classBuilder("fun").build()
    assertThat(type.toString()).isEqualTo("""
      |public class `fun`
      |""".trimMargin())
  }

  @Test fun escapeInnerClassName() {
    val tacoType = ClassName("com.squareup.tacos", "Taco", "object")
    val funSpec = FunSpec.builder("printTaco")
        .addParameter("taco", tacoType)
        .addStatement("print(taco)")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |public fun printTaco(taco: com.squareup.tacos.Taco.`object`): kotlin.Unit {
      |  print(taco)
      |}
      |""".trimMargin())
  }

  @Test fun escapeAllowedCharacters() {
    var typeSpec = TypeSpec.classBuilder("A\$B")
        .build()
    assertThat(typeSpec.toString()).isEqualTo("public class `A\$B`\n")
  }

  companion object {
    private val donutsPackage = "com.squareup.donuts"
  }
}
