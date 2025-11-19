/*
 * Copyright (C) 2015 Square, Inc.
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
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import javax.tools.JavaFileObject.Kind
import kotlin.test.Test

class FileReadingTest {
  @Test
  fun javaFileObjectUri() {
    val type = TypeSpec.classBuilder("Test").build()
    assertThat(FileSpec.get("", type).toJavaFileObject().toUri()).isEqualTo(URI.create("Test.kt"))
    assertThat(FileSpec.get("foo", type).toJavaFileObject().toUri())
      .isEqualTo(URI.create("foo/Test.kt"))
    assertThat(FileSpec.get("com.example", type).toJavaFileObject().toUri())
      .isEqualTo(URI.create("com/example/Test.kt"))
  }

  @Test
  fun javaFileObjectKind() {
    val source = FileSpec.get("", TypeSpec.classBuilder("Test").build())
    assertThat(source.toJavaFileObject().kind).isEqualTo(Kind.SOURCE)
  }

  @Test
  fun javaFileObjectCharacterContent() {
    val type =
      TypeSpec.classBuilder("Test")
        .addKdoc("Pi\u00f1ata\u00a1")
        .addFunction(FunSpec.builder("fooBar").build())
        .build()
    val source = FileSpec.get("foo", type)
    val javaFileObject = source.toJavaFileObject()

    // We can never have encoding issues (everything is in process)
    assertThat(javaFileObject.getCharContent(true)).isEqualTo(source.toString())
    assertThat(javaFileObject.getCharContent(false)).isEqualTo(source.toString())
  }

  @Test
  fun javaFileObjectInputStreamIsUtf8() {
    val source =
      FileSpec.builder("foo", "Test")
        .addType(TypeSpec.classBuilder("Test").build())
        .addFileComment("Pi\u00f1ata\u00a1")
        .build()
    val bytes = source.toJavaFileObject().openInputStream().use(InputStream::readBytes)

    // KotlinPoet always uses UTF-8.
    assertThat(bytes).isEqualTo(source.toString().toByteArray(UTF_8))
  }
}
