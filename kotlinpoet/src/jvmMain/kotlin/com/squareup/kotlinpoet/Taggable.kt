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

import kotlin.reflect.KClass

/** A type that can be tagged with extra metadata of the user's choice. */
@JvmDefaultWithCompatibility
public interface Taggable {

  /** Returns all tags. */
  public val tags: Map<KClass<*>, Any>
    get() = emptyMap()

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  public fun <T : Any> tag(type: Class<T>): T? = tag(type.kotlin)

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  public fun <T : Any> tag(type: KClass<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return tags[type] as T?
  }

  /** The builder analogue to [Taggable] types. */
  @JvmDefaultWithCompatibility
  public interface Builder<out T : Builder<T>> {

    /** Mutable map of the current tags this builder contains. */
    public val tags: MutableMap<KClass<*>, Any>

    /**
     * Attaches [tag] to the request using [type] as a key. Tags can be read from a request using
     * [Taggable.tag]. Use `null` to remove any existing tag assigned for [type].
     *
     * Use this API to attach originating elements, debugging, or other application data to a spec
     * so that you may read it in other APIs or callbacks.
     */
    public fun tag(type: Class<*>, tag: Any?): T = tag(type.kotlin, tag)

    /**
     * Attaches [tag] to the request using [type] as a key. Tags can be read from a request using
     * [Taggable.tag]. Use `null` to remove any existing tag assigned for [type].
     *
     * Use this API to attach originating elements, debugging, or other application data to a spec
     * so that you may read it in other APIs or callbacks.
     */
    @Suppress("UNCHECKED_CAST")
    public fun tag(type: KClass<*>, tag: Any?): T =
      apply {
        if (tag == null) {
          this.tags.remove(type)
        } else {
          this.tags[type] = tag
        }
      }
        as T
  }
}

/** Returns the tag attached with [T] as a key, or null if no tag is attached with that key. */
public inline fun <reified T : Any> Taggable.tag(): T? = tag(T::class)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
 * [Taggable.tag]. Use `null` to remove any existing tag assigned for [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> AnnotationSpec.Builder.tag(tag: T?): AnnotationSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
 * [Taggable.tag]. Use `null` to remove any existing tag assigned for [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> FileSpec.Builder.tag(tag: T?): FileSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
 * [Taggable.tag]. Use `null` to remove any existing tag assigned for [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> FunSpec.Builder.tag(tag: T?): FunSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
 * [Taggable.tag]. Use `null` to remove any existing tag assigned for [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> ParameterSpec.Builder.tag(tag: T?): ParameterSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
 * [Taggable.tag]. Use `null` to remove any existing tag assigned for [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> PropertySpec.Builder.tag(tag: T?): PropertySpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
 * [Taggable.tag]. Use `null` to remove any existing tag assigned for [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> TypeAliasSpec.Builder.tag(tag: T?): TypeAliasSpec.Builder =
  tag(T::class, tag)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
 * [Taggable.tag]. Use `null` to remove any existing tag assigned for [T].
 *
 * Use this API to attach debugging or other application data to a spec so that you may read it in
 * other APIs or callbacks.
 */
public inline fun <reified T : Any> TypeSpec.Builder.tag(tag: T?): TypeSpec.Builder =
  tag(T::class, tag)

internal fun Taggable.Builder<*>.buildTagMap(): TagMap = TagMap(tags)

@JvmInline
internal value class TagMap private constructor(override val tags: Map<KClass<*>, Any>) : Taggable {
  companion object {
    operator fun invoke(tags: Map<KClass<*>, Any>): TagMap = TagMap(tags.toImmutableMap())
  }
}
