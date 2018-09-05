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

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.test.Test

class CodeBlockTest {
  @Test fun equalsAndHashCode() {
    var a = CodeBlock.builder().build()
    var b = CodeBlock.builder().build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = CodeBlock.builder().add("%L", "taco").build()
    b = CodeBlock.builder().add("%L", "taco").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun of() {
    val a = CodeBlock.of("%L taco", "delicious")
    assertThat(a.toString()).isEqualTo("delicious taco")
  }

  @Test fun indentCannotBeIndexed() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1>", "taco").build()
    }.hasMessageThat().isEqualTo("%%, %>, %<, %[, %], and %W may not have an index")
  }

  @Test fun deindentCannotBeIndexed() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1<", "taco").build()
    }.hasMessageThat().isEqualTo("%%, %>, %<, %[, %], and %W may not have an index")
  }

  @Test fun percentEscapeCannotBeIndexed() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1%", "taco").build()
    }.hasMessageThat().isEqualTo("%%, %>, %<, %[, %], and %W may not have an index")
  }

  @Test fun statementBeginningCannotBeIndexed() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1[", "taco").build()
    }.hasMessageThat().isEqualTo("%%, %>, %<, %[, %], and %W may not have an index")
  }

  @Test fun statementEndingCannotBeIndexed() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1]", "taco").build()
    }.hasMessageThat().isEqualTo("%%, %>, %<, %[, %], and %W may not have an index")
  }

  @Test fun nameFormatCanBeIndexed() {
    val block = CodeBlock.builder().add("%1N", "taco").build()
    assertThat(block.toString()).isEqualTo("taco")
  }

  @Test fun literalFormatCanBeIndexed() {
    val block = CodeBlock.builder().add("%1L", "taco").build()
    assertThat(block.toString()).isEqualTo("taco")
  }

  @Test fun stringFormatCanBeIndexed() {
    val block = CodeBlock.builder().add("%1S", "taco").build()
    assertThat(block.toString()).isEqualTo("\"taco\"")
  }

  @Test fun typeFormatCanBeIndexed() {
    val block = CodeBlock.builder().add("%1T", String::class).build()
    assertThat(block.toString()).isEqualTo("kotlin.String")
  }

  @Test fun simpleNamedArgument() {
    val map = LinkedHashMap<String, Any>()
    map.put("text", "taco")
    val block = CodeBlock.builder().addNamed("%text:S", map).build()
    assertThat(block.toString()).isEqualTo("\"taco\"")
  }

  @Test fun repeatedNamedArgument() {
    val map = LinkedHashMap<String, Any>()
    map.put("text", "tacos")
    val block = CodeBlock.builder()
        .addNamed("\"I like \" + %text:S + \". Do you like \" + %text:S + \"?\"", map)
        .build()
    assertThat(block.toString()).isEqualTo(
        "\"I like \" + \"tacos\" + \". Do you like \" + \"tacos\" + \"?\"")
  }

  @Test fun namedAndNoArgFormat() {
    val map = LinkedHashMap<String, Any>()
    map.put("text", "tacos")
    val block = CodeBlock.builder()
        .addNamed("%>\n%text:L for %%3.50", map).build()
    assertThat(block.toString()).isEqualTo("\n    tacos for %3.50")
  }

  @Test fun missingNamedArgument() {
    assertThrows<IllegalArgumentException> {
      val map = LinkedHashMap<String, Any>()
      CodeBlock.builder().addNamed("%text:S", map).build()
    }.hasMessageThat().isEqualTo("Missing named argument for %text")
  }

  @Test fun lowerCaseNamed() {
    assertThrows<IllegalArgumentException> {
      val map = LinkedHashMap<String, Any>()
      map.put("Text", "tacos")
      CodeBlock.builder().addNamed("%Text:S", map).build()
    }.hasMessageThat().isEqualTo("argument 'Text' must start with a lowercase character")
  }

  @Test fun multipleNamedArguments() {
    val map = LinkedHashMap<String, Any>()
    map.put("pipe", System::class)
    map.put("text", "tacos")

    val block = CodeBlock.builder()
        .addNamed("%pipe:T.out.println(\"Let's eat some %text:L\");", map)
        .build()

    assertThat(block.toString()).isEqualTo(
        "java.lang.System.out.println(\"Let's eat some tacos\");")
  }

  @Test fun namedNewline() {
    val map = LinkedHashMap<String, Any>()
    map.put("clazz", java.lang.Integer::class)
    val block = CodeBlock.builder().addNamed("%clazz:T\n", map).build()
    assertThat(block.toString()).isEqualTo("kotlin.Int\n")
  }

  @Test fun danglingNamed() {
    val map = LinkedHashMap<String, Any>()
    map.put("clazz", Int::class)
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().addNamed("%clazz:T%", map).build()
    }.hasMessageThat().isEqualTo("dangling % at end")
  }

  @Test fun indexTooHigh() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%2T", String::class).build()
    }.hasMessageThat().isEqualTo("index 2 for '%2T' not in range (received 1 arguments)")
  }

  @Test fun indexIsZero() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%0T", String::class).build()
    }.hasMessageThat().isEqualTo("index 0 for '%0T' not in range (received 1 arguments)")
  }

  @Test fun indexIsNegative() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%-1T", String::class).build()
    }.hasMessageThat().isEqualTo("invalid format string: '%-1T'")
  }

  @Test fun indexWithoutFormatType() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1", String::class).build()
    }.hasMessageThat().isEqualTo("dangling format characters in '%1'")
  }

  @Test fun indexWithoutFormatTypeNotAtStringEnd() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1 taco", String::class).build()
    }.hasMessageThat().isEqualTo("invalid format string: '%1 taco'")
  }

  @Test fun indexButNoArguments() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%1T").build()
    }.hasMessageThat().isEqualTo("index 1 for '%1T' not in range (received 0 arguments)")
  }

  @Test fun formatIndicatorAlone() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("%", String::class).build()
    }.hasMessageThat().isEqualTo("dangling format characters in '%'")
  }

  @Test fun formatIndicatorWithoutIndexOrFormatType() {
    assertThrows<IllegalArgumentException> {
      CodeBlock.builder().add("% tacoString", String::class).build()
    }.hasMessageThat().isEqualTo("invalid format string: '% tacoString'")
  }

  @Test fun sameIndexCanBeUsedWithDifferentFormats() {
    val block = CodeBlock.builder()
        .add("%1T.out.println(%1S)", System::class.asClassName())
        .build()
    assertThat(block.toString()).isEqualTo("java.lang.System.out.println(\"java.lang.System\")")
  }

  @Test fun tooManyStatementEnters() {
    val codeBlock = CodeBlock.builder().add("%[%[").build()
    assertThrows<IllegalStateException> {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString()
    }.hasMessageThat().isEqualTo("statement enter %[ followed by statement enter %[")
  }

  @Test fun statementExitWithoutStatementEnter() {
    val codeBlock = CodeBlock.builder().add("%]").build()
    assertThrows<IllegalStateException> {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString()
    }.hasMessageThat().isEqualTo("statement exit %] has no matching statement enter %[")
  }

  @Test fun nullableType() {
    val type = String::class.asTypeName().asNullable()
    val typeBlock = CodeBlock.of("%T", type)
    assertThat(typeBlock.toString()).isEqualTo("kotlin.String?")

    val list = List::class.asClassName().asNullable()
        .parameterizedBy(Int::class.asTypeName().asNullable()).asNullable()
    val listBlock = CodeBlock.of("%T", list)
    assertThat(listBlock.toString()).isEqualTo("kotlin.collections.List<kotlin.Int?>?")

    val map = Map::class.asClassName().asNullable()
        .parameterizedBy(String::class.asTypeName().asNullable(), list).asNullable()
    val mapBlock = CodeBlock.of("%T", map)
    assertThat(mapBlock.toString())
        .isEqualTo("kotlin.collections.Map<kotlin.String?, kotlin.collections.List<kotlin.Int?>?>?")

    val rarr = WildcardTypeName.subtypeOf(String::class.asTypeName().asNullable())
    val rarrBlock = CodeBlock.of("%T", rarr)
    assertThat(rarrBlock.toString()).isEqualTo("out kotlin.String?")
  }

  @Test fun withoutPrefixMatching() {
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("")))
        .isEqualTo(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y"))
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("ab")))
        .isEqualTo(CodeBlock.of("cd %S efgh %S ijkl", "x", "y"))
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd ")))
        .isEqualTo(CodeBlock.of("%S efgh %S ijkl", "x", "y"))
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S", "x")))
        .isEqualTo(CodeBlock.of(" efgh %S ijkl", "y"))
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S ef", "x")))
        .isEqualTo(CodeBlock.of("gh %S ijkl", "y"))
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S efgh ", "x")))
        .isEqualTo(CodeBlock.of("%S ijkl", "y"))
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S efgh %S", "x", "y")))
        .isEqualTo(CodeBlock.of(" ijkl"))
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S efgh %S ij", "x", "y")))
        .isEqualTo(CodeBlock.of("kl"))
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")))
        .isEqualTo(CodeBlock.of(""))
  }

  @Test fun withoutPrefixNoArgs() {
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("")))
        .isEqualTo(CodeBlock.of("abcd %% efgh %% ijkl"))
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("ab")))
        .isEqualTo(CodeBlock.of("cd %% efgh %% ijkl"))
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("abcd ")))
        .isEqualTo(CodeBlock.of("%% efgh %% ijkl"))
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("abcd %%")))
        .isEqualTo(CodeBlock.of(" efgh %% ijkl"))
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("abcd %% ef")))
        .isEqualTo(CodeBlock.of("gh %% ijkl"))
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("abcd %% efgh ")))
        .isEqualTo(CodeBlock.of("%% ijkl"))
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("abcd %% efgh %%")))
        .isEqualTo(CodeBlock.of(" ijkl"))
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("abcd %% efgh %% ij")))
        .isEqualTo(CodeBlock.of("kl"))
    assertThat(CodeBlock.of("abcd %% efgh %% ijkl")
        .withoutPrefix(CodeBlock.of("abcd %% efgh %% ijkl")))
        .isEqualTo(CodeBlock.of(""))
  }

  @Test fun withoutPrefixArgMismatch() {
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S efgh %S ij", "x", "z")))
        .isNull()
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S efgh %S ij", "z", "y")))
        .isNull()
  }

  @Test fun withoutPrefixFormatPartMismatch() {
    assertThat(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S efgx %S ij", "x", "y")))
        .isNull()
    assertThat(CodeBlock.of("abcd %S efgh %% ijkl", "x")
        .withoutPrefix(CodeBlock.of("abcd %% efgh %S ij", "x")))
        .isNull()
  }

  @Test fun withoutPrefixTooShort() {
    assertThat(CodeBlock.of("abcd %S efgh %S", "x", "y")
        .withoutPrefix(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")))
        .isNull()
    assertThat(CodeBlock.of("abcd %S efgh", "x")
        .withoutPrefix(CodeBlock.of("abcd %S efgh %S ijkl", "x", "y")))
        .isNull()
  }

  @Test fun trimEmpty() {
    assertThat(CodeBlock.of("").trim())
        .isEqualTo(CodeBlock.of(""))
  }

  @Test fun trimNoPlaceholders() {
    assertThat(CodeBlock.of("return null").trim())
        .isEqualTo(CodeBlock.of("return null"))
  }

  @Test fun trimPlaceholdersWithArgs() {
    assertThat(CodeBlock.of("return %S", "taco").trim())
        .isEqualTo(CodeBlock.of("return %S", "taco"))
  }

  @Test fun trimNoArgPlaceholderMiddle() {
    assertThat(CodeBlock.of("this.taco =%W%S", "taco").trim())
        .isEqualTo(CodeBlock.of("this.taco =%W%S", "taco"))
  }

  @Test fun trimNoArgPlaceholderStart() {
    assertThat(CodeBlock.of("%>return ").trim())
        .isEqualTo(CodeBlock.of("return "))
  }

  @Test fun trimNoArgPlaceholderEnd() {
    assertThat(CodeBlock.of("return %W").trim())
        .isEqualTo(CodeBlock.of("return "))
  }

  @Test fun trimNoArgPlaceholdersStartEnd() {
    assertThat(CodeBlock.of("%[return this%]").trim())
        .isEqualTo(CodeBlock.of("return this"))
  }

  @Test fun trimMultipleNoArgPlaceholders() {
    assertThat(
        CodeBlock.of("%[return if (x > %L) %S %Welse %S%]", 1, "a", "b").trim())
        .isEqualTo(CodeBlock.of("return if (x > %L) %S %Welse %S", 1, "a", "b"))
  }

  @Test fun trimOnlyNoArgPlaceholders() {
    assertThat(CodeBlock.of("%[%W%]%>%<").trim())
        .isEqualTo(CodeBlock.of(""))
  }

  @Test fun replaceSimple() {
    assertThat(CodeBlock.of("%W%>%W").replaceAll("%W", ""))
        .isEqualTo(CodeBlock.of("%>"))
  }

  @Test fun replaceNoMatches() {
    assertThat(CodeBlock.of("%W%>%W").replaceAll("%<", ""))
        .isEqualTo(CodeBlock.of("%W%>%W"))
  }

  @Test fun replaceRegex() {
    assertThat(CodeBlock.of("%W%>%W%<").replaceAll("[%>|%<]", ""))
        .isEqualTo(CodeBlock.of("%W%W"))
  }

  @Test fun joinToCode() {
    val blocks = listOf(CodeBlock.of("%L", "taco1"), CodeBlock.of("%L", "taco2"), CodeBlock.of("%L", "taco3"))
    assertThat(blocks.joinToCode(prefix = "(", suffix = ")"))
        .isEqualTo(CodeBlock.of("(%L, %L, %L)", "taco1", "taco2", "taco3"))
  }

  @Test fun beginControlFlowWithParams() {
    val controlFlow = CodeBlock.builder()
        .beginControlFlow("list.forEach { element ->")
        .addStatement("println(element)")
        .endControlFlow()
        .build()
    assertThat(controlFlow.toString()).isEqualTo("""
      |list.forEach { element ->
      |    println(element)
      |}
      |""".trimMargin())
  }

  @Test fun beginControlFlowWithParamsAndTemplateString() {
    val controlFlow = CodeBlock.builder()
        .beginControlFlow("listOf(\"\${1.toString()}\").forEach { element ->")
        .addStatement("println(element)")
        .endControlFlow()
        .build()
    assertThat(controlFlow.toString()).isEqualTo("""
      |listOf("${'$'}{1.toString()}").forEach { element ->
      |    println(element)
      |}
      |""".trimMargin())
  }

  @Test fun booleanValue() =
      assertThat(CodeBlock.of("%V", true).toString())
          .isEqualTo("true")

  @Test fun byteValue() =
      assertThat(CodeBlock.of("%V", 8.toByte()).toString())
          .isEqualTo("8.toByte()")

  @Test fun shortValue() =
      assertThat(CodeBlock.of("%V", 0x9E37.toShort()).toString())
          .isEqualTo("-25033.toShort()")

  @Test fun intValue() =
      assertThat(CodeBlock.of("%V", 0xFA23).toString())
          .isEqualTo("64035")

  @Test fun longValue() = assertThat(CodeBlock.of("%V", 2L).toString()).isEqualTo("2L")

  @Test fun charValue() = assertThat(CodeBlock.of("%V", 'h').toString()).isEqualTo("'h'")

  @Test fun escapedCharValue() =
      assertThat(CodeBlock.of("%V", '\\').toString())
          .isEqualTo("""'\\'""")

  @Test fun floatValue() = assertThat(CodeBlock.of("%V", 1f).toString()).isEqualTo("1.0f")

  @Test fun doubleValue() = assertThat(CodeBlock.of("%V", 1.234).toString()).isEqualTo("1.234")

  @Test fun classValue() =
      assertThat(CodeBlock.of("%V", Test::class).toString())
          .isEqualTo("org.junit.Test::class")

  @Test fun enumValue() =
      assertThat(CodeBlock.of("%V", AnnotationRetention.SOURCE).toString())
          .isEqualTo("kotlin.annotation.AnnotationRetention.SOURCE")

  @Test fun stringValue() =
      assertThat(CodeBlock.of("%V", "My String with \$arg and \" escaped.").toString())
          .isEqualTo(""""My String with \${'$'}arg and \" escaped."""")

  @Test fun multiLineStringValue() =
      assertThat(CodeBlock.of("%V", "A multi-lined string\nwith a \$var in it.").toString())
          .isEqualTo("\"\"\"\n|A multi-lined string\n|with a \${'$'}var in it.\n\"\"\".trimMargin()")

  @Test fun booleanArrayValue() =
      assertThat(CodeBlock.of("%V", booleanArrayOf(true, true, false)).toString())
          .isEqualTo("booleanArrayOf(true, true, false)")

  @Test fun byteArrayValue() =
      assertThat(CodeBlock.of("%V", byteArrayOf(1, 2, 3)).toString())
          .isEqualTo("byteArrayOf(1, 2, 3)")

  @Test fun shortArrayValue() =
      assertThat(CodeBlock.of("%V", shortArrayOf(4, 5, 6)).toString())
          .isEqualTo("shortArrayOf(4, 5, 6)")

  @Test fun intArrayValue() =
      assertThat(CodeBlock.of("%V", intArrayOf(7, 8, 9)).toString())
          .isEqualTo("intArrayOf(7, 8, 9)")

  @Test fun longArrayValue() =
      assertThat(CodeBlock.of("%V", longArrayOf(10, 11, 12)).toString())
          .isEqualTo("longArrayOf(10, 11, 12)")

  @Test fun charArrayValue() =
      assertThat(CodeBlock.of("%V", charArrayOf('A', 'b', '\n')).toString())
          .isEqualTo("charArrayOf('A', 'b', '\\n')")

  @Test fun floatArrayValue() =
      assertThat(CodeBlock.of("%V", floatArrayOf(13f, 14.1f, 15.987f)).toString())
          .isEqualTo("floatArrayOf(13.0f, 14.1f, 15.987f)")

  @Test fun doubleArrayValue() =
      assertThat(CodeBlock.of("%V", doubleArrayOf(16.0, -17.6, 18.987)).toString())
          .isEqualTo("doubleArrayOf(16.0, -17.6, 18.987)")

  @Test fun arrayOfNumbersValue() =
      assertThat(CodeBlock.of("%V", arrayOf(1, -2L, 3.0, 4f, 5.toShort(), 6.toByte())).toString())
          .isEqualTo("arrayOf(1, -2L, 3.0, 4.0f, 5.toShort(), 6.toByte())")

  @Test fun emptyArrayValue() =
      assertThat(CodeBlock.of("%V", emptyArray<String>()).toString())
          .isEqualTo("arrayOf()")

  @Test fun pairValueOfIntToShort() =
      assertThat(CodeBlock.of("%V", 1 to 2.toShort()).toString())
          .isEqualTo("1 to 2.toShort()")

  @Test fun pairValueOfFloatToNull() =
      assertThat(CodeBlock.of("%V", 3f to null).toString())
          .isEqualTo("3.0f to null")

  @Test fun pairValueOfCharToSpec() =
      assertThat(CodeBlock.of("%V", 'A' to TypeSpec.anonymousClassBuilder().build()).toString())
          .isEqualTo("'A' to object {\n}")

  @Test fun mapEntryValueOfLongToDouble() =
      assertThat(CodeBlock.of("%V", mapOf(99L to 7.7).entries.single()).toString())
          .isEqualTo("99L to 7.7")

  @Test fun mapEntryValueOfBoolToNull() =
      assertThat(CodeBlock.of("%V", mapOf(false to null).entries.single()).toString())
          .isEqualTo("false to null")

  @Test fun mapEntryValueOfStringToSpec() {
    val spec = TypeSpec.anonymousClassBuilder().build()
    assertThat(CodeBlock.of("%V", mapOf("StringKey" to spec).entries.single()).toString())
        .isEqualTo("\"StringKey\" to object {\n}")
  }

  @Test fun listOfValue() {
    val list = listOf(true, 1.toByte(), 2.toShort(), 3, 4L, '5', 6.7f, 8.910, "Eleven")
    assertThat(CodeBlock.of("%V", list).toString())
        .isEqualTo("""listOf(true, 1.toByte(), 2.toShort(), 3, 4L, '5', 6.7f, 8.91, "Eleven")""")
  }

  @Test fun listOfPairsValue() {
    val list = listOf(1 to 2, 3 to null, "StringValue" to TypeSpec.anonymousClassBuilder().build())
    assertThat(CodeBlock.of("%V", list).toString())
        .isEqualTo("listOf(1 to 2, 3 to null, \"StringValue\" to object {\n})")
  }

  @Test fun emptyListValue() =
      assertThat(CodeBlock.of("%V", emptyList<String>()).toString())
          .isEqualTo("listOf()")

  @Test fun mapOfValue() {
    val map = mapOf(1 to 2, 3 to null, "StringKey" to TypeSpec.anonymousClassBuilder().build())
    assertThat(CodeBlock.of("%V", map).toString())
        .isEqualTo("mapOf(1 to 2, 3 to null, \"StringKey\" to object {\n})")
  }

  @Test fun setOfValue() =
      assertThat(CodeBlock.of("%V", setOf("a", "b", "c")).toString())
          .isEqualTo("""setOf("a", "b", "c")""")
}
