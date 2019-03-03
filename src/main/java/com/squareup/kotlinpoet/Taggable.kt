package com.squareup.kotlinpoet

import kotlin.reflect.KClass

/**
 * A type that can be tagged with extra metadata of the user's choice.
 */
interface Taggable {

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  fun <T : Any> tag(type: Class<out T>): T?

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  fun <T : Any> tag(type: KClass<out T>): T?

}

/** Returns the tag attached with [T] as a key, or null if no tag is attached with that key. */
inline fun <reified T : Any> Taggable.tag(): T? = tag(T::class)

internal class TagMap(val tags: Map<KClass<*>, Any>) : Taggable {
  override fun <T : Any> tag(type: Class<out T>): T? = tag(type.kotlin)

  override fun <T : Any> tag(type: KClass<out T>): T? {
    @Suppress("UNCHECKED_CAST")
    return tags[type] as? T
  }
}
