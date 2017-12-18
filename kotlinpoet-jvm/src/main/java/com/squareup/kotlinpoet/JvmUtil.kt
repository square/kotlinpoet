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
@file:JvmName("Utils")
@file:JvmMultifileClass

package com.squareup.kotlinpoet

import java.util.Collections

actual internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> =
    Collections.unmodifiableMap(LinkedHashMap(this))

actual internal fun <T> Collection<T>.toImmutableList(): List<T> =
    Collections.unmodifiableList(ArrayList(this))

actual internal fun <T> Collection<T>.toImmutableSet(): Set<T> =
    Collections.unmodifiableSet(LinkedHashSet(this))

actual internal fun String.format(vararg args: Any?) = String.format(this, *args)
