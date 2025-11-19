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
package com.squareup.kotlinpoet

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.squareup.kotlinpoet.WildcardTypeName.Companion.consumerOf
import com.squareup.kotlinpoet.WildcardTypeName.Companion.producerOf
import org.junit.Test

class WildcardTypeNameTest {

  @Test
  fun equalsAndHashCode() {
    val anyProducer1 = producerOf(Any::class)
    val anyProducer2 = producerOf(Any::class.asTypeName())
    assertThat(anyProducer1).isEqualTo(anyProducer2)
    assertThat(anyProducer1.hashCode()).isEqualTo(anyProducer2.hashCode())
    assertThat(anyProducer1.toString()).isEqualTo(anyProducer2.toString())

    val stringConsumer1 = consumerOf(String::class)
    val stringConsumer2 = consumerOf(String::class.asTypeName())
    assertThat(stringConsumer1).isEqualTo(stringConsumer2)
    assertThat(stringConsumer1.hashCode()).isEqualTo(stringConsumer2.hashCode())
    assertThat(stringConsumer1.toString()).isEqualTo(stringConsumer2.toString())
  }

  @Test
  fun equalsDifferentiatesNullabilityAndAnnotations() {
    val anyProducer = producerOf(Any::class)

    assertThat(anyProducer.copy(nullable = true)).isNotEqualTo(anyProducer)

    assertThat(
        anyProducer.copy(annotations = listOf(AnnotationSpec.builder(Suppress::class).build()))
      )
      .isNotEqualTo(anyProducer)
  }

  @Test
  fun equalsAndHashCodeIgnoreTags() {
    val anyProducer = producerOf(Any::class)
    val tagged = anyProducer.copy(tags = mapOf(String::class to "test"))

    assertThat(anyProducer).isEqualTo(tagged)
    assertThat(anyProducer.hashCode()).isEqualTo(tagged.hashCode())
  }
}
