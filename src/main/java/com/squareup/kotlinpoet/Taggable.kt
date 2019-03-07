package com.squareup.kotlinpoet

import kotlin.reflect.KClass

/** A type that can be tagged with extra metadata of the user's choice. */
interface Taggable {

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  fun <T : Any> tag(type: Class<T>): T? = tag(type.kotlin)

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  fun <T : Any> tag(type: KClass<T>): T?

  /** The builder analogue to [Taggable] types. */
  interface Builder {

    /** Mutable map of the current tags this builder contains. */
    val tags: MutableMap<KClass<*>, Any>

    /**
     * Attaches [tag] to the request using [type] as a key. Tags can be read from a
     * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
     * [type].
     *
     * Use this API to attach originating elements, debugging, or other application data to a spec
     * so that you may read it in other APIs or callbacks.
     */
    fun tag(type: Class<*>, tag: Any?) = tag(type.kotlin, tag)

    /**
     * Attaches [tag] to the request using [type] as a key. Tags can be read from a
     * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
     * [type].
     *
     * Use this API to attach originating elements, debugging, or other application data to a spec
     * so that you may read it in other APIs or callbacks.
     */
    fun tag(type: KClass<*>, tag: Any?) = apply {
      if (tag == null) {
        this.tags.remove(type)
      } else {
        this.tags[type] = tag
      }
    }
  }
}

/** Returns the tag attached with [T] as a key, or null if no tag is attached with that key. */
inline fun <reified T : Any> Taggable.tag(): T? = tag(T::class)

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach originating elements, debugging, or other application data to a spec
 * so that you may read it in other APIs or callbacks.
 */
inline fun <reified T : Any> Taggable.Builder.tag(tag: T?) = tag(T::class, tag)

internal fun Taggable.Builder.buildTagMap(): TagMap = TagMap(LinkedHashMap(tags)) // Defensive copy

internal class TagMap(val tags: Map<KClass<*>, Any>) : Taggable {
  override fun <T : Any> tag(type: KClass<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return tags[type] as T?
  }
}
