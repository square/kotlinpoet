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

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.FunSpec.Companion.GETTER
import com.squareup.kotlinpoet.FunSpec.Companion.SETTER
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.Callable
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter.methodsIn
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.Rule

@OptIn(ExperimentalKotlinPoetApi::class)
class FunSpecTest {
  @Rule @JvmField
  val compilation = CompilationRule()

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
    elements.firstOrNull { it.simpleName.toString() == name }
      ?: throw IllegalArgumentException("$name not found in $elements")

  @Target(AnnotationTarget.VALUE_PARAMETER)
  internal annotation class Nullable

  internal abstract class Everything {
    @Deprecated("")
    @Throws(IOException::class, SecurityException::class)
    protected abstract fun <T> everything(
      @Nullable thing: String,
      things: List<T>,
    ): Runnable where T : Runnable, T : Closeable
  }

  internal abstract class HasAnnotation {
    abstract override fun toString(): String
  }

  internal interface ExtendsOthers : Callable<Int>, Comparable<Long>

  annotation class TestAnnotation

  abstract class InvalidOverrideMethods {
    fun finalMethod() {
    }

    private fun privateMethod() {
    }

    companion object {
      @Suppress("NON_FINAL_MEMBER_IN_OBJECT")
      @JvmStatic
      open fun staticMethod() {
      }
    }
  }

