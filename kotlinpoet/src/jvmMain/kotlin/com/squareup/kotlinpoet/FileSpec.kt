/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import javax.annotation.processing.Filer
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation
import kotlin.DeprecationLevel.HIDDEN
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
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
public class FileSpec
private constructor(builder: Builder, private val tagMap: TagMap = builder.buildTagMap()) :
  Taggable by tagMap, Annotatable, TypeSpecHolder, MemberSpecHolder {
  override val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  override val typeSpecs: List<TypeSpec> =
    builder.members.filterIsInstance<TypeSpec>().toImmutableList()
  override val propertySpecs: List<PropertySpec> =
    builder.members.filterIsInstance<PropertySpec>().toImmutableList()
  override val funSpecs: List<FunSpec> =
    builder.members.filterIsInstance<FunSpec>().toImmutableList()
  public val comment: CodeBlock = builder.comment.build()
  public val packageName: String = builder.packageName
  public val name: String = builder.name
  public val members: List<Any> = builder.members.toList()
  public val defaultImports: Set<String> = builder.defaultImports.toSet()
  public val body: CodeBlock = builder.body.build()
  public val isScript: Boolean = builder.isScript
  private val memberImports = builder.memberImports.associateBy(Import::qualifiedName)
  private val indent = builder.indent
  private val extension = if (isScript) "kts" else "kt"

  /**
   * The relative path of the file which would be produced by a call to [writeTo]. This value always
   * uses unix-style path separators (`/`).
   */
  public val relativePath: String = buildString {
    for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
      append(packageComponent)
      append('/')
    }
    append(name)
    append('.')
    append(extension)
  }

  @Throws(IOException::class)
  public fun writeTo(out: Appendable) {
    val codeWriter =
      CodeWriter.withCollectedImports(
        out = out,
        indent = indent,
        memberImports = memberImports,
        emitStep = { importsCollector -> emit(importsCollector, collectingImports = true) },
      )
    emit(codeWriter, collectingImports = false)
    codeWriter.close()
  }

  @Deprecated("", level = HIDDEN)
  @JvmName("writeTo") // For binary compatibility.
  public fun oldWriteTo(directory: Path) {
    writeTo(directory)
  }

  /**
   * Writes this to [directory] as UTF-8 using the standard directory structure and returns the
   * newly output path.
   */
  @Throws(IOException::class)
  public fun writeTo(directory: Path): Path {
    require(directory.notExists() || directory.isDirectory()) {
      "path $directory exists but is not a directory."
    }
    val outputPath = directory.resolve(relativePath)
    outputPath.parent.createDirectories()
    outputPath.outputStream().bufferedWriter().use(::writeTo)
    return outputPath
  }

  @Deprecated("", level = HIDDEN)
  @JvmName("writeTo") // For binary compatibility.
  public fun oldWriteTo(directory: File) {
    writeTo(directory)
  }

  /**
   * Writes this to [directory] as UTF-8 using the standard directory structure and returns the
   * newly output file.
   */
  @Throws(IOException::class)
  public fun writeTo(directory: File): File = writeTo(directory.toPath()).toFile()

  /** Writes this to `filer`. */
  @Throws(IOException::class)
  public fun writeTo(filer: Filer) {
    val originatingElements =
      members
        .asSequence()
        .filterIsInstance<OriginatingElementsHolder>()
        .flatMap { it.originatingElements.asSequence() }
        .toSet()
    val filerSourceFile =
      filer.createResource(
        StandardLocation.SOURCE_OUTPUT,
        packageName,
        "$name.$extension",
        *originatingElements.toTypedArray(),
      )
    try {
      filerSourceFile.openWriter().use { writer -> writeTo(writer) }
    } catch (e: Exception) {
      try {
        filerSourceFile.delete()
      } catch (_: Exception) {}
      throw e
    }
  }

  private fun emit(codeWriter: CodeWriter, collectingImports: Boolean) {
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
      codeWriter.emitCode("package %L\n", escapedPackageName)
      codeWriter.emit("\n")
    }

    // If we don't have default imports or are collecting them, we don't need to filter
    var isDefaultImport: (String) -> Boolean = { false }
    if (!collectingImports && defaultImports.isNotEmpty()) {
      val defaultImports = defaultImports.map(String::escapeSegmentsIfNecessary)
      isDefaultImport = { importName -> importName.substringBeforeLast(".") in defaultImports }
    }
    // Aliased imports should always appear at the bottom of the imports list.
    val (aliasedImports, nonAliasedImports) =
      codeWriter.imports.values.partition { it.alias != null }
    val imports =
      nonAliasedImports
        .asSequence()
        .map { it.toString() }
        .filterNot(isDefaultImport)
        .toSortedSet()
        .plus(aliasedImports.map { it.toString() }.toSortedSet())

    if (imports.isNotEmpty()) {
      for (import in imports) {
        codeWriter.emitCode("import %L", import)
        codeWriter.emit("\n")
      }
      codeWriter.emit("\n")
    }

    if (isScript) {
      codeWriter.emitCode(body, omitImplicitModifiers = true)
    } else {
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
    val uri = URI.create(relativePath)
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
    val builder = Builder(packageName, name, isScript)
    builder.annotations.addAll(annotations)
    builder.comment.add(comment)
    builder.members.addAll(this.members)
    builder.indent = indent
    builder.memberImports.addAll(memberImports.values)
    builder.defaultImports.addAll(defaultImports)
    builder.tags += tagMap.tags
    builder.body.add(body)
    return builder
  }

  public class Builder
  internal constructor(
    public val packageName: String,
    public val name: String,
    public val isScript: Boolean,
  ) :
    Taggable.Builder<Builder>,
    Annotatable.Builder<Builder>,
    TypeSpecHolder.Builder<Builder>,
    MemberSpecHolder.Builder<Builder> {

    override val annotations: MutableList<AnnotationSpec> = mutableListOf()
    internal val comment = CodeBlock.builder()
    internal val memberImports = sortedSetOf<Import>()
    internal var indent = DEFAULT_INDENT
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()

    public val defaultImports: MutableSet<String> = mutableSetOf()
    public val imports: List<Import>
      get() = memberImports.toList()

    public val members: MutableList<Any> = mutableListOf()
    internal val body = CodeBlock.builder()

    /**
     * Add an annotation to the file.
     *
     * The annotation must either have a [`file` use-site target][AnnotationSpec.UseSiteTarget.FILE]
     * or not have a use-site target specified (in which case it will be changed to `file`).
     */
    override fun addAnnotation(annotationSpec: AnnotationSpec): Builder = apply {
      val spec =
        when (annotationSpec.useSiteTarget) {
          FILE -> annotationSpec
          null -> annotationSpec.toBuilder().useSiteTarget(FILE).build()
          else ->
            error(
              "Use-site target ${annotationSpec.useSiteTarget} not supported for file annotations."
            )
        }
      annotations += spec
    }

    /**
     * Adds a file-site comment. This is prefixed to the start of the file and different from
     * [addBodyComment].
     */
    public fun addFileComment(format: String, vararg args: Any): Builder = apply {
      comment.add(format, *args)
    }

    @Deprecated(
      "Use addFileComment() instead.",
      ReplaceWith("addFileComment(format, args)"),
      DeprecationLevel.ERROR,
    )
    public fun addComment(format: String, vararg args: Any): Builder = addFileComment(format, *args)

    public fun clearComment(): Builder = apply { comment.clear() }

    override fun addType(typeSpec: TypeSpec): Builder = apply {
      if (isScript) {
        body.add("%L", typeSpec)
      } else {
        members += typeSpec
      }
    }

    // region Overrides for binary compatibility
    @Suppress("RedundantOverride")
    override fun addTypes(typeSpecs: Iterable<TypeSpec>): Builder = super.addTypes(typeSpecs)

    // endregion

    override fun addFunction(funSpec: FunSpec): Builder = apply {
      require(!funSpec.isConstructor && !funSpec.isAccessor) {
        "cannot add ${funSpec.name} to file $name"
      }
      if (isScript) {
        body.add("%L", funSpec)
      } else {
        members += funSpec
      }
    }

    override fun addProperty(propertySpec: PropertySpec): Builder = apply {
      if (isScript) {
        body.add("%L", propertySpec)
      } else {
        members += propertySpec
      }
    }

    public fun addTypeAlias(typeAliasSpec: TypeAliasSpec): Builder = apply {
      if (isScript) {
        body.add("%L", typeAliasSpec)
      } else {
        members += typeAliasSpec
      }
    }

    public fun addImport(constant: Enum<*>): Builder =
      addImport(constant.declaringJavaClass.asClassName(), constant.name)

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

    public fun addImport(memberName: MemberName): Builder = apply {
      memberImports += Import(memberName.canonicalName)
    }

    public fun addImport(packageName: String, vararg names: String): Builder = apply {
      require(names.isNotEmpty()) { "names array is empty" }
      addImport(packageName, names.toList())
    }

    public fun addImport(packageName: String, names: Iterable<String>): Builder = apply {
      require("*" !in names) { "Wildcard imports are not allowed" }
      for (name in names) {
        memberImports +=
          if (packageName.isNotEmpty()) {
            Import("$packageName.$name")
          } else {
            Import(name)
          }
      }
    }

    public fun addImport(import: Import): Builder = apply { memberImports += import }

    public fun clearImports(): Builder = apply { memberImports.clear() }

    public fun addAliasedImport(`class`: Class<*>, `as`: String): Builder =
      addAliasedImport(`class`.asClassName(), `as`)

    public fun addAliasedImport(`class`: KClass<*>, `as`: String): Builder =
      addAliasedImport(`class`.asClassName(), `as`)

    public fun addAliasedImport(className: ClassName, `as`: String): Builder = apply {
      memberImports += Import(className.canonicalName, `as`)
    }

    public fun addAliasedImport(className: ClassName, memberName: String, `as`: String): Builder =
      apply {
        memberImports += Import("${className.canonicalName}.$memberName", `as`)
      }

    public fun addAliasedImport(memberName: MemberName, `as`: String): Builder = apply {
      memberImports += Import(memberName.canonicalName, `as`)
    }

    /**
     * Adds a default import for the given [packageName].
     *
     * The format of this should be the qualified name of the package, e.g. `kotlin`, `java.lang`,
     * `org.gradle.api`, etc.
     */
    public fun addDefaultPackageImport(packageName: String): Builder = apply {
      defaultImports += packageName
    }

    /**
     * Adds Kotlin's standard default package imports as described
     * [here](https://kotlinlang.org/docs/packages.html#default-imports).
     */
    public fun addKotlinDefaultImports(
      includeJvm: Boolean = true,
      includeJs: Boolean = true,
    ): Builder = apply {
      defaultImports += KOTLIN_DEFAULT_IMPORTS
      if (includeJvm) {
        defaultImports += KOTLIN_DEFAULT_JVM_IMPORTS
      }
      if (includeJs) {
        defaultImports += KOTLIN_DEFAULT_JS_IMPORTS
      }
    }

    public fun indent(indent: String): Builder = apply { this.indent = indent }

    public fun addCode(format: String, vararg args: Any?): Builder = apply {
      check(isScript) { "addCode() is only allowed in script files" }
      body.add(format, *args)
    }

    public fun addNamedCode(format: String, args: Map<String, *>): Builder = apply {
      check(isScript) { "addNamedCode() is only allowed in script files" }
      body.addNamed(format, args)
    }

    public fun addCode(codeBlock: CodeBlock): Builder = apply {
      check(isScript) { "addCode() is only allowed in script files" }
      body.add(codeBlock)
    }

    /** Adds a comment to the body of this script file in the order that it was added. */
    public fun addBodyComment(format: String, vararg args: Any): Builder = apply {
      check(isScript) { "addBodyComment() is only allowed in script files" }
      body.add("// $format\n", *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     *   Shouldn't contain braces or newline characters.
     */
    public fun beginControlFlow(controlFlow: String, vararg args: Any): Builder = apply {
      check(isScript) { "beginControlFlow() is only allowed in script files" }
      body.beginControlFlow(controlFlow, *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     *   Shouldn't contain braces or newline characters.
     */
    public fun nextControlFlow(controlFlow: String, vararg args: Any): Builder = apply {
      check(isScript) { "nextControlFlow() is only allowed in script files" }
      body.nextControlFlow(controlFlow, *args)
    }

    public fun endControlFlow(): Builder = apply {
      check(isScript) { "endControlFlow() is only allowed in script files" }
      body.endControlFlow()
    }

    public fun addStatement(format: String, vararg args: Any): Builder = apply {
      check(isScript) { "addStatement() is only allowed in script files" }
      body.addStatement(format, *args)
    }

    public fun clearBody(): Builder = apply {
      check(isScript) { "clearBody() is only allowed in script files" }
      body.clear()
    }

    // region Overrides for binary compatibility
    @Suppress("RedundantOverride")
    override fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder =
      super.addAnnotations(annotationSpecs)

    @Suppress("RedundantOverride")
    override fun addAnnotation(annotation: ClassName): Builder = super.addAnnotation(annotation)

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    override fun addAnnotation(annotation: Class<*>): Builder = super.addAnnotation(annotation)

    @Suppress("RedundantOverride")
    override fun addAnnotation(annotation: KClass<*>): Builder = super.addAnnotation(annotation)

    // endregion

    public fun build(): FileSpec {
      for (annotationSpec in annotations) {
        if (annotationSpec.useSiteTarget != FILE) {
          error(
            "Use-site target ${annotationSpec.useSiteTarget} not supported for file annotations."
          )
        }
      }
      return FileSpec(this)
    }
  }

  public companion object {
    @JvmStatic
    public fun get(packageName: String, typeSpec: TypeSpec): FileSpec {
      val fileName =
        typeSpec.name ?: throw IllegalArgumentException("file name required but type has no name")
      return builder(packageName, fileName).addType(typeSpec).build()
    }

    @JvmStatic
    public fun builder(className: ClassName): Builder {
      require(className.simpleNames.size == 1) {
        "nested types can't be used to name a file: ${className.simpleNames.joinToString(".")}"
      }
      return builder(className.packageName, className.simpleName)
    }

    @JvmStatic
    public fun builder(memberName: MemberName): Builder {
      return builder(memberName.packageName, memberName.simpleName)
    }

    @JvmStatic
    public fun builder(packageName: String, fileName: String): Builder =
      Builder(packageName, fileName, isScript = false)

    @JvmStatic
    public fun scriptBuilder(fileName: String, packageName: String = ""): Builder =
      Builder(packageName, fileName, isScript = true)
  }
}

internal const val DEFAULT_INDENT = "  "

private val KOTLIN_DEFAULT_IMPORTS =
  setOf(
    "kotlin",
    "kotlin.annotation",
    "kotlin.collections",
    "kotlin.comparisons",
    "kotlin.io",
    "kotlin.ranges",
    "kotlin.sequences",
    "kotlin.text",
  )
private val KOTLIN_DEFAULT_JVM_IMPORTS = setOf("java.lang")
private val KOTLIN_DEFAULT_JS_IMPORTS = setOf("kotlin.js")
