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

private typealias IdentifierMatcher = (String) -> Boolean

private fun createIdentifierMatcher(): IdentifierMatcher {
  val regExp = createRegExp(IDENTIFIER_REGEX_VALUE)
  return f@{ input ->
    // The logic here is similar to Regex.matches in JS platform.
    regExpReset(regExp)
    val match = regExpExec(regExp, input) ?: return@f false

    regExpMatchCheck(regExp, match, input)
  }
}

private val identifierMatcher = createIdentifierMatcher()

internal actual val String.isIdentifier: Boolean get() = identifierMatcher(this)

@Suppress("UNUSED_PARAMETER")
private fun createRegExp(pattern: String): JsAny =
  js("new RegExp(pattern, 'gu')")

@Suppress("UNUSED_PARAMETER")
private fun regExpReset(regExp: JsAny) {
  js("regExp.lastIndex = 0")
}

@Suppress("UNUSED_PARAMETER", "RedundantNullableReturnType")
private fun regExpExec(regExp: JsAny, input: String): JsAny? =
  js("regExp.exec(input)")

@Suppress("UNUSED_PARAMETER")
private fun regExpMatchCheck(
  regExp: JsAny,
  regExpMatch: JsAny,
  input: String,
): Boolean =
  js("regExpMatch.index == 0 && regExp.lastIndex == input.length")
