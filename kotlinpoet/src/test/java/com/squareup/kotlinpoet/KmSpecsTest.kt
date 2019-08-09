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
package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.KmSpecsTest.ElementHandlerType.ELEMENTS
import com.squareup.kotlinpoet.KmSpecsTest.ElementHandlerType.REFLECTIVE
import com.squareup.kotlinpoet.km.ImmutableKmClass
import com.squareup.kotlinpoet.km.ImmutableKmConstructor
import com.squareup.kotlinpoet.km.ImmutableKmFunction
import com.squareup.kotlinpoet.km.ImmutableKmProperty
import com.squareup.kotlinpoet.km.ImmutableKmValueParameter
import com.squareup.kotlinpoet.km.KotlinPoetKm
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

@KotlinPoetKm
@Suppress("unused", "UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class KmSpecsTest(
  elementHandlerType: ElementHandlerType,
  private val elementHandlerFactoryCreator: (KmSpecsTest) -> (() -> ElementHandler)
) {

  companion object {
    @Suppress("RedundantLambdaArrow") // Needed for lambda type resolution
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() : Collection<Array<*>> {
      return listOf(
          arrayOf<Any>(
              ElementHandlerType.REFLECTIVE,
              { _: KmSpecsTest -> { ElementHandler.reflective() } }
          ),
          arrayOf<Any>(
              ElementHandlerType.ELEMENTS,
              { test: KmSpecsTest -> {
                ElementHandler.fromElements(test.compilation.elements, test.compilation.types)
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
          val annotation = description.getAnnotation(IgnoreForHandlerType::class.java)
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
  val ignoreForElementsRule = IgnoreForElementsRule(elementHandlerType)

  private fun KClass<*>.toTypeSpecWithTestHandler(): TypeSpec {
    return toTypeSpec(elementHandlerFactoryCreator(this@KmSpecsTest)())
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
      class Supertype : com.squareup.kotlinpoet.KmSpecsTest.BaseType(), com.squareup.kotlinpoet.KmSpecsTest.BaseInterface
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
      abstract class OverriddenThings : com.squareup.kotlinpoet.KmSpecsTest.OverriddenThingsBase(), com.squareup.kotlinpoet.KmSpecsTest.OverriddenThingsInterface {
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
          fun toString(): kotlin.String {
            TODO("Stub!")
          }
        },

        BAR {
          fun toString(): kotlin.String {
            TODO("Stub!")
          }
        },

        BAZ {
          fun toString(): kotlin.String {
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
    val typeSpec = SomeInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      interface SomeInterface : com.squareup.kotlinpoet.KmSpecsTest.SomeInterfaceBase {
        fun testFunction()
      }
    """.trimIndent())
  }

  interface SomeInterfaceBase

  interface SomeInterface : SomeInterfaceBase {
    fun testFunction() {
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

    // TODO taggable TypeNames
//    val typeVar = typeSpec.typeVariables[0]
//    assertThat(typeVar.tag<ImmutableKmTypeParameter>()).isNotNull()

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
        companion object ComplexObject : com.squareup.kotlinpoet.KmSpecsTest.CompanionBase(), com.squareup.kotlinpoet.KmSpecsTest.CompanionInterface
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
      class AnnotationHolders @com.squareup.kotlinpoet.KmSpecsTest.ConstructorAnnotation constructor() {
        @field:com.squareup.kotlinpoet.KmSpecsTest.FieldAnnotation
        var field: kotlin.String? = null

        @get:com.squareup.kotlinpoet.KmSpecsTest.GetterAnnotation
        var getter: kotlin.String? = null
          get() {
            TODO("Stub!")
          }

        @com.squareup.kotlinpoet.KmSpecsTest.HolderAnnotation
        var holder: kotlin.String? = null

        @set:com.squareup.kotlinpoet.KmSpecsTest.SetterAnnotation
        var setter: kotlin.String? = null
          set
        @com.squareup.kotlinpoet.KmSpecsTest.ConstructorAnnotation
        constructor(value: kotlin.String)

        @com.squareup.kotlinpoet.KmSpecsTest.FunctionAnnotation
        fun function() {
        }
      }
    """.trimIndent())
  }

  class AnnotationHolders @ConstructorAnnotation constructor() {

    @ConstructorAnnotation constructor(value: String) : this()

    @field:FieldAnnotation var field: String? = null
    @get:GetterAnnotation var getter: String? = null
    @set:SetterAnnotation var setter: String? = null
    @HolderAnnotation @JvmField var holder: String? = null
    @FunctionAnnotation fun function() {
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

    // TODO check with objects
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
          get() {
            TODO("Stub!")
          }

        @set:kotlin.jvm.Synchronized
        var synchronizedSetProp: kotlin.String? = null
          set
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
}

private fun TypeSpec.trimmedToString(): String {
  return toString().trim()
}

inline class InlineClass(val value: String)

typealias TypeAliasName = String
typealias GenericTypeAlias = List<String>
typealias NestedTypeAlias = List<GenericTypeAlias>
