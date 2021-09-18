/*
 * Copyright (C) 2019 Square, Inc.
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

@file:Suppress("unused")
@file:JvmName("KmTypes")

package com.squareup.kotlinpoet.metadata

import kotlinx.metadata.KmType
import kotlinx.metadata.jvm.annotations

/**
 * `true` if this is an extension type (i.e. String.() -> Unit vs (String) -> Unit).
 *
 * See details: https://discuss.kotlinlang.org/t/announcing-kotlinx-metadata-jvm-library-for-reading-modifying-metadata-of-kotlin-jvm-class-files/7980/27
 */
public val KmType.isExtensionType: Boolean get() {
  return annotations.any { it.className == "kotlin/ExtensionFunctionType" }
}
