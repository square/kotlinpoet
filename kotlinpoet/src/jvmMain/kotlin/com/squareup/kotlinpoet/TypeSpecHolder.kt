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

/** A spec which can contain other [TypeSpec] */
public interface TypeSpecHolder {
  public val typeSpecs: List<TypeSpec>

  public interface Builder<out T : Builder<T>> {

    public fun addType(typeSpec: TypeSpec): T

    @Suppress("UNCHECKED_CAST")
    public fun addTypes(typeSpecs: Iterable<TypeSpec>): T =
      apply { for (typeSpec in typeSpecs) addType(typeSpec) } as T
  }
}
