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

import com.google.common.collect.Iterables.getOnlyElement
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.junit.Rule
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.Callable
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter.methodsIn
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.test.BeforeTest
import kotlin.test.Test

class FunSpecTest {
  @Rule @JvmField val compilation = CompilationRule()

  private lateinit var elements: Elements
  private lateinit var types: Types

  @BeforeTest fun setUp() {
    elements = compilation.elements
    types = compilation.types
  }

  private fun getElement(`class`: Class<*>): TypeElement {
    return elements.getTypeElement(`class`.canonicalName)
  }

  private fun findFirst(elements: Collection<ExecutableElement>, name: String) =
      elements.firstOrNull { it.simpleName.toString() == name } ?:
          throw IllegalArgumentException("$name not found in $elements")

  @Target(AnnotationTarget.VALUE_PARAMETER)
  internal annotation class Nullable

  internal abstract class Everything {
    @Deprecated("") @Throws(IOException::class, SecurityException::class)
    protected abstract fun <T> everything(
        @Nullable thing: String, things: List<T>): Runnable where T : Runnable, T : Closeable
  }

  internal abstract class HasAnnotation {
    abstract override fun toString(): String
  }

  internal interface ExtendsOthers : Callable<Int>, Comparable<Long>

  abstract class InvalidOverrideMethods {
    fun finalMethod() {
    }

    private fun privateMethod() {
    }

    companion object {
      @JvmStatic open fun staticMethod() {
      }
    }
  }

  @Test fun overrideEverything() {
    val classElement = getElement(Everything::class.java)
    val methodElement = getOnlyElement(methodsIn(classElement.enclosedElements))
    val funSpec = FunSpec.overriding(methodElement).build()
    assertThat(funSpec.toString()).isEqualTo("""
        |@kotlin.jvm.Throws(java.io.IOException::class, java.lang.SecurityException::class)
        |protected override fun <T> everything(arg0: java.lang.String, arg1: java.util.List<out T>): java.lang.Runnable
        |        where T : java.lang.Runnable, T : java.io.Closeable {
        |}
        |""".trimMargin())
  }

  @Test fun overrideDoesNotCopyOverrideAnnotation() {
    val classElement = getElement(HasAnnotation::class.java)
    val exec = getOnlyElement(methodsIn(classElement.enclosedElements))
    val funSpec = FunSpec.overriding(exec).build()
    assertThat(funSpec.toString()).isEqualTo("""
        |override fun toString(): java.lang.String {
        |}
        |""".trimMargin())
  }

  @Test fun overrideExtendsOthersWorksWithActualTypeParameters() {
    val classElement = getElement(ExtendsOthers::class.java)
    val classType = classElement.asType() as DeclaredType
    val methods = methodsIn(elements.getAllMembers(classElement))
    var exec = findFirst(methods, "call")
    var funSpec = FunSpec.overriding(exec, classType, types).build()
    assertThat(funSpec.toString()).isEqualTo("""
        |@kotlin.jvm.Throws(java.lang.Exception::class)
        |override fun call(): java.lang.Integer {
        |}
        |""".trimMargin())
    exec = findFirst(methods, "compareTo")
    funSpec = FunSpec.overriding(exec, classType, types).build()
    assertThat(funSpec.toString()).isEqualTo("""
        |override fun compareTo(arg0: java.lang.Long): kotlin.Int {
        |}
        |""".trimMargin())
  }

  @Test fun overrideInvalidModifiers() {
    val classElement = getElement(InvalidOverrideMethods::class.java)
    val methods = methodsIn(elements.getAllMembers(classElement))

    assertThrows<IllegalArgumentException> {
      FunSpec.overriding(findFirst(methods, "finalMethod"))
    }.hasMessageThat().isEqualTo("cannot override method with modifiers: [public, final]")

    assertThrows<IllegalArgumentException> {
      FunSpec.overriding(findFirst(methods, "privateMethod"))
    }.hasMessageThat().isEqualTo("cannot override method with modifiers: [private, final]")

    assertThrows<IllegalArgumentException> {
      FunSpec.overriding(findFirst(methods, "staticMethod"))
    }.hasMessageThat().isEqualTo("cannot override method with modifiers: [public, static]")
  }

