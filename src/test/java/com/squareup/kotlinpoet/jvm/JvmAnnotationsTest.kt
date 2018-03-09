/*
 * Copyright (C) 2018 Square, Inc.
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

package com.squareup.kotlinpoet.jvm

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.assertThrows
import java.io.IOException
import kotlin.test.Test

class JvmAnnotationsTest {

  @Test fun jvmField() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty(PropertySpec.builder("foo", String::class)
                .jvmField()
                .initializer("%S", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmField
      |
      |class Taco {
      |    @JvmField
      |    val foo: String = "foo"
      |}
      |""".trimMargin())
  }

  @Test fun jvmFieldConstructorParameter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("foo", String::class)
                .build())
            .addProperty(PropertySpec.builder("foo", String::class)
                .jvmField()
                .initializer("foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmField
      |
      |class Taco(@JvmField val foo: String)
      |""".trimMargin())
  }

  @Test fun jvmStaticProperty() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addType(TypeSpec.companionObjectBuilder()
                .addProperty(PropertySpec.builder("foo", String::class)
                    .jvmStatic()
                    .initializer("%S", "foo")
                    .build())
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmStatic
      |
      |class Taco {
      |    companion object {
      |        @JvmStatic
      |        val foo: String = "foo"
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun jvmStaticFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addType(TypeSpec.companionObjectBuilder()
                .addFunction(FunSpec.builder("foo")
                    .jvmStatic()
                    .addStatement("return %S", "foo")
                    .returns(String::class)
                    .build())
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmStatic
      |
      |class Taco {
      |    companion object {
      |        @JvmStatic
      |        fun foo(): String = "foo"
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun jvmStaticGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addType(TypeSpec.companionObjectBuilder()
                .addProperty(PropertySpec.builder("foo", String::class)
                    .getter(FunSpec.getterBuilder()
                        .jvmStatic()
                        .addStatement("return %S", "foo")
                        .build())
                    .build())
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmStatic
      |
      |class Taco {
      |    companion object {
      |        val foo: String
      |            @JvmStatic
      |            get() = "foo"
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun jvmStaticSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addType(TypeSpec.companionObjectBuilder()
                .addProperty(PropertySpec.varBuilder("foo", String::class)
                    .setter(FunSpec.setterBuilder()
                        .jvmStatic()
                        .addParameter("value", String::class)
                        .build())
                    .initializer("%S", "foo")
                    .build())
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmStatic
      |
      |class Taco {
      |    companion object {
      |        var foo: String = "foo"
      |            @JvmStatic
      |            set(value) {
      |            }
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun jvmStaticForbiddenOnConstructor() {
    assertThrows<IllegalStateException> {
      FunSpec.constructorBuilder()
          .jvmStatic()
    }.hasMessageThat().isEqualTo("Can't apply @JvmStatic to a constructor!")
  }

  @Test fun throwsFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .throws(IOException::class, IllegalArgumentException::class)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import java.io.IOException
      |import java.lang.IllegalArgumentException
      |import kotlin.jvm.Throws
      |
      |@Throws(
      |        IOException::class,
      |        IllegalArgumentException::class
      |)
      |fun foo() {
      |}
      |""".trimMargin())
  }

  @Test fun throwsFunctionCustomException() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .throws(ClassName("com.squareup.tacos", "IllegalTacoException"))
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.jvm.Throws
      |
      |@Throws(IllegalTacoException::class)
      |fun foo() {
      |}
      |""".trimMargin())
  }

  @Test fun throwsPrimaryConstructor() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .primaryConstructor(FunSpec.constructorBuilder()
                .throws(IOException::class)
                .addParameter("foo", String::class)
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import java.io.IOException
      |import kotlin.String
      |import kotlin.jvm.Throws
      |
      |class Taco @Throws(IOException::class) constructor(foo: String)
      |""".trimMargin())
  }

  @Test fun throwsGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.builder("foo", String::class)
            .getter(FunSpec.getterBuilder()
                .throws(IOException::class)
                .addStatement("return %S", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import java.io.IOException
      |import kotlin.String
      |import kotlin.jvm.Throws
      |
      |val foo: String
      |    @Throws(IOException::class)
      |    get() = "foo"
      |""".trimMargin())
  }

  @Test fun throwsSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.builder("foo", String::class)
            .setter(FunSpec.setterBuilder()
                .throws(IOException::class)
                .addParameter("value", String::class)
                .addStatement("print(%S)", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import java.io.IOException
      |import kotlin.String
      |import kotlin.jvm.Throws
      |
      |val foo: String
      |    @Throws(IOException::class)
      |    set(value) {
      |        print("foo")
      |    }
      |""".trimMargin())
  }

  @Test fun jvmOverloadsFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .jvmOverloads()
            .addParameter("bar", Int::class)
            .addParameter(ParameterSpec.builder("baz", String::class)
                .defaultValue("%S", "baz")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.String
      |import kotlin.jvm.JvmOverloads
      |
      |@JvmOverloads
      |fun foo(bar: Int, baz: String = "baz") {
      |}
      |""".trimMargin())
  }

  @Test fun jvmOverloadsPrimaryConstructor() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .primaryConstructor(FunSpec.constructorBuilder()
                .jvmOverloads()
                .addParameter("bar", Int::class)
                .addParameter(ParameterSpec.builder("baz", String::class)
                    .defaultValue("%S", "baz")
                    .build())
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.String
      |import kotlin.jvm.JvmOverloads
      |
      |class Taco @JvmOverloads constructor(bar: Int, baz: String = "baz")
      |""".trimMargin())
  }

  @Test fun jvmOverloadsOnGetterForbidden() {
    assertThrows<IllegalStateException> {
      FunSpec.getterBuilder()
          .jvmOverloads()
    }.hasMessageThat().isEqualTo("Can't apply @JvmOverloads to a getter!")
  }

  @Test fun jvmOverloadsOnSetterForbidden() {
    assertThrows<IllegalStateException> {
      FunSpec.setterBuilder()
          .jvmOverloads()
    }.hasMessageThat().isEqualTo("Can't apply @JvmOverloads to a setter!")
  }

  @Test fun jvmNameFile() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .jvmName("TacoUtils")
        .addProperty(PropertySpec.builder("foo", String::class)
            .initializer("%S", "foo")
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |@file:JvmName("TacoUtils")
      |
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmName
      |
      |val foo: String = "foo"
      |""".trimMargin())
  }

  @Test fun jvmNameFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .jvmName("getFoo")
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.jvm.JvmName
      |
      |@JvmName("getFoo")
      |fun foo() {
      |}
      |""".trimMargin())
  }

  @Test fun jvmNameGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.builder("foo", String::class)
            .getter(FunSpec.getterBuilder()
                .jvmName("foo")
                .addStatement("return %S", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmName
      |
      |val foo: String
      |    @JvmName("foo")
      |    get() = "foo"
      |""".trimMargin())
  }

  @Test fun jvmNameSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.varBuilder("foo", String::class)
            .initializer("%S", "foo")
            .setter(FunSpec.setterBuilder()
                .jvmName("foo")
                .addParameter("value", String::class)
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmName
      |
      |var foo: String = "foo"
      |    @JvmName("foo")
      |    set(value) {
      |    }
      |""".trimMargin())
  }

  @Test fun jvmNameForbiddenOnConstructor() {
    assertThrows<IllegalStateException> {
      FunSpec.constructorBuilder()
          .jvmName("notAConstructor")
    }.hasMessageThat().isEqualTo("Can't apply @JvmName to a constructor!")
  }

  @Test fun jvmMultifileClass() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .jvmMultifileClass()
        .addProperty(PropertySpec.builder("foo", String::class)
            .initializer("%S", "foo")
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |@file:JvmMultifileClass
      |
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmMultifileClass
      |
      |val foo: String = "foo"
      |""".trimMargin())
  }

  @Test fun jvmSuppressWildcardsClass() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .jvmSuppressWildcards()
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |@JvmSuppressWildcards
      |class Taco
      |""".trimMargin())
  }

  @Test fun jvmSuppressWildcardsFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .jvmSuppressWildcards(suppress = false)
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |@JvmSuppressWildcards(suppress = false)
      |fun foo() {
      |}
      |""".trimMargin())
  }

  @Test fun jvmSuppressWildcardsOnConstructorForbidden() {
    assertThrows<IllegalStateException> {
      FunSpec.constructorBuilder()
          .jvmSuppressWildcards()
    }.hasMessageThat().isEqualTo("Can't apply @JvmSuppressWildcards to a constructor!")
  }

  @Test fun jvmSuppressWildcardsOnGetterForbidden() {
    assertThrows<IllegalStateException> {
      FunSpec.getterBuilder()
          .jvmSuppressWildcards()
    }.hasMessageThat().isEqualTo("Can't apply @JvmSuppressWildcards to a getter!")
  }

  @Test fun jvmSuppressWildcardsOnSetterForbidden() {
    assertThrows<IllegalStateException> {
      FunSpec.setterBuilder()
          .jvmSuppressWildcards()
    }.hasMessageThat().isEqualTo("Can't apply @JvmSuppressWildcards to a setter!")
  }

  @Test fun jvmSuppressWildcardsProperty() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.builder("foo", String::class)
            .jvmSuppressWildcards(suppress = false)
            .initializer("%S", "foo")
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |@JvmSuppressWildcards(suppress = false)
      |val foo: String = "foo"
      |""".trimMargin())
  }

  @Test fun jvmSuppressWildcardsType() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .addParameter("a", ParameterizedTypeName.get(
                rawType = List::class.asClassName(),
                typeArguments = *arrayOf(Int::class.asTypeName().jvmSuppressWildcards())
            ))
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.collections.List
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |fun foo(a: List<@JvmSuppressWildcards Int>) {
      |}
      |""".trimMargin())
  }

  @Test fun jvmWildcardType() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .addParameter("a", ParameterizedTypeName.get(
                rawType = List::class.asClassName(),
                typeArguments = *arrayOf(Int::class.asTypeName().jvmWildcard())
            ))
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.collections.List
      |import kotlin.jvm.JvmWildcard
      |
      |fun foo(a: List<@JvmWildcard Int>) {
      |}
      |""".trimMargin())
  }

  @Test fun synchronizedFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .synchronized()
            .addStatement("return %S", "foo")
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.jvm.Synchronized
      |
      |@Synchronized
      |fun foo() = "foo"
      |""".trimMargin())
  }

  @Test fun synchronizedGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.builder("foo", String::class)
            .getter(FunSpec.getterBuilder()
                .synchronized()
                .addStatement("return %S", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Synchronized
      |
      |val foo: String
      |    @Synchronized
      |    get() = "foo"
      |""".trimMargin())
  }

  @Test fun synchronizedSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.varBuilder("foo", String::class)
            .initializer("%S", "foo")
            .setter(FunSpec.setterBuilder()
                .synchronized()
                .addParameter("value", String::class)
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Synchronized
      |
      |var foo: String = "foo"
      |    @Synchronized
      |    set(value) {
      |    }
      |""".trimMargin())
  }

  @Test fun synchronizedOnConstructorForbidden() {
    assertThrows<IllegalStateException> {
      FunSpec.constructorBuilder()
          .synchronized()
    }.hasMessageThat().isEqualTo("Can't apply @Synchronized to a constructor!")
  }

  @Test fun transient() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty(PropertySpec.builder("foo", String::class)
                .transient()
                .initializer("%S", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Transient
      |
      |class Taco {
      |    @Transient
      |    val foo: String = "foo"
      |}
      |""".trimMargin())
  }

  @Test fun transientConstructorParameter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("foo", String::class)
                .build())
            .addProperty(PropertySpec.builder("foo", String::class)
                .transient()
                .initializer("foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Transient
      |
      |class Taco(@Transient val foo: String)
      |""".trimMargin())
  }

  @Test fun volatile() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .addProperty(PropertySpec.builder("foo", String::class)
                .volatile()
                .initializer("%S", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Volatile
      |
      |class Taco {
      |    @Volatile
      |    val foo: String = "foo"
      |}
      |""".trimMargin())
  }

  @Test fun volatileConstructorParameter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("foo", String::class)
                .build())
            .addProperty(PropertySpec.builder("foo", String::class)
                .volatile()
                .initializer("foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Volatile
      |
      |class Taco(@Volatile val foo: String)
      |""".trimMargin())
  }

  @Test fun strictfpFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addFunction(FunSpec.builder("foo")
            .strictfp()
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.jvm.Strictfp
      |
      |@Strictfp
      |fun foo() {
      |}
      |""".trimMargin())
  }

  @Test fun strictfpPrimaryConstructor() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addType(TypeSpec.classBuilder("Taco")
            .primaryConstructor(FunSpec.constructorBuilder()
                .strictfp()
                .addParameter("foo", String::class)
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Strictfp
      |
      |class Taco @Strictfp constructor(foo: String)
      |""".trimMargin())
  }

  @Test fun strictfpGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.builder("foo", String::class)
            .getter(FunSpec.getterBuilder()
                .strictfp()
                .addStatement("return %S", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Strictfp
      |
      |val foo: String
      |    @Strictfp
      |    get() = "foo"
      |""".trimMargin())
  }

  @Test fun strictfpSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
        .addProperty(PropertySpec.builder("foo", String::class)
            .setter(FunSpec.setterBuilder()
                .strictfp()
                .addParameter("value", String::class)
                .addStatement("print(%S)", "foo")
                .build())
            .build())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Strictfp
      |
      |val foo: String
      |    @Strictfp
      |    set(value) {
      |        print("foo")
      |    }
      |""".trimMargin())
  }
}
