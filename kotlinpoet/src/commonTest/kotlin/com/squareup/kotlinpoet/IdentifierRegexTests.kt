/*
 * Copyright (C) 2024 Square, Inc.
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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdentifierRegexTests {
  @Test
  fun multiplatformIdentifierRegexTest() {
    assertTrue("foo".isIdentifier)
    assertTrue("bAr1".isIdentifier)
    assertFalse("1".isIdentifier)
    assertFalse("♦♥♠♣".isIdentifier)
    assertTrue("`♦♥♠♣`".isIdentifier)
    assertTrue("`  ♣ !`".isIdentifier)
    assertFalse("€".isIdentifier)
    assertTrue("`€`".isIdentifier)
    assertTrue("`1`".isIdentifier)
    assertFalse("```".isIdentifier)
    assertFalse("``".isIdentifier)
    assertFalse("\n".isIdentifier)
    assertFalse("`\n`".isIdentifier)
    assertFalse("\r".isIdentifier)
    assertFalse("`\r`".isIdentifier)
    assertTrue("when".isIdentifier)
    assertTrue("fun".isIdentifier)
    assertFalse("".isIdentifier)
  }
}
