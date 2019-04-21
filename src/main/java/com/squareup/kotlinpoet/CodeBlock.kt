/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("CodeBlocks")

package com.squareup.kotlinpoet

import java.lang.reflect.Type
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * A fragment of a .kt file, potentially containing declarations, statements, and documentation.
 * Code blocks are not necessarily well-formed Kotlin code, and are not validated. This class
 * assumes kotlinc will check correctness later!
 *
 * Code blocks support placeholders like [java.text.Format]. This class primarily uses a percent
 * sign `%` but has its own set of permitted placeholders:
 *
 *  * `%L` emits a *literal* value with no escaping. Arguments for literals may be strings,
 *    primitives, [type declarations][TypeSpec], [annotations][AnnotationSpec] and even other code
 *    blocks.
 *  * `%N` emits a *name*, using name collision avoidance where necessary. Arguments for names may
 *    be strings (actually any [character sequence][CharSequence]), [parameters][ParameterSpec],
 *    [properties][PropertySpec], [functions][FunSpec], and [types][TypeSpec].
 *  * `%S` escapes the value as a *string*, wraps it with double quotes, and emits that. For
 *    example, `6" sandwich` is emitted `"6\" sandwich"`. `%S` will also escape all dollar signs
 *    (`$`), use `%P` for string templates.
 *  * `%P` - Similar to `%S`, but doesn't escape dollar signs (`$`) to allow creation of string
 *    templates. If the string contains dollar signs that should be escaped - use `%S`.
 *  * `%T` emits a *type* reference. Types will be imported if possible. Arguments for types may be
 *    [classes][Class], [type mirrors][javax.lang.model.type.TypeMirror], and
 *    [elements][javax.lang.model.element.Element].
 *  * `%M` emits a *member* reference. A member is either a function or a property that can be
 *    accessed in a static context, e.g. a top-level function or a property inside an object.
 *    Members will be imported if possible. Arguments for members must be of type [MemberName].
 *  * `%%` emits a percent sign.
 *  * `·` emits a space that never wraps. KotlinPoet prefers to wrap lines longer than 100 columns.
 *    It does this by replacing normal spaces with a newline and indent. Note that spaces in strings
 *    are never wrapped.
 *  * `⇥` increases the indentation level.
 *  * `⇤` decreases the indentation level.
 *  * `«` begins a statement. For multiline statements, every line after the first line is
 *    double-indented.
 *  * `»` ends a statement.
 */
