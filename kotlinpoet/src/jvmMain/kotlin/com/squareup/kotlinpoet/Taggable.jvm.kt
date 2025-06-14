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
package com.squareup.kotlinpoet

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */

public inline fun <reified T : Any> AnnotationSpec.Builder.tag(tag: T?): AnnotationSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> FileSpec.Builder.tag(tag: T?): FileSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> FunSpec.Builder.tag(tag: T?): FunSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> ParameterSpec.Builder.tag(tag: T?): ParameterSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> PropertySpec.Builder.tag(tag: T?): PropertySpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> TypeAliasSpec.Builder.tag(tag: T?): TypeAliasSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> TypeSpec.Builder.tag(tag: T?): TypeSpec.Builder =
  tag(T::class, tag)
