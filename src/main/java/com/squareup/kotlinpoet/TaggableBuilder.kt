package com.squareup.kotlinpoet

import kotlin.reflect.KClass

/**
 * The builder analogue to [Taggable] types.
 */
interface TaggableBuilder {

  /**
   * Mutable map of the current tags this builder contains.
   */
  val tags: MutableMap<KClass<*>, Any>

  /**
   * Attaches [tag] to the request using [type] as a key. Tags can be read from a
   * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
   * [type].
   *
   * Use this API to attach originating elements, debugging, or other application data to a spec
   * so that you may read it in other APIs or callbacks.
   */
  fun <T : Any> tag(type: Class<out T>, tag: T?) = tag(type.kotlin, tag)

  /**
   * Attaches [tag] to the request using [type] as a key. Tags can be read from a
   * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
   * [type].
   *
   * Use this API to attach originating elements, debugging, or other application data to a spec
   * so that you may read it in other APIs or callbacks.
   */
  fun <T : Any> tag(type: KClass<out T>, tag: T?) = apply {
    if (tag == null) {
      this.tags.remove(type)
    } else {
      this.tags[type] = tag
    }
  }

}

/**
 * Attaches [tag] to the request using [T] as a key. Tags can be read from a
 * request using [Taggable.tag]. Use `null` to remove any existing tag assigned for
 * [T].
 *
 * Use this API to attach originating elements, debugging, or other application data to a spec
 * so that you may read it in other APIs or callbacks.
 */
inline fun <reified T : Any> TaggableBuilder.tag(tag: T?) = tag(T::class, tag)