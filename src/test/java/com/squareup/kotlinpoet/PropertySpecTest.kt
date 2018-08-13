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
    val type = String::class.asClassName().asNullable()
    val a = PropertySpec.builder("foo", type).build()
    assertThat(a.toString()).isEqualTo("val foo: kotlin.String?\n")
  }

  @Test fun delegated() {
    val prop = PropertySpec.builder("foo", String::class)
        .delegate("Delegates.notNull()")
        .build()
    assertThat(prop.toString()).isEqualTo("val foo: kotlin.String by Delegates.notNull()\n")
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
      |    inline get() = "foo"
      |""".trimMargin())
  }

  @Test fun inlineBothAccessors() {
    val prop = PropertySpec.varBuilder("foo", String::class)
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
      |    get() = "foo"
      |    set(value) {
      |    }
      |""".trimMargin())
  }

  @Test fun inlineForbiddenOnProperty() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("foo", String::class)
          .addModifiers(KModifier.INLINE)
          .build()
    }
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

  @Test
  fun externalTopLevel() {
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
    val prop = PropertySpec.builder("tacos", Int::class)
        .mutable(true)
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
        .build()

    assertThat(prop.toBuilder().build()).isEqualTo(prop)
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
      |    inline get() = stuff as T
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
      |private val <T, R : kotlin.Any> java.util.function.Function<T, R>.property: kotlin.String
      |        where T : java.io.Serializable, T : kotlin.Cloneable
      |    get() = ""
      |""".trimMargin())
  }

  @Test fun reifiedTypeVariable() {
    val t = TypeVariableName("T").reified(true)
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
      |    inline get() = stuff as T
      |""".trimMargin())
  }

  @Test fun reifiedTypeVariableNotAllowedWhenNoAccessors() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("property", String::class)
          .addTypeVariable(TypeVariableName("T").reified(true))
          .build()
    }.hasMessageThat().isEqualTo(
        "only type parameters of properties with inline getters and/or setters can be reified!")
  }

  @Test fun reifiedTypeVariableNotAllowedWhenGetterNotInline() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("property", String::class)
          .addTypeVariable(TypeVariableName("T").reified(true))
          .getter(FunSpec.getterBuilder()
              .addStatement("return %S", "")
              .build())
          .build()
    }.hasMessageThat().isEqualTo(
        "only type parameters of properties with inline getters and/or setters can be reified!")
  }

  @Test fun reifiedTypeVariableNotAllowedWhenSetterNotInline() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.varBuilder("property", String::class)
          .addTypeVariable(TypeVariableName("T").reified(true))
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
      PropertySpec.varBuilder("property", String::class)
          .addTypeVariable(TypeVariableName("T").reified(true))
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
}
