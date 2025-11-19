/*
 * Copyright (C) 2017 Square, Inc.
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

@ExposedCopyVisibility
public data class Import
internal constructor(val qualifiedName: String, val alias: String? = null) : Comparable<Import> {

  private val importString = buildString {
    append(qualifiedName.escapeSegmentsIfNecessary())
    if (alias != null) {
      append(" as ${alias.escapeIfNecessary()}")
    }
  }

  override fun toString(): String = importString

  override fun compareTo(other: Import): Int = importString.compareTo(other.importString)
}
