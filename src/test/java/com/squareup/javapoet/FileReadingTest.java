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
package com.squareup.javapoet;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.URI;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

@RunWith(JUnit4.class)
public class FileReadingTest {
  @Test public void javaFileObjectUri() {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    assertThat(KotlinFile.builder("", type).build().toJavaFileObject().toUri())
        .isEqualTo(URI.create("Test.java"));
    assertThat(KotlinFile.builder("foo", type).build().toJavaFileObject().toUri())
        .isEqualTo(URI.create("foo/Test.java"));
    assertThat(KotlinFile.builder("com.example", type).build().toJavaFileObject().toUri())
        .isEqualTo(URI.create("com/example/Test.java"));
  }

  @Test public void javaFileObjectKind() {
    KotlinFile kotlinFile = KotlinFile.builder("", TypeSpec.classBuilder("Test").build()).build();
    assertThat(kotlinFile.toJavaFileObject().getKind()).isEqualTo(Kind.SOURCE);
  }

  @Test public void javaFileObjectCharacterContent() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test")
        .addJavadoc("Pi\u00f1ata\u00a1")
        .addMethod(MethodSpec.methodBuilder("fooBar").build())
        .build();
    KotlinFile kotlinFile = KotlinFile.builder("foo", type).build();
    JavaFileObject javaFileObject = kotlinFile.toJavaFileObject();

    // We can never have encoding issues (everything is in process)
    assertThat(javaFileObject.getCharContent(true)).isEqualTo(kotlinFile.toString());
    assertThat(javaFileObject.getCharContent(false)).isEqualTo(kotlinFile.toString());
  }

  @Test public void javaFileObjectInputStreamIsUtf8() throws IOException {
    KotlinFile kotlinFile = KotlinFile.builder("foo", TypeSpec.classBuilder("Test").build())
        .addFileComment("Pi\u00f1ata\u00a1")
        .build();
    byte[] bytes = ByteStreams.toByteArray(kotlinFile.toJavaFileObject().openInputStream());

    // JavaPoet always uses UTF-8.
    assertThat(bytes).isEqualTo(kotlinFile.toString().getBytes(UTF_8));
  }
}
