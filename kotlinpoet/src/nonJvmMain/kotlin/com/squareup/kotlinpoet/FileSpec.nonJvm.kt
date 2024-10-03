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

import com.squareup.kotlinpoet.jvm.alias.JvmFiler
import com.squareup.kotlinpoet.jvm.alias.JvmIOException
import com.squareup.kotlinpoet.jvm.alias.JvmJavaFileObject
import com.squareup.kotlinpoet.jvm.alias.JvmPath

@JvmTypeAliasKotlinPoetApi
@Throws(JvmIOException::class)
internal actual fun FileSpec.writeToPath(directory: JvmPath): JvmPath =
  throw UnsupportedOperationException()

@JvmTypeAliasKotlinPoetApi
@Throws(JvmIOException::class)
internal actual fun FileSpec.writeToFiler(filer: JvmFiler, extension: String) {
  throw UnsupportedOperationException()
}

@JvmTypeAliasKotlinPoetApi
internal actual inline fun FileSpec.toJavaFileObjectInternal(
  crossinline toString: () -> String,
): JvmJavaFileObject {
  throw UnsupportedOperationException()
}
