/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.kotlinpoet.ksp.test.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.message
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.test.assertFailsWith
import org.junit.Test

class KsTypesTest {
  // Regression test for https://github.com/square/kotlinpoet/issues/1178
  @Test
  fun errorTypesShouldFail() {
    val type = object : KSType {
      override val isError: Boolean = true

      // Boilerplate
      override val annotations: Sequence<KSAnnotation>
        get() = throw NotImplementedError()
      override val arguments: List<KSTypeArgument>
        get() = emptyList()
      override val declaration: KSDeclaration
        get() = throw NotImplementedError()
      override val isFunctionType: Boolean
        get() = throw NotImplementedError()
      override val isMarkedNullable: Boolean
        get() = throw NotImplementedError()
      override val isSuspendFunctionType: Boolean
        get() = throw NotImplementedError()
      override val nullability: Nullability
        get() = throw NotImplementedError()

      override fun isAssignableFrom(that: KSType): Boolean {
        throw NotImplementedError()
      }

      override fun isCovarianceFlexible(): Boolean {
        throw NotImplementedError()
      }

      override fun isMutabilityFlexible(): Boolean {
        throw NotImplementedError()
      }

      override fun makeNotNullable(): KSType {
        throw NotImplementedError()
      }

      override fun makeNullable(): KSType {
        throw NotImplementedError()
      }

      override fun replace(arguments: List<KSTypeArgument>): KSType {
        throw NotImplementedError()
      }

      override fun starProjection(): KSType {
        throw NotImplementedError()
      }
    }

    val exception1 = assertFailsWith<IllegalArgumentException> {
      type.toClassName()
    }
    assertThat(exception1).message()
      .isNotNull()
      .contains("is not resolvable in the current round of processing")

    val exception2 = assertFailsWith<IllegalArgumentException> {
      type.toTypeName()
    }
    assertThat(exception2).message()
      .isNotNull()
      .contains("is not resolvable in the current round of processing")
  }
}
