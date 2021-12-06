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
package com.squareup.kotlinpoet.javapoet

/** Various JavaPoet and KotlinPoet representations of some common types. */
@OptIn(KotlinPoetJavaPoetPreview::class)
internal object PoetInterop {
  internal val CN_JAVA_CHAR_SEQUENCE = JClassName.get("java.lang", "CharSequence")
  internal val CN_JAVA_STRING = JClassName.get("java.lang", "String")
  internal val CN_JAVA_LIST = JClassName.get("java.util", "List")
  internal val CN_JAVA_SET = JClassName.get("java.util", "Set")
  internal val CN_JAVA_MAP = JClassName.get("java.util", "Map")
  internal val CN_JAVA_ENUM = JClassName.get("java.lang", "Enum")
}
