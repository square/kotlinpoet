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

import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject
import kotlin.reflect.KClass

/**
 * A Kotlin file containing top level objects like classes, objects, functions, properties, and type
 * aliases.
 *
 * Items are output in the following order:
 * - Comment
 * - Annotations
 * - Package
 * - Imports
 * - Members
 */
class FileSpec private constructor(builder: FileSpec.Builder) {
  val annotations = builder.annotations.toImmutableList()
  val comment = builder.comment.build()
  val packageName = builder.packageName
  val name = builder.name
  val members = builder.members.toList()
  private val memberImports = builder.memberImports.associateBy(Import::qualifiedName)
  private val indent = builder.indent

  @Throws(IOException::class)
  fun writeTo(out: Appendable) {
    // First pass: emit the entire class, just to collect the types we'll need to import.
    val importsCollector = CodeWriter(NullAppendable, indent, memberImports)
    emit(importsCollector)
    val suggestedImports = importsCollector.suggestedImports()

    // Second pass: write the code, taking advantage of the imports.
    val codeWriter = CodeWriter(out, indent, memberImports, suggestedImports)
    emit(codeWriter)
  }

  /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
  @Throws(IOException::class)
  fun writeTo(directory: Path) {
    require(Files.notExists(directory) || Files.isDirectory(directory)) {
      "path $directory exists but is not a directory."
    }
    var outputDirectory = directory
    if (packageName.isNotEmpty()) {
      for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
        outputDirectory = outputDirectory.resolve(packageComponent)
      }
      Files.createDirectories(outputDirectory)
    }

