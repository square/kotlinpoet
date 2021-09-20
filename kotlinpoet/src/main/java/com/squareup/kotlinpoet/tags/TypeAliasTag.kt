package com.squareup.kotlinpoet.tags

import com.squareup.kotlinpoet.TypeName

/**
 * This tag indicates that this [TypeName] represents a `typealias` type.
 *
 * @property [abbreviatedType] the underlying type for this alias.
 */
public class TypeAliasTag(public val abbreviatedType: TypeName)
