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
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.Date
import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FileWritingTest {
  // Used for testing java.io File behavior.
  @JvmField @Rule val tmp = TemporaryFolder()

  // Used for testing java.nio.file Path behavior.
  private val fs = Jimfs.newFileSystem(Configuration.unix())
  private val fsRoot = fs.rootDirectories.iterator().next()

  // Used for testing annotation processor Filer behavior.
  private val filer = TestFiler(fs, fsRoot)

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

  @Test fun filerDefaultPackage() {
    val type = TypeSpec.classBuilder("Test").build()
    FileSpec.get("", type).writeTo(filer)

    val testPath = fsRoot.resolve("Test.kt")
    assertThat(Files.exists(testPath)).isTrue()
  }

  @Test fun pathDefaultPackage() {
    val type = TypeSpec.classBuilder("Test").build()
    FileSpec.get("", type).writeTo(fsRoot)

    val testPath = fsRoot.resolve("Test.kt")
    assertThat(Files.exists(testPath)).isTrue()
  }

  @Test fun pathDefaultPackageWithSubdirectory() {
    val type = TypeSpec.classBuilder("Test").build()
    FileSpec.get("", type).writeTo(fsRoot.resolve("sub"))

    val testPath = fsRoot.resolve("sub/Test.kt")
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

  @Test fun filerNestedClasses() {
    val type = TypeSpec.classBuilder("Test").build()
    FileSpec.get("foo", type).writeTo(filer)
    FileSpec.get("foo.bar", type).writeTo(filer)
    FileSpec.get("foo.bar.baz", type).writeTo(filer)

    val fooPath = fsRoot.resolve(fs.getPath("foo", "Test.kt"))
    val barPath = fsRoot.resolve(fs.getPath("foo", "bar", "Test.kt"))
    val bazPath = fsRoot.resolve(fs.getPath("foo", "bar", "baz", "Test.kt"))
    assertThat(Files.exists(fooPath)).isTrue()
    assertThat(Files.exists(barPath)).isTrue()
    assertThat(Files.exists(bazPath)).isTrue()
  }

  @Suppress("LocalVariableName")
  @Test fun filerPassesOriginatingElements() {
    // TypeSpecs
    val element1_1 = FakeElement()
    val test1 = TypeSpec.classBuilder("Test1")
        .addOriginatingElement(element1_1)
        .build()

    val element2_1 = FakeElement()
    val element2_2 = FakeElement()
    val test2 = TypeSpec.classBuilder("Test2")
        .addOriginatingElement(element2_1)
        .addOriginatingElement(element2_2)
        .build()

    // FunSpecs
    val element3_1 = FakeElement()
    val element3_2 = FakeElement()
    val test3 = FunSpec.builder("fun3")
        .addOriginatingElement(element3_1)
        .addOriginatingElement(element3_2)
        .build()

    // PropertySpecs
    val element4_1 = FakeElement()
    val element4_2 = FakeElement()
    val test4 = PropertySpec.builder("property4", String::class)
        .addOriginatingElement(element4_1)
        .addOriginatingElement(element4_2)
        .build()

    FileSpec.get("example", test1).writeTo(filer)
    FileSpec.get("example", test2).writeTo(filer)
    FileSpec.builder("example", "Test3")
        .addFunction(test3)
        .build()
        .writeTo(filer)
    FileSpec.builder("example", "Test4")
        .addProperty(test4)
        .build()
        .writeTo(filer)

    // Mixed
    FileSpec.builder("example", "Mixed")
        .addType(test1)
        .addType(test2)
        .addFunction(test3)
        .addProperty(test4)
        .build()
        .writeTo(filer)

    val testPath1 = fsRoot.resolve(fs.getPath("example", "Test1.kt"))
    assertThat(filer.getOriginatingElements(testPath1)).containsExactly(element1_1)
    val testPath2 = fsRoot.resolve(fs.getPath("example", "Test2.kt"))
    assertThat(filer.getOriginatingElements(testPath2)).containsExactly(element2_1, element2_2)
    val testPath3 = fsRoot.resolve(fs.getPath("example", "Test3.kt"))
    assertThat(filer.getOriginatingElements(testPath3)).containsExactly(element3_1, element3_2)
    val testPath4 = fsRoot.resolve(fs.getPath("example", "Test4.kt"))
    assertThat(filer.getOriginatingElements(testPath4)).containsExactly(element4_1, element4_2)

    val mixed = fsRoot.resolve(fs.getPath("example", "Mixed.kt"))
    assertThat(filer.getOriginatingElements(mixed)).containsExactly(
        element1_1,
        element2_1,
        element2_2,
        element3_1,
        element3_2,
        element4_1,
        element4_2
    )
  }

  @Test fun filerPassesOnlyUniqueOriginatingElements() {
    val element1 = FakeElement()
    val fun1 = FunSpec.builder("test1")
        .addOriginatingElement(element1)
        .build()

    val element2 = FakeElement()
    val fun2 = FunSpec.builder("test2")
        .addOriginatingElement(element1)
        .addOriginatingElement(element2)
        .build()

    FileSpec.builder("example", "File")
        .addFunction(fun1)
        .addFunction(fun2)
        .build()
        .writeTo(filer)

    val file = fsRoot.resolve(fs.getPath("example", "File.kt"))
    assertThat(filer.getOriginatingElements(file)).containsExactly(element1, element2)
  }

  @Test fun filerClassesWithTabIndent() {
    val test = TypeSpec.classBuilder("Test")
        .addProperty("madeFreshDate", Date::class)
        .addFunction(FunSpec.builder("main")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("args", Array<String>::class.java)
            .addCode("%T.out.println(%S);\n", System::class, "Hello World!")
            .build())
        .build()
    FileSpec.builder("foo", "Test")
        .addType(test)
        .indent("\t")
        .build()
        .writeTo(filer)

    val fooPath = fsRoot.resolve(fs.getPath("foo", "Test.kt"))
    assertThat(Files.exists(fooPath)).isTrue()
    val source = String(Files.readAllBytes(fooPath))

    assertThat(source).isEqualTo("""
        |package foo
        |
        |import java.lang.String
        |import java.lang.System
        |import java.util.Date
        |import kotlin.Array
        |import kotlin.Unit
        |
        |public class Test {
        |${"\t"}public val madeFreshDate: Date
        |
        |${"\t"}public fun main(args: Array<String>): Unit {
        |${"\t\t"}System.out.println("Hello World!");
        |${"\t"}}
        |}
        |""".trimMargin())
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
        |public class Taco
        |""".trimMargin())
  }

  @Test fun fileWithKeywordName() {
    val type = TypeSpec.classBuilder("fun").build()
    FileSpec.get("", type).writeTo(filer)

    val testPath = fsRoot.resolve("fun.kt")
    assertThat(Files.exists(testPath)).isTrue()
  }
}
