package com.squareup.kotlinpoet

import javax.lang.model.element.Element

/** A type that can have originating [elements][Element]. */
interface OriginatingElementsHolder {

  /** The originating elements of this type. */
  val originatingElements: List<Element>

  /** The builder analogue to [OriginatingElementsHolder] types. */
  interface Builder<out T : Builder<T>> {

    /** Mutable map of the current originating elements this builder contains. */
    val originatingElements: MutableList<Element>

    /** Adds an [originatingElement] to this type's list of originating elements. */
    @Suppress("UNCHECKED_CAST")
    fun addOriginatingElement(originatingElement: Element): T = apply {
      originatingElements += originatingElement
    } as T
  }
}

internal fun OriginatingElementsHolder.Builder<*>.buildOriginatingElements() = OriginatingElements(originatingElements.toImmutableList())

internal fun List<Element>.buildOriginatingElements() = OriginatingElements(toImmutableList())

internal class OriginatingElements(override val originatingElements: List<Element>) : OriginatingElementsHolder
