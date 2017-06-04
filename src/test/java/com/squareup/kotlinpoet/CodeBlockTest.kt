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
import org.junit.Assert.fail
import org.junit.Test

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
    try {
      CodeBlock.builder().add("%1>", "taco").build()
      fail()
    } catch (exp: IllegalArgumentException) {
      assertThat(exp).hasMessage("%%, %>, %<, %[, %], and %W may not have an index")
    }

  }

  @Test fun deindentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("%1<", "taco").build()
      fail()
    } catch (exp: IllegalArgumentException) {
      assertThat(exp).hasMessage("%%, %>, %<, %[, %], and %W may not have an index")
    }

  }

  @Test fun percentEscapeCannotBeIndexed() {
    try {
      CodeBlock.builder().add("%1%", "taco").build()
      fail()
    } catch (exp: IllegalArgumentException) {
      assertThat(exp).hasMessage("%%, %>, %<, %[, %], and %W may not have an index")
    }

  }

  @Test fun statementBeginningCannotBeIndexed() {
    try {
      CodeBlock.builder().add("%1[", "taco").build()
      fail()
    } catch (exp: IllegalArgumentException) {
      assertThat(exp).hasMessage("%%, %>, %<, %[, %], and %W may not have an index")
    }

  }

  @Test fun statementEndingCannotBeIndexed() {
    try {
      CodeBlock.builder().add("%1]", "taco").build()
      fail()
    } catch (exp: IllegalArgumentException) {
      assertThat(exp).hasMessage("%%, %>, %<, %[, %], and %W may not have an index")
    }

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
    assertThat(block.toString()).isEqualTo("java.lang.String")
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
    assertThat(block.toString()).isEqualTo("\n  tacos for %3.50")
  }

  @Test fun missingNamedArgument() {
    try {
      val map = LinkedHashMap<String, Any>()
      CodeBlock.builder().addNamed("%text:S", map).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("Missing named argument for %text")
    }

  }

  @Test fun lowerCaseNamed() {
    try {
      val map = LinkedHashMap<String, Any>()
      map.put("Text", "tacos")
      CodeBlock.builder().addNamed("%Text:S", map).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("argument 'Text' must start with a lowercase character")
    }

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
    assertThat(block.toString()).isEqualTo("java.lang.Integer\n")
  }

  @Test fun danglingNamed() {
    val map = LinkedHashMap<String, Any>()
    map.put("clazz", Int::class)
    try {
      CodeBlock.builder().addNamed("%clazz:T%", map).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("dangling % at end")
    }

  }

  @Test fun indexTooHigh() {
    try {
      CodeBlock.builder().add("%2T", String::class).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("index 2 for '%2T' not in range (received 1 arguments)")
    }

  }

  @Test fun indexIsZero() {
    try {
      CodeBlock.builder().add("%0T", String::class).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("index 0 for '%0T' not in range (received 1 arguments)")
    }

  }

  @Test fun indexIsNegative() {
    try {
      CodeBlock.builder().add("%-1T", String::class).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("invalid format string: '%-1T'")
    }

  }

  @Test fun indexWithoutFormatType() {
    try {
      CodeBlock.builder().add("%1", String::class).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("dangling format characters in '%1'")
    }

  }

  @Test fun indexWithoutFormatTypeNotAtStringEnd() {
    try {
      CodeBlock.builder().add("%1 taco", String::class).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("invalid format string: '%1 taco'")
    }

  }

  @Test fun indexButNoArguments() {
    try {
      CodeBlock.builder().add("%1T").build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("index 1 for '%1T' not in range (received 0 arguments)")
    }

  }

  @Test fun formatIndicatorAlone() {
    try {
      CodeBlock.builder().add("%", String::class).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("dangling format characters in '%'")
    }

  }

  @Test fun formatIndicatorWithoutIndexOrFormatType() {
    try {
      CodeBlock.builder().add("% tacoString", String::class).build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("invalid format string: '% tacoString'")
    }

  }

  @Test fun sameIndexCanBeUsedWithDifferentFormats() {
    val block = CodeBlock.builder()
        .add("%1T.out.println(%1S)", ClassName.get(System::class))
        .build()
    assertThat(block.toString()).isEqualTo("java.lang.System.out.println(\"java.lang.System\")")
  }

  @Test fun tooManyStatementEnters() {
    val codeBlock = CodeBlock.builder().add("%[%[").build()
    try {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString()
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("statement enter %[ followed by statement enter %[")
    }

  }

  @Test fun statementExitWithoutStatementEnter() {
    val codeBlock = CodeBlock.builder().add("%]").build()
    try {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString()
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("statement exit %] has no matching statement enter %[")
    }
  }

  @Test fun nullableType() {
    val type = TypeName.get(String::class).asNullable()
    val typeBlock = CodeBlock.of("%T", type)
    assertThat(typeBlock.toString()).isEqualTo("java.lang.String?")

    val list = ParameterizedTypeName.get(ClassName.get(List::class).asNullable(),
        TypeName.get(Int::class).asNullable()).asNullable()
    val listBlock = CodeBlock.of("%T", list)
    assertThat(listBlock.toString()).isEqualTo("java.util.List<kotlin.Int?>?")

    val map = ParameterizedTypeName.get(ClassName.get(Map::class).asNullable(),
        TypeName.get(String::class).asNullable(), list).asNullable()
    val mapBlock = CodeBlock.of("%T", map)
    assertThat(mapBlock.toString())
        .isEqualTo("java.util.Map<java.lang.String?, java.util.List<kotlin.Int?>?>?")

    val rarr = WildcardTypeName.subtypeOf(TypeName.get(String::class).asNullable())
    val rarrBlock = CodeBlock.of("%T", rarr)
    assertThat(rarrBlock.toString()).isEqualTo("out java.lang.String?")
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
}