  @Test fun nullableParam() {
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder("string", String::class.asTypeName().asNullable())
            .build())
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(string: kotlin.String?) {
      |}
      |""".trimMargin())
  }

  @Test fun nullableReturnType() {
    val funSpec = FunSpec.builder("foo")
        .returns(String::class.asTypeName().asNullable())
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(): kotlin.String? {
      |}
      |""".trimMargin())
  }

  @Test fun functionParamWithKdoc() {
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder("string", String::class.asTypeName())
            .addKdoc("A string parameter.\n")
            .build())
        .addParameter(ParameterSpec.builder("number", Int::class.asTypeName())
            .addKdoc("A number with a multi-line doc comment.\nYes,\nthese\nthings\nhappen.\n")
            .build())
        .addParameter(ParameterSpec.builder("nodoc", Boolean::class.asTypeName()).build())
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |/**
      | * @param string A string parameter.
      | * @param number A number with a multi-line doc comment.
      | * Yes,
      | * these
      | * things
      | * happen.
      | */
      |fun foo(
      |    string: kotlin.String,
      |    number: kotlin.Int,
      |    nodoc: kotlin.Boolean
      |) {
      |}
      |""".trimMargin())
  }

  @Test fun functionParamNoLambdaParam() {
    val unitType = UNIT
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder("f", LambdaTypeName.get(returnType = unitType)).build())
        .returns(String::class)
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(f: () -> kotlin.Unit): kotlin.String {
      |}
      |""".trimMargin())
  }

  @Test fun functionParamNoLambdaParamWithReceiver() {
    val unitType = UNIT
    val lambdaTypeName = LambdaTypeName.get(receiver = INT, returnType = unitType)
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder("f", lambdaTypeName).build())
        .returns(String::class)
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(f: kotlin.Int.() -> kotlin.Unit): kotlin.String {
      |}
      |""".trimMargin())
  }

  @Test fun functionParamSingleLambdaParam() {
    val unitType = UNIT
    val booleanType = BOOLEAN
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder("f", LambdaTypeName.get(
            parameters = *arrayOf(booleanType),
            returnType = unitType))
            .build())
        .returns(String::class)
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(f: (kotlin.Boolean) -> kotlin.Unit): kotlin.String {
      |}
      |""".trimMargin())
  }

  @Test fun functionParamMultipleLambdaParam() {
    val unitType = UNIT
    val booleanType = BOOLEAN
    val stringType = String::class.asClassName()
    val lambdaType = LambdaTypeName.get(parameters = *arrayOf(booleanType, stringType), returnType = unitType)
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder("f", lambdaType).build())
        .returns(String::class)
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(f: (kotlin.Boolean, kotlin.String) -> kotlin.Unit): kotlin.String {
      |}
      |""".trimMargin())
  }

  @Test fun functionParamMultipleLambdaParamNullableLambda() {
    val unitType = Unit::class.asClassName()
    val booleanType = Boolean::class.asClassName()
    val stringType = String::class.asClassName()
    val lambdaTypeName = LambdaTypeName.get(parameters = *arrayOf(booleanType, stringType), returnType = unitType)
        .asNullable()
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder("f", lambdaTypeName).build())
        .returns(String::class)
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(f: ((kotlin.Boolean, kotlin.String) -> kotlin.Unit)?): kotlin.String {
      |}
      |""".trimMargin())
  }

  @Test fun functionParamMultipleNullableLambdaParam() {
    val unitType = Unit::class.asClassName()
    val booleanType = Boolean::class.asClassName()
    val stringType = String::class.asClassName().asNullable()
    val lambdaTypeName = LambdaTypeName.get(parameters = *arrayOf(booleanType, stringType), returnType = unitType).asNullable()
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder("f", lambdaTypeName).build())
        .returns(String::class)
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(f: ((kotlin.Boolean, kotlin.String?) -> kotlin.Unit)?): kotlin.String {
      |}
      |""".trimMargin())
  }

  @Test fun thisConstructorDelegate() {
    val funSpec = FunSpec.constructorBuilder()
        .addParameter("list", List::class.parameterizedBy(Int::class))
        .callThisConstructor("list[0]", "list[1]")
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |constructor(list: kotlin.collections.List<kotlin.Int>) : this(list[0], list[1])
      |""".trimMargin())
  }

  @Test fun superConstructorDelegate() {
    val funSpec = FunSpec.constructorBuilder()
        .addParameter("list", List::class.parameterizedBy(Int::class))
        .callSuperConstructor("list[0]", "list[1]")
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |constructor(list: kotlin.collections.List<kotlin.Int>) : super(list[0], list[1])
      |""".trimMargin())
  }

  @Test fun emptyConstructorDelegate() {
    val funSpec = FunSpec.constructorBuilder()
        .addParameter("a", Int::class)
        .callThisConstructor()
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |constructor(a: kotlin.Int) : this()
      |""".trimMargin())
  }

  @Test fun constructorDelegateWithBody() {
    val funSpec = FunSpec.constructorBuilder()
        .addParameter("a", Int::class)
        .callThisConstructor("a")
        .addStatement("println()")
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |constructor(a: kotlin.Int) : this(a) {
      |    println()
      |}
      |""".trimMargin())
  }

  @Test fun addingDelegateParametersToNonConstructorForbidden() {
    assertThrows<IllegalStateException> {
      FunSpec.builder("main")
          .callThisConstructor("a", "b", "c")
    }.hasMessageThat().isEqualTo("only constructors can delegate to other constructors!")
  }

  @Test fun emptySecondaryConstructor() {
    val constructorSpec = FunSpec.constructorBuilder()
        .addParameter("a", Int::class)
        .build()

    assertThat(constructorSpec.toString()).isEqualTo("""
      |constructor(a: kotlin.Int)
      |""".trimMargin())
  }

  @Test fun reifiedTypesOnNonInlineFunctionsForbidden() {
    assertThrows<IllegalArgumentException> {
      FunSpec.builder("foo")
          .addTypeVariable(TypeVariableName("T").reified())
          .build()
    }.hasMessageThat().isEqualTo("only type parameters of inline functions can be reified!")
  }

  @Test fun equalsAndHashCode() {
    var a = FunSpec.constructorBuilder().build()
    var b = FunSpec.constructorBuilder().build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = FunSpec.builder("taco").build()
    b = FunSpec.builder("taco").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    val classElement = getElement(Everything::class.java)
    val methodElement = getOnlyElement(methodsIn(classElement.enclosedElements))
    a = FunSpec.overriding(methodElement).build()
    b = FunSpec.overriding(methodElement).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun escapeKeywordInFunctionName() {
    val funSpec = FunSpec.builder("if")
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |fun `if`() {
      |}
      |""".trimMargin())
  }

  @Test fun escapePunctuationInFunctionName() {
    val funSpec = FunSpec.builder("with-hyphen")
        .build()

    assertThat(funSpec.toString()).isEqualTo("""
      |fun `with-hyphen`() {
      |}
      |""".trimMargin())
  }

  @Test fun generalBuilderEqualityTest() {
    val funSpec = FunSpec.Builder("getConfig")
        .addKdoc("Fix me")
        .addAnnotation(AnnotationSpec.builder(SuppressWarnings::class)
            .build())
        .addModifiers(KModifier.PROTECTED)
        .addTypeVariable(TypeVariableName("T"))
        .receiver(String::class)
        .returns(String::class)
        .addParameter(ParameterSpec.builder("config", String::class)
            .build())
        .addParameter(ParameterSpec.builder("override", TypeVariableName("T"))
            .build())
        .beginControlFlow("return when")
        .addStatement("    override is String -> config + override")
        .addStatement("    else -> config + %S", "{ttl:500}")
        .endControlFlow()
        .build();

    assertThat(funSpec.toBuilder().build()).isEqualTo(funSpec);
  }

  @Test fun constructorBuilderEqualityTest() {
    val funSpec = FunSpec.constructorBuilder()
        .addParameter("list", List::class.parameterizedBy(Int::class))
        .callThisConstructor("list[0]", "list[1]")
        .build()

    assertThat(funSpec.toBuilder().build()).isEqualTo(funSpec)
  }

  // https://github.com/square/kotlinpoet/issues/398
  @Test fun changingDelegateConstructorOverridesArgs() {
    val funSpec = FunSpec.constructorBuilder()
        .addParameter("values", List::class.parameterizedBy(String::class))
        .callSuperConstructor("values")
        .build()
    val updatedFunSpec = funSpec.toBuilder()
        .callSuperConstructor("values.toImmutableList()")
        .build()
    assertThat(updatedFunSpec.toString()).isEqualTo("""
      |constructor(values: kotlin.collections.List<kotlin.String>) : super(values.toImmutableList())
      |""".trimMargin())
  }

  @Test fun modifyModifiers() {
    val builder = FunSpec.builder("taco")
        .addModifiers(KModifier.PRIVATE)

    builder.modifiers.clear()
    builder.modifiers.add(KModifier.INTERNAL)

    assertThat(builder.build().modifiers).containsExactly(KModifier.INTERNAL)
  }

  @Test fun modifyAnnotations() {
    val builder = FunSpec.builder("taco")
        .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
            .addMember("name = %S", "jvmWord")
            .build())

    val javaWord = AnnotationSpec.builder(JvmName::class.asClassName())
        .addMember("name = %S", "javaWord")
        .build()
    builder.annotations.clear()
    builder.annotations.add(javaWord)

    assertThat(builder.build().annotations).containsExactly(javaWord)
  }

  @Test fun modifyTypeVariableNames() {
    val builder = FunSpec.builder("taco")
        .addTypeVariable(TypeVariableName("V"))

    val tVar = TypeVariableName("T")
    builder.typeVariables.clear()
    builder.typeVariables.add(tVar)

    assertThat(builder.build().typeVariables).containsExactly(tVar)
  }

  @Test fun modifyParameters() {
    val builder = FunSpec.builder("taco")
        .addParameter(ParameterSpec.builder("topping", String::class.asClassName()).build())

    val seasoning = ParameterSpec.builder("seasoning", String::class.asClassName()).build()
    builder.parameters.clear()
    builder.parameters.add(seasoning)

    assertThat(builder.build().parameters).containsExactly(seasoning)
  }
}
