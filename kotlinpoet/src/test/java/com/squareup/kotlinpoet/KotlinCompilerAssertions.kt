/*
 * Copyright (C) 2019 Square, Inc.
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

import java.nio.file.Files
import com.google.common.truth.Truth.assertThat

object KotlinCompilerAssertions {
  fun assertKotlinCodeCompiles(fileSpec: FileSpec) {
      assertKotlinCodeCompiles(listOf(fileSpec))
  }

  fun assertKotlinCodeCompiles(fileSpecs: List<FileSpec>) {
    val sourceDir = Files.createTempDirectory(KotlinCompilerAssertions::class.java.getName()).toFile()
    sourceDir.deleteOnExit()
    fileSpecs.forEach {
        it.writeTo(sourceDir)
    }
    assertThat(KotlinCompiler.compile(sourceDir.toPath()).hasErrors()).isFalse()
  }
}
