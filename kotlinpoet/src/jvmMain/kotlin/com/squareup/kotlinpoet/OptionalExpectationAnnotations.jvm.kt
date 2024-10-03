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
@file:Suppress("unused")

package com.squareup.kotlinpoet

import kotlin.reflect.KClass

internal actual val JvmNameClass: KClass<out Annotation>?
  get() = JvmName::class
internal actual val JvmMultifileClassClass: KClass<out Annotation>?
  get() = JvmMultifileClass::class
internal actual val JvmSuppressWildcardsClass: KClass<out Annotation>?
  get() = JvmSuppressWildcards::class
internal actual val JvmInlineClass: KClass<out Annotation>?
  get() = JvmInline::class
internal actual val JvmRecordClass: KClass<out Annotation>?
  get() = JvmRecord::class
internal actual val JvmStaticClass: KClass<out Annotation>?
  get() = JvmStatic::class
internal actual val JvmOverloadsClass: KClass<out Annotation>?
  get() = JvmOverloads::class
internal actual val ThrowsClass: KClass<out Annotation>?
  get() = Throws::class
internal actual val SynchronizedClass: KClass<out Annotation>?
  get() = Synchronized::class
internal actual val StrictfpClass: KClass<out Annotation>?
  get() = Strictfp::class
internal actual val JvmFieldClass: KClass<out Annotation>?
  get() = JvmField::class
internal actual val TransientClass: KClass<out Annotation>?
  get() = Transient::class
internal actual val VolatileClass: KClass<out Annotation>?
  get() = kotlin.concurrent.Volatile::class
internal actual val JvmWildcardClass: KClass<out Annotation>?
  get() = JvmWildcard::class
