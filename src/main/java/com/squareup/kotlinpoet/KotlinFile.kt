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
import javax.lang.model.SourceVersion
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject
import kotlin.reflect.KClass

/**
 * A Kotlin file containing top level objects like classes, objects, functions, properties, and type
 * aliases.
 */
class KotlinFile private constructor(builder: KotlinFile.Builder) {
  val fileAnnotations = builder.fileAnnotations.toImmutableList()
  val fileComment = builder.fileComment.build()
  val packageName = builder.packageName
  val fileName = builder.fileName
  val members = builder.members.toList()
  val skipJavaLangImports = builder.skipJavaLangImports
  private val memberImports = builder.memberImports.toImmutableSet()
  private val indent = builder.indent

  @Throws(IOException::class)
  fun writeTo(out: Appendable) {
    // First pass: emit the entire class, just to collect the types we'll need to import.
    val importsCollector = CodeWriter(NULL_APPENDABLE, indent, memberImports)
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

    val outputPath = outputDirectory.resolve("$fileName.kt")
    OutputStreamWriter(Files.newOutputStream(outputPath), UTF_8).use { writer -> writeTo(writer) }
  }

  /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
  @Throws(IOException::class)
  fun writeTo(directory: File) = writeTo(directory.toPath())

  @Throws(IOException::class)
  private fun emit(codeWriter: CodeWriter) {
    if (fileAnnotations.isNotEmpty()) {
      codeWriter.emitAnnotations(fileAnnotations, inline = false)
      codeWriter.emit("\n")
    }

    codeWriter.pushPackage(packageName)

    if (!fileComment.isEmpty()) {
      codeWriter.emitComment(fileComment)
    }

    if (packageName.isNotEmpty()) {
      codeWriter.emitCode("package %L\n", packageName)
      codeWriter.emit("\n")
    }

    val imports = codeWriter.importedTypes().values
        .filterNot { skipJavaLangImports && it.packageName() == "java.lang" }
        .map { it.canonicalName }
        .plus(memberImports)

    var importedTypesCount = 0
    for (className in imports.toSortedSet()) {
      codeWriter.emitCode("import %L\n", className)
      importedTypesCount++
    }
    if (importedTypesCount > 0) {
      codeWriter.emit("\n")
    }

    for ((i, member) in members.withIndex()) {
      if (i > 0) codeWriter.emit("\n")
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

  override fun toString(): String {
    try {
      val result = StringBuilder()
      writeTo(result)
      return result.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }
  }

  fun toJavaFileObject(): JavaFileObject {
    val uri = URI.create((if (packageName.isEmpty())
      fileName else
      packageName.replace('.', '/') + '/' + fileName) + ".kt")
    return object : SimpleJavaFileObject(uri, Kind.SOURCE) {
      private val lastModified = System.currentTimeMillis()
      override fun getCharContent(ignoreEncodingErrors: Boolean): String {
        return this@KotlinFile.toString()
      }

      @Throws(IOException::class)
      override fun openInputStream(): InputStream {
        return ByteArrayInputStream(getCharContent(true).toByteArray(UTF_8))
      }

      override fun getLastModified(): Long {
        return lastModified
      }
    }
  }

  fun toBuilder(): Builder {
    val builder = Builder(packageName, fileName)
    builder.fileAnnotations.addAll(fileAnnotations)
    builder.fileComment.add(fileComment)
    builder.members.addAll(this.members)
    builder.skipJavaLangImports = skipJavaLangImports
    builder.indent = indent
    return builder
  }

  class Builder internal constructor(
      internal val packageName: String,
      internal val fileName: String) {
    internal val fileAnnotations = mutableListOf<AnnotationSpec>()
    internal val fileComment = CodeBlock.builder()
    internal val memberImports = sortedSetOf<String>()
    internal var skipJavaLangImports: Boolean = false
    internal var indent = "  "
    internal val members = mutableListOf<Any>()

    init {
      require(SourceVersion.isName(fileName)) { "not a valid file name: $fileName" }
    }

    fun addFileAnnotation(annotationSpec: AnnotationSpec) = apply {
      when (annotationSpec.useSiteTarget) {
        FILE -> fileAnnotations += annotationSpec
        null -> fileAnnotations += annotationSpec.toBuilder().useSiteTarget(FILE).build()
        else -> error(
            "Use-site target ${annotationSpec.useSiteTarget} not supported for file annotations.")
      }
    }

    fun addFileAnnotation(annotation: ClassName)
        = addFileAnnotation(AnnotationSpec.builder(annotation).build())

    fun addFileAnnotation(annotation: Class<*>) =
        addFileAnnotation(annotation.asClassName())

    fun addFileAnnotation(annotation: KClass<*>) =
        addFileAnnotation(annotation.asClassName())

    fun addFileComment(format: String, vararg args: Any) = apply {
      this.fileComment.add(format, *args)
    }

    fun addType(typeSpec: TypeSpec) = apply {
      members += typeSpec
    }

    fun addFun(funSpec: FunSpec) = apply {
      require(!funSpec.isConstructor && !funSpec.isAccessor) {
        "cannot add ${funSpec.name} to file $fileName"
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
      check(names.isNotEmpty()) { "names array is empty" }
      for (name in names) {
        memberImports += className.canonicalName + "." + name
      }
    }

    fun addStaticImport(packageName: String, vararg names: String) = apply {
      check(names.isNotEmpty()) { "names array is empty" }
      for (name in names) {
        memberImports += packageName + "." + name
      }
    }

    /**
     * Call this to omit imports for classes in `java.lang`, such as `java.lang.String`.
     *
     * By default, JavaPoet explicitly imports types in `java.lang` to defend against naming
     * conflicts. Suppose an (ill-advised) class is named `com.example.String`. When `java.lang`
     * imports are skipped, generated code in `com.example` that references `java.lang.String` will
     * get `com.example.String` instead.
     */
    fun skipJavaLangImports(skipJavaLangImports: Boolean) = apply {
      this.skipJavaLangImports = skipJavaLangImports
    }

    fun indent(indent: String) = apply {
      this.indent = indent
    }

    fun build() = KotlinFile(this)
  }

  companion object {
    @JvmStatic fun get(packageName: String, typeSpec: TypeSpec): KotlinFile {
      val fileName = typeSpec.name
          ?: throw IllegalArgumentException("file name required but type has no name")
      return builder(packageName, fileName).addType(typeSpec).build()
    }

    @JvmStatic fun builder(packageName: String, fileName: String) = Builder(packageName, fileName)
  }
}
