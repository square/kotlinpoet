package com.squareup.kotlinpoet

/**
 * A [CodeLiteral] can be used as an argument for a `%L` template in code to let the value itself
 * convert to source code.
 *
 * Example converting a `Float` to a `CodeLiteral`
 * ```
 * private val Float.literal: CodeLiteral
 *   get() {
 *     val v = this
 *     return object : CodeLiteral {
 *       override fun toCodeBlock() = CodeBlock.of("%Lf", v)
 *     }
 *   }
 * ```
 */
interface CodeLiteral {
  /** @return a [CodeBlock] representation of this value. */
  fun toCodeBlock(): CodeBlock
}
