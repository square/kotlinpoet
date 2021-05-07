package com.squareup.kotlinpoet

import com.google.common.io.ByteStreams
import com.google.common.truth.Truth.assertThat
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import javax.tools.JavaFileObject.Kind
import kotlin.test.Test

class FileReadingTest {
  @Test fun javaFileObjectUri() {
    val type = TypeSpec.classBuilder("Test").build()
    assertThat(FileSpec.get("", type).toJavaFileObject().toUri())
      .isEqualTo(URI.create("Test.kt"))
    assertThat(FileSpec.get("foo", type).toJavaFileObject().toUri())
      .isEqualTo(URI.create("foo/Test.kt"))
    assertThat(FileSpec.get("com.example", type).toJavaFileObject().toUri())
      .isEqualTo(URI.create("com/example/Test.kt"))
  }

  @Test fun javaFileObjectKind() {
    val source = FileSpec.get("", TypeSpec.classBuilder("Test").build())
    assertThat(source.toJavaFileObject().kind).isEqualTo(Kind.SOURCE)
  }

  @Test fun javaFileObjectCharacterContent() {
    val type = TypeSpec.classBuilder("Test")
      .addKdoc("Pi\u00f1ata\u00a1")
      .addFunction(FunSpec.builder("fooBar").build())
      .build()
    val source = FileSpec.get("foo", type)
    val javaFileObject = source.toJavaFileObject()

    // We can never have encoding issues (everything is in process)
    assertThat(javaFileObject.getCharContent(true)).isEqualTo(source.toString())
    assertThat(javaFileObject.getCharContent(false)).isEqualTo(source.toString())
  }

  @Test fun javaFileObjectInputStreamIsUtf8() {
    val source = FileSpec.builder("foo", "Test")
      .addType(TypeSpec.classBuilder("Test").build())
      .addComment("Pi\u00f1ata\u00a1")
      .build()
    val bytes = ByteStreams.toByteArray(source.toJavaFileObject().openInputStream())

    // KotlinPoet always uses UTF-8.
    assertThat(bytes).isEqualTo(source.toString().toByteArray(UTF_8))
  }
}
