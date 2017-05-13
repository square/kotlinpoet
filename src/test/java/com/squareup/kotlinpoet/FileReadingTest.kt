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

import com.google.common.io.ByteStreams
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import javax.tools.JavaFileObject.Kind

class FileReadingTest {
  @Test fun javaFileObjectUri() {
    val type = TypeSpec.classBuilder("Test").build()
    assertThat(KotlinFile.get("", type).toJavaFileObject().toUri())
        .isEqualTo(URI.create("Test.kt"))
    assertThat(KotlinFile.get("foo", type).toJavaFileObject().toUri())
        .isEqualTo(URI.create("foo/Test.kt"))
    assertThat(KotlinFile.get("com.example", type).toJavaFileObject().toUri())
        .isEqualTo(URI.create("com/example/Test.kt"))
  }

  @Test fun javaFileObjectKind() {
    val kotlinFile = KotlinFile.get("", TypeSpec.classBuilder("Test").build())
    assertThat(kotlinFile.toJavaFileObject().kind).isEqualTo(Kind.SOURCE)
  }

  @Test fun javaFileObjectCharacterContent() {
    val type = TypeSpec.classBuilder("Test")
        .addKdoc("Pi\u00f1ata\u00a1")
        .addFun(FunSpec.builder("fooBar").build())
        .build()
    val kotlinFile = KotlinFile.get("foo", type)
    val javaFileObject = kotlinFile.toJavaFileObject()

    // We can never have encoding issues (everything is in process)
    assertThat(javaFileObject.getCharContent(true)).isEqualTo(kotlinFile.toString())
    assertThat(javaFileObject.getCharContent(false)).isEqualTo(kotlinFile.toString())
  }

  @Test fun javaFileObjectInputStreamIsUtf8() {
    val kotlinFile = KotlinFile.builder("foo", "Test")
        .addType(TypeSpec.classBuilder("Test").build())
        .addFileComment("Pi\u00f1ata\u00a1")
        .build()
    val bytes = ByteStreams.toByteArray(kotlinFile.toJavaFileObject().openInputStream())

    // KotlinPoet always uses UTF-8.
    assertThat(bytes).isEqualTo(kotlinFile.toString().toByteArray(UTF_8))
  }
}
