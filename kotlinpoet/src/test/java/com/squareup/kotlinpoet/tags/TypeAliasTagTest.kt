/*
 * Copyright (C) 2023 Square, Inc.
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
package com.squareup.kotlinpoet.tags

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import org.junit.Test

class TypeAliasTagTest {

  @Test fun equalsAndHashCode() {
    val foo = TypeAliasTag(ClassName("com.example", "Foo"))

    assertThat(foo).isEqualTo(TypeAliasTag(ClassName("com.example", "Foo")))
    assertThat(foo.hashCode()).isEqualTo(ClassName("com.example", "Foo").hashCode())

    assertThat(foo).isNotEqualTo(ClassName("com.example", "Bar"))
  }
}
