package com.squareup.kotlinpoet

internal actual val String.isIdentifier: Boolean
  get() {
    val regExp = RegExp(IDENTIFIER_REGEX_VALUE, "gu")
    regExp.reset()

    val match = regExp.exec(this) ?: return false
    return match.index == 0 && regExp.lastIndex == length
  }

internal external interface RegExpMatch {
  val index: Int
  val length: Int
}

internal external class RegExp(pattern: String, flags: String? = definedExternally) : JsAny {
  fun exec(str: String): RegExpMatch?
  override fun toString(): String

  var lastIndex: Int
}

internal fun RegExp.reset() {
  lastIndex = 0
}
