/*
 * Copyright (C) 2014 Square, Inc.
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

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

class FileWritingTest {
  // Used for testing java.io File behavior.
  @JvmField @Rule val tmp = TemporaryFolder()

  // Used for testing java.nio.file Path behavior.
  private val fs = Jimfs.newFileSystem(Configuration.unix())
  private val fsRoot = fs.rootDirectories.iterator().next()

  @Test fun pathNotDirectory() {
    val type = TypeSpec.classBuilder("Test").build()
    val source = FileSpec.get("example", type)
    val path = fs.getPath("/foo/bar")
    Files.createDirectories(path.parent)
    Files.createFile(path)
    assertThrows<IllegalArgumentException> {
      source.writeTo(path)
    }.hasMessageThat().isEqualTo("path /foo/bar exists but is not a directory.")
  }

  @Test fun fileNotDirectory() {
    val type = TypeSpec.classBuilder("Test").build()
    val source = FileSpec.get("example", type)
    val file = File(tmp.newFolder("foo"), "bar")
    file.createNewFile()
    assertThrows<IllegalArgumentException> {
      source.writeTo(file)
    }.hasMessageThat().isEqualTo("path ${file.path} exists but is not a directory.")
  }

  @Test fun pathDefaultPackage() {
    val type = TypeSpec.classBuilder("Test").build()
    FileSpec.get("", type).writeTo(fsRoot)

    val testPath = fsRoot.resolve("Test.kt")
    assertThat(Files.exists(testPath)).isTrue()
  }

  @Test fun fileDefaultPackage() {
    val type = TypeSpec.classBuilder("Test").build()
    FileSpec.get("", type).writeTo(tmp.root)

    val testFile = File(tmp.root, "Test.kt")
    assertThat(testFile.exists()).isTrue()
  }

  @Test fun pathNestedClasses() {
    val type = TypeSpec.classBuilder("Test").build()
    FileSpec.get("foo", type).writeTo(fsRoot)
    FileSpec.get("foo.bar", type).writeTo(fsRoot)
    FileSpec.get("foo.bar.baz", type).writeTo(fsRoot)

    val fooPath = fsRoot.resolve(fs.getPath("foo", "Test.kt"))
    val barPath = fsRoot.resolve(fs.getPath("foo", "bar", "Test.kt"))
    val bazPath = fsRoot.resolve(fs.getPath("foo", "bar", "baz", "Test.kt"))
    assertThat(Files.exists(fooPath)).isTrue()
    assertThat(Files.exists(barPath)).isTrue()
    assertThat(Files.exists(bazPath)).isTrue()
  }

  @Test fun fileNestedClasses() {
    val type = TypeSpec.classBuilder("Test").build()
    FileSpec.get("foo", type).writeTo(tmp.root)
    FileSpec.get("foo.bar", type).writeTo(tmp.root)
    FileSpec.get("foo.bar.baz", type).writeTo(tmp.root)

    val fooDir = File(tmp.root, "foo")
    val fooFile = File(fooDir, "Test.kt")
    val barDir = File(fooDir, "bar")
    val barFile = File(barDir, "Test.kt")
    val bazDir = File(barDir, "baz")
    val bazFile = File(bazDir, "Test.kt")
    assertThat(fooFile.exists()).isTrue()
    assertThat(barFile.exists()).isTrue()
    assertThat(bazFile.exists()).isTrue()
  }

  /**
   * This test confirms that KotlinPoet ignores the host charset and always uses UTF-8. The host
   * charset is customized with `-Dfile.encoding=ISO-8859-1`.
   */
  @Test fun fileIsUtf8() {
    val source = FileSpec.builder("foo", "Taco")
        .addType(TypeSpec.classBuilder("Taco").build())
        .addComment("Pi\u00f1ata\u00a1")
        .build()
    source.writeTo(fsRoot)

    val fooPath = fsRoot.resolve(fs.getPath("foo", "Taco.kt"))
    assertThat(String(Files.readAllBytes(fooPath), UTF_8)).isEqualTo("""
        |// Piñata¡
        |package foo
        |
        |class Taco
        |""".trimMargin())
  }
}
