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
package com.squareup.kotlinpoet

/**
 * Marks declarations in the KotlinPoet API that are **delicate** &mdash; they have limited use-case
 * and shall be used with care in general code. Any use of a delicate declaration has to be
 * carefully reviewed to make sure it is properly used and does not create problems like lossy Java
 * -> Kotlin type parsing. Carefully read documentation and [message] of any declaration marked as
 * `DelicateKotlinPoetApi`.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
  level = RequiresOptIn.Level.WARNING,
  message =
    "This is a delicate API and its use requires care." +
      " Make sure you fully read and understand documentation of the declaration that is marked as a delicate API.",
)
public annotation class DelicateKotlinPoetApi(val message: String)
