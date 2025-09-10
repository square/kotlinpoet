/*
 * Copyright (C) 2018 Square, Inc.
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
package com.squareup.kotlinpoet.jvm

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.io.IOException
import kotlin.test.Test

class JvmAnnotationsTest {

  @Test fun jvmField() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addProperty(
            PropertySpec.builder("foo", String::class)
              .jvmField()
              .initializer("%S", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmField
      |
      |public class Taco {
      |  @JvmField
      |  public val foo: String = "foo"
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmFieldConstructorParameter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("foo", String::class)
              .build(),
          )
          .addProperty(
            PropertySpec.builder("foo", String::class)
              .jvmField()
              .initializer("foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmField
      |
      |public class Taco(
      |  @JvmField
      |  public val foo: String,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmStaticProperty() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addType(
            TypeSpec.companionObjectBuilder()
              .addProperty(
                PropertySpec.builder("foo", String::class)
                  .jvmStatic()
                  .initializer("%S", "foo")
                  .build(),
              )
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmStatic
      |
      |public class Taco {
      |  public companion object {
      |    @JvmStatic
      |    public val foo: String = "foo"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmStaticFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addType(
            TypeSpec.companionObjectBuilder()
              .addFunction(
                FunSpec.builder("foo")
                  .jvmStatic()
                  .addStatement("return %S", "foo")
                  .returns(String::class)
                  .build(),
              )
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmStatic
      |
      |public class Taco {
      |  public companion object {
      |    @JvmStatic
      |    public fun foo(): String = "foo"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmStaticGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addType(
            TypeSpec.companionObjectBuilder()
              .addProperty(
                PropertySpec.builder("foo", String::class)
                  .getter(
                    FunSpec.getterBuilder()
                      .jvmStatic()
                      .addStatement("return %S", "foo")
                      .build(),
                  )
                  .build(),
              )
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmStatic
      |
      |public class Taco {
      |  public companion object {
      |    public val foo: String
      |      @JvmStatic
      |      get() = "foo"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmStaticSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addType(
            TypeSpec.companionObjectBuilder()
              .addProperty(
                PropertySpec.builder("foo", String::class.asTypeName())
                  .mutable()
                  .setter(
                    FunSpec.setterBuilder()
                      .jvmStatic()
                      .addParameter("value", String::class)
                      .build(),
                  )
                  .initializer("%S", "foo")
                  .build(),
              )
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmStatic
      |
      |public class Taco {
      |  public companion object {
      |    public var foo: String = "foo"
      |      @JvmStatic
      |      set(`value`) {
      |      }
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmStaticForbiddenOnConstructor() {
    assertFailure {
      FunSpec.constructorBuilder()
        .jvmStatic()
    }.isInstanceOf<IllegalStateException>()
      .hasMessage("Can't apply @JvmStatic to a constructor!")
  }

  @Test fun throwsFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .throws(IOException::class, IllegalArgumentException::class)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import java.io.IOException
      |import java.lang.IllegalArgumentException
      |import kotlin.jvm.Throws
      |
      |@Throws(
      |  IOException::class,
      |  IllegalArgumentException::class,
      |)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun throwsFunctionCustomException() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .throws(ClassName("com.squareup.tacos", "IllegalTacoException"))
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.jvm.Throws
      |
      |@Throws(IllegalTacoException::class)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun throwsPrimaryConstructor() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .throws(IOException::class)
              .addParameter("foo", String::class)
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import java.io.IOException
      |import kotlin.String
      |import kotlin.jvm.Throws
      |
      |public class Taco @Throws(IOException::class) constructor(
      |  foo: String,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun throwsGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .getter(
            FunSpec.getterBuilder()
              .throws(IOException::class)
              .addStatement("return %S", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import java.io.IOException
      |import kotlin.String
      |import kotlin.jvm.Throws
      |
      |public val foo: String
      |  @Throws(IOException::class)
      |  get() = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun throwsSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .mutable()
          .setter(
            FunSpec.setterBuilder()
              .throws(IOException::class)
              .addParameter("value", String::class)
              .addStatement("print(%S)", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import java.io.IOException
      |import kotlin.String
      |import kotlin.jvm.Throws
      |
      |public var foo: String
      |  @Throws(IOException::class)
      |  set(`value`) {
      |    print("foo")
      |  }
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmOverloadsFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .jvmOverloads()
          .addParameter("bar", Int::class)
          .addParameter(
            ParameterSpec.builder("baz", String::class)
              .defaultValue("%S", "baz")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.String
      |import kotlin.jvm.JvmOverloads
      |
      |@JvmOverloads
      |public fun foo(bar: Int, baz: String = "baz") {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmOverloadsPrimaryConstructor() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .jvmOverloads()
              .addParameter("bar", Int::class)
              .addParameter(
                ParameterSpec.builder("baz", String::class)
                  .defaultValue("%S", "baz")
                  .build(),
              )
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.String
      |import kotlin.jvm.JvmOverloads
      |
      |public class Taco @JvmOverloads constructor(
      |  bar: Int,
      |  baz: String = "baz",
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmOverloadsOnGetterForbidden() {
    assertFailure {
      FunSpec.getterBuilder()
        .jvmOverloads()
    }.isInstanceOf<IllegalStateException>()
      .hasMessage("Can't apply @JvmOverloads to a getter!")
  }

  @Test fun jvmOverloadsOnSetterForbidden() {
    assertFailure {
      FunSpec.setterBuilder()
        .jvmOverloads()
    }.isInstanceOf<IllegalStateException>()
      .hasMessage("Can't apply @JvmOverloads to a setter!")
  }

  @Test fun jvmNameFile() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .jvmName("TacoUtils")
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .initializer("%S", "foo")
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |@file:JvmName("TacoUtils")
      |
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmName
      |
      |public val foo: String = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmNameFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .jvmName("getFoo")
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.jvm.JvmName
      |
      |@JvmName("getFoo")
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmNameGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .getter(
            FunSpec.getterBuilder()
              .jvmName("foo")
              .addStatement("return %S", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmName
      |
      |public val foo: String
      |  @JvmName("foo")
      |  get() = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmNameSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class.asTypeName())
          .mutable()
          .initializer("%S", "foo")
          .setter(
            FunSpec.setterBuilder()
              .jvmName("foo")
              .addParameter("value", String::class)
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmName
      |
      |public var foo: String = "foo"
      |  @JvmName("foo")
      |  set(`value`) {
      |  }
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmNameForbiddenOnConstructor() {
    assertFailure {
      FunSpec.constructorBuilder()
        .jvmName("notAConstructor")
    }.isInstanceOf<IllegalStateException>()
      .hasMessage("Can't apply @JvmName to a constructor!")
  }

  @Test fun jvmMultifileClass() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .jvmMultifileClass()
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .initializer("%S", "foo")
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |@file:JvmMultifileClass
      |
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmMultifileClass
      |
      |public val foo: String = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmSuppressWildcardsClass() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .jvmSuppressWildcards()
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |@JvmSuppressWildcards
      |public class Taco
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmSuppressWildcardsFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .jvmSuppressWildcards(suppress = false)
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |@JvmSuppressWildcards(suppress = false)
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmSuppressWildcardsOnConstructorForbidden() {
    assertFailure {
      FunSpec.constructorBuilder()
        .jvmSuppressWildcards()
    }.isInstanceOf<IllegalStateException>()
      .hasMessage("Can't apply @JvmSuppressWildcards to a constructor!")
  }

  @Test fun jvmSuppressWildcardsOnGetterForbidden() {
    assertFailure {
      FunSpec.getterBuilder()
        .jvmSuppressWildcards()
    }.isInstanceOf<IllegalStateException>()
      .hasMessage("Can't apply @JvmSuppressWildcards to a getter!")
  }

  @Test fun jvmSuppressWildcardsOnSetterForbidden() {
    assertFailure {
      FunSpec.setterBuilder()
        .jvmSuppressWildcards()
    }.isInstanceOf<IllegalStateException>()
      .hasMessage("Can't apply @JvmSuppressWildcards to a setter!")
  }

  @Test fun jvmSuppressWildcardsProperty() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .jvmSuppressWildcards(suppress = false)
          .initializer("%S", "foo")
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |@JvmSuppressWildcards(suppress = false)
      |public val foo: String = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmSuppressWildcardsType() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .addParameter(
            "a",
            List::class.asClassName()
              .parameterizedBy(Int::class.asTypeName().jvmSuppressWildcards()),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.collections.List
      |import kotlin.jvm.JvmSuppressWildcards
      |
      |public fun foo(a: List<@JvmSuppressWildcards Int>) {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmWildcardType() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .addParameter(
            "a",
            List::class.asClassName()
              .parameterizedBy(Int::class.asTypeName().jvmWildcard()),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Int
      |import kotlin.collections.List
      |import kotlin.jvm.JvmWildcard
      |
      |public fun foo(a: List<@JvmWildcard Int>) {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun synchronizedFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .synchronized()
          .returns(STRING)
          .addStatement("return %S", "foo")
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Synchronized
      |
      |@Synchronized
      |public fun foo(): String = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun synchronizedGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .getter(
            FunSpec.getterBuilder()
              .synchronized()
              .addStatement("return %S", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Synchronized
      |
      |public val foo: String
      |  @Synchronized
      |  get() = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun synchronizedSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class.asTypeName())
          .mutable()
          .initializer("%S", "foo")
          .setter(
            FunSpec.setterBuilder()
              .synchronized()
              .addParameter("value", String::class)
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Synchronized
      |
      |public var foo: String = "foo"
      |  @Synchronized
      |  set(`value`) {
      |  }
      |
      """.trimMargin(),
    )
  }

  @Test fun synchronizedOnConstructorForbidden() {
    assertFailure {
      FunSpec.constructorBuilder()
        .synchronized()
    }.isInstanceOf<IllegalStateException>()
      .hasMessage("Can't apply @Synchronized to a constructor!")
  }

  @Test fun transient() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addProperty(
            PropertySpec.builder("foo", String::class)
              .transient()
              .initializer("%S", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Transient
      |
      |public class Taco {
      |  @Transient
      |  public val foo: String = "foo"
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun transientConstructorParameter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("foo", String::class)
              .build(),
          )
          .addProperty(
            PropertySpec.builder("foo", String::class)
              .transient()
              .initializer("foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Transient
      |
      |public class Taco(
      |  @Transient
      |  public val foo: String,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun volatile() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addProperty(
            PropertySpec.builder("foo", String::class)
              .volatile()
              .initializer("%S", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Volatile
      |
      |public class Taco {
      |  @Volatile
      |  public val foo: String = "foo"
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun volatileConstructorParameter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("foo", String::class)
              .build(),
          )
          .addProperty(
            PropertySpec.builder("foo", String::class)
              .volatile()
              .initializer("foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Volatile
      |
      |public class Taco(
      |  @Volatile
      |  public val foo: String,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun strictfpFunction() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .strictfp()
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.jvm.Strictfp
      |
      |@Strictfp
      |public fun foo() {
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun strictfpPrimaryConstructor() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .strictfp()
              .addParameter("foo", String::class)
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Strictfp
      |
      |public class Taco @Strictfp constructor(
      |  foo: String,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun strictfpGetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .getter(
            FunSpec.getterBuilder()
              .strictfp()
              .addStatement("return %S", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Strictfp
      |
      |public val foo: String
      |  @Strictfp
      |  get() = "foo"
      |
      """.trimMargin(),
    )
  }

  @Test fun strictfpSetter() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addProperty(
        PropertySpec.builder("foo", String::class)
          .mutable()
          .setter(
            FunSpec.setterBuilder()
              .strictfp()
              .addParameter("value", String::class)
              .addStatement("print(%S)", "foo")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.Strictfp
      |
      |public var foo: String
      |  @Strictfp
      |  set(`value`) {
      |    print("foo")
      |  }
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmInlineClass() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .addModifiers(KModifier.VALUE)
          .jvmInline()
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("value", STRING)
              .build(),
          )
          .addProperty(
            PropertySpec.builder("value", STRING)
              .initializer("value")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmInline
      |
      |@JvmInline
      |public value class Taco(
      |  public val `value`: String,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun jvmRecordClass() {
    val file = FileSpec.builder("com.squareup.tacos", "Taco")
      .addType(
        TypeSpec.classBuilder("Taco")
          .jvmRecord()
          .addModifiers(DATA)
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("value", STRING)
              .build(),
          )
          .addProperty(
            PropertySpec.builder("value", STRING)
              .initializer("value")
              .build(),
          )
          .build(),
      )
      .build()
    assertThat(file.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.String
      |import kotlin.jvm.JvmRecord
      |
      |@JvmRecord
      |public data class Taco(
      |  public val `value`: String,
      |)
      |
      """.trimMargin(),
    )
  }
}