    val outputPath = outputDirectory.resolve("$name.kt")
    OutputStreamWriter(Files.newOutputStream(outputPath), UTF_8).use { writer -> writeTo(writer) }
  }

  /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
  @Throws(IOException::class)
  fun writeTo(directory: File) = writeTo(directory.toPath())

  private fun emit(codeWriter: CodeWriter) {
    if (comment.isNotEmpty()) {
      codeWriter.emitComment(comment)
    }

    if (annotations.isNotEmpty()) {
      codeWriter.emitAnnotations(annotations, inline = false)
      codeWriter.emit("\n")
    }

    codeWriter.pushPackage(packageName)

    val escapedPackageName = packageName.split('.')
        .joinToString(".") { escapeIfKeyword(it) }

    if (escapedPackageName.isNotEmpty()) {
      codeWriter.emitCode("package %L\n", escapedPackageName)
      codeWriter.emit("\n")
    }

    val imports = codeWriter.importedTypes().values
        .map { it.canonicalName }
        .filterNot { it in memberImports.keys }
        .plus(memberImports.map { it.value.toString() })

    if (imports.isNotEmpty()) {
      for (className in imports.toSortedSet()) {
        codeWriter.emitCode("import %L", className)
        codeWriter.emit("\n")
      }
      codeWriter.emit("\n")
    }

    members.forEachIndexed { index, member ->
      if (index > 0) codeWriter.emit("\n")
      when (member) {
        is TypeSpec -> member.emit(codeWriter, null)
        is FunSpec -> member.emit(codeWriter, null, setOf(KModifier.PUBLIC))
        is PropertySpec -> member.emit(codeWriter, setOf(KModifier.PUBLIC))
        is TypeAliasSpec -> member.emit(codeWriter)
        else -> throw AssertionError()
      }
    }

    codeWriter.popPackage()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString { writeTo(this) }

  fun toJavaFileObject(): JavaFileObject {
    val uri = URI.create((if (packageName.isEmpty())
      name else
      packageName.replace('.', '/') + '/' + name) + ".kt")
    return object : SimpleJavaFileObject(uri, Kind.SOURCE) {
      private val lastModified = System.currentTimeMillis()
      override fun getCharContent(ignoreEncodingErrors: Boolean): String {
        return this@FileSpec.toString()
      }

      override fun openInputStream(): InputStream {
        return ByteArrayInputStream(getCharContent(true).toByteArray(UTF_8))
      }

      override fun getLastModified() = lastModified
    }
  }

  fun toBuilder(): Builder {
    val builder = Builder(packageName, name)
    builder.annotations.addAll(annotations)
    builder.comment.add(comment)
    builder.members.addAll(this.members)
    builder.indent = indent
    return builder
  }

  class Builder internal constructor(
    internal val packageName: String,
    internal val name: String
  ) {
    internal val annotations = mutableListOf<AnnotationSpec>()
    internal val comment = CodeBlock.builder()
    internal val memberImports = sortedSetOf<Import>()
    internal var indent = DEFAULT_INDENT
    internal val members = mutableListOf<Any>()

    init {
      require(name.isName) { "not a valid file name: $name" }
    }

    /**
     * Add an annotation to the file.
     *
     * The annotation must either have a [`file` use-site target][AnnotationSpec.UseSiteTarget.FILE]
     * or not have a use-site target specified (in which case it will be changed to `file`).
     */
    fun addAnnotation(annotationSpec: AnnotationSpec) = apply {
      when (annotationSpec.useSiteTarget) {
        FILE -> annotations += annotationSpec
        null -> annotations += annotationSpec.toBuilder().useSiteTarget(FILE).build()
        else -> error(
            "Use-site target ${annotationSpec.useSiteTarget} not supported for file annotations.")
      }
    }

    fun addAnnotation(annotation: ClassName)
        = addAnnotation(AnnotationSpec.builder(annotation).build())

    fun addAnnotation(annotation: Class<*>) = addAnnotation(annotation.asClassName())

    fun addAnnotation(annotation: KClass<*>) = addAnnotation(annotation.asClassName())

    fun addComment(format: String, vararg args: Any) = apply {
      comment.add(format, *args)
    }

    fun addType(typeSpec: TypeSpec) = apply {
      members += typeSpec
    }

    fun addFunction(funSpec: FunSpec) = apply {
      require(!funSpec.isConstructor && !funSpec.isAccessor) {
        "cannot add ${funSpec.name} to file $name"
      }
      members += funSpec
    }

    fun addProperty(propertySpec: PropertySpec) = apply {
      members += propertySpec
    }

    fun addTypeAlias(typeAliasSpec: TypeAliasSpec) = apply {
      members += typeAliasSpec
    }

    fun addStaticImport(constant: Enum<*>)
        = addStaticImport(
        (constant as java.lang.Enum<*>).getDeclaringClass().asClassName(), constant.name)

    fun addStaticImport(`class`: Class<*>, vararg names: String)
        = addStaticImport(`class`.asClassName(), *names)

    fun addStaticImport(`class`: KClass<*>, vararg names: String)
        = addStaticImport(`class`.asClassName(), *names)

    fun addStaticImport(className: ClassName, vararg names: String) = apply {
      require(names.isNotEmpty()) { "names array is empty" }
      require("*" !in names) { "Wildcard imports are not allowed" }
      for (name in names) {
        memberImports += Import(className.canonicalName + "." + name)
      }
    }

    fun addStaticImport(packageName: String, vararg names: String) = apply {
      require(names.isNotEmpty()) { "names array is empty" }
      require("*" !in names) { "Wildcard imports are not allowed" }
      for (name in names) {
        memberImports += Import(packageName + "." + name)
      }
    }

    fun addAliasedImport(`class`: Class<*>, `as`: String) =
        addAliasedImport(`class`.asClassName(), `as`)

    fun addAliasedImport(`class`: KClass<*>, `as`: String) =
        addAliasedImport(`class`.asClassName(), `as`)

    fun addAliasedImport(className: ClassName, `as`: String) = apply {
      memberImports += Import(className.canonicalName, `as`)
    }

    fun addAliasedImport(className: ClassName, memberName: String, `as`: String) = apply {
      memberImports += Import("${className.canonicalName}.$memberName", `as`)
    }

    fun indent(indent: String) = apply {
      this.indent = indent
    }

    fun build() = FileSpec(this)
  }

  companion object {
    @JvmStatic fun get(packageName: String, typeSpec: TypeSpec): FileSpec {
      val fileName = typeSpec.name
          ?: throw IllegalArgumentException("file name required but type has no name")
      return builder(packageName, fileName).addType(typeSpec).build()
    }

    @JvmStatic fun builder(packageName: String, fileName: String) = Builder(packageName, fileName)
  }
}

internal const val DEFAULT_INDENT = "    "
