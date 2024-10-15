package com.squareup.kotlinpoet

internal actual fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> =
  toMap()

internal actual fun <T> Collection<T>.toImmutableList(): List<T> =
  toList()

internal actual fun <T> Collection<T>.toImmutableSet(): Set<T> =
  toSet()

internal actual fun formatIsoControlCode(code: Int): String {
  return buildString(6) {
    append("\\u")
    appendFormat04x(code)
  }
}

@OptIn(ExperimentalStdlibApi::class)
private val HexFormatWithoutLeadingZeros = HexFormat {
  number {
    removeLeadingZeros = true
  }
}

@OptIn(ExperimentalStdlibApi::class)
internal fun Appendable.appendFormat04x(code: Int) {
  val hex = code.toHexString(HexFormatWithoutLeadingZeros)
  if (hex.length < 4) {
    repeat(4 - hex.length) { append('0') }
  }
  append(hex)
}

@OptIn(ExperimentalStdlibApi::class)
internal actual fun Int.toHexStr(): String =
  toHexString(HexFormatWithoutLeadingZeros)

internal actual inline fun <reified E : Enum<E>> enumSetOf(vararg values: E): MutableSet<E> =
  values.toMutableSet()

internal actual fun Char.isJavaIdentifierStart(): Boolean {
  return isLetter() ||
    this in CharCategory.LETTER_NUMBER ||
    this == '$' ||
    this == '_'
}


internal actual fun Char.isJavaIdentifierPart(): Boolean {
  //  TODO
  //   A character may be part of a Java identifier if any of the following conditions are true:
  //   - it is a letter
  //   - it is a currency symbol (such as '$')
  //   - it is a connecting punctuation character (such as '_')
  //   - it is a digit
  //   - it is a numeric letter (such as a Roman numeral character)
  //   - it is a combining mark
  //   - it is a non-spacing mark
  //   isIdentifierIgnorable returns true for the character.
  //   Also missing here:
  //   - a combining mark
  return isLetter() ||
    isDigit() ||
    this in CharCategory.LETTER_NUMBER ||
    this in CharCategory.NON_SPACING_MARK ||
    this == '_' ||
    this == '$' ||
    isIdentifierIgnorable()
  //
}

internal fun Char.isIdentifierIgnorable(): Boolean {
   // The following Unicode characters are ignorable in a Java identifier or a Unicode identifier:
   // - ISO control characters that are not whitespace
   //   - '\u0000' through '\u0008'
   //   - '\u000E' through '\u001B'
   //   - '\u007F' through '\u009F'
   // - all characters that have the FORMAT general category value
  return (
    isISOControl() && (
        this in '\u0000'..'\u0008' ||
        this in '\u000E'..'\u001B' ||
        this in '\u007F'..'\u009F'
      )
    ) || this in CharCategory.FORMAT
}
