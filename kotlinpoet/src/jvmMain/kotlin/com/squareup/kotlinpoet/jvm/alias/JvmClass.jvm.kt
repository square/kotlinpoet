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
@file:JvmName("JvmClasses")
@file:JvmMultifileClass

package com.squareup.kotlinpoet.jvm.alias

import java.lang.reflect.Type
import kotlin.jvm.kotlin as kJvmKotlin
import kotlin.reflect.KClass

/**
 * Type alias for [Type].
 */
public actual typealias JvmType = Type

public actual fun JvmType.typeName(): String = typeName

/**
 * Type alias for [Class].
 */
public actual typealias JvmClass<T> = Class<T>

public actual val <T : Any> JvmClass<T>.kotlin: KClass<T>
  get() = kJvmKotlin
