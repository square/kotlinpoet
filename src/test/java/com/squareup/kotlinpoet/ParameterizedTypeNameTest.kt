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

package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import org.junit.Test

class ParameterizedTypeNameTest {
  @Test fun classNamePlusParameter() {
    val typeName = ClassName("kotlin.collections", "List")
        .plusParameter(ClassName("kotlin", "String"))
    assertThat(typeName.toString()).isEqualTo("kotlin.collections.List<kotlin.String>")
  }

  @Test fun classNamePlusTwoParameters() {
    val typeName = ClassName("kotlin.collections", "Map")
        .plusParameter(ClassName("kotlin", "String"))
        .plusParameter(ClassName("kotlin", "Int"))
    assertThat(typeName.toString()).isEqualTo("kotlin.collections.Map<kotlin.String, kotlin.Int>")
  }

  @Test fun classNamePlusTypeVariableParameter() {
    val t = TypeVariableName("T")
    val mapOfT = Map::class.asTypeName().plusParameter(t)
    assertThat(mapOfT.toString()).isEqualTo("kotlin.collections.Map<T>")
  }

  @Test fun kClassPlusParameter() {
    val typeName = List::class.plusParameter(String::class)
    assertThat(typeName.toString()).isEqualTo("kotlin.collections.List<kotlin.String>")
  }

  @Test fun kClassPlusTwoParameters() {
    val typeName = Map::class
        .plusParameter(String::class)
        .plusParameter(Int::class)
    assertThat(typeName.toString()).isEqualTo("kotlin.collections.Map<kotlin.String, kotlin.Int>")
  }

  @Test fun classPlusParameter() {
    val typeName = java.util.List::class.java.plusParameter(java.lang.String::class.java)
    assertThat(typeName.toString()).isEqualTo("java.util.List<java.lang.String>")
  }

  @Test fun classPlusTwoParameters() {
    val typeName = java.util.Map::class.java
        .plusParameter(java.lang.String::class.java)
        .plusParameter(java.lang.Integer::class.java)
    assertThat(typeName.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>")
  }
}
