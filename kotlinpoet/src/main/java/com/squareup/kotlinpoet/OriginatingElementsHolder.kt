package com.squareup.kotlinpoet

import javax.lang.model.element.Element

/** A type that can have originating [elements][Element]. */
public interface OriginatingElementsHolder {

  /** The originating elements of this type. */
  public val originatingElements: List<Element>

  /** The builder analogue to [OriginatingElementsHolder] types. */
  public interface Builder<out T : Builder<T>> {

    /** Mutable map of the current originating elements this builder contains. */
    public val originatingElements: MutableList<Element>

    /** Adds an [originatingElement] to this type's list of originating elements. */
    @Suppress("UNCHECKED_CAST")
    public fun addOriginatingElement(originatingElement: Element): T = apply {
      originatingElements += originatingElement
    } as T
  }
}

internal fun OriginatingElementsHolder.Builder<*>.buildOriginatingElements() =
  OriginatingElements(originatingElements.toImmutableList())

internal fun List<Element>.buildOriginatingElements() =
  OriginatingElements(toImmutableList())

internal class OriginatingElements(
  override val originatingElements: List<Element>
) : OriginatingElementsHolder
