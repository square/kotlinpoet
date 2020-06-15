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
import javax.annotation.processing.Filer
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation
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
public class FileSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap()
) : Taggable by tagMap {
  public val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  public val comment: CodeBlock = builder.comment.build()
  public val packageName: String = builder.packageName
  public val name: String = builder.name
  public val members: List<Any> = builder.members.toList()
  private val memberImports = builder.memberImports.associateBy(Import::qualifiedName)
  private val indent = builder.indent

  @Throws(IOException::class)
  public fun writeTo(out: Appendable) {
    // First pass: emit the entire class, just to collect the types we'll need to import.
    val importsCollector = CodeWriter(NullAppendable, indent, memberImports,
        columnLimit = Integer.MAX_VALUE)
    emit(importsCollector)
    val suggestedTypeImports = importsCollector.suggestedTypeImports()
    val suggestedMemberImports = importsCollector.suggestedMemberImports()
    importsCollector.close()

    // Second pass: write the code, taking advantage of the imports.
    val codeWriter = CodeWriter(out, indent, memberImports, suggestedTypeImports,
        suggestedMemberImports)
    emit(codeWriter)
    codeWriter.close()
  }

  /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
  @Throws(IOException::class)
  public fun writeTo(directory: Path) {
    require(Files.notExists(directory) || Files.isDirectory(directory)) {
      "path $directory exists but is not a directory."
    }
    var outputDirectory = directory
    if (packageName.isNotEmpty()) {
      for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
        outputDirectory = outputDirectory.resolve(packageComponent)
      }
    }

    Files.createDirectories(outputDirectory)

    val outputPath = outputDirectory.resolve("$name.kt")
    OutputStreamWriter(Files.newOutputStream(outputPath), UTF_8).use { writer -> writeTo(writer) }
  }

  /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
  @Throws(IOException::class)
  public fun writeTo(directory: File): Unit = writeTo(directory.toPath())

  /** Writes this to `filer`.  */
  @Throws(IOException::class)
  public fun writeTo(filer: Filer) {
    val originatingElements = members.asSequence()
        .filterIsInstance<OriginatingElementsHolder>()
        .flatMap { it.originatingElements.asSequence() }
        .toSet()
    val filerSourceFile = filer.createResource(StandardLocation.SOURCE_OUTPUT,
        packageName,
        "$name.kt",
        *originatingElements.toTypedArray()
    )
    try {
      filerSourceFile.openWriter().use { writer -> writeTo(writer) }
    } catch (e: Exception) {
      try {
        filerSourceFile.delete()
      } catch (ignored: Exception) {
      }
      throw e
    }
  }

  private fun emit(codeWriter: CodeWriter) {
    if (comment.isNotEmpty()) {
      codeWriter.emitComment(comment)
    }

    if (annotations.isNotEmpty()) {
      codeWriter.emitAnnotations(annotations, inline = false)
      codeWriter.emit("\n")
    }

    codeWriter.pushPackage(packageName)

    val escapedPackageName = packageName.escapeSegmentsIfNecessary()

    if (escapedPackageName.isNotEmpty()) {
      codeWriter.emitCode("package·%L\n", escapedPackageName)
      codeWriter.emit("\n")
    }

    val importedTypeNames = codeWriter.importedTypes.values.map { it.canonicalName }
    val importedMemberNames = codeWriter.importedMembers.values.map { it.canonicalName }
    // Aliased imports should always appear at the bottom of the imports list.
    val (aliasedImports, nonAliasedImports) = memberImports.values.partition { it.alias != null }
    val imports = (importedTypeNames + importedMemberNames)
        .filterNot { it in memberImports.keys }
        .map { it.escapeSegmentsIfNecessary() }
        .plus(nonAliasedImports.map { it.toString() })
        .toSortedSet()
        .plus(aliasedImports.map { it.toString() }.toSortedSet())

    if (imports.isNotEmpty()) {
      for (import in imports) {
        codeWriter.emitCode("import·%L", import)
        codeWriter.emit("\n")
      }
      codeWriter.emit("\n")
    }

    members.forEachIndexed { index, member ->
      if (index > 0) codeWriter.emit("\n")
      when (member) {
        is TypeSpec -> member.emit(codeWriter, null)
        is FunSpec -> member.emit(codeWriter, null, setOf(KModifier.PUBLIC), true)
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

  override fun hashCode(): Int = toString().hashCode()

  override fun toString(): String = buildString { writeTo(this) }

  public fun toJavaFileObject(): JavaFileObject {
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

  @JvmOverloads
  public fun toBuilder(packageName: String = this.packageName, name: String = this.name): Builder {
    val builder = Builder(packageName, name)
    builder.annotations.addAll(annotations)
    builder.comment.add(comment)
    builder.members.addAll(this.members)
    builder.indent = indent
    builder.memberImports.addAll(memberImports.values)
    builder.tags += tagMap.tags
    return builder
  }

  public class Builder internal constructor(
    public val packageName: String,
    public val name: String
  ) : Taggable.Builder<Builder> {
    internal val comment = CodeBlock.builder()
    internal val memberImports = sortedSetOf<Import>()
    internal var indent = DEFAULT_INDENT
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()

    public val imports: List<Import> get() = memberImports.toList()
    public val members: MutableList<Any> = mutableListOf()
    public val annotations: MutableList<AnnotationSpec> = mutableListOf()

    /**
     * Add an annotation to the file.
     *
     * The annotation must either have a [`file` use-site target][AnnotationSpec.UseSiteTarget.FILE]
     * or not have a use-site target specified (in which case it will be changed to `file`).
     */
    public fun addAnnotation(annotationSpec: AnnotationSpec): Builder = apply {
      annotations += when (annotationSpec.useSiteTarget) {
        FILE -> annotationSpec
        null -> annotationSpec.toBuilder().useSiteTarget(FILE).build()
        else -> error(
            "Use-site target ${annotationSpec.useSiteTarget} not supported for file annotations.")
      }
    }

    public fun addAnnotation(annotation: ClassName): Builder =
        addAnnotation(AnnotationSpec.builder(annotation).build())

    public fun addAnnotation(annotation: Class<*>): Builder =
        addAnnotation(annotation.asClassName())

    public fun addAnnotation(annotation: KClass<*>): Builder =
        addAnnotation(annotation.asClassName())

    public fun addComment(format: String, vararg args: Any): Builder = apply {
      comment.add(format.replace(' ', '·'), *args)
    }

    public fun clearComment(): Builder = apply {
      comment.clear()
    }

    public fun addType(typeSpec: TypeSpec): Builder = apply {
      members += typeSpec
    }

    public fun addFunction(funSpec: FunSpec): Builder = apply {
      require(!funSpec.isConstructor && !funSpec.isAccessor) {
        "cannot add ${funSpec.name} to file $name"
      }
      members += funSpec
    }

    public fun addProperty(propertySpec: PropertySpec): Builder = apply {
      members += propertySpec
    }

    public fun addTypeAlias(typeAliasSpec: TypeAliasSpec): Builder = apply {
      members += typeAliasSpec
    }

    public fun addImport(constant: Enum<*>): Builder = addImport(
        (constant as java.lang.Enum<*>).getDeclaringClass().asClassName(), constant.name)

    public fun addImport(`class`: Class<*>, vararg names: String): Builder = apply {
      require(names.isNotEmpty()) { "names array is empty" }
      addImport(`class`.asClassName(), names.toList())
    }

    public fun addImport(`class`: KClass<*>, vararg names: String): Builder = apply {
      require(names.isNotEmpty()) { "names array is empty" }
      addImport(`class`.asClassName(), names.toList())
    }

    public fun addImport(className: ClassName, vararg names: String): Builder = apply {
      require(names.isNotEmpty()) { "names array is empty" }
      addImport(className, names.toList())
    }

    public fun addImport(`class`: Class<*>, names: Iterable<String>): Builder =
        addImport(`class`.asClassName(), names)

    public fun addImport(`class`: KClass<*>, names: Iterable<String>): Builder =
        addImport(`class`.asClassName(), names)

    public fun addImport(className: ClassName, names: Iterable<String>): Builder = apply {
      require("*" !in names) { "Wildcard imports are not allowed" }
      for (name in names) {
        memberImports += Import(className.canonicalName + "." + name)
      }
    }

    public fun addImport(packageName: String, vararg names: String): Builder = apply {
      require(names.isNotEmpty()) { "names array is empty" }
      addImport(packageName, names.toList())
    }

    public fun addImport(packageName: String, names: Iterable<String>): Builder = apply {
      require("*" !in names) { "Wildcard imports are not allowed" }
      for (name in names) {
        memberImports += if (packageName.isNotEmpty()) {
          Import("$packageName.$name")
        } else {
          Import(name)
        }
      }
    }

    public fun addImport(import: Import): Builder = apply {
      memberImports += import
    }

    public fun clearImports(): Builder = apply {
      memberImports.clear()
    }

    public fun addAliasedImport(`class`: Class<*>, `as`: String): Builder =
        addAliasedImport(`class`.asClassName(), `as`)

    public fun addAliasedImport(`class`: KClass<*>, `as`: String): Builder =
        addAliasedImport(`class`.asClassName(), `as`)

    public fun addAliasedImport(className: ClassName, `as`: String): Builder = apply {
      memberImports += Import(className.canonicalName, `as`)
    }

    public fun addAliasedImport(
      className: ClassName,
      memberName: String,
      `as`: String
    ): Builder = apply {
      memberImports += Import("${className.canonicalName}.$memberName", `as`)
    }

    public fun addAliasedImport(memberName: MemberName, `as`: String): Builder = apply {
      memberImports += Import(memberName.canonicalName, `as`)
    }

    public fun indent(indent: String): Builder = apply {
      this.indent = indent
    }

    public fun build(): FileSpec {
      for (annotationSpec in annotations) {
        if (annotationSpec.useSiteTarget != FILE) {
          error(
              "Use-site target ${annotationSpec.useSiteTarget} not supported for file annotations.")
        }
      }
      return FileSpec(this)
    }
  }

  public companion object {
    @JvmStatic public fun get(packageName: String, typeSpec: TypeSpec): FileSpec {
      val fileName = typeSpec.name
          ?: throw IllegalArgumentException("file name required but type has no name")
      return builder(packageName, fileName).addType(typeSpec).build()
    }

    @JvmStatic public fun builder(packageName: String, fileName: String): Builder =
        Builder(packageName, fileName)
  }
}

internal const val DEFAULT_INDENT = "  "
