/*
 * Copyright (C) 2019 Square, Inc.
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
@file:Suppress(
  "NOTHING_TO_INLINE",
  "RedundantSuspendModifier",
  "RedundantUnitReturnType",
  "RedundantVisibilityModifier",
  "RemoveEmptyPrimaryConstructor",
  "RemoveRedundantQualifierName",
  "UNUSED_PARAMETER",
  "unused",
)

package com.squareup.kotlinpoet.metadata.specs

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.specs.MultiClassInspectorTest.ClassInspectorType.ELEMENTS
import com.squareup.kotlinpoet.metadata.specs.MultiClassInspectorTest.ClassInspectorType.REFLECTIVE
import com.squareup.kotlinpoet.tag
import com.squareup.kotlinpoet.tags.TypeAliasTag
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.TYPE_PARAMETER
import kotlin.metadata.KmClass
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.KmTypeParameter
import kotlin.metadata.KmValueParameter
import kotlin.properties.Delegates
import kotlin.test.fail
import org.junit.Ignore
import org.junit.Test

class KotlinPoetMetadataSpecsTest : MultiClassInspectorTest() {

  @Test
  fun constructorData() {
    val typeSpec = ConstructorClass::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class ConstructorClass(
        public val foo: kotlin.String,
        vararg bar: kotlin.Int,
      ) {
        public constructor(bar: kotlin.Int)
      }
      """.trimIndent(),
    )
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Supertype() : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.BaseType(), com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.BaseInterface
      """.trimIndent(),
    )
  }

  abstract class BaseType
  interface BaseInterface
  class Supertype : BaseType(), BaseInterface

  @IgnoreForHandlerType(
    reason = "Elements properly resolves the string constant",
    handlerType = ELEMENTS,
  )
  @Test
  fun propertiesReflective() {
    val typeSpec = Properties::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Properties() {
        public val foo: kotlin.String = throw NotImplementedError("Stub!")

        public val bar: kotlin.String? = null

        public var baz: kotlin.Int = throw NotImplementedError("Stub!")

        public var aList: kotlin.collections.List<kotlin.Int> = throw NotImplementedError("Stub!")
      }
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    reason = "Elements properly resolves the string constant",
    handlerType = REFLECTIVE,
  )
  @Test
  fun propertiesElements() {
    val typeSpec = Properties::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Properties() {
        public val foo: kotlin.String = throw NotImplementedError("Stub!")

        public val bar: kotlin.String? = null

        public var baz: kotlin.Int = throw NotImplementedError("Stub!")

        public var aList: kotlin.collections.List<kotlin.Int> = throw NotImplementedError("Stub!")
      }
      """.trimIndent(),
    )
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class CompanionObject() {
        public companion object
      }
      """.trimIndent(),
    )
  }

  class CompanionObject {
    companion object
  }

  @Test
  fun namedCompanionObject() {
    val typeSpec = NamedCompanionObject::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class NamedCompanionObject() {
        public companion object Named
      }
      """.trimIndent(),
    )
  }

  class NamedCompanionObject {
    companion object Named
  }

  @Test
  fun generics() {
    val typeSpec = Generics::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Generics<out T, in R, V>(
        public val genericInput: T,
      )
      """.trimIndent(),
    )
  }

  class Generics<out T, in R, V>(val genericInput: T)

  @Test
  fun typeAliases() {
    val typeSpec = TypeAliases::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class TypeAliases(
        public val foo: com.squareup.kotlinpoet.metadata.specs.TypeAliasName,
        public val bar: com.squareup.kotlinpoet.metadata.specs.GenericTypeAlias,
      )
      """.trimIndent(),
    )

    val fooPropertyType = typeSpec.propertySpecs.first { it.name == "foo" }.type
    val fooAliasData = fooPropertyType.tag<TypeAliasTag>()
    checkNotNull(fooAliasData)
    assertThat(fooAliasData.abbreviatedType).isEqualTo(STRING)

    val barPropertyType = typeSpec.propertySpecs.first { it.name == "bar" }.type
    val barAliasData = barPropertyType.tag<TypeAliasTag>()
    checkNotNull(barAliasData)
    assertThat(barAliasData.abbreviatedType).isEqualTo(LIST.parameterizedBy(STRING))
  }

  class TypeAliases(val foo: TypeAliasName, val bar: GenericTypeAlias)

  @Test
  fun propertyMutability() {
    val typeSpec = PropertyMutability::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class PropertyMutability(
        public val foo: kotlin.String,
        public var mutableFoo: kotlin.String,
      )
      """.trimIndent(),
    )
  }

  class PropertyMutability(val foo: String, var mutableFoo: String)

  @Test
  fun collectionMutability() {
    val typeSpec = CollectionMutability::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class CollectionMutability(
        public val immutableList: kotlin.collections.List<kotlin.String>,
        public val mutableList: kotlin.collections.MutableList<kotlin.String>,
      )
      """.trimIndent(),
    )
  }

  class CollectionMutability(val immutableList: List<String>, val mutableList: MutableList<String>)

  @Test
  fun suspendTypes() {
    val typeSpec = SuspendTypes::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class SuspendTypes() {
        public val testProp: suspend (kotlin.Int, kotlin.Long) -> kotlin.String = throw NotImplementedError("Stub!")

        public fun testFun(body: suspend (kotlin.Int, kotlin.Long) -> kotlin.String) {
        }

        public suspend fun testSuspendFun(param1: kotlin.String) {
        }

        public suspend fun testComplexSuspendFun(body: suspend (kotlin.Int, suspend (kotlin.Long) -> kotlin.String) -> kotlin.String) {
        }
      }
      """.trimIndent(),
    )
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Parameters() {
        public inline fun `inline`(crossinline param1: () -> kotlin.String) {
        }

        public inline fun `noinline`(noinline param1: () -> kotlin.String): kotlin.String = throw NotImplementedError("Stub!")

        public inline fun hasDefault(param1: kotlin.String = throw NotImplementedError("Stub!")) {
        }
      }
      """.trimIndent(),
    )
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class LambdaReceiver() {
        public fun lambdaReceiver(block: kotlin.String.() -> kotlin.Unit) {
        }

        public fun lambdaReceiver2(block: kotlin.String.(kotlin.Int) -> kotlin.Unit) {
        }

        public fun lambdaReceiver3(block: kotlin.String.(kotlin.Int, kotlin.String) -> kotlin.Unit) {
        }
      }
      """.trimIndent(),
    )
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class NestedTypeAliasTest() {
        public val prop: com.squareup.kotlinpoet.metadata.specs.NestedTypeAlias = throw NotImplementedError("Stub!")
      }
      """.trimIndent(),
    )
  }

  class NestedTypeAliasTest {
    val prop: NestedTypeAlias = listOf(listOf(""))
  }

  @Test
  fun valueClass() {
    val typeSpec = ValueClass::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      @kotlin.jvm.JvmInline
      public value class ValueClass(
        public val `value`: kotlin.String,
      )
      """.trimIndent(),
    )
  }

  @Test
  fun functionReferencingTypeParam() {
    val typeSpec = FunctionsReferencingTypeParameters::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class FunctionsReferencingTypeParameters<T>() {
        public fun test(`param`: T) {
        }
      }
      """.trimIndent(),
    )
  }

  class FunctionsReferencingTypeParameters<T> {
    fun test(param: T) {
    }
  }

  @Test
  fun overriddenThings() {
    val typeSpec = OverriddenThings::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public abstract class OverriddenThings() : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.OverriddenThingsBase(), com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.OverriddenThingsInterface {
        override var openProp: kotlin.String = throw NotImplementedError("Stub!")

        override var openPropInterface: kotlin.String = throw NotImplementedError("Stub!")

        override fun openFunction() {
        }

        override fun openFunctionInterface() {
        }
      }
      """.trimIndent(),
    )
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class DelegatedProperties() {
        /**
         * Note: delegation is ABI stub only and not guaranteed to match source code.
         */
        public val immutable: kotlin.String by kotlin.lazy { throw NotImplementedError("Stub!") }

        /**
         * Note: delegation is ABI stub only and not guaranteed to match source code.
         */
        public val immutableNullable: kotlin.String? by kotlin.lazy { throw NotImplementedError("Stub!") }

        /**
         * Note: delegation is ABI stub only and not guaranteed to match source code.
         */
        public var mutable: kotlin.String by kotlin.properties.Delegates.notNull()

        /**
         * Note: delegation is ABI stub only and not guaranteed to match source code.
         */
        public var mutableNullable: kotlin.String? by kotlin.properties.Delegates.observable(null) { _, _, _ -> }
      }
      """.trimIndent(),
    )
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class ClassDelegation<T>(
        delegate: List<T>
      ): List<T> by delegate
      """.trimIndent(),
    )
  }

  class ClassDelegation<T>(delegate: List<T>) : List<T> by delegate

  @Test
  fun simpleEnum() {
    val typeSpec = SimpleEnum::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public enum class SimpleEnum() {
        FOO,
        BAR,
        BAZ,
      }
      """.trimIndent(),
    )
  }

  enum class SimpleEnum {
    FOO, BAR, BAZ
  }

  @Test
  fun complexEnum() {
    val typeSpec = ComplexEnum::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public enum class ComplexEnum(
        public val `value`: kotlin.String,
      ) {
        FOO {
          override fun toString(): kotlin.String = throw NotImplementedError("Stub!")
        },
        BAR {
          override fun toString(): kotlin.String = throw NotImplementedError("Stub!")
        },
        BAZ {
          override fun toString(): kotlin.String = throw NotImplementedError("Stub!")
        },
        ;
      }
      """.trimIndent(),
    )
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
    },
  }

  @Test
  fun enumWithAnnotation() {
    val typeSpec = EnumWithAnnotation::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public enum class EnumWithAnnotation() {
        FOO,
        @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.FieldAnnotation
        BAR,
        BAZ,
      }
      """.trimIndent(),
    )
  }

  enum class EnumWithAnnotation {
    FOO, @FieldAnnotation
    BAR, BAZ
  }

  @Test
  fun complexEnumWithAnnotation() {
    val typeSpec = ComplexEnumWithAnnotation::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public enum class ComplexEnumWithAnnotation(
        public val `value`: kotlin.String,
      ) {
        FOO {
          override fun toString(): kotlin.String = throw NotImplementedError("Stub!")
        },
        BAR {
          override fun toString(): kotlin.String = throw NotImplementedError("Stub!")
        },
        BAZ {
          override fun toString(): kotlin.String = throw NotImplementedError("Stub!")
        },
        ;
      }
      """.trimIndent(),
    )
  }

  enum class ComplexEnumWithAnnotation(val value: String) {
    FOO("foo") {
      override fun toString(): String {
        return "foo1"
      }
    },

    @FieldAnnotation
    BAR("bar") {
      override fun toString(): String {
        return "bar1"
      }
    },
    BAZ("baz") {
      override fun toString(): String {
        return "baz1"
      }
    },
  }

  @Test
  fun interfaces() {
    val testInterfaceSpec = TestInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(testInterfaceSpec.trimmedToString()).isEqualTo(
      """
      public interface TestInterface {
        public fun noDefault()

        public fun noDefaultWithInput(input: kotlin.String)

        public fun noDefaultWithInputDefault(input: kotlin.String = throw NotImplementedError("Stub!"))

        public fun hasDefault() {
        }

        public fun hasDefaultSingleParam(input: kotlin.String): kotlin.String = throw NotImplementedError("Stub!")

        public fun hasDefaultMultiParam(input: kotlin.String, input2: kotlin.String): kotlin.String = throw NotImplementedError("Stub!")

        public fun complex(input: kotlin.String, input2: kotlin.String = throw NotImplementedError("Stub!")): kotlin.String = throw NotImplementedError("Stub!")
      }
      """.trimIndent(),
    )

    val subInterfaceSpec = SubInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(subInterfaceSpec.trimmedToString()).isEqualTo(
      """
      public interface SubInterface : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.TestInterface {
        public fun subInterfaceFunction() {
        }

        override fun hasDefault() {
        }
      }
      """.trimIndent(),
    )

    val implSpec = TestSubInterfaceImpl::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(implSpec.trimmedToString()).isEqualTo(
      """
      public class TestSubInterfaceImpl() : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.SubInterface {
        override fun noDefault() {
        }

        override fun noDefaultWithInput(input: kotlin.String) {
        }

        override fun noDefaultWithInputDefault(input: kotlin.String) {
        }
      }
      """.trimIndent(),
    )
  }

  interface TestInterface {

    fun noDefault()

    fun noDefaultWithInput(input: String)

    fun noDefaultWithInputDefault(input: String = "")

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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public interface BackwardReferencingTypeVars<T> : kotlin.collections.List<kotlin.collections.Set<T>>
      """.trimIndent(),
    )
  }

  interface BackwardReferencingTypeVars<T> : List<Set<T>>

  @Test
  fun taggedTypes() {
    val typeSpec = TaggedTypes::class.toTypeSpecWithTestHandler()
    assertThat(typeSpec.tag<KmClass>()).isNotNull()

    val constructorSpec = typeSpec.primaryConstructor ?: fail("No constructor found!")
    assertThat(constructorSpec.tag<KmConstructor>()).isNotNull()

    val parameterSpec = constructorSpec.parameters[0]
    assertThat(parameterSpec.tag<KmValueParameter>()).isNotNull()

    val typeVar = typeSpec.typeVariables[0]
    assertThat(typeVar.tag<KmTypeParameter>()).isNotNull()

    val funSpec = typeSpec.funSpecs[0]
    assertThat(funSpec.tag<KmFunction>()).isNotNull()

    val propertySpec = typeSpec.propertySpecs[0]
    assertThat(propertySpec.tag<KmProperty>()).isNotNull()
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public annotation class MyAnnotation(
        public val `value`: kotlin.String,
      )
      """.trimIndent(),
    )
  }

  annotation class MyAnnotation(val value: String)

  @Test
  fun functionTypeArgsSupersedeClass() {
    val typeSpec = GenericClass::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class GenericClass<T>() {
        public fun functionWithT(`param`: T) {
        }

        public fun <T> functionAlsoWithT(`param`: T) {
        }

        public fun <R> functionWithADifferentType(`param`: R) {
        }

        /**
         * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
         */
        public inline fun <reified T> `reified`(`param`: T) {
        }
      }
      """.trimIndent(),
    )

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

    // Regression for https://github.com/square/kotlinpoet/issues/829
    inline fun <reified T> reified(param: T) {
    }
  }

  @Test
  fun complexCompanionObject() {
    val typeSpec = ComplexCompanionObject::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class ComplexCompanionObject() {
        public companion object ComplexObject : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CompanionBase(), com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CompanionInterface
      }
      """.trimIndent(),
    )
  }

  interface CompanionInterface
  open class CompanionBase

  class ComplexCompanionObject {
    companion object ComplexObject : CompanionBase(), CompanionInterface
  }

  @IgnoreForHandlerType(
    reason = "TODO Synthetic methods that hold annotations aren't visible in these tests",
    handlerType = ELEMENTS,
  )
  @Test
  fun annotationsAreCopied() {
    val typeSpec = AnnotationHolders::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class AnnotationHolders @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.ConstructorAnnotation constructor() {
        @field:com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.FieldAnnotation
        public var `field`: kotlin.String? = null

        @get:com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.GetterAnnotation
        public var getter: kotlin.String? = null

        @set:com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.SetterAnnotation
        public var setter: kotlin.String? = null

        @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.HolderAnnotation
        @kotlin.jvm.JvmField
        public var holder: kotlin.String? = null

        @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.ConstructorAnnotation
        public constructor(`value`: kotlin.String)

        @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.FunctionAnnotation
        public fun function() {
        }
      }
      """.trimIndent(),
    )
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
    @JvmField
    var holder: String? = null

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
    handlerType = REFLECTIVE,
  )
  @Test
  fun constantValuesElements() {
    val typeSpec = Constants::class.toTypeSpecWithTestHandler()

    // Note: formats like hex/binary/underscore are not available as formatted at runtime
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Constants(
        public val `param`: kotlin.String = throw NotImplementedError("Stub!"),
      ) {
        public val boolProp: kotlin.Boolean = throw NotImplementedError("Stub!")

        public val binaryProp: kotlin.Int = throw NotImplementedError("Stub!")

        public val intProp: kotlin.Int = throw NotImplementedError("Stub!")

        public val underscoresProp: kotlin.Int = throw NotImplementedError("Stub!")

        public val hexProp: kotlin.Int = throw NotImplementedError("Stub!")

        public val underscoresHexProp: kotlin.Long = throw NotImplementedError("Stub!")

        public val longProp: kotlin.Long = throw NotImplementedError("Stub!")

        public val floatProp: kotlin.Float = throw NotImplementedError("Stub!")

        public val doubleProp: kotlin.Double = throw NotImplementedError("Stub!")

        public val stringProp: kotlin.String = throw NotImplementedError("Stub!")

        public companion object {
          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_BOOL_PROP: kotlin.Boolean = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_BINARY_PROP: kotlin.Int = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_INT_PROP: kotlin.Int = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_UNDERSCORES_PROP: kotlin.Int = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_HEX_PROP: kotlin.Int = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_UNDERSCORES_HEX_PROP: kotlin.Long = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_LONG_PROP: kotlin.Long = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_FLOAT_PROP: kotlin.Float = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_DOUBLE_PROP: kotlin.Double = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_STRING_PROP: kotlin.String = throw NotImplementedError("Stub!")

          public const val CONST_BOOL_PROP: kotlin.Boolean = false

          public const val CONST_BINARY_PROP: kotlin.Int = 11

          public const val CONST_INT_PROP: kotlin.Int = 1

          public const val CONST_UNDERSCORES_PROP: kotlin.Int = 1_000_000

          public const val CONST_HEX_PROP: kotlin.Int = 15

          public const val CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4_293_713_502L

          public const val CONST_LONG_PROP: kotlin.Long = 1L

          public const val CONST_FLOAT_PROP: kotlin.Float = 1.0F

          public const val CONST_DOUBLE_PROP: kotlin.Double = 1.0

          public const val CONST_STRING_PROP: kotlin.String = "prop"
        }
      }
      """.trimIndent(),
    )

    // TODO check with objects
  }

  @IgnoreForHandlerType(
    reason = "Elements properly resolves the regular properties + JvmStatic, but reflection will not",
    handlerType = ELEMENTS,
  )
  @Test
  fun constantValuesReflective() {
    val typeSpec = Constants::class.toTypeSpecWithTestHandler()

    // Note: formats like hex/binary/underscore are not available as formatted in elements
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Constants(
        public val `param`: kotlin.String = throw NotImplementedError("Stub!"),
      ) {
        public val boolProp: kotlin.Boolean = throw NotImplementedError("Stub!")

        public val binaryProp: kotlin.Int = throw NotImplementedError("Stub!")

        public val intProp: kotlin.Int = throw NotImplementedError("Stub!")

        public val underscoresProp: kotlin.Int = throw NotImplementedError("Stub!")

        public val hexProp: kotlin.Int = throw NotImplementedError("Stub!")

        public val underscoresHexProp: kotlin.Long = throw NotImplementedError("Stub!")

        public val longProp: kotlin.Long = throw NotImplementedError("Stub!")

        public val floatProp: kotlin.Float = throw NotImplementedError("Stub!")

        public val doubleProp: kotlin.Double = throw NotImplementedError("Stub!")

        public val stringProp: kotlin.String = throw NotImplementedError("Stub!")

        public companion object {
          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_BOOL_PROP: kotlin.Boolean = false

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_BINARY_PROP: kotlin.Int = 11

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_INT_PROP: kotlin.Int = 1

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_UNDERSCORES_PROP: kotlin.Int = 1_000_000

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_HEX_PROP: kotlin.Int = 15

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4_293_713_502L

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_LONG_PROP: kotlin.Long = 1L

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_FLOAT_PROP: kotlin.Float = 1.0F

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_DOUBLE_PROP: kotlin.Double = 1.0

          @kotlin.jvm.JvmStatic
          public val STATIC_CONST_STRING_PROP: kotlin.String = "prop"

          public const val CONST_BOOL_PROP: kotlin.Boolean = false

          public const val CONST_BINARY_PROP: kotlin.Int = 11

          public const val CONST_INT_PROP: kotlin.Int = 1

          public const val CONST_UNDERSCORES_PROP: kotlin.Int = 1_000_000

          public const val CONST_HEX_PROP: kotlin.Int = 15

          public const val CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4_293_713_502L

          public const val CONST_LONG_PROP: kotlin.Long = 1L

          public const val CONST_FLOAT_PROP: kotlin.Float = 1.0F

          public const val CONST_DOUBLE_PROP: kotlin.Double = 1.0

          public const val CONST_STRING_PROP: kotlin.String = "prop"
        }
      }
      """.trimIndent(),
    )
  }

  class Constants(
    val param: String = "param",
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class JvmAnnotations() {
        @kotlin.jvm.Transient
        public val transientProp: kotlin.String? = null

        @kotlin.jvm.Volatile
        public var volatileProp: kotlin.String? = null

        @get:kotlin.jvm.Synchronized
        public val synchronizedGetProp: kotlin.String? = null

        @set:kotlin.jvm.Synchronized
        public var synchronizedSetProp: kotlin.String? = null

        @kotlin.jvm.Synchronized
        public fun synchronizedFun() {
        }
      }
      """.trimIndent(),
    )

    val interfaceSpec = JvmAnnotationsInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(interfaceSpec.trimmedToString()).isEqualTo(
      """
      public interface JvmAnnotationsInterface {
        public fun notDefaultMethod()
      }
      """.trimIndent(),
    )
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
    fun notDefaultMethod()
  }

  @Test
  fun nestedClasses() {
    val typeSpec = NestedClasses::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class NestedClasses() {
        public abstract class NestedClass<T>() : kotlin.collections.List<T>

        public inner class NestedInnerClass()
      }
      """.trimIndent(),
    )
  }

  class NestedClasses {
    abstract class NestedClass<T> : List<T>
    inner class NestedInnerClass
  }

  @IgnoreForHandlerType(
    reason = "Reflection properly resolves companion properties + JvmStatic + JvmName, but " +
      "elements will not",
    handlerType = ELEMENTS,
  )
  @Test
  fun jvmNamesReflective() {
    val typeSpec = JvmNameData::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class JvmNameData(
        @get:kotlin.jvm.JvmName(name = "jvmParam")
        public val `param`: kotlin.String,
      ) {
        @get:kotlin.jvm.JvmName(name = "jvmPropertyGet")
        public val propertyGet: kotlin.String? = null

        @set:kotlin.jvm.JvmName(name = "jvmPropertySet")
        public var propertySet: kotlin.String? = null

        @get:kotlin.jvm.JvmName(name = "jvmPropertyGetAndSet")
        @set:kotlin.jvm.JvmName(name = "jvmPropertyGetAndSet")
        public var propertyGetAndSet: kotlin.String? = null

        @kotlin.jvm.JvmName(name = "jvmFunction")
        public fun function() {
        }

        public interface InterfaceWithJvmName {
          public companion object {
            @get:kotlin.jvm.JvmName(name = "fooBoolJvm")
            @kotlin.jvm.JvmStatic
            public val FOO_BOOL: kotlin.Boolean = false

            @kotlin.jvm.JvmName(name = "jvmStaticFunction")
            @kotlin.jvm.JvmStatic
            public fun staticFunction() {
            }
          }
        }
      }
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    reason = "Reflection properly resolves companion properties + JvmStatic + JvmName, but " +
      "elements will not",
    handlerType = REFLECTIVE,
  )
  @Test
  fun jvmNamesElements() {
    val typeSpec = JvmNameData::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class JvmNameData(
        @get:kotlin.jvm.JvmName(name = "jvmParam")
        public val `param`: kotlin.String,
      ) {
        @get:kotlin.jvm.JvmName(name = "jvmPropertyGet")
        public val propertyGet: kotlin.String? = null

        @set:kotlin.jvm.JvmName(name = "jvmPropertySet")
        public var propertySet: kotlin.String? = null

        @get:kotlin.jvm.JvmName(name = "jvmPropertyGetAndSet")
        @set:kotlin.jvm.JvmName(name = "jvmPropertyGetAndSet")
        public var propertyGetAndSet: kotlin.String? = null

        @kotlin.jvm.JvmName(name = "jvmFunction")
        public fun function() {
        }

        public interface InterfaceWithJvmName {
          public companion object {
            @get:kotlin.jvm.JvmName(name = "fooBoolJvm")
            @kotlin.jvm.JvmStatic
            public val FOO_BOOL: kotlin.Boolean = throw NotImplementedError("Stub!")

            @kotlin.jvm.JvmName(name = "jvmStaticFunction")
            @kotlin.jvm.JvmStatic
            public fun staticFunction() {
            }
          }
        }
      }
      """.trimIndent(),
    )
  }

  class JvmNameData(
    @get:JvmName("jvmParam") val param: String,
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
    handlerType = REFLECTIVE,
  )
  @Test
  fun overloads() {
    val typeSpec = Overloads::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Overloads @kotlin.jvm.JvmOverloads constructor(
        public val param1: kotlin.String,
        public val optionalParam2: kotlin.String = throw NotImplementedError("Stub!"),
        public val nullableParam3: kotlin.String? = throw NotImplementedError("Stub!"),
      ) {
        @kotlin.jvm.JvmOverloads
        public fun testFunction(
          param1: kotlin.String,
          optionalParam2: kotlin.String = throw NotImplementedError("Stub!"),
          nullableParam3: kotlin.String? = throw NotImplementedError("Stub!"),
        ) {
        }
      }
      """.trimIndent(),
    )
  }

  class Overloads @JvmOverloads constructor(
    val param1: String,
    val optionalParam2: String = "",
    val nullableParam3: String? = null,
  ) {
    @JvmOverloads
    fun testFunction(
      param1: String,
      optionalParam2: String = "",
      nullableParam3: String? = null,
    ) {
    }
  }

  @IgnoreForHandlerType(
    reason = "Elements generates initializer values.",
    handlerType = ELEMENTS,
  )
  @Test
  fun jvmFields_reflective() {
    val typeSpec = Fields::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Fields(
        @property:kotlin.jvm.JvmField
        public val param1: kotlin.String,
      ) {
        @kotlin.jvm.JvmField
        public val fieldProp: kotlin.String = throw NotImplementedError("Stub!")

        public companion object {
          @kotlin.jvm.JvmField
          public val companionProp: kotlin.String = ""

          @kotlin.jvm.JvmStatic
          public val staticCompanionProp: kotlin.String = ""

          public const val constCompanionProp: kotlin.String = ""
        }
      }
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    reason = "Elements generates initializer values.",
    handlerType = REFLECTIVE,
  )
  @Test
  fun jvmFields_elements() {
    val typeSpec = Fields::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Fields(
        @property:kotlin.jvm.JvmField
        public val param1: kotlin.String,
      ) {
        @kotlin.jvm.JvmField
        public val fieldProp: kotlin.String = throw NotImplementedError("Stub!")

        public companion object {
          @kotlin.jvm.JvmField
          public val companionProp: kotlin.String = throw NotImplementedError("Stub!")

          @kotlin.jvm.JvmStatic
          public val staticCompanionProp: kotlin.String = throw NotImplementedError("Stub!")

          public const val constCompanionProp: kotlin.String = ""
        }
      }
      """.trimIndent(),
    )
  }

  class Fields(
    @JvmField val param1: String,
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
    handlerType = ELEMENTS,
  )
  @Test
  fun synthetics_reflective() {
    val typeSpec = Synthetics::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Synthetics(
        @get:kotlin.jvm.JvmSynthetic
        public val `param`: kotlin.String,
      ) {
        @field:kotlin.jvm.JvmSynthetic
        public val `property`: kotlin.String? = null

        @field:kotlin.jvm.JvmSynthetic
        public val fieldProperty: kotlin.String? = null

        @get:kotlin.jvm.JvmSynthetic
        public val propertyGet: kotlin.String? = null

        @set:kotlin.jvm.JvmSynthetic
        public var propertySet: kotlin.String? = null

        @get:kotlin.jvm.JvmSynthetic
        @set:kotlin.jvm.JvmSynthetic
        public var propertyGetAndSet: kotlin.String? = null

        /**
         * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
         */
        @kotlin.jvm.JvmSynthetic
        public fun function() {
        }

        public interface InterfaceWithJvmName {
          /**
           * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
           */
          @kotlin.jvm.JvmSynthetic
          public fun interfaceFunction()

          public companion object {
            @get:kotlin.jvm.JvmSynthetic
            @kotlin.jvm.JvmStatic
            public val FOO_BOOL: kotlin.Boolean = false

            /**
             * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
             */
            @kotlin.jvm.JvmStatic
            @kotlin.jvm.JvmSynthetic
            public fun staticFunction() {
            }
          }
        }
      }
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    reason = "Synthetic constructs aren't available in elements, so some information like " +
      "JvmStatic can't be deduced.",
    handlerType = REFLECTIVE,
  )
  @Test
  fun synthetics_elements() {
    val typeSpec = Synthetics::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Synthetics(
        @get:kotlin.jvm.JvmSynthetic
        public val `param`: kotlin.String,
      ) {
        @field:kotlin.jvm.JvmSynthetic
        public val `property`: kotlin.String? = null

        @field:kotlin.jvm.JvmSynthetic
        public val fieldProperty: kotlin.String? = null

        @get:kotlin.jvm.JvmSynthetic
        public val propertyGet: kotlin.String? = null

        @set:kotlin.jvm.JvmSynthetic
        public var propertySet: kotlin.String? = null

        @get:kotlin.jvm.JvmSynthetic
        @set:kotlin.jvm.JvmSynthetic
        public var propertyGetAndSet: kotlin.String? = null

        /**
         * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
         */
        @kotlin.jvm.JvmSynthetic
        public fun function() {
        }

        public interface InterfaceWithJvmName {
          /**
           * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
           */
          @kotlin.jvm.JvmSynthetic
          public fun interfaceFunction()

          public companion object {
            @get:kotlin.jvm.JvmSynthetic
            @kotlin.jvm.JvmStatic
            public val FOO_BOOL: kotlin.Boolean = throw NotImplementedError("Stub!")

            /**
             * Note: Since this is a synthetic function, some JVM information (annotations, modifiers) may be missing.
             */
            @kotlin.jvm.JvmSynthetic
            public fun staticFunction() {
            }
          }
        }
      }
      """.trimIndent(),
    )
  }

  class Synthetics(
    @get:JvmSynthetic val param: String,
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
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Throwing @kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class]) constructor() {
        @get:kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        public val getterThrows: kotlin.String? = null

        @set:kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        public var setterThrows: kotlin.String? = null

        @get:kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        @set:kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        public var getterAndSetterThrows: kotlin.String? = null

        @kotlin.jvm.Throws(exceptionClasses = [java.lang.IllegalStateException::class])
        public fun testFunction() {
        }
      }
      """.trimIndent(),
    )
  }

  class Throwing
  @Throws(IllegalStateException::class)
  constructor() {

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
  @IgnoreForHandlerType(
    reason = "Reflection can't parse non-runtime retained annotations",
    handlerType = REFLECTIVE,
  )
  @Test
  fun metaTest_elements() {
    val typeSpec = Metadata::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      @kotlin.SinceKotlin(version = "1.3")
      @kotlin.`annotation`.Retention(value = kotlin.`annotation`.AnnotationRetention.RUNTIME)
      @kotlin.`annotation`.Target(allowedTargets = arrayOf(kotlin.`annotation`.AnnotationTarget.CLASS))
      public annotation class Metadata(
        @get:kotlin.jvm.JvmName(name = "k")
        public val kind: kotlin.Int = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "mv")
        public val metadataVersion: kotlin.IntArray = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "bv")
        public val bytecodeVersion: kotlin.IntArray = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "d1")
        public val data1: kotlin.Array<kotlin.String> = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "d2")
        public val data2: kotlin.Array<kotlin.String> = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "xs")
        public val extraString: kotlin.String = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "pn")
        public val packageName: kotlin.String = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "xi")
        public val extraInt: kotlin.Int = throw NotImplementedError("Stub!"),
      )
      """.trimIndent(),
    )
  }

  // The meta-ist of metadata meta-tests.
  @IgnoreForHandlerType(
    reason = "Reflection can't parse non-runtime retained annotations",
    handlerType = ELEMENTS,
  )
  @Test
  fun metaTest_reflection() {
    val typeSpec = Metadata::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      @kotlin.`annotation`.Retention(value = kotlin.`annotation`.AnnotationRetention.RUNTIME)
      @kotlin.`annotation`.Target(allowedTargets = arrayOf(kotlin.`annotation`.AnnotationTarget.CLASS))
      public annotation class Metadata(
        @get:kotlin.jvm.JvmName(name = "k")
        public val kind: kotlin.Int = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "mv")
        public val metadataVersion: kotlin.IntArray = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "bv")
        public val bytecodeVersion: kotlin.IntArray = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "d1")
        public val data1: kotlin.Array<kotlin.String> = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "d2")
        public val data2: kotlin.Array<kotlin.String> = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "xs")
        public val extraString: kotlin.String = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "pn")
        public val packageName: kotlin.String = throw NotImplementedError("Stub!"),
        @get:kotlin.jvm.JvmName(name = "xi")
        public val extraInt: kotlin.Int = throw NotImplementedError("Stub!"),
      )
      """.trimIndent(),
    )
  }

  @Test
  fun classNamesAndNesting() {
    // Make sure we parse class names correctly at all levels
    val typeSpec = ClassNesting::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class ClassNesting() {
        public class NestedClass() {
          public class SuperNestedClass() {
            public inner class SuperDuperInnerClass()
          }
        }
      }
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    reason = "compile-testing can't handle class names with dashes, will throw " +
      "\"class file for com.squareup.kotlinpoet.metadata.specs.Fuzzy\$ClassNesting\$-Nested not found\"",
    handlerType = ELEMENTS,
  )
  @Test
  fun classNamesAndNesting_pathological() {
    // Make sure we parse class names correctly at all levels
    val typeSpec = `Fuzzy$ClassNesting`::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class `Fuzzy${'$'}ClassNesting`() {
        public class `-Nested`() {
          public class SuperNestedClass() {
            public inner class `-${'$'}Fuzzy${'$'}Super${'$'}Weird-Nested${'$'}Name`()
          }
        }
      }
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    reason = "Property site-target annotations are always stored on the synthetic annotations " +
      "method, which is not accessible in the elements API",
    handlerType = ELEMENTS,
  )
  @Test
  fun parameterAnnotations_reflective() {
    val typeSpec = ParameterAnnotations::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class ParameterAnnotations(
        @property:com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CustomAnnotation(name = "${'$'}{'${'$'}'}a")
        @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CustomAnnotation(name = "b")
        public val param1: kotlin.String,
        @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CustomAnnotation(name = "2")
        param2: kotlin.String,
      ) {
        public fun function(@com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CustomAnnotation(name = "woo") param1: kotlin.String) {
        }
      }
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    reason = "Property site-target annotations are always stored on the synthetic annotations " +
      "method, which is not accessible in the elements API",
    handlerType = REFLECTIVE,
  )
  @Test
  fun parameterAnnotations_elements() {
    val typeSpec = ParameterAnnotations::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class ParameterAnnotations(
        @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CustomAnnotation(name = "b")
        public val param1: kotlin.String,
        @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CustomAnnotation(name = "2")
        param2: kotlin.String,
      ) {
        public fun function(@com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.CustomAnnotation(name = "woo") param1: kotlin.String) {
        }
      }
      """.trimIndent(),
    )
  }

  annotation class CustomAnnotation(val name: String)

  class ParameterAnnotations(
    @property:CustomAnnotation("\$a")
    @param:CustomAnnotation("b")
    val param1: String,
    @CustomAnnotation("2") param2: String,
  ) {
    fun function(@CustomAnnotation("woo") param1: String) {
    }
  }

  @IgnoreForHandlerType(
    reason = "Non-runtime annotations are not present for reflection.",
    handlerType = ELEMENTS,
  )
  @Test
  fun classAnnotations_reflective() {
    val typeSpec = ClassAnnotations::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.RuntimeCustomClassAnnotation(name = "Runtime")
      public class ClassAnnotations()
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    reason = "Non-runtime annotations are not present for reflection.",
    handlerType = REFLECTIVE,
  )
  @Test
  fun classAnnotations_elements() {
    val typeSpec = ClassAnnotations::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.BinaryCustomClassAnnotation(name = "Binary")
      @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.RuntimeCustomClassAnnotation(name = "Runtime")
      public class ClassAnnotations()
      """.trimIndent(),
    )
  }

  @Retention(AnnotationRetention.SOURCE)
  annotation class SourceCustomClassAnnotation(val name: String)

  @Retention(AnnotationRetention.BINARY)
  annotation class BinaryCustomClassAnnotation(val name: String)

  @Retention(AnnotationRetention.RUNTIME)
  annotation class RuntimeCustomClassAnnotation(val name: String)

  @SourceCustomClassAnnotation("Source")
  @BinaryCustomClassAnnotation("Binary")
  @RuntimeCustomClassAnnotation("Runtime")
  class ClassAnnotations

  @Test
  fun typeAnnotations() {
    val typeSpec = TypeAnnotations::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class TypeAnnotations() {
        public val foo: kotlin.collections.List<@com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.TypeAnnotation kotlin.String> = throw NotImplementedError("Stub!")

        public fun <T> bar(input: @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.TypeAnnotation kotlin.String, input2: @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.TypeAnnotation (@com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.TypeAnnotation kotlin.Int) -> @com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.TypeAnnotation kotlin.String) {
        }
      }
      """.trimIndent(),
    )
  }

  @Target(TYPE, TYPE_PARAMETER)
  annotation class TypeAnnotation

  class TypeAnnotations {
    val foo: List<@TypeAnnotation String> = emptyList()

    fun <@TypeAnnotation T> bar(
      input: @TypeAnnotation String,
      input2: @TypeAnnotation (@TypeAnnotation Int) -> @TypeAnnotation String,
    ) {
    }
  }

  // Regression test for https://github.com/square/kotlinpoet/issues/812
  @Test
  fun backwardTypeVarReferences() {
    val typeSpec = Asset::class.toTypeSpecWithTestHandler()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public class Asset<A : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.Asset<A>>() {
        public fun <D : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.Asset<D>, C : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.Asset<A>> function() {
        }

        public class AssetOut<out B : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.Asset.AssetOut<B>>()

        public class AssetIn<in C : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.Asset.AssetIn<C>>()
      }
      """.trimIndent(),
    )
  }

  class Asset<A : Asset<A>> {
    fun <D : Asset<D>, C : Asset<A>> function() {
    }

    class AssetOut<out B : AssetOut<B>>
    class AssetIn<in C : AssetIn<C>>
  }

  // Regression test for https://github.com/square/kotlinpoet/issues/821
  @Test
  fun abstractClass() {
    val typeSpec = AbstractClass::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public abstract class AbstractClass() {
        public abstract val foo: kotlin.String

        public val baz: kotlin.String? = null

        public abstract fun bar()

        public abstract fun barWithReturn(): kotlin.String

        public fun fuz() {
        }

        public fun fuzWithReturn(): kotlin.String = throw NotImplementedError("Stub!")
      }
      """.trimIndent(),
    )
  }

  abstract class AbstractClass {
    abstract val foo: String
    abstract fun bar()
    abstract fun barWithReturn(): String

    val baz: String? = null
    fun fuz() {}
    fun fuzWithReturn(): String {
      return ""
    }
  }

  // Regression test for https://github.com/square/kotlinpoet/issues/820
  @Test
  fun internalAbstractProperty() {
    val typeSpec = InternalAbstractPropertyHolder::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public abstract class InternalAbstractPropertyHolder() {
        internal abstract val valProp: kotlin.String

        internal abstract var varProp: kotlin.String
      }
      """.trimIndent(),
    )
  }

  abstract class InternalAbstractPropertyHolder {
    internal abstract val valProp: String
    internal abstract var varProp: String
  }

  @Test
  fun modalities() {
    val abstractModalities = AbstractModalities::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(abstractModalities.trimmedToString()).isEqualTo(
      """
      public abstract class AbstractModalities() : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.ModalitiesInterface {
        override val interfaceProp: kotlin.String? = null

        public val implicitFinalProp: kotlin.String? = null

        public open val openProp: kotlin.String? = null

        override fun interfaceFun() {
        }

        public fun implicitFinalFun() {
        }

        public open fun openFun() {
        }
      }
      """.trimIndent(),
    )

    val finalAbstractModalities = FinalAbstractModalities::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(finalAbstractModalities.trimmedToString()).isEqualTo(
      """
      public abstract class FinalAbstractModalities() : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.ModalitiesInterface {
        final override val interfaceProp: kotlin.String? = null

        final override fun interfaceFun() {
        }
      }
      """.trimIndent(),
    )

    val modalities = Modalities::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(modalities.trimmedToString()).isEqualTo(
      """
      public class Modalities() : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.AbstractModalities() {
        override val interfaceProp: kotlin.String? = null

        override val openProp: kotlin.String? = null

        override fun interfaceFun() {
        }

        override fun openFun() {
        }
      }
      """.trimIndent(),
    )
  }

  interface ModalitiesInterface {
    val interfaceProp: String?
    fun interfaceFun()
  }
  abstract class AbstractModalities : ModalitiesInterface {
    override val interfaceProp: String? = null
    override fun interfaceFun() {
    }
    val implicitFinalProp: String? = null
    fun implicitFinalFun() {
    }
    open val openProp: String? = null
    open fun openFun() {
    }
  }
  abstract class FinalAbstractModalities : ModalitiesInterface {
    final override val interfaceProp: String? = null
    final override fun interfaceFun() {
    }
  }
  class Modalities : AbstractModalities() {
    override val interfaceProp: String? = null
    override fun interfaceFun() {
    }

    override val openProp: String? = null
    override fun openFun() {
    }
  }

  @Test
  fun funClass() {
    val funInterface = FunInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(funInterface.trimmedToString()).isEqualTo(
      """
      public fun interface FunInterface {
        public fun example()
      }
      """.trimIndent(),
    )

    val subFunInterface = SubFunInterface::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(subFunInterface.trimmedToString()).isEqualTo(
      """
      public fun interface SubFunInterface : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.FunInterface
      """.trimIndent(),
    )
  }

  fun interface FunInterface {
    fun example()
  }

  fun interface SubFunInterface : FunInterface

  @Test
  fun selfReferencingTypeParams() {
    val typeSpec = Node::class.toTypeSpecWithTestHandler()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo(
      """
      public open class Node<T : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.Node<T, R>, R : com.squareup.kotlinpoet.metadata.specs.KotlinPoetMetadataSpecsTest.Node<R, T>>() {
        public var t: T? = null

        public var r: R? = null
      }
      """.trimIndent(),
    )
  }

  open class Node<T : Node<T, R>, R : Node<R, T>> {
    var t: T? = null
    var r: R? = null
  }
}

class ClassNesting {
  class NestedClass {
    class SuperNestedClass {
      inner class SuperDuperInnerClass
    }
  }
}

class `Fuzzy$ClassNesting` {
  class `-Nested` {
    class SuperNestedClass {
      inner class `-$Fuzzy$Super$Weird-Nested$Name`
    }
  }
}

private fun TypeSpec.trimmedToString(): String {
  return toString().trim()
}

@JvmInline
value class ValueClass(val value: String)

typealias TypeAliasName = String
typealias GenericTypeAlias = List<String>
typealias NestedTypeAlias = List<GenericTypeAlias>
