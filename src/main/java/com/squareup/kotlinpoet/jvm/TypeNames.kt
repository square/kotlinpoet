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
@file:JvmName("TypeNamesJvm")

package com.squareup.kotlinpoet.jvm

import com.squareup.kotlinpoet.TypeName
import java.lang.reflect.Type
import javax.lang.model.type.TypeMirror

/** Returns a [TypeName] equivalent to this [TypeMirror]. */
@JvmName("get")
fun TypeMirror.asTypeName() = TypeName.get(this, mutableMapOf())

/** Returns a [TypeName] equivalent to this [Type].  */
@JvmName("get")
fun Type.asTypeName() = TypeName.get(this, mutableMapOf())
