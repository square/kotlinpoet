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

class LambdaSpecTest {

  @Test fun emptyLambda() {
    val lambda = LambdaSpec.builder().build()
    assertThat(lambda.toString()).isEqualTo("{}")
  }

  @Test fun noParamLambda() {
    val lambda = LambdaSpec.builder("it.trim()").build()
    assertThat(lambda.toString()).isEqualTo("{ it.trim() }")
  }

  @Test fun oneParamLambda() {
    val lambda = LambdaSpec.builder("println(str)")
        .addParameter("str", String::class)
        .build()
    assertThat(lambda.toString()).isEqualTo("""
      |{ str: kotlin.String ->
      |  println(str)
      |}
      """.trimMargin())
  }

  @Test fun twoParamLambda() {
    val lambda = LambdaSpec.builder("x + y")
        .addParameter("x", Int::class)
        .addParameter("y", Int::class)
        .build()
    assertThat(lambda.toString()).isEqualTo("""
      |{ x: kotlin.Int, y: kotlin.Int ->
      |  x + y
      |}
      """.trimMargin())
  }

  @Test fun multilineBody() {
    val lambda = LambdaSpec.builder()
        .addParameter("str", String::class)
        .addBody("println(str)")
        .addBody("\nstr.trim()")
        .build()
    assertThat(lambda.toString()).isEqualTo("""
      |{ str: kotlin.String ->
      |  println(str)
      |  str.trim()
      |}
      """.trimMargin())
  }

  @Test fun lambdaWithParamsAndNoBodyForbidden() {
    assertThrows<IllegalStateException> {
      LambdaSpec.builder()
          .addParameter("x", Int::class)
          .build()
    }.hasMessage("a lambda expression with parameters must have a body!")
  }
}
