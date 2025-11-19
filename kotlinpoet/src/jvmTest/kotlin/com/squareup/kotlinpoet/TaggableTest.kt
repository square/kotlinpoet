/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.squareup.kotlinpoet.KModifier.CROSSINLINE
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TaggableTest(val builder: Taggable.Builder<*>) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() =
      arrayOf(
        AnnotationSpec.builder(JvmStatic::class),
        FileSpec.builder("test", "Test"),
        FunSpec.builder("test"),
        ParameterSpec.builder("test", String::class.asClassName()),
        PropertySpec.builder("test", String::class.asClassName()),
        TypeAliasSpec.builder("Test", String::class.asClassName()),
        TypeSpec.classBuilder("Test"),
      )
  }

  @Before
  fun setUp() {
    builder.tags.clear()
  }

  @Test
  fun builderShouldMakeDefensiveCopy() {
    builder.tag(String::class, "test")
    val taggable = builder.buildTaggable()
    builder.tags.remove(String::class)
    assertThat(taggable.tag<String>()).isEqualTo("test")
  }

  @Test
  fun missingShouldBeNull() {
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag<Int>()).isNull()
  }

  @Test
  fun kclassParamFlow() {
    builder.tag(String::class, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag(String::class)).isEqualTo("test")
  }

  @Test
  fun javaClassParamFlow() {
    builder.tag(String::class.java, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag(String::class.java)).isEqualTo("test")
  }

  @Test
  fun kclassInJavaClassOut() {
    builder.tag(String::class, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag(String::class.java)).isEqualTo("test")
  }

  @Test
  fun javaClassInkClassOut() {
    builder.tag(String::class.java, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag(String::class)).isEqualTo("test")
  }

  private fun Taggable.Builder<*>.buildTaggable(): Taggable {
    // Apply blocks test inline builder tag functions don't break the chain. Result is discarded
    return when (this) {
      is AnnotationSpec.Builder ->
        build().apply { toBuilder().tag(1).addMember(CodeBlock.of("")).build() }
      is FileSpec.Builder -> build().apply { toBuilder().tag(1).addFileComment("Test").build() }
      is FunSpec.Builder -> build().apply { toBuilder().tag(1).returns(String::class).build() }
      is ParameterSpec.Builder ->
        build().apply { toBuilder().tag(1).addModifiers(CROSSINLINE).build() }
      is PropertySpec.Builder ->
        build().apply { toBuilder().tag(1).initializer(CodeBlock.of("")).build() }
      is TypeAliasSpec.Builder ->
        build().apply { toBuilder().tag(1).addKdoc(CodeBlock.of("")).build() }
      is TypeSpec.Builder -> build().apply { toBuilder().tag(1).addKdoc(CodeBlock.of("")).build() }
      else -> TODO("Unsupported type ${this::class.simpleName}")
    }
  }
}
