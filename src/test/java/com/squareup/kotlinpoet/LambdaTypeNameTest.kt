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

import com.squareup.kotlinpoet.KModifier.VARARG
import org.junit.Test
import javax.annotation.Nullable

class LambdaTypeNameTest {

  @Test fun paramsWithAnnotationsForbidden() {
    assertThrows<IllegalArgumentException> {
      LambdaTypeName.get(
          parameters = *arrayOf(ParameterSpec.builder("foo", Int::class)
              .addAnnotation(Nullable::class)
              .build()),
          returnType = Unit::class.asTypeName())
    }.hasMessageThat().isEqualTo("Parameters with annotations are not allowed")
  }

  @Test fun paramsWithModifiersForbidden() {
    assertThrows<IllegalArgumentException> {
      LambdaTypeName.get(
          parameters = *arrayOf(ParameterSpec.builder("foo", Int::class)
              .addModifiers(VARARG)
              .build()),
          returnType = Unit::class.asTypeName())
    }.hasMessageThat().isEqualTo("Parameters with modifiers are not allowed")
  }

  @Test fun paramsWithDefaultValueForbidden() {
    assertThrows<IllegalArgumentException> {
      LambdaTypeName.get(
          parameters = *arrayOf(ParameterSpec.builder("foo", Int::class)
              .defaultValue("42")
              .build()),
          returnType = Unit::class.asTypeName())
    }.hasMessageThat().isEqualTo("Parameters with default values are not allowed")
  }
}
