package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.TypeName

/**
 * This tag indicates that this [TypeName] represents a `typealias` type.
 *
 * @property [type] the underlying type for this alias.
 */
public class TypeNameAliasTag(public val type: TypeName)
