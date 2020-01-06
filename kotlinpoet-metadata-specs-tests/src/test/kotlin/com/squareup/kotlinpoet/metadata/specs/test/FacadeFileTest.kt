package com.squareup.kotlinpoet.metadata.specs.test

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.junit.Test

@KotlinPoetMetadataPreview
class FacadeFileTest : MultiClassInspectorTest() {

  @Test
  fun facadeFile() {
    val fileSpec = Class.forName(
        "com.squareup.kotlinpoet.metadata.specs.test.FacadeFile").kotlin.toFileSpecWithTestHandler()
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      // TODO
    """.trimIndent())
  }

}

private fun FileSpec.trimmedToString(): String {
  return buildString { writeTo(this) }.trim()
}