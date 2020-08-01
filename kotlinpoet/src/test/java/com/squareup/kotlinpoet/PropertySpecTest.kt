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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.Serializable
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.test.Test

class PropertySpecTest {
  @Test fun nullable() {
    val type = String::class.asClassName().copy(nullable = true)
    val a = PropertySpec.builder("foo", type).build()
    assertThat(a.toString()).isEqualTo("val foo: kotlin.String?\n")
  }

  @Test fun delegated() {
    val prop = PropertySpec.builder("foo", String::class)
        .delegate("Delegates.notNull()")
        .build()
    assertThat(prop.toString()).isEqualTo("val foo: kotlin.String by Delegates.notNull()\n")
  }

  @Test fun emptySetter() {
    val prop = PropertySpec.builder("foo", String::class)
        .mutable()
        .setter(FunSpec.setterBuilder()
            .addModifiers(KModifier.PRIVATE)
            .build())
        .build()

    assertThat(prop.toString()).isEqualTo("""
      |var foo: kotlin.String
      |  private set
      |""".trimMargin())
  }

  // https://github.com/square/kotlinpoet/issues/952
  @Test fun emptySetterCannotHaveBody() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("foo", String::class)
          .mutable()
          .setter(FunSpec.setterBuilder()
              .addStatement("body()")
              .build())
          .build()
    }.hasMessageThat().isEqualTo("parameterless setter cannot have code")
  }

  @Test fun inlineSingleAccessor() {
    val prop = PropertySpec.builder("foo", String::class)
        .getter(FunSpec.getterBuilder()
            .addModifiers(KModifier.INLINE)
            .addStatement("return %S", "foo")
            .build())
        .build()

    assertThat(prop.toString()).isEqualTo("""
      |val foo: kotlin.String
      |  inline get() = "foo"
      |""".trimMargin())
  }

  @Test fun inlineBothAccessors() {
    val prop = PropertySpec.builder("foo", String::class.asTypeName())
        .mutable()
        .getter(FunSpec.getterBuilder()
            .addModifiers(KModifier.INLINE)
            .addStatement("return %S", "foo")
            .build())
        .setter(FunSpec.setterBuilder()
            .addModifiers(KModifier.INLINE)
            .addParameter("value", String::class)
            .build())
        .build()

    assertThat(prop.toString()).isEqualTo("""
      |inline var foo: kotlin.String
      |  get() = "foo"
      |  set(value) {
      |  }
      |""".trimMargin())
  }

  @Test fun inlineForbiddenOnProperty() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("foo", String::class)
          .addModifiers(KModifier.INLINE)
          .build()
    }.hasMessageThat().isEqualTo("KotlinPoet doesn't allow setting the inline modifier on " +
        "properties. You should mark either the getter, the setter, or both inline.")
  }

  @Test fun equalsAndHashCode() {
    val type = Int::class
    var a = PropertySpec.builder("foo", type).build()
    var b = PropertySpec.builder("foo", type).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = PropertySpec.builder("FOO", type, KModifier.PUBLIC, KModifier.LATEINIT).build()
    b = PropertySpec.builder("FOO", type, KModifier.PUBLIC, KModifier.LATEINIT).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun escapeKeywordInPropertyName() {
    val prop = PropertySpec.builder("object", String::class)
        .build()
    assertThat(prop.toString()).isEqualTo("""
      |val `object`: kotlin.String
      |""".trimMargin())
  }

  @Test fun escapeKeywordInVariableName() {
    val prop = PropertySpec.builder("object", String::class)
        .mutable()
        .build()
    assertThat(prop.toString()).isEqualTo("""
      |var `object`: kotlin.String
      |""".trimMargin())
  }

  @Test fun externalTopLevel() {
    val prop = PropertySpec.builder("foo", String::class)
        .addModifiers(KModifier.EXTERNAL)
        .build()

    assertThat(prop.toString()).isEqualTo("""
      |external val foo: kotlin.String
      |""".trimMargin())
  }

  @Test fun escapePunctuationInPropertyName() {
    val prop = PropertySpec.builder("with-hyphen", String::class)
        .build()

    assertThat(prop.toString()).isEqualTo("""
      |val `with-hyphen`: kotlin.String
      |""".trimMargin())
  }

  @Test fun generalBuilderEqualityTest() {
    val originatingElement = FakeElement()
    val prop = PropertySpec.builder("tacos", Int::class)
        .mutable()
        .addAnnotation(ClassName("com.squareup.kotlinpoet", "Vegan"))
        .addKdoc("Can make it vegan!")
        .addModifiers(KModifier.PUBLIC)
        .addTypeVariable(TypeVariableName("T"))
        .delegate("Delegates.notNull()")
        .receiver(Int::class)
        .getter(FunSpec.getterBuilder()
            .addModifiers(KModifier.INLINE)
            .addStatement("return %S", 42)
            .build())
        .setter(FunSpec.setterBuilder()
            .addModifiers(KModifier.INLINE)
            .addParameter("value", Int::class)
            .build())
        .addOriginatingElement(originatingElement)
        .build()

    val newProp = prop.toBuilder().build()
    assertThat(newProp).isEqualTo(prop)
    assertThat(newProp.originatingElements).containsExactly(originatingElement)
  }

  @Test fun modifyModifiers() {
    val builder = PropertySpec
        .builder("word", String::class)
        .addModifiers(KModifier.PRIVATE)

    builder.modifiers.clear()
    builder.modifiers.add(KModifier.INTERNAL)

    assertThat(builder.build().modifiers).containsExactly(KModifier.INTERNAL)
  }

  @Test fun modifyAnnotations() {
    val builder = PropertySpec
        .builder("word", String::class)
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

  // https://github.com/square/kotlinpoet/issues/437
  @Test fun typeVariable() {
    val t = TypeVariableName("T", Any::class)
    val prop = PropertySpec.builder("someFunction", t, KModifier.PRIVATE)
        .addTypeVariable(t)
        .receiver(KClass::class.asClassName().parameterizedBy(t))
        .getter(FunSpec.getterBuilder()
            .addModifiers(KModifier.INLINE)
            .addStatement("return stuff as %T", t)
            .build())
        .build()
    assertThat(prop.toString()).isEqualTo("""
      |private val <T : kotlin.Any> kotlin.reflect.KClass<T>.someFunction: T
      |  inline get() = stuff as T
      |""".trimMargin())
  }

  @Test fun typeVariablesWithWhere() {
    val t = TypeVariableName("T", Serializable::class, Cloneable::class)
    val r = TypeVariableName("R", Any::class)
    val function = Function::class.asClassName().parameterizedBy(t, r)
    val prop = PropertySpec.builder("property", String::class, KModifier.PRIVATE)
        .receiver(function)
        .addTypeVariables(listOf(t, r))
        .getter(FunSpec.getterBuilder()
            .addStatement("return %S", "")
            .build())
        .build()
    assertThat(prop.toString()).isEqualTo("""
      |private val <T, R : kotlin.Any> java.util.function.Function<T, R>.property: kotlin.String where T : java.io.Serializable, T : kotlin.Cloneable
      |  get() = ""
      |""".trimMargin())
  }

  @Test fun reifiedTypeVariable() {
    val t = TypeVariableName("T").copy(reified = true)
    val prop = PropertySpec.builder("someFunction", t, KModifier.PRIVATE)
        .addTypeVariable(t)
        .receiver(KClass::class.asClassName().parameterizedBy(t))
        .getter(FunSpec.getterBuilder()
            .addModifiers(KModifier.INLINE)
            .addStatement("return stuff as %T", t)
            .build())
        .build()
    assertThat(prop.toString()).isEqualTo("""
      |private val <reified T> kotlin.reflect.KClass<T>.someFunction: T
      |  inline get() = stuff as T
      |""".trimMargin())
  }

  @Test fun reifiedTypeVariableNotAllowedWhenNoAccessors() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("property", String::class)
          .addTypeVariable(TypeVariableName("T").copy(reified = true))
          .build()
    }.hasMessageThat().isEqualTo(
        "only type parameters of properties with inline getters and/or setters can be reified!")
  }

  @Test fun reifiedTypeVariableNotAllowedWhenGetterNotInline() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("property", String::class)
          .addTypeVariable(TypeVariableName("T").copy(reified = true))
          .getter(FunSpec.getterBuilder()
              .addStatement("return %S", "")
              .build())
          .build()
    }.hasMessageThat().isEqualTo(
        "only type parameters of properties with inline getters and/or setters can be reified!")
  }

  @Test fun reifiedTypeVariableNotAllowedWhenSetterNotInline() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("property", String::class.asTypeName())
          .mutable()
          .addTypeVariable(TypeVariableName("T").copy(reified = true))
          .setter(FunSpec.setterBuilder()
              .addParameter("value", String::class)
              .addStatement("println()")
              .build())
          .build()
    }.hasMessageThat().isEqualTo(
        "only type parameters of properties with inline getters and/or setters can be reified!")
  }

  @Test fun reifiedTypeVariableNotAllowedWhenOnlySetterIsInline() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("property", String::class.asTypeName())
          .mutable()
          .addTypeVariable(TypeVariableName("T").copy(reified = true))
          .getter(FunSpec.getterBuilder()
              .addStatement("return %S", "")
              .build())
          .setter(FunSpec.setterBuilder()
              .addModifiers(KModifier.INLINE)
              .addParameter("value", String::class)
              .addStatement("println()")
              .build())
          .build()
    }.hasMessageThat().isEqualTo(
        "only type parameters of properties with inline getters and/or setters can be reified!")
  }

  @Test fun setterNotAllowedWhenPropertyIsNotMutable() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("property", String::class.asTypeName())
          .setter(FunSpec.setterBuilder()
              .addModifiers(KModifier.INLINE)
              .addParameter("value", String::class)
              .addStatement("println()")
              .build())
          .build()
    }.hasMessageThat().isEqualTo("only a mutable property can have a setter")
  }

  // https://github.com/square/kotlinpoet/issues/462
  @Test fun codeBlockInitializer() {
    val param = ParameterSpec.builder("arg", ANY).build()
    val initializer = CodeBlock.builder()
        .beginControlFlow("{ %L ->", param)
        .addStatement("println(\"arg=\$%N\")", param)
        .endControlFlow()
        .build()
    val lambdaTypeName = ClassName.bestGuess("com.example.SomeTypeAlias")
    val property = PropertySpec.builder("property", lambdaTypeName)
        .initializer(initializer)
        .build()
    assertThat(property.toString()).isEqualTo("""
      |val property: com.example.SomeTypeAlias = { arg: kotlin.Any ->
      |  println("arg=${'$'}arg")
      |}
      |
      |""".trimMargin())
  }

  @Test fun propertyKdocWithoutLinebreak() {
    val property = PropertySpec.builder("topping", String::class)
        .addKdoc("The topping you want on your pizza")
        .build()
    assertThat(property.toString()).isEqualTo("""
      |/**
      | * The topping you want on your pizza
      | */
      |val topping: kotlin.String
      |""".trimMargin())
  }

  @Test fun propertyKdocWithLinebreak() {
    val property = PropertySpec.builder("topping", String::class)
        .addKdoc("The topping you want on your pizza\n")
        .build()
    assertThat(property.toString()).isEqualTo("""
      |/**
      | * The topping you want on your pizza
      | */
      |val topping: kotlin.String
      |""".trimMargin())
  }

  @Test fun getterKdoc() {
    val property = PropertySpec.builder("amount", Int::class)
        .initializer("4")
        .getter(FunSpec.getterBuilder()
            .addKdoc("Simple multiplier")
            .addStatement("return %L * 5", "field")
            .build())
        .build()

    assertThat(property.toString()).isEqualTo("""
      |val amount: kotlin.Int = 4
      |  /**
      |   * Simple multiplier
      |   */
      |  get() = field * 5
      |""".trimMargin())
  }

  @Test fun constProperty() {
    val text = "This is a long string with a newline\nin the middle."
    val spec = FileSpec.builder("testsrc", "Test")
        .addProperty(PropertySpec.builder("FOO", String::class, KModifier.CONST)
            .initializer("%S", text)
            .build())
        .build()
    assertThat(spec.toString()).isEqualTo("""
      |package testsrc
      |
      |import kotlin.String
      |
      |public const val FOO: String = "This is a long string with a newline\nin the middle."
      |""".trimMargin())
  }

  @Test fun annotatedLambdaType() {
    val annotation = AnnotationSpec.builder(ClassName("com.squareup.tacos", "Annotation")).build()
    val type = LambdaTypeName.get(returnType = UNIT).copy(annotations = listOf(annotation))
    val spec = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.builder("foo", type).build())
        .build()
    assertThat(spec.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Unit
      |
      |public val foo: @Annotation () -> Unit
      |""".trimMargin())
  }
}
