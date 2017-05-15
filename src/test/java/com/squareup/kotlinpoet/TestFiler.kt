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

import java.io.OutputStream
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import javax.annotation.processing.Filer
import javax.lang.model.element.Element
import javax.tools.FileObject
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

internal class TestFiler(
    fileSystem: FileSystem,
    private val fileSystemRoot: Path) : Filer {

  internal inner class Source(private val path: Path)
    : SimpleJavaFileObject(path.toUri(), JavaFileObject.Kind.SOURCE) {
    override fun openOutputStream(): OutputStream {
      val parent = path.parent
      if (!Files.exists(parent)) fileSystemProvider.createDirectory(parent)
      return fileSystemProvider.newOutputStream(path)
    }
  }

  private val separator = fileSystem.separator
  private val fileSystemProvider = fileSystem.provider()
  private val originatingElementsMap = mutableMapOf<Path, Set<Element>>()

  fun getOriginatingElements(path: Path) = originatingElementsMap[path]!!

  override fun createSourceFile(
      name: CharSequence, vararg originatingElements: Element): JavaFileObject {
    val relative = name.toString().replace(".", separator) + ".java" // Assumes well-formed.
    val path = fileSystemRoot.resolve(relative)
    originatingElementsMap.put(path, originatingElements.toSet())
    return Source(path)
  }

  override fun createClassFile(name: CharSequence, vararg originatingElements: Element)
      = throw UnsupportedOperationException("Not implemented.")

  override fun createResource(location: JavaFileManager.Location, pkg: CharSequence,
      relativeName: CharSequence, vararg originatingElements: Element): FileObject {
    val relative = pkg.toString().replace(".", separator) + separator + relativeName
    val path = fileSystemRoot.resolve(relative)
    originatingElementsMap.put(path, originatingElements.toSet())
    return Source(path)
  }

  override fun getResource(
      location: JavaFileManager.Location, pkg: CharSequence, relativeName: CharSequence)
      = throw UnsupportedOperationException("Not implemented.")
}
