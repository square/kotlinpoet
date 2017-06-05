/*
 * Copyright (C) 2014 Google, Inc.
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

import com.google.testing.compile.CompilationRule
import org.junit.Ignore
import org.junit.Rule
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@Ignore("Not clear this test is useful to retain in the Kotlin world")
class TypesTest : AbstractTypesTest() {
  @JvmField @Rule val compilation = CompilationRule()

  override val elements : Elements
    get() = compilation.elements

  override val types : Types
    get() = compilation.types
}
