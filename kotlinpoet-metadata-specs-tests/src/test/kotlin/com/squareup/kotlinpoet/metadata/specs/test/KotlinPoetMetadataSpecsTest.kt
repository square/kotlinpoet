/*
 * Copyright (C) 2019 Square, Inc.
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
@file:Suppress("RemoveRedundantQualifierName", "RedundantSuspendModifier", "NOTHING_TO_INLINE")
package com.squareup.kotlinpoet.metadata.specs.test

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.elementhandler.elements.ElementsElementHandler
import com.squareup.kotlinpoet.elementhandler.reflective.ReflectiveElementHandler
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmConstructor
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.ImmutableKmTypeParameter
import com.squareup.kotlinpoet.metadata.ImmutableKmValueParameter
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ElementHandler
import com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.ElementHandlerType.ELEMENTS
import com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.ElementHandlerType.REFLECTIVE
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.tag
import org.junit.Assume
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.model.Statement
import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import kotlin.test.fail

@KotlinPoetMetadataPreview
@Suppress("unused", "UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class KotlinPoetMetadataSpecsTest(
  elementHandlerType: ElementHandlerType,
  private val elementHandlerFactoryCreator: (KotlinPoetMetadataSpecsTest) -> (() -> ElementHandler)
) {

  companion object {
    @Suppress("RedundantLambdaArrow") // Needed for lambda type resolution
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<*>> {
      return listOf(
          arrayOf<Any>(
              ElementHandlerType.REFLECTIVE,
              { _: KotlinPoetMetadataSpecsTest -> { ReflectiveElementHandler.create() } }
          ),
          arrayOf<Any>(
              ElementHandlerType.ELEMENTS,
              { test: KotlinPoetMetadataSpecsTest -> {
                ElementsElementHandler.create(test.compilation.elements, test.compilation.types)
              } }
          )
      )
    }
  }

  enum class ElementHandlerType {
    REFLECTIVE, ELEMENTS
  }

  @Retention(RUNTIME)
  @Target(AnnotationTarget.FUNCTION)
  @Inherited
  annotation class IgnoreForHandlerType(
    val reason: String,
    val handlerType: ElementHandlerType
  )

  class IgnoreForElementsRule(private val handlerType: ElementHandlerType) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
      return object : Statement() {
        override fun evaluate() {
          val annotation = description.getAnnotation(
              IgnoreForHandlerType::class.java)
          val shouldIgnore = annotation?.handlerType == handlerType
          Assume.assumeTrue(
              "Ignoring ${description.methodName}: ${annotation?.reason}",
              !shouldIgnore
          )
          base.evaluate()
        }
      }
    }
  }

  @Rule
  @JvmField
  val compilation = CompilationRule()

  @Rule
  @JvmField
  val ignoreForElementsRule = IgnoreForElementsRule(
      elementHandlerType)

  private fun KClass<*>.toTypeSpecWithTestHandler(): TypeSpec {
    return toTypeSpec(elementHandlerFactoryCreator(this@KotlinPoetMetadataSpecsTest)())
  }

  @Test
  fun constructorData() {
    val typeSpec = ConstructorClass::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class ConstructorClass(
        val foo: kotlin.String,
        vararg bar: kotlin.Int
      ) {
        constructor(bar: kotlin.Int)
      }
    """.trimIndent())
  }

  class ConstructorClass(val foo: String, vararg bar: Int) {
    // Secondary constructors are ignored, so we expect this constructor to not be the one picked
    // up in the test.
    constructor(bar: Int) : this("defaultFoo")
  }

  @Test
  fun supertype() {
    val typeSpec = Supertype::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Supertype : com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.BaseType(), com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.BaseInterface
    """.trimIndent())
  }

  abstract class BaseType
  interface BaseInterface
  class Supertype : BaseType(), BaseInterface

  @IgnoreForHandlerType(
      reason = "Elements properly resolves the string constant",
      handlerType = ELEMENTS
  )
  @Test
  fun propertiesReflective() {
    val typeSpec = Properties::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Properties {
        var aList: kotlin.collections.List<kotlin.Int> = TODO("Stub!")

        val bar: kotlin.String? = null

        var baz: kotlin.Int = TODO("Stub!")

        val foo: kotlin.String = TODO("Stub!")
      }
    """.trimIndent())
  }

  @IgnoreForHandlerType(
      reason = "Elements properly resolves the string constant",
      handlerType = REFLECTIVE
  )
  @Test
  fun propertiesElements() {
    val typeSpec = Properties::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Properties {
        var aList: kotlin.collections.List<kotlin.Int> = TODO("Stub!")

        val bar: kotlin.String? = null

        var baz: kotlin.Int = TODO("Stub!")

        val foo: kotlin.String = ""
      }
    """.trimIndent())
  }

  class Properties {
    val foo: String = ""
    val bar: String? = null
    var baz: Int = 0
    var aList: List<Int> = emptyList()
  }

  @Test
  fun companionObject() {
    val typeSpec = CompanionObject::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class CompanionObject {
        companion object
      }
    """.trimIndent())
  }

  class CompanionObject {
    companion object
  }

  @Test
  fun namedCompanionObject() {
    val typeSpec = NamedCompanionObject::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class NamedCompanionObject {
        companion object Named
      }
    """.trimIndent())
  }

  class NamedCompanionObject {
    companion object Named
  }

  @Test
  fun generics() {
    val typeSpec = Generics::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Generics<T, in R, V>(
        val genericInput: T
      )
    """.trimIndent())
  }

  class Generics<out T, in R, V>(val genericInput: T)

  @Test
  fun typeAliases() {
    val typeSpec = TypeAliases::class.toTypeSpecWithTestHandler()

    // We always resolve the underlying type of typealiases
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class TypeAliases(
        val foo: kotlin.String,
        val bar: kotlin.collections.List<kotlin.String>
      )
    """.trimIndent())
  }

  class TypeAliases(val foo: TypeAliasName, val bar: GenericTypeAlias)

  @Test
  fun propertyMutability() {
    val typeSpec = PropertyMutability::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class PropertyMutability(
        val foo: kotlin.String,
        var mutableFoo: kotlin.String
      )
    """.trimIndent())
  }

  class PropertyMutability(val foo: String, var mutableFoo: String)

  @Test
  fun collectionMutability() {
    val typeSpec = CollectionMutability::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class CollectionMutability(
        val immutableList: kotlin.collections.List<kotlin.String>,
        val mutableList: kotlin.collections.MutableList<kotlin.String>
      )
    """.trimIndent())
  }

  class CollectionMutability(val immutableList: List<String>, val mutableList: MutableList<String>)

  @Test
  fun suspendTypes() {
    val typeSpec = SuspendTypes::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class SuspendTypes {
        val testProp: suspend (kotlin.Int, kotlin.Long) -> kotlin.String = TODO("Stub!")

        suspend fun testComplexSuspendFun(body: suspend (kotlin.Int, suspend (kotlin.Long) -> kotlin.String) -> kotlin.String) {
        }

        fun testFun(body: suspend (kotlin.Int, kotlin.Long) -> kotlin.String) {
        }

        suspend fun testSuspendFun(param1: kotlin.String) {
        }
      }
    """.trimIndent())
  }

  class SuspendTypes {
    val testProp: suspend (Int, Long) -> String = { _, _ -> "" }

    fun testFun(body: suspend (Int, Long) -> String) {
    }

    suspend fun testSuspendFun(param1: String) {
    }

    suspend fun testComplexSuspendFun(body: suspend (Int, suspend (Long) -> String) -> String) {
    }
  }

  @Test
  fun parameters() {
    val typeSpec = Parameters::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Parameters {
        inline fun hasDefault(param1: kotlin.String = TODO("Stub!")) {
        }

        inline fun inline(crossinline param1: () -> kotlin.String) {
        }

        inline fun noinline(noinline param1: () -> kotlin.String): kotlin.String {
          TODO("Stub!")
        }
      }
    """.trimIndent())
  }

  class Parameters {
    inline fun inline(crossinline param1: () -> String) {
    }

    inline fun noinline(noinline param1: () -> String): String {
      return ""
    }

    inline fun hasDefault(param1: String = "Nope") {
    }
  }

  @Test
  fun lambdaReceiver() {
    val typeSpec = LambdaReceiver::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class LambdaReceiver {
        fun lambdaReceiver(block: kotlin.String.() -> kotlin.Unit) {
        }

        fun lambdaReceiver2(block: kotlin.String.(kotlin.Int) -> kotlin.Unit) {
        }

        fun lambdaReceiver3(block: kotlin.String.(kotlin.Int, kotlin.String) -> kotlin.Unit) {
        }
      }
    """.trimIndent())
  }

  class LambdaReceiver {
    fun lambdaReceiver(block: String.() -> Unit) {
    }
    fun lambdaReceiver2(block: String.(Int) -> Unit) {
    }
    fun lambdaReceiver3(block: String.(Int, String) -> Unit) {
    }
  }

  @Test
  fun nestedTypeAlias() {
    val typeSpec = NestedTypeAliasTest::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class NestedTypeAliasTest {
        val prop: kotlin.collections.List<kotlin.collections.List<kotlin.String>> = TODO("Stub!")
      }
    """.trimIndent())
  }

  class NestedTypeAliasTest {
    val prop: NestedTypeAlias = listOf(listOf(""))
  }

  @Test
  fun inlineClass() {
    val typeSpec = InlineClass::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      inline class InlineClass(
        val value: kotlin.String
      )
    """.trimIndent())
  }

  @Test
  fun functionReferencingTypeParam() {
    val typeSpec = FunctionsReferencingTypeParameters::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class FunctionsReferencingTypeParameters<T> {
        fun test(param: T) {
        }
      }
    """.trimIndent())
  }

  class FunctionsReferencingTypeParameters<T> {
    fun test(param: T) {
    }
  }

  @Test
  fun overriddenThings() {
    val typeSpec = OverriddenThings::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      abstract class OverriddenThings : com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.OverriddenThingsBase(), com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.OverriddenThingsInterface {
        override var openProp: kotlin.String = TODO("Stub!")

        override var openPropInterface: kotlin.String = TODO("Stub!")

        override fun openFunction() {
        }

        override fun openFunctionInterface() {
        }
      }
    """.trimIndent())
  }

  abstract class OverriddenThingsBase {
    abstract var openProp: String

    abstract fun openFunction()
  }

  interface OverriddenThingsInterface {
    var openPropInterface: String

    fun openFunctionInterface()
  }

  abstract class OverriddenThings : OverriddenThingsBase(), OverriddenThingsInterface {
    override var openProp: String = ""
    override var openPropInterface: String = ""

    override fun openFunction() {
    }

    override fun openFunctionInterface() {
    }
  }

  @Test
  fun delegatedProperties() {
    val typeSpec = DelegatedProperties::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class DelegatedProperties {
        /**
         * Note: delegation is ABI stub only and not guaranteed to match source code.
         */
        val immutable: kotlin.String by kotlin.lazy { TODO("Stub!") }

        /**
         * Note: delegation is ABI stub only and not guaranteed to match source code.
         */
        val immutableNullable: kotlin.String? by kotlin.lazy { TODO("Stub!") }

        /**
         * Note: delegation is ABI stub only and not guaranteed to match source code.
         */
        var mutable: kotlin.String by kotlin.properties.Delegates.notNull()

        /**
         * Note: delegation is ABI stub only and not guaranteed to match source code.
         */
        var mutableNullable: kotlin.String? by kotlin.properties.Delegates.observable(null) { _, _, _ -> }
      }
    """.trimIndent())
  }

  class DelegatedProperties {
    val immutable: String by lazy { "" }
    val immutableNullable: String? by lazy { "" }
    var mutable: String by Delegates.notNull()
    var mutableNullable: String? by Delegates.observable(null) { _, _, _ -> }
  }

  @Ignore("Need to be able to know about class delegation in metadata")
  @Test
  fun classDelegation() {
    val typeSpec = ClassDelegation::class.toTypeSpecWithTestHandler()

    // TODO Assert this also excludes functions handled by the delegate
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class ClassDelegation<T>(
        delegate: List<T>
      ): List<T> by delegate
    """.trimIndent())
  }

  class ClassDelegation<T>(delegate: List<T>) : List<T> by delegate

  @Test
  fun simpleEnum() {
    val typeSpec = SimpleEnum::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      enum class SimpleEnum {
        FOO,

        BAR,

        BAZ
      }
    """.trimIndent())
  }

  enum class SimpleEnum {
    FOO, BAR, BAZ
  }

  @Test
  fun complexEnum() {
    val typeSpec = ComplexEnum::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      enum class ComplexEnum(
        val value: kotlin.String
      ) {
        FOO {
          override fun toString(): kotlin.String {
            TODO("Stub!")
          }
        },

        BAR {
          override fun toString(): kotlin.String {
            TODO("Stub!")
          }
        },

        BAZ {
          override fun toString(): kotlin.String {
            TODO("Stub!")
          }
        };
      }
    """.trimIndent())
  }

  enum class ComplexEnum(val value: String) {
    FOO("foo") {
      override fun toString(): String {
        return "foo1"
      }
    },
    BAR("bar") {
      override fun toString(): String {
        return "bar1"
      }
    },
    BAZ("baz") {
      override fun toString(): String {
        return "baz1"
      }
    }
  }

  @Test
  fun interfaces() {
    val testInterfaceSpec = TestInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(testInterfaceSpec.trimmedToString()).isEqualTo("""
      interface TestInterface {
        fun complex(input: kotlin.String, input2: kotlin.String = TODO("Stub!")): kotlin.String {
          TODO("Stub!")
        }

        fun hasDefault() {
        }

        fun hasDefaultMultiParam(input: kotlin.String, input2: kotlin.String): kotlin.String {
          TODO("Stub!")
        }

        fun hasDefaultSingleParam(input: kotlin.String): kotlin.String {
          TODO("Stub!")
        }

        @kotlin.jvm.JvmDefault
        fun hasJvmDefault() {
        }

        fun noDefault()

        fun noDefaultWithInput(input: kotlin.String)

        fun noDefaultWithInputDefault(input: kotlin.String = TODO("Stub!"))
      }
    """.trimIndent())

    val subInterfaceSpec = SubInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(subInterfaceSpec.trimmedToString()).isEqualTo("""
      interface SubInterface : com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.TestInterface {
        override fun hasDefault() {
        }

        @kotlin.jvm.JvmDefault
        override fun hasJvmDefault() {
        }

        fun subInterfaceFunction() {
        }
      }
    """.trimIndent())

    val implSpec = TestSubInterfaceImpl::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(implSpec.trimmedToString()).isEqualTo("""
      class TestSubInterfaceImpl : com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.SubInterface {
        override fun noDefault() {
        }

        override fun noDefaultWithInput(input: kotlin.String) {
        }

        override fun noDefaultWithInputDefault(input: kotlin.String) {
        }
      }
    """.trimIndent())
  }

  interface TestInterface {

    fun noDefault()

    fun noDefaultWithInput(input: String)

    fun noDefaultWithInputDefault(input: String = "")

    @JvmDefault
    fun hasJvmDefault() {
    }

    fun hasDefault() {
    }

    fun hasDefaultSingleParam(input: String): String {
      return "1234"
    }

    fun hasDefaultMultiParam(input: String, input2: String): String {
      return "1234"
    }

    fun complex(input: String, input2: String = ""): String {
      return "5678"
    }
  }

  interface SubInterface : TestInterface {
    fun subInterfaceFunction() {
    }

    @JvmDefault
    override fun hasJvmDefault() {
      super.hasJvmDefault()
    }

    override fun hasDefault() {
      super.hasDefault()
    }
  }

  class TestSubInterfaceImpl : SubInterface {
    override fun noDefault() {
    }

    override fun noDefaultWithInput(input: String) {
    }

    override fun noDefaultWithInputDefault(input: String) {
    }
  }

  @Test
  fun backwardReferencingTypeVars() {
    val typeSpec = BackwardReferencingTypeVars::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      interface BackwardReferencingTypeVars<T> : kotlin.collections.List<kotlin.collections.Set<T>>
    """.trimIndent())
  }

  interface BackwardReferencingTypeVars<T> : List<Set<T>>

  @Test
  fun taggedTypes() {
    val typeSpec = TaggedTypes::class.toTypeSpecWithTestHandler()
    assertThat(typeSpec.tag<ImmutableKmClass>()).isNotNull()

    val constructorSpec = typeSpec.primaryConstructor ?: fail("No constructor found!")
    assertThat(constructorSpec.tag<ImmutableKmConstructor>()).isNotNull()

    val parameterSpec = constructorSpec.parameters[0]
    assertThat(parameterSpec.tag<ImmutableKmValueParameter>()).isNotNull()

    val typeVar = typeSpec.typeVariables[0]
    assertThat(typeVar.tag<ImmutableKmTypeParameter>()).isNotNull()

    val funSpec = typeSpec.funSpecs[0]
    assertThat(funSpec.tag<ImmutableKmFunction>()).isNotNull()

    val propertySpec = typeSpec.propertySpecs[0]
    assertThat(propertySpec.tag<ImmutableKmProperty>()).isNotNull()
  }

  class TaggedTypes<T>(val param: T) {
    val property: String = ""

    fun function() {
    }
  }

  @Test
  fun annotations() {
    val typeSpec = MyAnnotation::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      annotation class MyAnnotation(
        val value: kotlin.String
      )
    """.trimIndent())
  }

  annotation class MyAnnotation(val value: String)

  @Test
  fun functionTypeArgsSupersedeClass() {
    val typeSpec = GenericClass::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class GenericClass<T> {
        fun <T> functionAlsoWithT(param: T) {
        }

        fun <R> functionWithADifferentType(param: R) {
        }

        fun functionWithT(param: T) {
        }
      }
    """.trimIndent())

    val func1TypeVar = typeSpec.funSpecs.find { it.name == "functionAlsoWithT" }!!.typeVariables.first()
    val classTypeVar = typeSpec.typeVariables.first()

    assertThat(func1TypeVar).isNotSameInstanceAs(classTypeVar)
  }

  class GenericClass<T> {
    fun functionWithT(param: T) {
    }
    fun <T> functionAlsoWithT(param: T) {
    }
    fun <R> functionWithADifferentType(param: R) {
    }
  }

  @Test
  fun complexCompanionObject() {
    val typeSpec = ComplexCompanionObject::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class ComplexCompanionObject {
        companion object ComplexObject : com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.CompanionBase(), com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.CompanionInterface
      }
    """.trimIndent())
  }

  interface CompanionInterface
  open class CompanionBase

  class ComplexCompanionObject {
    companion object ComplexObject : CompanionBase(), CompanionInterface
  }

  @IgnoreForHandlerType(
      reason = "TODO Synthetic methods that hold annotations aren't visible in these tests",
      handlerType = ELEMENTS
  )
  @Test
  fun annotationsAreCopied() {
    val typeSpec = AnnotationHolders::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class AnnotationHolders @com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.ConstructorAnnotation constructor() {
        @field:com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.FieldAnnotation
        var field: kotlin.String? = null

        @get:com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.GetterAnnotation
        var getter: kotlin.String? = null

        @com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.HolderAnnotation
        @kotlin.jvm.JvmField
        var holder: kotlin.String? = null

        @set:com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.SetterAnnotation
        var setter: kotlin.String? = null

        @com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.ConstructorAnnotation
        constructor(value: kotlin.String)

        @com.squareup.kotlinpoet.metadata.specs.test.KotlinPoetMetadataSpecsTest.FunctionAnnotation
        fun function() {
        }
      }
    """.trimIndent())
  }

  class AnnotationHolders @ConstructorAnnotation constructor() {

    @ConstructorAnnotation
    constructor(value: String) : this()

    @field:FieldAnnotation
    var field: String? = null
    @get:GetterAnnotation
    var getter: String? = null
    @set:SetterAnnotation
    var setter: String? = null
    @HolderAnnotation
    @JvmField var holder: String? = null
    @FunctionAnnotation
    fun function() {
    }
  }

  @Retention(RUNTIME)
  annotation class ConstructorAnnotation

  @Retention(RUNTIME)
  annotation class FieldAnnotation

  @Retention(RUNTIME)
  annotation class GetterAnnotation

  @Retention(RUNTIME)
  annotation class SetterAnnotation

  @Retention(RUNTIME)
  annotation class HolderAnnotation

  @Retention(RUNTIME)
  annotation class FunctionAnnotation

  @IgnoreForHandlerType(
      reason = "Elements properly resolves the regular properties + JvmStatic, but reflection will not",
      handlerType = REFLECTIVE
  )
  @Test
  fun constantValuesElements() {
    val typeSpec = Constants::class.toTypeSpecWithTestHandler()

    // Note: formats like hex/binary/underscore are not available as formatted at runtime
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Constants(
        val param: kotlin.String = TODO("Stub!")
      ) {
        val binaryProp: kotlin.Int = 11

        val boolProp: kotlin.Boolean = false

        val doubleProp: kotlin.Double = 1.0

        val floatProp: kotlin.Float = 1.0F

        val hexProp: kotlin.Int = 15

        val intProp: kotlin.Int = 1

        val longProp: kotlin.Long = 1L

        val stringProp: kotlin.String = "prop"

        val underscoresHexProp: kotlin.Long = 4293713502L

        val underscoresProp: kotlin.Int = 1000000

        companion object {
          const val CONST_BINARY_PROP: kotlin.Int = 11

          const val CONST_BOOL_PROP: kotlin.Boolean = false

          const val CONST_DOUBLE_PROP: kotlin.Double = 1.0

          const val CONST_FLOAT_PROP: kotlin.Float = 1.0F

          const val CONST_HEX_PROP: kotlin.Int = 15

          const val CONST_INT_PROP: kotlin.Int = 1

          const val CONST_LONG_PROP: kotlin.Long = 1L

          const val CONST_STRING_PROP: kotlin.String = "prop"

          const val CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4293713502L

          const val CONST_UNDERSCORES_PROP: kotlin.Int = 1000000

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_BINARY_PROP: kotlin.Int = 11

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_BOOL_PROP: kotlin.Boolean = false

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_DOUBLE_PROP: kotlin.Double = 1.0

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_FLOAT_PROP: kotlin.Float = 1.0F

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_HEX_PROP: kotlin.Int = 15

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_INT_PROP: kotlin.Int = 1

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_LONG_PROP: kotlin.Long = 1L

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_STRING_PROP: kotlin.String = "prop"

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4293713502L

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_UNDERSCORES_PROP: kotlin.Int = 1000000
        }
      }
    """.trimIndent())

    // TODO check with objects
  }

  @IgnoreForHandlerType(
      reason = "Elements properly resolves the regular properties + JvmStatic, but reflection will not",
      handlerType = ELEMENTS
  )
  @Test
  fun constantValuesReflective() {
    val typeSpec = Constants::class.toTypeSpecWithTestHandler()

    // Note: formats like hex/binary/underscore are not available as formatted in elements
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Constants(
        val param: kotlin.String = TODO("Stub!")
      ) {
        val binaryProp: kotlin.Int = TODO("Stub!")

        val boolProp: kotlin.Boolean = TODO("Stub!")

        val doubleProp: kotlin.Double = TODO("Stub!")

        val floatProp: kotlin.Float = TODO("Stub!")

        val hexProp: kotlin.Int = TODO("Stub!")

        val intProp: kotlin.Int = TODO("Stub!")

        val longProp: kotlin.Long = TODO("Stub!")

        val stringProp: kotlin.String = TODO("Stub!")

        val underscoresHexProp: kotlin.Long = TODO("Stub!")

        val underscoresProp: kotlin.Int = TODO("Stub!")

        companion object {
          const val CONST_BINARY_PROP: kotlin.Int = 11

          const val CONST_BOOL_PROP: kotlin.Boolean = false

          const val CONST_DOUBLE_PROP: kotlin.Double = 1.0

          const val CONST_FLOAT_PROP: kotlin.Float = 1.0F

          const val CONST_HEX_PROP: kotlin.Int = 15

          const val CONST_INT_PROP: kotlin.Int = 1

          const val CONST_LONG_PROP: kotlin.Long = 1L

          const val CONST_STRING_PROP: kotlin.String = "prop"

          const val CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4293713502L

          const val CONST_UNDERSCORES_PROP: kotlin.Int = 1000000

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_BINARY_PROP: kotlin.Int = 11

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_BOOL_PROP: kotlin.Boolean = false

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_DOUBLE_PROP: kotlin.Double = 1.0

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_FLOAT_PROP: kotlin.Float = 1.0F

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_HEX_PROP: kotlin.Int = 15

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_INT_PROP: kotlin.Int = 1

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_LONG_PROP: kotlin.Long = 1L

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_STRING_PROP: kotlin.String = "prop"

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4293713502L

          @kotlin.jvm.JvmStatic
          val STATIC_CONST_UNDERSCORES_PROP: kotlin.Int = 1000000
        }
      }
    """.trimIndent())
  }

  class Constants(
    val param: String = "param"
  ) {
    val boolProp = false
    val binaryProp = 0b00001011
    val intProp = 1
    val underscoresProp = 1_000_000
    val hexProp = 0x0F
    val underscoresHexProp = 0xFF_EC_DE_5E
    val longProp = 1L
    val floatProp = 1.0F
    val doubleProp = 1.0
    val stringProp = "prop"

    companion object {
      @JvmStatic val STATIC_CONST_BOOL_PROP = false
      @JvmStatic val STATIC_CONST_BINARY_PROP = 0b00001011
      @JvmStatic val STATIC_CONST_INT_PROP = 1
      @JvmStatic val STATIC_CONST_UNDERSCORES_PROP = 1_000_000
      @JvmStatic val STATIC_CONST_HEX_PROP = 0x0F
      @JvmStatic val STATIC_CONST_UNDERSCORES_HEX_PROP = 0xFF_EC_DE_5E
      @JvmStatic val STATIC_CONST_LONG_PROP = 1L
      @JvmStatic val STATIC_CONST_FLOAT_PROP = 1.0f
      @JvmStatic val STATIC_CONST_DOUBLE_PROP = 1.0
      @JvmStatic val STATIC_CONST_STRING_PROP = "prop"

      const val CONST_BOOL_PROP = false
      const val CONST_BINARY_PROP = 0b00001011
      const val CONST_INT_PROP = 1
      const val CONST_UNDERSCORES_PROP = 1_000_000
      const val CONST_HEX_PROP = 0x0F
      const val CONST_UNDERSCORES_HEX_PROP = 0xFF_EC_DE_5E
      const val CONST_LONG_PROP = 1L
      const val CONST_FLOAT_PROP = 1.0f
      const val CONST_DOUBLE_PROP = 1.0
      const val CONST_STRING_PROP = "prop"
    }
  }

  @Test
  fun jvmAnnotations() {
    val typeSpec = JvmAnnotations::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class JvmAnnotations {
        @get:kotlin.jvm.Synchronized
        val synchronizedGetProp: kotlin.String? = null

        @set:kotlin.jvm.Synchronized
        var synchronizedSetProp: kotlin.String? = null

        @kotlin.jvm.Transient
        val transientProp: kotlin.String? = null

        @kotlin.jvm.Volatile
        var volatileProp: kotlin.String? = null

        @kotlin.jvm.Synchronized
        fun synchronizedFun() {
        }
      }
    """.trimIndent())

    val interfaceSpec = JvmAnnotationsInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(interfaceSpec.trimmedToString()).isEqualTo("""
      interface JvmAnnotationsInterface {
        @kotlin.jvm.JvmDefault
        fun defaultMethod() {
        }

        fun notDefaultMethod()
      }
    """.trimIndent())
  }

  class JvmAnnotations {
    @Transient val transientProp: String? = null
    @Volatile var volatileProp: String? = null
    @get:Synchronized val synchronizedGetProp: String? = null
    @set:Synchronized var synchronizedSetProp: String? = null

    @Synchronized
    fun synchronizedFun() {
    }
  }

  interface JvmAnnotationsInterface {
    @JvmDefault
    fun defaultMethod() {
    }
    fun notDefaultMethod()
  }

  @Test
  fun nestedClasses() {
    val typeSpec = NestedClasses::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class NestedClasses {
        abstract class NestedClass<T> : kotlin.collections.List<T>

        inner class NestedInnerClass
      }
    """.trimIndent())
  }

  class NestedClasses {
    abstract class NestedClass<T> : List<T>
    inner class NestedInnerClass
  }

  @Test
  fun jvmNames() {
    val typeSpec = JvmNameData::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class JvmNameData(
        @get:kotlin.jvm.JvmName(name = "jvmParam")
        val param: kotlin.String
      ) {
        @get:kotlin.jvm.JvmName(name = "jvmPropertyGet")
        val propertyGet: kotlin.String? = null

        @get:kotlin.jvm.JvmName(name = "jvmPropertyGetAndSet")
        @set:kotlin.jvm.JvmName(name = "jvmPropertyGetAndSet")
        var propertyGetAndSet: kotlin.String? = null

        @set:kotlin.jvm.JvmName(name = "jvmPropertySet")
        var propertySet: kotlin.String? = null

        @kotlin.jvm.JvmName(name = "jvmFunction")
        fun function() {
        }

        interface InterfaceWithJvmName {
          companion object {
            @kotlin.jvm.JvmStatic
            val FOO_BOOL: kotlin.Boolean = false

            @kotlin.jvm.JvmStatic
            @kotlin.jvm.JvmName(name = "jvmStaticFunction")
            fun staticFunction() {
            }
          }
        }
      }
    """.trimIndent())
  }

  class JvmNameData(
    @get:JvmName("jvmParam") val param: String
  ) {

    @get:JvmName("jvmPropertyGet")
    val propertyGet: String? = null

    @set:JvmName("jvmPropertySet")
    var propertySet: String? = null

    @set:JvmName("jvmPropertyGetAndSet")
    @get:JvmName("jvmPropertyGetAndSet")
    var propertyGetAndSet: String? = null

    @JvmName("jvmFunction")
    fun function() {
    }

    // Interfaces can't have JvmName, but covering a potential edge case of having a companion
    // object with JvmName elements. Also covers an edge case where constants have getters
    interface InterfaceWithJvmName {
      companion object {
        @JvmStatic
        @get:JvmName("fooBoolJvm")
        val FOO_BOOL = false

        @JvmName("jvmStaticFunction")
        @JvmStatic
        fun staticFunction() {
        }
      }
    }
  }

  @IgnoreForHandlerType(
      reason = "JvmOverloads is not runtime retained and thus not visible to reflection.",
      handlerType = REFLECTIVE
  )
  @Test
  fun overloads() {
    val typeSpec = Overloads::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Overloads @kotlin.jvm.JvmOverloads constructor(
        val param1: kotlin.String,
        val optionalParam2: kotlin.String = TODO("Stub!"),
        val nullableParam3: kotlin.String? = TODO("Stub!")
      ) {
        @kotlin.jvm.JvmOverloads
        fun testFunction(
          param1: kotlin.String,
          optionalParam2: kotlin.String = TODO("Stub!"),
          nullableParam3: kotlin.String? = TODO("Stub!")
        ) {
        }
      }
    """.trimIndent())
  }

  class Overloads @JvmOverloads constructor(
    val param1: String,
    val optionalParam2: String = "",
    val nullableParam3: String? = null
  ) {
    @JvmOverloads
    fun testFunction(
      param1: String,
      optionalParam2: String = "",
      nullableParam3: String? = null
    ) {
    }
  }

  @IgnoreForHandlerType(
      reason = "Elements generates initializer values.",
      handlerType = ELEMENTS
  )
  @Test
  fun jvmFields_reflective() {
    val typeSpec = Fields::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Fields(
        @kotlin.jvm.JvmField
        val param1: kotlin.String
      ) {
        @kotlin.jvm.JvmField
        val fieldProp: kotlin.String = TODO("Stub!")

        companion object {
          @kotlin.jvm.JvmField
          val companionProp: kotlin.String = ""

          const val constCompanionProp: kotlin.String = ""

          @kotlin.jvm.JvmStatic
          val staticCompanionProp: kotlin.String = ""
        }
      }
    """.trimIndent())
  }

  @IgnoreForHandlerType(
      reason = "Elements generates initializer values.",
      handlerType = REFLECTIVE
  )
  @Test
  fun jvmFields_elements() {
    val typeSpec = Fields::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Fields(
        @field:kotlin.jvm.JvmField
        val param1: kotlin.String
      ) {
        @field:kotlin.jvm.JvmField
        val fieldProp: kotlin.String = ""

        companion object {
          @kotlin.jvm.JvmField
          val companionProp: kotlin.String = ""

          const val constCompanionProp: kotlin.String = ""

          @kotlin.jvm.JvmStatic
          val staticCompanionProp: kotlin.String = ""
        }
      }
    """.trimIndent())
  }

  class Fields(
    @JvmField val param1: String
  ) {
    @JvmField val fieldProp: String = ""

    companion object {
      @JvmField val companionProp: String = ""
      @JvmStatic val staticCompanionProp: String = ""
      const val constCompanionProp: String = ""
    }
  }

  @IgnoreForHandlerType(
      reason = "Synthetic constructs aren't available in elements, so some information like " +
          "JvmStatic can't be deduced.",
      handlerType = ELEMENTS
  )
  @Test
  fun synthetics_reflective() {
    val typeSpec = Synthetics::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Synthetics(
        @get:kotlin.jvm.JvmSynthetic
        val param: kotlin.String
      ) {
        @field:kotlin.jvm.JvmSynthetic
        val fieldProperty: kotlin.String? = null

        @field:kotlin.jvm.JvmSynthetic
        val property: kotlin.String? = null

        @get:kotlin.jvm.JvmSynthetic
        val propertyGet: kotlin.String? = null

        @get:kotlin.jvm.JvmSynthetic
        @set:kotlin.jvm.JvmSynthetic
        var propertyGetAndSet: kotlin.String? = null

        @set:kotlin.jvm.JvmSynthetic
        var propertySet: kotlin.String? = null

        /**
         * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
         */
        @kotlin.jvm.JvmSynthetic
        fun function() {
        }

        interface InterfaceWithJvmName {
          /**
           * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
           */
          @kotlin.jvm.JvmSynthetic
          fun interfaceFunction()

          companion object {
            @kotlin.jvm.JvmStatic
            val FOO_BOOL: kotlin.Boolean = false

            /**
             * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
             */
            @kotlin.jvm.JvmSynthetic
            @kotlin.jvm.JvmStatic
            fun staticFunction() {
            }
          }
        }
      }
    """.trimIndent())
  }

  @IgnoreForHandlerType(
      reason = "Synthetic constructs aren't available in elements, so some information like " +
          "JvmStatic can't be deduced.",
      handlerType = REFLECTIVE
  )
  @Test
  fun synthetics_elements() {
    val typeSpec = Synthetics::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Synthetics(
        @get:kotlin.jvm.JvmSynthetic
        val param: kotlin.String
      ) {
        @field:kotlin.jvm.JvmSynthetic
        val fieldProperty: kotlin.String? = null

        @field:kotlin.jvm.JvmSynthetic
        val property: kotlin.String? = null

        @get:kotlin.jvm.JvmSynthetic
        val propertyGet: kotlin.String? = null

        @get:kotlin.jvm.JvmSynthetic
        @set:kotlin.jvm.JvmSynthetic
        var propertyGetAndSet: kotlin.String? = null

        @set:kotlin.jvm.JvmSynthetic
        var propertySet: kotlin.String? = null

        /**
         * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
         */
        @kotlin.jvm.JvmSynthetic
        fun function() {
        }

        interface InterfaceWithJvmName {
          /**
           * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
           */
          @kotlin.jvm.JvmSynthetic
          fun interfaceFunction()

          companion object {
            @kotlin.jvm.JvmStatic
            val FOO_BOOL: kotlin.Boolean = false

            /**
             * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
             */
            @kotlin.jvm.JvmSynthetic
            fun staticFunction() {
            }
          }
        }
      }
    """.trimIndent())
  }

  class Synthetics(
    @get:JvmSynthetic val param: String
  ) {

    @JvmSynthetic
    val property: String? = null

    @field:JvmSynthetic
    val fieldProperty: String? = null

    @get:JvmSynthetic
    val propertyGet: String? = null

    @set:JvmSynthetic
    var propertySet: String? = null

    @set:JvmSynthetic
    @get:JvmSynthetic
    var propertyGetAndSet: String? = null

    @JvmSynthetic
    fun function() {
    }

    // Interfaces can have JvmSynthetic, so covering a potential edge case of having a companion
    // object with JvmSynthetic elements. Also covers an edge case where constants have getters
    interface InterfaceWithJvmName {
      @JvmSynthetic
      fun interfaceFunction()

      companion object {
        @JvmStatic
        @get:JvmSynthetic
        val FOO_BOOL = false

        @JvmSynthetic
        @JvmStatic
        fun staticFunction() {
        }
      }
    }
  }

  @Test
  fun throws() {
    val typeSpec = Throwing::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Throwing @kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class]) constructor() {
        @get:kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        @set:kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        var getterAndSetterThrows: kotlin.String? = null

        @get:kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        val getterThrows: kotlin.String? = null

        @set:kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        var setterThrows: kotlin.String? = null

        @kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        fun testFunction() {
        }
      }
      """.trimIndent())
  }

  class Throwing @Throws(IllegalStateException::class) constructor() {

    @get:Throws(IllegalStateException::class)
    val getterThrows: String? = null

    @set:Throws(IllegalStateException::class)
    var setterThrows: String? = null

    @get:Throws(IllegalStateException::class)
    @set:Throws(IllegalStateException::class)
    var getterAndSetterThrows: String? = null

    @Throws(IllegalStateException::class)
    fun testFunction() {
    }
  }

  // The meta-ist of metadata meta-tests.
  @Test
  fun metaTest() {
    val typeSpec = Metadata::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      annotation class Metadata(
        @get:kotlin.jvm.JvmName(name = "k")
        val kind: kotlin.Int = TODO("Stub!"),
        @get:kotlin.jvm.JvmName(name = "mv")
        val metadataVersion: kotlin.IntArray = TODO("Stub!"),
        @get:kotlin.jvm.JvmName(name = "bv")
        val bytecodeVersion: kotlin.IntArray = TODO("Stub!"),
        @get:kotlin.jvm.JvmName(name = "d1")
        val data1: kotlin.Array<kotlin.String> = TODO("Stub!"),
        @get:kotlin.jvm.JvmName(name = "d2")
        val data2: kotlin.Array<kotlin.String> = TODO("Stub!"),
        @get:kotlin.jvm.JvmName(name = "xs")
        val extraString: kotlin.String = TODO("Stub!"),
        @get:kotlin.jvm.JvmName(name = "pn")
        val packageName: kotlin.String = TODO("Stub!"),
        @get:kotlin.jvm.JvmName(name = "xi")
        val extraInt: kotlin.Int = TODO("Stub!")
      )
      """.trimIndent())
  }

  @Test
  fun classNamesAndNesting() {
    // Make sure we parse class names correctly at all levels
    val typeSpec = ClassNesting::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class ClassNesting {
        class NestedClass {
          class SuperNestedClass {
            inner class SuperDuperInnerClass
          }
        }
      }
      """.trimIndent())
  }
}

class ClassNesting {
  class NestedClass {
    class SuperNestedClass {
      inner class SuperDuperInnerClass
    }
  }
}

private fun TypeSpec.trimmedToString(): String {
  return toString().trim()
}

inline class InlineClass(val value: String)

typealias TypeAliasName = String
typealias GenericTypeAlias = List<String>
typealias NestedTypeAlias = List<GenericTypeAlias>
