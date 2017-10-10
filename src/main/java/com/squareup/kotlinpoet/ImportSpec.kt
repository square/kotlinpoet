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

data class ImportSpec internal constructor(
    val className: String,
    val memberName: String? = null,
    val rename: String? = null
) : Comparable<ImportSpec> {

  private val import = buildString {
    append(className)
    if (memberName != null) {
      append(".$memberName")
    }
    if (rename != null) {
      append(" as $rename")
    }
  }

  /**
   * Turn this [ImportSpec] into a renamed import of the following form:
   *
   * ```kotlin
   * className.memberName as name
   * ```
   *
   * @param name Name of the import, that will be used in the `as` clause
   */
  fun renameTo(name: String) = ImportSpec(className, memberName, name)

  override fun toString() = import

  override fun compareTo(other: ImportSpec) = import.compareTo(other.import)

  companion object {

    fun forType(typeName: String) = ImportSpec(typeName)

    fun forType(typeName: ClassName) = forType(typeName.canonicalName)

    fun forType(packageName: String, typeName: String) =
        forType("$packageName.$typeName")

    fun static(className: String, memberName: String) =
        ImportSpec(className, memberName)

    fun static(className: ClassName, memberName: String) =
        static(className.canonicalName, memberName)

    fun wildcard(className: String) = ImportSpec(className, "*")
  }
}
