package com.squareup.kotlinpoet

private val IDENTIFIER_REGEX = IDENTIFIER_REGEX_VALUE.toRegex()

internal actual val String.isIdentifier: Boolean get() = IDENTIFIER_REGEX.matches(this)
