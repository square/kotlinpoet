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

internal data class Import(
  internal val qualifiedName: String,
  internal val alias: String? = null
) : Comparable<Import> {

  private val importString = buildString {
    append(qualifiedName)
    if (alias != null) {
      append(" as $alias")
    }
  }

  override fun toString() = importString

  override fun compareTo(other: Import) = importString.compareTo(other.importString)
}
