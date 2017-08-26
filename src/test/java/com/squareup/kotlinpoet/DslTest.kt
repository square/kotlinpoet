/*
 * Copyright (C) 2017 Square, Inc.
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
import org.junit.Test

class DslTest {

  @Test fun dslDemo() {
    val greeterClass = ClassName("", "Greeter")
    val kotlinFile = KotlinFile.builder("", "HelloWorld").init {
      addType(TypeSpec.classBuilder("Greeter").init {
        primaryConstructor(FunSpec.constructorBuilder().init {
          addParameter("name", String::class)
        })
        addProperty(PropertySpec.builder("name", String::class).init {
          initializer("name")
        })
        addFunction(FunSpec.builder("greet").init {
          addStatement("println(%S)", "Hello, \$name")
        })
      })
      addFunction(FunSpec.builder("main").init {
        addParameter("args", String::class, KModifier.VARARG)
        addStatement("%T(args[0]).greet()", greeterClass)
      })
    }

    assertThat(kotlinFile.toString()).isEqualTo("""
      |import kotlin.String
      |
      |class Greeter(val name: String) {
      |  fun greet() {
      |    println("Hello, ${'$'}name")
      |  }
      |}
      |
      |fun main(vararg args: String) {
      |  Greeter(args[0]).greet()
      |}
      |""".trimMargin())
  }
}
