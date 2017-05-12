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
package com.squareup.kotlinpoet

import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * A fragment of a .kt file, potentially containing declarations, statements, and documentation.
 * Code blocks are not necessarily well-formed Kotlin code, and are not validated. This class
 * assumes kotlinc will check correctness later!
 *
 * Code blocks support placeholders like [java.text.Format]. This class uses a percent sign
 * `%` but has its own set of permitted placeholders:
 *
 *  * `%L` emits a *literal* value with no escaping. Arguments for literals may be strings,
 *    primitives, [type declarations][TypeSpec], [annotations][AnnotationSpec] and even other code
 *    blocks.
 *  * `%N` emits a *name*, using name collision avoidance where necessary. Arguments for names may
 *    be strings (actually any [character sequence][CharSequence]), [parameters][ParameterSpec],
 *    [properties][PropertySpec], [functions][FunSpec], and [types][TypeSpec].
 *  * `%S` escapes the value as a *string*, wraps it with double quotes, and emits that. For
 *    example, `6" sandwich` is emitted `"6\" sandwich"`.
 *  * `%T` emits a *type* reference. Types will be imported if possible. Arguments for types may be
 *    [classes][Class], [type mirrors][javax.lang.model.type.TypeMirror], and
 *    [elements][javax.lang.model.element.Element].
 *  * `%%` emits a percent sign.
 *  * `%W` emits a space or a newline, depending on its position on the line. This prefers to wrap
 *    lines before 100 columns.
 *  * `%>` increases the indentation level.
 *  * `%<` decreases the indentation level.
 *  * `%[` begins a statement. For multiline statements, every line after the first line is
 *    double-indented.
 *  * `%]` ends a statement.
 */
class CodeBlock private constructor(builder: CodeBlock.Builder) {
  /** A heterogeneous list containing string literals and value placeholders.  */
  internal val formatParts: List<String> = Util.immutableList(builder.formatParts)
  internal val args: List<Any?> = Util.immutableList(builder.args)

  fun isEmpty() = formatParts.isEmpty()

  fun formatParts() = formatParts

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String {
    val out = StringWriter()
    try {
      CodeWriter(out).emit(this)
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }

  }

  fun toBuilder(): Builder {
    val builder = Builder()
    builder.formatParts.addAll(formatParts)
    builder.args.addAll(args)
    return builder
  }

