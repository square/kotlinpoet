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
@file:JvmName("FacadeFile")
@file:FileAnnotation("file annotations!")
@file:Suppress("unused", "UNUSED_PARAMETER")

package com.squareup.kotlinpoet.metadata.specs

import kotlin.annotation.AnnotationTarget.FILE

@Target(FILE) annotation class FileAnnotation(val value: String)

@JvmName("jvmStaticFunction") fun jvmNameFunction() {}

fun regularFun() {}

@Synchronized fun synchronizedFun() {}

@JvmOverloads
fun jvmOverloads(param1: String, optionalParam2: String = "", nullableParam3: String? = null) {}

val BOOL_PROP = false
val BINARY_PROP = 0b00001011
val INT_PROP = 1
val UNDERSCORES_PROP = 1_000_000
val HEX_PROP = 0x0F
val UNDERSCORES_HEX_PROP = 0xFF_EC_DE_5E
val LONG_PROP = 1L
val FLOAT_PROP = 1.0f
val DOUBLE_PROP = 1.0
val STRING_PROP = "prop"
var VAR_BOOL_PROP = false
var VAR_BINARY_PROP = 0b00001011
var VAR_INT_PROP = 1
var VAR_UNDERSCORES_PROP = 1_000_000
var VAR_HEX_PROP = 0x0F
var VAR_UNDERSCORES_HEX_PROP = 0xFF_EC_DE_5E
var VAR_LONG_PROP = 1L
var VAR_FLOAT_PROP = 1.0f
var VAR_DOUBLE_PROP = 1.0
var VAR_STRING_PROP = "prop"

const val CONST_BOOL_PROP = false
const val CONST_BINARY_PROP = 0b00001011
const val CONST_INT_PROP = 1
const val CONST_UNDERSCORES_PROP = 1_000_000
const val CONST_HEX_PROP = 0x0F
const val CONST_UNDERSCORES_HEX_PROP = 0xFF_EC_DE_5E
const val CONST_LONG_PROP = 1L
const val CONST_FLOAT_PROP = 1.0f
const val CONST_DOUBLE_PROP = 1.0
const val CONST_STRING_PROP = "prop"

@JvmField @JvmSynthetic val syntheticFieldProperty: kotlin.String? = null

@field:JvmSynthetic val syntheticProperty: kotlin.String? = null

@get:JvmSynthetic val syntheticPropertyGet: kotlin.String? = null

@get:JvmSynthetic @set:JvmSynthetic var syntheticPropertyGetAndSet: kotlin.String? = null

@set:JvmSynthetic var syntheticPropertySet: kotlin.String? = null

typealias FacadeTypeAliasName = String

typealias FacadeGenericTypeAlias = List<String>

typealias FacadeNestedTypeAlias = List<GenericTypeAlias>
