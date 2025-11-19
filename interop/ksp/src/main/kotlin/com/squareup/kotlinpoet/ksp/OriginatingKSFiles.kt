/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.kotlinpoet.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.Taggable
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.tag
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * A simple holder class for containing originating [KSFiles][KSFile], which are used by KSP to
 * inform its incremental processing.
 *
 * See [the docs](https://kotlinlang.org/docs/ksp-incremental.html) for more information.
 */
public interface OriginatingKSFiles {
  public val files: List<KSFile>
}

/** Returns this spec's originating [KSFiles][KSFile] for use with incremental processing. */
public fun TypeSpec.originatingKSFiles(): List<KSFile> = getKSFilesTag()

/** Returns this spec's originating [KSFiles][KSFile] for use with incremental processing. */
public fun FunSpec.originatingKSFiles(): List<KSFile> = getKSFilesTag()

/** Returns this spec's originating [KSFiles][KSFile] for use with incremental processing. */
public fun PropertySpec.originatingKSFiles(): List<KSFile> = getKSFilesTag()

/** Returns this spec's originating [KSFiles][KSFile] for use with incremental processing. */
public fun TypeAliasSpec.originatingKSFiles(): List<KSFile> = getKSFilesTag()

/**
 * Returns the list of all files added to the contained [TypeSpecs][TypeSpec],
 * [PropertySpecs][PropertySpec], [FunSpecs][FunSpec], or [TypeAliasSpecs][TypeAliasSpec] contained
 * in this spec.
 */
public fun FileSpec.originatingKSFiles(): List<KSFile> {
  return members
    .flatMap {
      when (it) {
        is FunSpec -> it.originatingKSFiles()
        is PropertySpec -> it.originatingKSFiles()
        is TypeSpec -> it.originatingKSFiles()
        is TypeAliasSpec -> it.originatingKSFiles()
        else -> emptyList()
      }
    }
    .distinct()
}

/** Adds the given [ksFile] to this builder's tags for use with [originatingKSFiles]. */
public fun TypeAliasSpec.Builder.addOriginatingKSFile(ksFile: KSFile): TypeAliasSpec.Builder =
  apply {
    getOrCreateKSFilesTag().add(ksFile)
  }

/** Adds the given [ksFile] to this builder's tags for use with [originatingKSFiles]. */
public fun PropertySpec.Builder.addOriginatingKSFile(ksFile: KSFile): PropertySpec.Builder = apply {
  getOrCreateKSFilesTag().add(ksFile)
}

/** Adds the given [ksFile] to this builder's tags for use with [originatingKSFiles]. */
public fun FunSpec.Builder.addOriginatingKSFile(ksFile: KSFile): FunSpec.Builder = apply {
  getOrCreateKSFilesTag().add(ksFile)
}

/** Adds the given [ksFile] to this builder's tags for use with [originatingKSFiles]. */
public fun TypeSpec.Builder.addOriginatingKSFile(ksFile: KSFile): TypeSpec.Builder = apply {
  getOrCreateKSFilesTag().add(ksFile)
}

/**
 * Writes this [FileSpec] to a given [codeGenerator] with the given [originatingKSFiles].
 *
 * Note that if none are specified, the [originatingKSFiles] argument defaults to using
 * [FileSpec.originatingKSFiles], which will automatically resolve any files added to the contained
 * declarations.
 *
 * See [the docs](https://github.com/google/ksp/blob/main/docs/incremental.md) for more information.
 *
 * @param codeGenerator the [CodeGenerator] to write to.
 * @param aggregating flag indicating if this is an aggregating symbol processor.
 * @see FileSpec.originatingKSFiles
 */
public fun FileSpec.writeTo(
  codeGenerator: CodeGenerator,
  aggregating: Boolean,
  originatingKSFiles: Iterable<KSFile> = originatingKSFiles(),
) {
  val dependencies = kspDependencies(aggregating, originatingKSFiles)
  writeTo(codeGenerator, dependencies)
}

/**
 * Writes this [FileSpec] to a given [codeGenerator] with the given [dependencies].
 *
 * See [the docs](https://github.com/google/ksp/blob/main/docs/incremental.md) for more information.
 *
 * @param codeGenerator the [CodeGenerator] to write to.
 * @param dependencies the [Dependencies] to create a new file with.
 * @see FileSpec.originatingKSFiles
 * @see kspDependencies
 */
public fun FileSpec.writeTo(codeGenerator: CodeGenerator, dependencies: Dependencies) {
  val file = codeGenerator.createNewFile(dependencies, packageName, name)
  // Don't use writeTo(file) because that tries to handle directories under the hood
  OutputStreamWriter(file, StandardCharsets.UTF_8).use(::writeTo)
}

/**
 * Returns a KSP [Dependencies] component of this [FileSpec] with the given [originatingKSFiles],
 * intended to be used in tandem with [writeTo].
 *
 * Note that if no [originatingKSFiles] are specified, the [originatingKSFiles] argument defaults to
 * using [FileSpec.originatingKSFiles], which will automatically resolve any files added to the
 * contained declarations.
 *
 * See [the docs](https://github.com/google/ksp/blob/main/docs/incremental.md) for more information.
 *
 * @param aggregating flag indicating if this is an aggregating symbol processor.
 * @see FileSpec.originatingKSFiles
 * @see FileSpec.writeTo
 */
public fun FileSpec.kspDependencies(
  aggregating: Boolean,
  originatingKSFiles: Iterable<KSFile> = originatingKSFiles(),
): Dependencies = Dependencies(aggregating, *originatingKSFiles.toList().toTypedArray())

/**
 * A mutable [OriginatingKSFiles] instance for use with KotlinPoet Builders via [Taggable.Builder].
 */
private interface MutableOriginatingKSFiles : OriginatingKSFiles {
  override val files: MutableList<KSFile>
}

private data class MutableOriginatingKSFilesImpl(
  override val files: MutableList<KSFile> = mutableListOf()
) : MutableOriginatingKSFiles

private fun Taggable.getKSFilesTag(): List<KSFile> {
  return tag<OriginatingKSFiles>()?.files.orEmpty()
}

private fun Taggable.Builder<*>.getOrCreateKSFilesTag(): MutableList<KSFile> {
  val holder =
    tags.getOrPut(OriginatingKSFiles::class, ::MutableOriginatingKSFilesImpl)
      as MutableOriginatingKSFiles
  return holder.files
}
