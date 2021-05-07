package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StringsTest {
  @Test fun singleLineStringWithDollarSymbols() {
    val stringWithTemplate = "$" + "annoyingUser" + " is annoying."
    val funSpec = FunSpec.builder("getString")
      .addStatement("return %S", stringWithTemplate)
      .build()
    assertThat(funSpec.toString())
      .isEqualTo("public fun getString() = \"\${\'\$\'}annoyingUser is annoying.\"\n")
  }

  @Test fun multilineStringWithDollarSymbols() {
    val stringWithTemplate = "Some string\n" + "$" + "annoyingUser" + " is annoying."
    val funSpec = FunSpec.builder("getString")
      .addStatement("return %S", stringWithTemplate)
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      "public fun getString() = \"\"\"\n" +
        "|Some string\n" +
        "|\${\'\$\'}annoyingUser is annoying.\n" +
        "\"\"\".trimMargin()\n"
    )
  }

  @Test fun singleLineStringTemplate() {
    val stringWithTemplate = "$" + "annoyingUser" + " is annoying."
    val funSpec = FunSpec.builder("getString")
      .addStatement("return %P", stringWithTemplate)
      .build()
    assertThat(funSpec.toString())
      .isEqualTo("public fun getString() = \"\"\"\$annoyingUser is annoying.\"\"\"\n")
  }

  @Test fun multilineStringTemplate() {
    val stringWithTemplate = "Some string\n" + "$" + "annoyingUser" + " is annoying."
    val funSpec = FunSpec.builder("getString")
      .addStatement("return %P", stringWithTemplate)
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      "public fun getString() = \"\"\"\n" +
        "|Some string\n" +
        "|\$annoyingUser is annoying.\n" +
        "\"\"\".trimMargin()\n"
    )
  }

  // https://github.com/square/kotlinpoet/issues/572
  @Test fun templateStringWithStringLiteralReference() {
    val string = "SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId \${ if (userId == null) \"IS\" else \"=\" } ?1 ORDER BY datetime(creation_time) DESC"
    val funSpec = FunSpec.builder("getString")
      .addStatement("return %P", string)
      .build()
    assertThat(funSpec.toString())
      .isEqualTo("public fun getString() = \"\"\"SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId \${ if (userId == null) \"IS\" else \"=\" } ?1 ORDER BY datetime(creation_time) DESC\"\"\"\n")
  }
}
