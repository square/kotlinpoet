package com.squareup.kotlinpoet

import kotlin.reflect.KClass

/** A type that can be tagged with extra metadata of the user's choice. */
interface Taggable {

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  fun <ValueType : Any> tag(type: Class<*>): ValueType? = tag(type.kotlin)

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  fun <ValueType : Any> tag(type: KClass<*>): ValueType?

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

/** Returns the tag attached with [KeyType] as a key, or null if no tag is attached with that key. */
inline fun <reified KeyType : Any, reified ValueType : Any> Taggable.tag(): ValueType? = tag(KeyType::class)

/**
 * Attaches [tag] to the request using [KeyType] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [KeyType].
 *
 * Use this API to attach originating elements, debugging, or other application data to a spec
 * so that you may read it in other APIs or callbacks.
 */
inline fun <reified KeyType : Any> Taggable.Builder.tag(tag: Any?) = tag(KeyType::class, tag)

internal fun Taggable.Builder.buildTagMap(): TagMap = TagMap(LinkedHashMap(tags)) // Defensive copy

internal class TagMap(val tags: Map<KClass<*>, Any>) : Taggable {
  override fun <ValueType : Any> tag(keyType: KClass<*>): ValueType? {
    @Suppress("UNCHECKED_CAST")
    return tags[keyType] as ValueType?
  }
}