  class Builder {
    internal val formatParts: MutableList<String> = ArrayList()
    internal val args: MutableList<Any?> = ArrayList()

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
    fun addNamed(format: String, arguments: Map<String, *>): Builder {
      var p = 0

      for (argument in arguments.keys) {
        require(LOWERCASE.matcher(argument).matches()) {
            "argument '$argument' must start with a lowercase character" }
      }

      while (p < format.length) {
        val nextP = format.indexOf("%", p)
        if (nextP == -1) {
          formatParts.add(format.substring(p, format.length))
          break
        }

        if (p != nextP) {
          formatParts.add(format.substring(p, nextP))
          p = nextP
        }

        var matcher: Matcher? = null
        val colon = format.indexOf(':', p)
        if (colon != -1) {
          val endIndex = Math.min(colon + 2, format.length)
          matcher = NAMED_ARGUMENT.matcher(format.substring(p, endIndex))
        }
        if (matcher != null && matcher.lookingAt()) {
          val argumentName = matcher.group("argumentName")
          require(arguments.containsKey(argumentName)) {
            "Missing named argument for %$argumentName" }
          val formatChar = matcher.group("typeChar")[0]
          addArgument(format, formatChar, arguments[argumentName])
          formatParts.add("%" + formatChar)
          p += matcher.regionEnd()
        } else {
          require(p < format.length - 1) { "dangling % at end" }
          require(isNoArgPlaceholder(format[p + 1])) {
            "unknown format %${format[p + 1]} at ${p + 1} in '$format'" }
          formatParts.add(format.substring(p, p + 2))
          p += 2
        }
      }

      return this
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
    fun add(format: String, vararg args: Any?): Builder {
      var hasRelative = false
      var hasIndexed = false

      var relativeParameterCount = 0
      val indexedParameterCount = IntArray(args.size)

      var p = 0
      while (p < format.length) {
        if (format[p] != '%') {
          var nextP = format.indexOf('%', p + 1)
          if (nextP == -1) nextP = format.length
          formatParts.add(format.substring(p, nextP))
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
        } while (c >= '0' && c <= '9')
        val indexEnd = p - 1

        // If 'c' doesn't take an argument, we're done.
        if (isNoArgPlaceholder(c)) {
          require(indexStart == indexEnd) {
            "%%, %>, %<, %[, %], and %W may not have an index" }
          formatParts.add("%" + c)
          continue
        }

        // Find either the indexed argument, or the relative argument. (0-based).
        val index: Int
        if (indexStart < indexEnd) {
          index = Integer.parseInt(format.substring(indexStart, indexEnd)) - 1
          hasIndexed = true
          if (args.size > 0) {
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

        formatParts.add("%" + c)
      }

      if (hasRelative) {
        require(relativeParameterCount >= args.size) {
            "unused arguments: expected $relativeParameterCount, received ${args.size}" }
      }
      if (hasIndexed) {
        val unused = ArrayList<String>()
        for (i in args.indices) {
          if (indexedParameterCount[i] == 0) {
            unused.add("%" + (i + 1))
          }
        }
        val s = if (unused.size == 1) "" else "s"
        require(unused.isEmpty()) { "unused argument$s: ${unused.joinToString(", ")}" }
      }
      return this
    }

    private fun isNoArgPlaceholder(c: Char)
        = c == '%' || c == '>' || c == '<' || c == '[' || c == ']' || c == 'W'

    private fun addArgument(format: String, c: Char, arg: Any?) {
      when (c) {
        'N' -> this.args.add(argToName(arg))
        'L' -> this.args.add(argToLiteral(arg))
        'S' -> this.args.add(argToString(arg))
        'T' -> this.args.add(argToType(arg))
        else -> throw IllegalArgumentException(
            String.format("invalid format string: '%s'", format))
      }
    }

    private fun argToName(o: Any?): String {
      when (o) {
        is CharSequence -> return o.toString()
        is ParameterSpec -> return o.name
        is PropertySpec -> return o.name
        is FunSpec -> return o.name
        is TypeSpec -> return o.name!!
        else -> throw IllegalArgumentException("expected name but was " + o)
      }
    }

    private fun argToLiteral(o: Any?) = o

    private fun argToString(o: Any?) = o?.toString()

    private fun argToType(o: Any?): TypeName {
      when (o) {
        is TypeName -> return o
        is TypeMirror -> return TypeName.get(o)
        is Element -> return TypeName.get(o.asType())
        is Type -> return TypeName.get(o)
        is KClass<*> -> return TypeName.get(o)
        else -> throw IllegalArgumentException("expected type but was " + o)
      }
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     *     Shouldn't contain braces or newline characters.
     */
    fun beginControlFlow(controlFlow: String, vararg args: Any?): Builder {
      add(controlFlow + " {\n", *args)
      indent()
      return this
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     *     Shouldn't contain braces or newline characters.
     */
    fun nextControlFlow(controlFlow: String, vararg args: Any?): Builder {
      unindent()
      add("} $controlFlow {\n", *args)
      indent()
      return this
    }

    fun endControlFlow(): Builder {
      unindent()
      add("}\n")
      return this
    }

    fun addStatement(format: String, vararg args: Any?): Builder {
      add("%[")
      add(format, *args)
      add("\n%]")
      return this
    }

    fun add(codeBlock: CodeBlock): Builder {
      formatParts.addAll(codeBlock.formatParts)
      args.addAll(codeBlock.args)
      return this
    }

    fun indent(): Builder {
      this.formatParts.add("%>")
      return this
    }

    fun unindent(): Builder {
      this.formatParts.add("%<")
      return this
    }

    fun build() = CodeBlock(this)
  }

  companion object {
    @JvmField internal val NAMED_ARGUMENT
        = Pattern.compile("%(?<argumentName>[\\w_]+):(?<typeChar>[\\w]).*")
    @JvmField internal val LOWERCASE
        = Pattern.compile("[a-z]+[\\w_]*")
    @JvmStatic fun of(format: String, vararg args: Any?) = Builder().add(format, *args).build()
    @JvmStatic fun builder() = Builder()
  }
}
