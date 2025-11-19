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
package com.squareup.kotlinpoet.ksp

import com.google.devtools.ksp.symbol.Visibility
import com.google.devtools.ksp.symbol.Visibility.JAVA_PACKAGE
import com.google.devtools.ksp.symbol.Visibility.LOCAL
import com.squareup.kotlinpoet.KModifier

/**
 * Returns the [KModifier] representation of this visibility or null if this is [JAVA_PACKAGE] or
 * [LOCAL] (which do not have obvious [KModifier] alternatives).
 */
public fun Visibility.toKModifier(): KModifier? {
  return when (this) {
    Visibility.PUBLIC -> KModifier.PUBLIC
    Visibility.PRIVATE -> KModifier.PRIVATE
    Visibility.PROTECTED -> KModifier.PROTECTED
    Visibility.INTERNAL -> KModifier.INTERNAL
    else -> null
  }
}