class CodeBlock private constructor(
  internal val formatParts: List<String>,
  internal val args: List<Any?>
) {
  /** A heterogeneous list containing string literals and value placeholders.  */

  fun isEmpty() = formatParts.isEmpty()

  fun isNotEmpty() = !isEmpty()

  /**
   * Returns a code block with `prefix` stripped off, or null if this code block doesn't start with
   * `prefix`.
   *
   * This is a pretty basic implementation that might not cover cases like mismatched whitespace. We
   * could offer something more lenient if necessary.
   */
  internal fun withoutPrefix(prefix: CodeBlock): CodeBlock? {
    if (formatParts.size < prefix.formatParts.size) return null
    if (args.size < prefix.args.size) return null

    var prefixArgCount = 0
    var firstFormatPart: String? = null

    // Walk through the formatParts of prefix to confirm that it's a of this.
    prefix.formatParts.forEachIndexed { index, formatPart ->
      if (formatParts[index] != formatPart) {
        // We've found a format part that doesn't match. If this is the very last format part check
        // for a string prefix match. If that doesn't match, we're done.
        if (index == prefix.formatParts.size - 1 && formatParts[index].startsWith(formatPart)) {
          firstFormatPart = formatParts[index].substring(formatPart.length)
        } else {
          return null
        }
      }

      // If the matching format part has an argument, check that too.
      if (formatPart.startsWith("%") && !isMultiCharNoArgPlaceholder(formatPart[1])) {
        if (args[prefixArgCount] != prefix.args[prefixArgCount]) {
          return null // Argument doesn't match.
        }
        prefixArgCount++
      }
    }

    // We found a prefix. Prepare the suffix as a result.
    val resultFormatParts = ArrayList<String>()
    firstFormatPart?.let {
      resultFormatParts.add(it)
    }
    for (i in prefix.formatParts.size until formatParts.size) {
      resultFormatParts.add(formatParts[i])
    }

    val resultArgs = ArrayList<Any?>()
    for (i in prefix.args.size until args.size) {
      resultArgs.add(args[i])
    }

    return CodeBlock(resultFormatParts, resultArgs)
  }

  /**
   * Returns a copy of the code block without leading and trailing no-arg placeholders
   * (`⇥`, `⇤`, `«`, `»`).
   */
  internal fun trim(): CodeBlock {
    var start = 0
    var end = formatParts.size
    while (start < end && formatParts[start] in NO_ARG_PLACEHOLDERS) {
      start++
    }
    while (start < end && formatParts[end - 1] in NO_ARG_PLACEHOLDERS) {
      end--
    }
    return when {
      start > 0 || end < formatParts.size -> CodeBlock(formatParts.subList(start, end), args)
      else -> this
    }
  }

  /**
   * Returns a copy of the code block with selected format parts replaced, similar to
   * [java.lang.String.replaceAll].
   *
   * **Warning!** This method leaves the arguments list unchanged. Take care when replacing
   * placeholders with arguments, such as `%L`, as it can result in a code block, where
   * placeholders don't match their arguments.
   */
  internal fun replaceAll(oldValue: String, newValue: String) =
      CodeBlock(formatParts.map { it.replace(oldValue, newValue) }, args)

  internal fun hasStatements() = formatParts.any { "«" in it }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String = buildCodeString { emitCode(this@CodeBlock) }

  internal fun toString(codeWriter: CodeWriter): String = buildCodeString(codeWriter) {
    emitCode(this@CodeBlock)
  }

  fun toBuilder(): Builder {
    val builder = Builder()
    builder.formatParts += formatParts
    builder.args.addAll(args)
    return builder
  }

  class Builder {
    internal val formatParts = mutableListOf<String>()
    internal val args = mutableListOf<Any?>()

    fun isEmpty() = formatParts.isEmpty()

    fun isNotEmpty() = !isEmpty()

    /**
     * Adds code using named arguments.
     *
     * Named arguments specify their name after the '%' followed by : and the corresponding type
     * character. Argument names consist of characters in `a-z, A-Z, 0-9, and _` and must start
     * with a lowercase character.
     *
     * For example, to refer to the type [java.lang.Integer] with the argument name `clazz` use a
     * format string containing `%clazz:T` and include the key `clazz` with value
     * `java.lang.Integer.class` in the argument map.
     */
    fun addNamed(format: String, arguments: Map<String, *>) = apply {
      var p = 0

      for (argument in arguments.keys) {
        require(LOWERCASE matches argument) {
          "argument '$argument' must start with a lowercase character"
        }
      }

      while (p < format.length) {
        val nextP = nextPotentialPlaceholderPosition(format, p)
        if (nextP == -1) {
          formatParts += format.substring(p, format.length)
          break
        }

        if (p != nextP) {
          formatParts += format.substring(p, nextP)
          p = nextP
        }

        var matchResult: MatchResult? = null
        val colon = format.indexOf(':', p)
        if (colon != -1) {
          val endIndex = Math.min(colon + 2, format.length)
          matchResult = NAMED_ARGUMENT.matchEntire(format.substring(p, endIndex))
        }
        if (matchResult != null) {
          val argumentName = matchResult.groupValues[ARG_NAME]
          require(arguments.containsKey(argumentName)) {
            "Missing named argument for %$argumentName"
          }
          val formatChar = matchResult.groupValues[TYPE_NAME].first()
          addArgument(format, formatChar, arguments[argumentName])
          formatParts += "%$formatChar"
          p += matchResult.range.endInclusive + 1
        } else if (isSingleCharNoArgPlaceholder(format[p])) {
          formatParts += format.substring(p, p + 1)
          p++
        } else {
          require(p < format.length - 1) { "dangling % at end" }
          require(isMultiCharNoArgPlaceholder(format[p + 1])) {
            "unknown format %${format[p + 1]} at ${p + 1} in '$format'"
          }
          formatParts += format.substring(p, p + 2)
          p += 2
        }
      }
    }

    /**
     * Add code with positional or relative arguments.
     *
     * Relative arguments map 1:1 with the placeholders in the format string.
     *
     * Positional arguments use an index after the placeholder to identify which argument index
     * to use. For example, for a literal to reference the 3rd argument: "%3L" (1 based index)
     *
     * Mixing relative and positional arguments in a call to add is invalid and will result in an
     * error.
     */
    fun add(format: String, vararg args: Any?) = apply {
      var hasRelative = false
      var hasIndexed = false

      var relativeParameterCount = 0
      val indexedParameterCount = IntArray(args.size)

      var p = 0
      while (p < format.length) {
        if (isSingleCharNoArgPlaceholder(format[p])) {
          formatParts += format[p].toString()
          p++
          continue
        }

        if (format[p] != '%') {
          var nextP = nextPotentialPlaceholderPosition(format, p + 1)
          if (nextP == -1) nextP = format.length
          formatParts += format.substring(p, nextP)
          p = nextP
          continue
        }

        p++ // '%'.

        // Consume zero or more digits, leaving 'c' as the first non-digit char after the '%'.
        val indexStart = p
        var c: Char
        do {
          require(p < format.length) { "dangling format characters in '$format'" }
          c = format[p++]
        } while (c in '0'..'9')
        val indexEnd = p - 1

        // If 'c' doesn't take an argument, we're done.
        if (isMultiCharNoArgPlaceholder(c)) {
          require(indexStart == indexEnd) { "%% may not have an index" }
          formatParts += "%$c"
          continue
        }

        // Find either the indexed argument, or the relative argument. (0-based).
        val index: Int
        if (indexStart < indexEnd) {
          index = Integer.parseInt(format.substring(indexStart, indexEnd)) - 1
          hasIndexed = true
          if (args.isNotEmpty()) {
            indexedParameterCount[index % args.size]++ // modulo is needed, checked below anyway
          }
        } else {
          index = relativeParameterCount
          hasRelative = true
          relativeParameterCount++
        }

        require(index >= 0 && index < args.size) {
          "index ${index + 1} for '${format.substring(indexStart - 1,
              indexEnd + 1)}' not in range (received ${args.size} arguments)"
        }
        require(!hasIndexed || !hasRelative) { "cannot mix indexed and positional parameters" }

        addArgument(format, c, args[index])

        formatParts += "%$c"
      }

      if (hasRelative) {
        require(relativeParameterCount >= args.size) {
          "unused arguments: expected $relativeParameterCount, received ${args.size}"
        }
      }
      if (hasIndexed) {
        val unused = mutableListOf<String>()
        for (i in args.indices) {
          if (indexedParameterCount[i] == 0) {
            unused += "%" + (i + 1)
          }
        }
        val s = if (unused.size == 1) "" else "s"
        require(unused.isEmpty()) { "unused argument$s: ${unused.joinToString(", ")}" }
      }
    }

    private fun addArgument(format: String, c: Char, arg: Any?) {
      when (c) {
        'N' -> this.args += argToName(arg).escapeIfNecessary()
        'L' -> this.args += argToLiteral(arg)
        'S' -> this.args += argToString(arg)
        'P' -> this.args += if (arg is CodeBlock) arg else argToString(arg)
        'T' -> this.args += argToType(arg)
        'M' -> this.args += arg
        else -> throw IllegalArgumentException(
            String.format("invalid format string: '%s'", format))
      }
    }

    private fun argToName(o: Any?) = when (o) {
      is CharSequence -> o.toString()
      is ParameterSpec -> o.name
      is PropertySpec -> o.name
      is FunSpec -> o.name
      is TypeSpec -> o.name!!
      is MemberName -> o.simpleName
      else -> throw IllegalArgumentException("expected name but was $o")
    }

    private fun argToLiteral(o: Any?) = o

    private fun argToString(o: Any?) = o?.toString()

    private fun argToType(o: Any?) = when (o) {
      is TypeName -> o
      is TypeMirror -> o.asTypeName()
      is Element -> o.asType().asTypeName()
      is Type -> o.asTypeName()
      is KClass<*> -> o.asTypeName()
      else -> throw IllegalArgumentException("expected type but was $o")
    }

    /**
     * @param controlFlow the control flow construct and its code, such as `if (foo == 5)`.
     *     Shouldn't contain newline characters. Can contain opening braces, e.g.
     *     `beginControlFlow("list.forEach { element ->")`. If there's no opening brace at the end
     *     of the string, it will be added.
     */
    fun beginControlFlow(controlFlow: String, vararg args: Any?) = apply {
      add(controlFlow.withOpeningBrace(), *args)
      indent()
    }

    private fun String.withOpeningBrace(): String {
      for (i in length - 1 downTo 0) {
        if (this[i] == '{') {
          return "$this\n"
        } else if (this[i] == '}') {
          break
        }
      }
      return "$this·{\n"
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     *     Shouldn't contain braces or newline characters.
     */
    fun nextControlFlow(controlFlow: String, vararg args: Any?) = apply {
      unindent()
      add("}·$controlFlow·{\n", *args)
      indent()
    }

    fun endControlFlow() = apply {
      unindent()
      add("}\n")
    }

    fun addStatement(format: String, vararg args: Any?) = apply {
      add("«")
      add(format, *args)
      add("\n»")
    }

    fun add(codeBlock: CodeBlock) = apply {
      formatParts += codeBlock.formatParts
      args.addAll(codeBlock.args)
    }

    fun indent() = apply {
      formatParts += "⇥"
    }

    fun unindent() = apply {
      formatParts += "⇤"
    }

    fun build() = CodeBlock(formatParts.toImmutableList(), args.toImmutableList())
  }

  companion object {
    private val NAMED_ARGUMENT = Regex("%([\\w_]+):([\\w]).*")
    private val LOWERCASE = Regex("[a-z]+[\\w_]*")
    private const val ARG_NAME = 1
    private const val TYPE_NAME = 2
    private val NO_ARG_PLACEHOLDERS = setOf("⇥", "⇤", "«", "»")
    internal val EMPTY = CodeBlock(emptyList(), emptyList())

    @JvmStatic fun of(format: String, vararg args: Any?) = Builder().add(format, *args).build()
    @JvmStatic fun builder() = Builder()

    internal fun isMultiCharNoArgPlaceholder(c: Char) = c == '%'
    internal fun isSingleCharNoArgPlaceholder(c: Char) = c.isOneOf('⇥', '⇤', '«', '»')
    internal fun isPlaceholder(s: String) =
        (s.length == 1 && isSingleCharNoArgPlaceholder(s.first())) ||
            (s.length == 2 && isMultiCharNoArgPlaceholder(s.first()))
    internal fun nextPotentialPlaceholderPosition(s: String, startIndex: Int) = s.indexOfAny(
        charArrayOf('%', '«', '»', '⇥', '⇤'), startIndex)
  }
}

@JvmOverloads
fun Collection<CodeBlock>.joinToCode(
  separator: CharSequence = ", ",
  prefix: CharSequence = "",
  suffix: CharSequence = ""
): CodeBlock {
  val blocks = toTypedArray()
  val placeholders = Array(blocks.size) { "%L" }
  return CodeBlock.of(placeholders.joinToString(separator, prefix, suffix), *blocks)
}

/**
 * Builds new [CodeBlock] by populating newly created [CodeBlock.Builder] using provided
 * [builderAction] and then converting it to [CodeBlock].
 */
inline fun buildCodeBlock(builderAction: CodeBlock.Builder.() -> Unit): CodeBlock {
  return CodeBlock.builder().apply(builderAction).build()
}
