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
@file:JvmName("OptionalExpectationAnnotations")
@file:JvmMultifileClass

package com.squareup.kotlinpoet

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/** [KClass] for [kotlin.jvm.JvmName] */
internal expect val JvmNameClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.JvmMultifileClass] */
internal expect val JvmMultifileClassClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.JvmSuppressWildcards] */
internal expect val JvmSuppressWildcardsClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.JvmInline] */
internal expect val JvmInlineClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.JvmRecord] */
internal expect val JvmRecordClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.JvmStatic] */
internal expect val JvmStaticClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.JvmOverloads] */
internal expect val JvmOverloadsClass: KClass<out Annotation>?

/** [KClass] for [kotlin.Throws] */
internal expect val ThrowsClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.Synchronized] */
internal expect val SynchronizedClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.Strictfp] */
internal expect val StrictfpClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.JvmField] */
internal expect val JvmFieldClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.Transient] */
internal expect val TransientClass: KClass<out Annotation>?

/** [KClass] for [kotlin.concurrent.Volatile] */
internal expect val VolatileClass: KClass<out Annotation>?

/** [KClass] for [kotlin.jvm.JvmWildcard] */
internal expect val JvmWildcardClass: KClass<out Annotation>?

/** [ClassName] for [kotlin.jvm.JvmName] */
internal val JvmNameClassName: ClassName = ClassName("kotlin.jvm", "JvmName")

/** [ClassName] for [kotlin.jvm.JvmMultifileClass] */
internal val JvmMultifileClassClassName: ClassName = ClassName("kotlin.jvm", "JvmMultifileClass")

/** [ClassName] for [kotlin.jvm.JvmSuppressWildcards] */
internal val JvmSuppressWildcardsClassName: ClassName = ClassName("kotlin.jvm", "JvmSuppressWildcards")

/** [ClassName] for [kotlin.jvm.JvmInline] */
internal val JvmInlineClassName: ClassName = ClassName("kotlin.jvm", "JvmInline")

/** [ClassName] for [kotlin.jvm.JvmRecord] */
internal val JvmRecordClassName: ClassName = ClassName("kotlin.jvm", "JvmRecord")

/** [ClassName] for [kotlin.jvm.JvmStatic] */
internal val JvmStaticClassName: ClassName = ClassName("kotlin.jvm", "JvmStatic")

/** [ClassName] for [kotlin.jvm.JvmOverloads] */
internal val JvmOverloadsClassName: ClassName = ClassName("kotlin.jvm", "JvmOverloads")

/** [ClassName] for [kotlin.Throws] */
internal val ThrowsClassName: ClassName = ClassName("kotlin", "Throws")

/** [ClassName] for [kotlin.jvm.Throws] */
internal val JvmThrowsClassName: ClassName = ClassName("kotlin.jvm", "Throws")

/** [ClassName] for [kotlin.jvm.Synchronized] */
internal val SynchronizedClassName: ClassName = ClassName("kotlin.jvm", "Synchronized")

/** [ClassName] for [kotlin.jvm.Strictfp] */
internal val StrictfpClassName: ClassName = ClassName("kotlin.jvm", "Strictfp")

/** [ClassName] for [kotlin.jvm.JvmField] */
internal val JvmFieldClassName: ClassName = ClassName("kotlin.jvm", "JvmField")

/** [ClassName] for [kotlin.jvm.Transient] */
internal val TransientClassName: ClassName = ClassName("kotlin.jvm", "Transient")

/** [ClassName] for [kotlin.concurrent.Volatile] */
internal val VolatileClassName: ClassName = ClassName("kotlin.concurrent", "Volatile")

/** [ClassName] for [kotlin.jvm.Volatile] */
internal val JvmVolatileClassName: ClassName = ClassName("kotlin.jvm", "Volatile")

/** [ClassName] for [kotlin.jvm.JvmWildcard] */
internal val JvmWildcardClassName: ClassName = ClassName("kotlin.jvm", "JvmWildcard")
