/*
 * Copyright (C) 2015 Square, Inc.
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
@file:JvmName("WildcardTypeNames")

package com.squareup.kotlinpoet

import java.lang.reflect.WildcardType
import kotlin.DeprecationLevel.WARNING

@Deprecated(
    message = "Mirror APIs don't give complete information on Kotlin types. Consider using" +
        " the kotlinpoet-metadata APIs instead.",
    level = WARNING
)
@JvmName("get")
public fun javax.lang.model.type.WildcardType.asWildcardTypeName(): TypeName =
    WildcardTypeName.get(this, mutableMapOf())

@JvmName("get")
public fun WildcardType.asWildcardTypeName(): TypeName =
    WildcardTypeName.get(this, mutableMapOf())