  @Test fun overrideEverything() {
    val classElement = getElement(Everything::class.java)
    val methodElement = methodsIn(classElement.enclosedElements).single()
    val funSpec = FunSpec.overriding(methodElement).build()
    assertThat(funSpec.toString()).isEqualTo(
      """
        |@kotlin.jvm.Throws(java.io.IOException::class, java.lang.SecurityException::class)
        |protected override fun <T> everything(arg0: java.lang.String, arg1: java.util.List<out T>): java.lang.Runnable where T : java.lang.Runnable, T : java.io.Closeable {
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun overrideDoesNotCopyOverrideAnnotation() {
    val classElement = getElement(HasAnnotation::class.java)
    val exec = methodsIn(classElement.enclosedElements).single()
    val funSpec = FunSpec.overriding(exec).build()
    assertThat(funSpec.toString()).isEqualTo(
      """
        |public override fun toString(): java.lang.String {
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun overrideExtendsOthersWorksWithActualTypeParameters() {
    val classElement = getElement(ExtendsOthers::class.java)
    val classType = classElement.asType() as DeclaredType
    val methods = methodsIn(elements.getAllMembers(classElement))
    var exec = findFirst(methods, "call")

    @Suppress("DEPRECATION")
    var funSpec = FunSpec.overriding(exec, classType, types).build()
    assertThat(funSpec.toString()).isEqualTo(
      """
        |@kotlin.jvm.Throws(java.lang.Exception::class)
        |public override fun call(): java.lang.Integer {
        |}
        |
      """.trimMargin(),
    )
    exec = findFirst(methods, "compareTo")
    @Suppress("DEPRECATION")
    funSpec = FunSpec.overriding(exec, classType, types).build()
    assertThat(funSpec.toString()).isEqualTo(
      """
        |public override fun compareTo(arg0: java.lang.Long): kotlin.Int {
        |}
        |
      """.trimMargin(),
    )
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
      .addParameter(
        ParameterSpec
          .builder("string", String::class.asTypeName().copy(nullable = true))
          .build(),
      )
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(string: kotlin.String?) {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun nullableReturnType() {
    val funSpec = FunSpec.builder("foo")
      .returns(String::class.asTypeName().copy(nullable = true))
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(): kotlin.String? {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun returnsUnitWithoutExpressionBody() {
    val funSpec = FunSpec.builder("foo")
      .returns(Unit::class)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun returnsUnitWithExpressionBody() {
    val funSpec = FunSpec.builder("foo")
      .returns(Unit::class)
      .addStatement("return bar()")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(): kotlin.Unit = bar()
      |
      """.trimMargin(),
    )
  }

  @Test fun returnsLongExpression() {
    val funSpec = FunSpec.builder("foo")
      .returns(String::class)
      .addStatement("val placeholder = 1")
      .addStatement("return %S", "Loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong")
      .build()
    val sb = StringBuilder()
    // The FunSpec#toString columnLimit is Integer.MAX_VALUE,
    // It will not cause problems with returns long expressions.
    CodeWriter(sb).use {
      funSpec.emit(
        codeWriter = it,
        enclosingName = null,
        implicitModifiers = setOf(KModifier.PUBLIC),
        includeKdocTags = false,
      )
    }
    assertThat(sb.toString()).isEqualTo(
      """
      |public fun foo(): kotlin.String {
      |  val placeholder = 1
      |  return "Loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong"
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionParamWithKdoc() {
    val funSpec = FunSpec.builder("foo")
      .addParameter(
        ParameterSpec.builder("string", String::class.asTypeName())
          .addKdoc("A string parameter.")
          .build(),
      )
      .addParameter(
        ParameterSpec.builder("number", Int::class.asTypeName())
          .addKdoc("A number with a multi-line doc comment.\nYes,\nthese\nthings\nhappen.")
          .build(),
      )
      .addParameter(ParameterSpec.builder("nodoc", Boolean::class.asTypeName()).build())
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * @param string A string parameter.
      | * @param number A number with a multi-line doc comment.
      | * Yes,
      | * these
      | * things
      | * happen.
      | */
      |public fun foo(
      |  string: kotlin.String,
      |  number: kotlin.Int,
      |  nodoc: kotlin.Boolean,
      |) {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionParamWithKdocToBuilder() {
    val funSpec = FunSpec.builder("foo")
      .addParameter(
        ParameterSpec.builder("string", String::class.asTypeName())
          .addKdoc("A string parameter.")
          .build()
          .toBuilder()
          .addKdoc(" This is non null")
          .build(),
      )
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * @param string A string parameter. This is non null
      | */
      |public fun foo(string: kotlin.String) {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun originatingElementToBuilder() {
    val originatingElement = FakeElement()
    val funSpec = FunSpec.builder("foo")
      .addOriginatingElement(originatingElement)
      .build()

    val newSpec = funSpec.toBuilder().build()
    assertThat(newSpec.originatingElements).containsExactly(originatingElement)
  }

  @Test fun functionParamWithKdocAndReturnKdoc() {
    val funSpec = FunSpec.builder("foo")
      .addParameter(
        ParameterSpec.builder("string", String::class)
          .addKdoc("A string parameter.")
          .build(),
      )
      .addParameter(ParameterSpec.builder("nodoc", Boolean::class).build())
      .returns(String::class, kdoc = "the foo.")
      .addStatement("return %S", "foo")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * @param string A string parameter.
      | * @return the foo.
      | */
      |public fun foo(string: kotlin.String, nodoc: kotlin.Boolean): kotlin.String = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithModifiedReturnKdoc() {
    val funSpec = FunSpec.builder("foo")
      .addParameter("nodoc", Boolean::class)
      .returns(String::class, kdoc = "the foo.")
      .addStatement("return %S", "foo")
      .build()
      .toBuilder()
      .returns(String::class, kdoc = "the modified foo.")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * @return the modified foo.
      | */
      |public fun foo(nodoc: kotlin.Boolean): kotlin.String = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithThrows() {
    val funSpec = FunSpec.builder("foo")
      .addStatement("throw %T()", AssertionError::class)
      .returns(NOTHING)
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(): kotlin.Nothing = throw java.lang.AssertionError()
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithWordThrowDoesntConvertToExpressionFunction() {
    val throwSomethingElseFun = FunSpec.builder("throwOrDoSomethingElse")
      .build()

    val funSpec = FunSpec.builder("foo")
      .addStatement("%N()", throwSomethingElseFun)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo() {
      |  throwOrDoSomethingElse()
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun expressionBodyIsDetectedReturnWithNonBreakingSpace() {
    val funSpec = FunSpec.builder("foo")
      .returns(INT)
      .addStatement("return·1")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(): kotlin.Int = 1
      |
      """.trimMargin(),
    )
  }

  @Test fun expressionBodyIsDetectedThrowWithNonBreakingSpace() {
    val funSpec = FunSpec.builder("foo")
      .addStatement("throw·%T()", AssertionError::class)
      .returns(NOTHING)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(): kotlin.Nothing = throw java.lang.AssertionError()
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithReturnKDocAndMainKdoc() {
    val funSpec = FunSpec.builder("foo")
      .addParameter("nodoc", Boolean::class)
      .returns(String::class, kdoc = "the foo.")
      .addStatement("return %S", "foo")
      .addKdoc("Do the foo")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * Do the foo
      | *
      | * @return the foo.
      | */
      |public fun foo(nodoc: kotlin.Boolean): kotlin.String = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun functionParamNoLambdaParam() {
    val unitType = UNIT
    val funSpec = FunSpec.builder("foo")
      .addParameter(ParameterSpec.builder("f", LambdaTypeName.get(returnType = unitType)).build())
      .returns(String::class)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(f: () -> kotlin.Unit): kotlin.String {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithReturnKDoc() {
    val funSpec = FunSpec.builder("foo")
      .addParameter(ParameterSpec.builder("f", LambdaTypeName.get(returnType = UNIT)).build())
      .returns(String::class, CodeBlock.of("the foo."))
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * @return the foo.
      | */
      |public fun foo(f: () -> kotlin.Unit): kotlin.String {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionParamNoLambdaParamWithReceiver() {
    val unitType = UNIT
    val lambdaTypeName = LambdaTypeName.get(receiver = INT, returnType = unitType)
    val funSpec = FunSpec.builder("foo")
      .addParameter(ParameterSpec.builder("f", lambdaTypeName).build())
      .returns(String::class)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(f: kotlin.Int.() -> kotlin.Unit): kotlin.String {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithContextReceiver() {
    val stringType = STRING

    val funSpec = FunSpec.builder("foo")
      .contextReceivers(stringType)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(kotlin.String)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithMultipleContextReceivers() {
    val stringType = STRING
    val intType = INT
    val booleanType = BOOLEAN

    val funSpec = FunSpec.builder("foo")
      .contextReceivers(stringType, intType, booleanType)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(kotlin.String, kotlin.Int, kotlin.Boolean)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithGenericContextReceiver() {
    val genericType = TypeVariableName("T")

    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(genericType)
      .contextReceivers(genericType)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(T)
      |public fun <T> foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun annotatedFunctionWithContextReceiver() {
    val funSpec = FunSpec.builder("foo")
      .addAnnotation(AnnotationSpec.get(TestAnnotation()))
      .contextReceivers(STRING)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(kotlin.String)
      |@com.squareup.kotlinpoet.FunSpecTest.TestAnnotation
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithAnnotatedContextReceiver() {
    val genericType = STRING.copy(annotations = listOf(AnnotationSpec.get(TestAnnotation())))

    val funSpec = FunSpec.builder("foo")
      .contextReceivers(genericType)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(@com.squareup.kotlinpoet.FunSpecTest.TestAnnotation kotlin.String)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun constructorWithContextReceiver() {
    assertThrows<IllegalStateException> {
      FunSpec.constructorBuilder()
        .contextReceivers(STRING)
    }.hasMessageThat().isEqualTo("constructors cannot have context receivers")
  }

  @Test fun accessorWithContextReceiver() {
    assertThrows<IllegalStateException> {
      FunSpec.getterBuilder()
        .contextReceivers(STRING)
    }.hasMessageThat().isEqualTo("$GETTER cannot have context receivers")

    assertThrows<IllegalStateException> {
      FunSpec.setterBuilder()
        .contextReceivers(STRING)
    }.hasMessageThat().isEqualTo("$SETTER cannot have context receivers")
  }

  @Test fun functionWithContextParameter() {
    val funSpec = FunSpec.builder("foo")
      .contextParameter("user", STRING)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(user: kotlin.String)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithMultipleContextParameters() {
    val funSpec = FunSpec.builder("foo")
      .contextParameter("user", STRING)
      .contextParameter("counter", INT)
      .contextParameter("enabled", BOOLEAN)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(user: kotlin.String, counter: kotlin.Int, enabled: kotlin.Boolean)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithUnnamedContextParameter() {
    val funSpec = FunSpec.builder("foo")
      .contextParameter(STRING)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(_: kotlin.String)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithGenericContextParameter() {
    val genericType = TypeVariableName("T")
    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(genericType)
      .contextParameter("value", genericType)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(value: T)
      |public fun <T> foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun annotatedFunctionWithContextParameter() {
    val funSpec = FunSpec.builder("foo")
      .addAnnotation(AnnotationSpec.get(TestAnnotation()))
      .contextParameter("user", STRING)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(user: kotlin.String)
      |@com.squareup.kotlinpoet.FunSpecTest.TestAnnotation
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithAnnotatedContextParameter() {
    val annotatedType = STRING.copy(annotations = listOf(AnnotationSpec.get(TestAnnotation())))
    val funSpec = FunSpec.builder("foo")
      .contextParameter("user", annotatedType)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |context(user: @com.squareup.kotlinpoet.FunSpecTest.TestAnnotation kotlin.String)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionWithBothContextReceiverAndContextParameter() {
    assertThrows<IllegalStateException> {
      FunSpec.builder("foo")
        .contextReceivers(listOf(STRING))
        .contextParameter("num", INT)
        .build()
    }.hasMessageThat().isEqualTo("Using both context receivers and context parameters is not allowed")
  }

  @Test fun contextParameterInAddStatement() {
    val loggerType = ClassName("java.util.logging", "Logger")
    val configType = ClassName("com.example", "Config")

    val logger = ContextParameter("logger", loggerType)
    val config = ContextParameter("config", configType)

    val processData = FunSpec.builder("processData")
      .contextParameter(logger)
      .contextParameter(config)
      .addStatement("%N.info(\"Processing with config: ${'$'}%N\")", logger, config)
      .build()

    assertThat(processData.toString()).isEqualTo(
      """
      |context(logger: java.util.logging.Logger, config: com.example.Config)
      |public fun processData() {
      |  logger.info("Processing with config: ${'$'}config")
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun constructorWithContextParameter() {
    assertThrows<IllegalStateException> {
      FunSpec.constructorBuilder()
        .contextParameter("user", STRING)
    }.hasMessageThat().isEqualTo("constructors cannot have context parameters")
  }

  @Test fun accessorWithContextParameter() {
    assertThrows<IllegalStateException> {
      FunSpec.getterBuilder()
        .contextParameter("user", STRING)
    }.hasMessageThat().isEqualTo("$GETTER cannot have context parameters")

    assertThrows<IllegalStateException> {
      FunSpec.setterBuilder()
        .contextParameter("user", STRING)
    }.hasMessageThat().isEqualTo("$SETTER cannot have context parameters")
  }

  @Test fun functionParamSingleLambdaParam() {
    val unitType = UNIT
    val booleanType = BOOLEAN
    val funSpec = FunSpec.builder("foo")
      .addParameter(
        ParameterSpec.builder(
          "f",
          LambdaTypeName.get(
            parameters = arrayOf(booleanType),
            returnType = unitType,
          ),
        )
          .build(),
      )
      .returns(String::class)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(f: (kotlin.Boolean) -> kotlin.Unit): kotlin.String {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionParamMultipleLambdaParam() {
    val unitType = UNIT
    val booleanType = BOOLEAN
    val stringType = String::class.asClassName()
    val lambdaType = LambdaTypeName.get(parameters = arrayOf(booleanType, stringType), returnType = unitType)
    val funSpec = FunSpec.builder("foo")
      .addParameter(ParameterSpec.builder("f", lambdaType).build())
      .returns(String::class)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(f: (kotlin.Boolean, kotlin.String) -> kotlin.Unit): kotlin.String {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionParamMultipleLambdaParamNullableLambda() {
    val unitType = Unit::class.asClassName()
    val booleanType = Boolean::class.asClassName()
    val stringType = String::class.asClassName()
    val lambdaTypeName = LambdaTypeName
      .get(parameters = arrayOf(booleanType, stringType), returnType = unitType)
      .copy(nullable = true)
    val funSpec = FunSpec.builder("foo")
      .addParameter(ParameterSpec.builder("f", lambdaTypeName).build())
      .returns(String::class)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(f: ((kotlin.Boolean, kotlin.String) -> kotlin.Unit)?): kotlin.String {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun functionParamMultipleNullableLambdaParam() {
    val unitType = Unit::class.asClassName()
    val booleanType = Boolean::class.asClassName()
    val stringType = String::class.asClassName().copy(nullable = true)
    val lambdaTypeName = LambdaTypeName
      .get(parameters = arrayOf(booleanType, stringType), returnType = unitType)
      .copy(nullable = true)
    val funSpec = FunSpec.builder("foo")
      .addParameter(ParameterSpec.builder("f", lambdaTypeName).build())
      .returns(String::class)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun foo(f: ((kotlin.Boolean, kotlin.String?) -> kotlin.Unit)?): kotlin.String {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun setterWithPublicModifier() {
    val funSpec = FunSpec.setterBuilder()
      .addParameter("value", String::class.asClassName())
      .addStatement("this.value = this.value")
      .addModifiers(KModifier.PUBLIC)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public set(`value`) {
      |  this.value = this.value
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun getterWithPublicModifier() {
    val funSpec = FunSpec.getterBuilder()
      .addStatement("return value")
      .addModifiers(KModifier.PUBLIC)
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public get() = value
      |
      """.trimMargin(),
    )
  }

  // This does not produce correct Kotlin, but it does at least verify that we do not drop the
  // explicitly specified public modifier.
  @Test fun methodWithMultipleVisibilityModifiers() {
    val funSpec =
      FunSpec.builder("myMethod")
        .addModifiers(KModifier.PUBLIC, KModifier.INTERNAL, KModifier.PRIVATE)
        .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public private internal fun myMethod() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun methodWithRepeatedVisibilityModifier() {
    val funSpec =
      FunSpec.builder("myMethod")
        .addModifiers(KModifier.PUBLIC, KModifier.PUBLIC, KModifier.PUBLIC)
        .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun myMethod() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun thisConstructorDelegate() {
    val funSpec = FunSpec.constructorBuilder()
      .addParameter("list", List::class.parameterizedBy(Int::class))
      .callThisConstructor("list[0]", "list[1]")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public constructor(list: kotlin.collections.List<kotlin.Int>) : this(list[0], list[1])
      |
      """.trimMargin(),
    )
  }

  @Test fun superConstructorDelegate() {
    val funSpec = FunSpec.constructorBuilder()
      .addParameter("list", List::class.parameterizedBy(Int::class))
      .callSuperConstructor("list[0]", "list[1]")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public constructor(list: kotlin.collections.List<kotlin.Int>) : super(list[0], list[1])
      |
      """.trimMargin(),
    )
  }

  @Test fun emptyConstructorDelegate() {
    val funSpec = FunSpec.constructorBuilder()
      .addParameter("a", Int::class)
      .callThisConstructor()
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public constructor(a: kotlin.Int) : this()
      |
      """.trimMargin(),
    )
  }

  @Test fun constructorDelegateWithBody() {
    val funSpec = FunSpec.constructorBuilder()
      .addParameter("a", Int::class)
      .callThisConstructor("a")
      .addStatement("println()")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public constructor(a: kotlin.Int) : this(a) {
      |  println()
      |}
      |
      """.trimMargin(),
    )
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

    assertThat(constructorSpec.toString()).isEqualTo(
      """
      |public constructor(a: kotlin.Int)
      |
      """.trimMargin(),
    )
  }

  @Test fun reifiedTypesOnNonInlineFunctionsForbidden() {
    assertThrows<IllegalArgumentException> {
      FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T").copy(reified = true))
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
    val methodElement = methodsIn(classElement.enclosedElements).single()
    a = FunSpec.overriding(methodElement).build()
    b = FunSpec.overriding(methodElement).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun escapeKeywordInFunctionName() {
    val funSpec = FunSpec.builder("if")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun `if`() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun escapePunctuationInFunctionName() {
    val funSpec = FunSpec.builder("with-hyphen")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun `with-hyphen`() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun generalBuilderEqualityTest() {
    val funSpec = FunSpec.Builder("getConfig")
      .addKdoc("Fix me")
      .addAnnotation(
        AnnotationSpec.builder(SuppressWarnings::class)
          .build(),
      )
      .addModifiers(KModifier.PROTECTED)
      .addTypeVariable(TypeVariableName("T"))
      .receiver(String::class)
      .returns(String::class)
      .addParameter(
        ParameterSpec.builder("config", String::class)
          .build(),
      )
      .addParameter(
        ParameterSpec.builder("override", TypeVariableName("T"))
          .build(),
      )
      .beginControlFlow("return when")
      .addStatement("    override is String -> config + override")
      .addStatement("    else -> config + %S", "{ttl:500}")
      .endControlFlow()
      .build()

    assertThat(funSpec.toBuilder().build()).isEqualTo(funSpec)
  }

  @Test fun receiverWithKdoc() {
    val funSpec = FunSpec.builder("toBar")
      .receiver(String::class, kdoc = "the string to transform.")
      .returns(String::class)
      .addStatement("return %S", "bar")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * @receiver the string to transform.
      | */
      |public fun kotlin.String.toBar(): kotlin.String = "bar"
      |
      """.trimMargin(),
    )
  }

  @Test fun receiverWithKdocAndMainKDoc() {
    val funSpec = FunSpec.builder("toBar")
      .receiver(String::class, kdoc = "the string to transform.")
      .returns(String::class)
      .addKdoc("%L", "Converts to bar")
      .addStatement("return %S", "bar")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * Converts to bar
      | *
      | * @receiver the string to transform.
      | */
      |public fun kotlin.String.toBar(): kotlin.String = "bar"
      |
      """.trimMargin(),
    )
  }

  @Test fun withAllKdocTags() {
    val funSpec = FunSpec.builder("charAt")
      .receiver(String::class, kdoc = "the string you want the char from.")
      .returns(Char::class, kdoc = "The char at the given [position].")
      .addParameter(
        ParameterSpec.builder("position", Int::class)
          .addKdoc("the index of the character that is returned.")
          .build(),
      )
      .addKdoc("Returns the character at the given [position].\n\n")
      .addStatement("return -1")
      .build()

    assertThat(funSpec.toString()).isEqualTo(
      """
      |/**
      | * Returns the character at the given [position].
      | *
      | * @receiver the string you want the char from.
      | * @param position the index of the character that is returned.
      | * @return The char at the given [position].
      | */
      |public fun kotlin.String.charAt(position: kotlin.Int): kotlin.Char = -1
      |
      """.trimMargin(),
    )
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
    assertThat(updatedFunSpec.toString()).isEqualTo(
      """
      |public constructor(values: kotlin.collections.List<kotlin.String>) : super(values.toImmutableList())
      |
      """.trimMargin(),
    )
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
      .addAnnotation(
        AnnotationSpec.builder(JvmName::class.asClassName())
          .addMember("name = %S", "jvmWord")
          .build(),
      )

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

  @Test fun jvmStaticModifier() {
    val builder = FunSpec.builder("staticMethod")
    builder.jvmModifiers(listOf(Modifier.STATIC))

    assertThat(builder.build().toString()).isEqualTo(
      """
      |@kotlin.jvm.JvmStatic
      |internal fun staticMethod() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmFinalModifier() {
    val builder = FunSpec.builder("finalMethod")
    builder.jvmModifiers(listOf(Modifier.FINAL))

    assertThat(builder.build().toString()).isEqualTo(
      """
      |internal final fun finalMethod() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmSynchronizedModifier() {
    val builder = FunSpec.builder("synchronizedMethod")
    builder.jvmModifiers(listOf(Modifier.SYNCHRONIZED))

    assertThat(builder.build().toString()).isEqualTo(
      """
      |@kotlin.jvm.Synchronized
      |internal fun synchronizedMethod() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun ensureTrailingNewline() {
    val methodSpec = FunSpec.builder("function")
      .addCode("codeWithNoNewline()")
      .build()

    assertThat(methodSpec.toString()).isEqualTo(
      """
      |public fun function() {
      |  codeWithNoNewline()
      |}
      |
      """.trimMargin(),
    )
  }

  /** Ensures that we don't add a duplicate newline if one is already present.  */
  @Test fun ensureTrailingNewlineWithExistingNewline() {
    val methodSpec = FunSpec.builder("function")
      .addCode("codeWithNoNewline()\n") // Have a newline already, so ensure we're not adding one
      .build()

    assertThat(methodSpec.toString()).isEqualTo(
      """
      |public fun function() {
      |  codeWithNoNewline()
      |}
      |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/947
  @Test fun ensureTrailingNewlineWithExpressionBody() {
    val methodSpec = FunSpec.builder("function")
      .returns(STRING)
      .addCode("return codeWithNoNewline()")
      .build()

    assertThat(methodSpec.toString()).isEqualTo(
      """
      |public fun function(): kotlin.String = codeWithNoNewline()
      |
      """.trimMargin(),
    )
  }

  @Test fun ensureTrailingNewlineWithExpressionBodyAndExistingNewline() {
    val methodSpec = FunSpec.builder("function")
      .returns(STRING)
      .addCode("return codeWithNoNewline()\n") // Have a newline already, so ensure we're not adding one
      .build()

    assertThat(methodSpec.toString()).isEqualTo(
      """
      |public fun function(): kotlin.String = codeWithNoNewline()
      |
      """.trimMargin(),
    )
  }

  @Test fun ensureKdocTrailingNewline() {
    val methodSpec = FunSpec.builder("function")
      .addKdoc("This is a comment with no initial newline")
      .build()

    assertThat(methodSpec.toString()).isEqualTo(
      """
      |/**
      | * This is a comment with no initial newline
      | */
      |public fun function() {
      |}
      |
      """.trimMargin(),
    )
  }

  /** Ensures that we don't add a duplicate newline if one is already present.  */
  @Test fun ensureKdocTrailingNewlineWithExistingNewline() {
    val methodSpec = FunSpec.builder("function")
      .addKdoc("This is a comment with an initial newline\n")
      .build()

    assertThat(methodSpec.toString()).isEqualTo(
      """
      |/**
      | * This is a comment with an initial newline
      | */
      |public fun function() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun annotatedLambdaReceiverType() {
    val annotation = AnnotationSpec.builder(ClassName("com.squareup.tacos", "Annotation")).build()
    val type = LambdaTypeName.get(returnType = UNIT).copy(annotations = listOf(annotation))
    val spec = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .receiver(type)
          .build(),
      )
      .build()
    assertThat(spec.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Unit
      |
      |public fun (@Annotation () -> Unit).foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun annotatedLambdaReturnType() {
    val annotation = AnnotationSpec.builder(ClassName("com.squareup.tacos", "Annotation")).build()
    val type = LambdaTypeName.get(returnType = UNIT).copy(annotations = listOf(annotation))
    val spec = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .returns(type)
          .build(),
      )
      .build()
    assertThat(spec.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Unit
      |
      |public fun foo(): @Annotation () -> Unit {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun memberNameBuilder() {
    val name = MemberName("com.example", "myCoolFunction")
    val spec = FunSpec.builder(name)
      .returns(STRING)
      .addStatement("""return "hey"""")
      .build()
    assertThat(spec.toString()).isEqualTo(
      """
      |public fun myCoolFunction(): kotlin.String = "hey"
      |
      """.trimMargin(),
    )
  }

  @Test fun importAliasWithNullableReturn() {
    val packageName = "org.example"
    val className = ClassName(packageName, "PersonId")
    val spec = FileSpec.builder("org.example", "SomeFile")
      .addAliasedImport(className, "PID")
      .addFunction(
        FunSpec.builder("pid")
          .returns(className.copy(nullable = true))
          .addCode("return %T()", className)
          .build(),
      )
      .build()
    assertThat(spec.toString()).isEqualTo(
      """
      |package org.example
      |
      |import org.example.PersonId as PID
      |
      |public fun pid(): PID? = PID()
      |
      """.trimMargin(),
    )
  }

  @Test fun importAliasWithNullableReturnNestedClass() {
    val packageName = "org.example"
    val className = ClassName(packageName, "Person", "Id")
    val spec = FileSpec.builder("org.example", "SomeFile")
      .addAliasedImport(className, "PID")
      .addFunction(
        FunSpec.builder("pid")
          .returns(className.copy(nullable = true))
          .addCode("return %T()", className)
          .build(),
      )
      .build()
    assertThat(spec.toString()).isEqualTo(
      """
    |package org.example
    |
    |import org.example.Person.Id as PID
    |
    |public fun pid(): PID? = PID()
    |
      """.trimMargin(),
    )
  }

  // https://github.com/square/kotlinpoet/issues/1979
  @Test fun returnExpressionMultipleStatements() {
    val spec = FunSpec.builder("three")
      .returns(INT)
      .addStatement("return 1")
      .addStatement(".plus(2)")
      .build()

    assertThat(spec.toString()).isEqualTo(
      """
      |public fun three(): kotlin.Int {
      |  return 1
      |  .plus(2)
      |}
      |
      """.trimMargin(),
    )
  }
}
