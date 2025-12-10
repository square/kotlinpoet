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

import java.io.Closeable
import kotlin.math.min

/** Sentinel value that indicates that no user-provided package has been set. */
private val NO_PACKAGE = String()

internal val NULLABLE_ANY = ANY.copy(nullable = true)

private const val ESCAPED_STAR = "&#42;"

private fun extractMemberName(part: String): String {
  require(Character.isJavaIdentifierStart(part[0])) { "not an identifier: $part" }
  for (i in 1..part.length) {
    if (!part.substring(0, i).isIdentifier) {
      return part.substring(0, i - 1)
    }
  }
  return part
}

internal inline fun buildCodeString(builderAction: CodeWriter.() -> Unit): String {
  val stringBuilder = StringBuilder()
  CodeWriter(stringBuilder, columnLimit = Integer.MAX_VALUE).use { it.builderAction() }
  return stringBuilder.toString()
}

internal fun buildCodeString(codeWriter: CodeWriter, builderAction: CodeWriter.() -> Unit): String {
  val stringBuilder = StringBuilder()
  codeWriter.emitInto(stringBuilder, builderAction)
  return stringBuilder.toString()
}

/**
 * Converts a [FileSpec] to a string suitable to both human- and kotlinc-consumption. This honors
 * imports, indentation, and deferred variable names.
 */
internal class CodeWriter(
  out: Appendable,
  private val indent: String = DEFAULT_INDENT,
  imports: Map<String, Import> = emptyMap(),
  private val importedTypes: Map<String, ClassName> = emptyMap(),
  private val importedMembers: Map<String, Set<MemberName>> = emptyMap(),
  columnLimit: Int = 100,
) : Closeable {
  private var out = LineWrapper(out, indent, columnLimit)
  private var indentLevel = 0

  private var kdoc = false
  private var comment = false
  private var packageName = NO_PACKAGE
  private val typeSpecStack = mutableListOf<TypeSpec>()
  private val memberImportNames = mutableSetOf<String>()
  private val importableTypes = mutableMapOf<String, List<ClassName>>().withDefault { emptyList() }
  private val importableMembers =
    mutableMapOf<String, List<MemberName>>().withDefault { emptyList() }
  private val referencedNames = mutableSetOf<String>()
  private var trailingNewline = false

  val imports =
    imports.also {
      for ((memberName, _) in imports) {
        val lastDotIndex = memberName.lastIndexOf('.')
        if (lastDotIndex >= 0) {
          memberImportNames.add(memberName.substring(0, lastDotIndex))
        }
      }
    }

  /**
   * When emitting a statement, this is the line of the statement currently being written. The first
   * line of a statement is indented normally and subsequent wrapped lines are double-indented. This
   * is -1 when the currently-written line isn't part of a statement.
   */
  var statementLine = -1

  fun indent(levels: Int = 1) = apply { indentLevel += levels }

  fun unindent(levels: Int = 1) = apply {
    require(indentLevel - levels >= 0) { "cannot unindent $levels from $indentLevel" }
    indentLevel -= levels
  }

  fun pushPackage(packageName: String) = apply {
    check(this.packageName === NO_PACKAGE) { "package already set: ${this.packageName}" }
    this.packageName = packageName
  }

  fun popPackage() = apply {
    check(packageName !== NO_PACKAGE) { "package already set: $packageName" }
    packageName = NO_PACKAGE
  }

  fun pushType(type: TypeSpec) = apply { this.typeSpecStack.add(type) }

  fun popType() = apply { this.typeSpecStack.removeAt(typeSpecStack.size - 1) }

  fun emitComment(codeBlock: CodeBlock) {
    trailingNewline = true // Force the '//' prefix for the comment.
    comment = true
    try {
      emitCode(codeBlock)
      emit("\n")
    } finally {
      comment = false
    }
  }

  fun emitKdoc(kdocCodeBlock: CodeBlock) {
    if (kdocCodeBlock.isEmpty()) return

    emit("/**\n")
    kdoc = true
    try {
      emitCode(kdocCodeBlock, ensureTrailingNewline = true)
    } finally {
      kdoc = false
    }
    emit(" */\n")
  }

  fun emitAnnotations(annotations: List<AnnotationSpec>, inline: Boolean) {
    for (annotationSpec in annotations) {
      annotationSpec.emit(this, inline)
      emit(if (inline) " " else "\n")
    }
  }

  /**
   * Emits `modifiers` in the standard order. Modifiers in `implicitModifiers` will not be emitted
   * except for [KModifier.PUBLIC]
   */
  fun emitModifiers(modifiers: Set<KModifier>, implicitModifiers: Set<KModifier> = emptySet()) {
    if (shouldEmitPublicModifier(modifiers, implicitModifiers)) {
      emit(KModifier.PUBLIC.keyword)
      emit(" ")
    }
    val uniqueNonPublicExplicitOnlyModifiers =
      modifiers
        .filterNot { it == KModifier.PUBLIC }
        .filterNot { implicitModifiers.contains(it) }
        .toEnumSet()
    for (modifier in uniqueNonPublicExplicitOnlyModifiers) {
      emit(modifier.keyword)
      emit(" ")
    }
  }

  /** Emits the `context` block for [contextReceivers]. */
  fun emitContextReceivers(contextReceivers: List<TypeName>, suffix: String = "") {
    if (contextReceivers.isNotEmpty()) {
      val receivers =
        contextReceivers
          .map { CodeBlock.of("%T", it) }
          .joinToCode(prefix = "context(", suffix = ")")
      emitCode(receivers)
      emit(suffix)
    }
  }

  /** Emits the `context` block for [contextParameters]. */
  fun emitContextParameters(contextParameters: List<ContextParameter>, suffix: String = "") {
    emitContextParameters(
      contextParameters.map { CodeBlock.of("%L: %T", it.name, it.type) },
      suffix,
    )
  }

  /** Emits the `context` block for [contextParameters]. */
  @JvmName("emitContextParametersFromTypeNames")
  fun emitContextParameters(contextParameters: List<TypeName>, suffix: String = "") {
    emitContextParameters(contextParameters.map { CodeBlock.of("%T", it) }, suffix)
  }

  @JvmName("emitContextParametersFromCodeBlocks")
  private fun emitContextParameters(contextParameters: List<CodeBlock>, suffix: String = "") {
    if (contextParameters.isNotEmpty()) {
      val parameters = contextParameters.joinToCode(prefix = "context(", suffix = ")")
      emitCode(parameters)
      emit(suffix)
    }
  }

  /**
   * Emit type variables with their bounds. If a type variable has more than a single bound - call
   * [emitWhereBlock] with same input to produce an additional `where` block.
   *
   * This should only be used when declaring type variables; everywhere else bounds are omitted.
   */
  fun emitTypeVariables(typeVariables: List<TypeVariableName>) {
    if (typeVariables.isEmpty()) return

    emit("<")
    typeVariables.forEachIndexed { index, typeVariable ->
      if (index > 0) emit(", ")
      if (typeVariable.variance != null) {
        emit("${typeVariable.variance.keyword} ")
      }
      if (typeVariable.isReified) {
        emit("reified ")
      }
      emitCode("%L", typeVariable.name)
      if (typeVariable.bounds.size == 1 && typeVariable.bounds[0] != NULLABLE_ANY) {
        emitCode(" : %T", typeVariable.bounds[0])
      }
    }
    emit(">")
  }

  /**
   * Emit a `where` block containing type bounds for each type variable that has at least two
   * bounds.
   */
  fun emitWhereBlock(typeVariables: List<TypeVariableName>) {
    if (typeVariables.isEmpty()) return

    var firstBound = true
    for (typeVariable in typeVariables) {
      if (typeVariable.bounds.size > 1) {
        for (bound in typeVariable.bounds) {
          if (!firstBound) emitCode(", ") else emitCode(" where ")
          emitCode("%L : %T", typeVariable.name, bound)
          firstBound = false
        }
      }
    }
  }

  fun emitCode(s: String) = emitCode(CodeBlock.of(s))

  fun emitCode(format: String, vararg args: Any?) = emitCode(CodeBlock.of(format, *args))

  fun emitCode(
    codeBlock: CodeBlock,
    isConstantContext: Boolean = false,
    ensureTrailingNewline: Boolean = false,
    omitImplicitModifiers: Boolean = false,
  ) = apply {
    var a = 0
    var deferredTypeName: ClassName? = null // used by "import static" logic
    val partIterator = codeBlock.formatParts.listIterator()
    while (partIterator.hasNext()) {
      when (val part = partIterator.next()) {
        "%L" -> emitLiteral(codeBlock.args[a++], isConstantContext, omitImplicitModifiers)

        "%N" -> emit(codeBlock.args[a++] as String)

        "%S" -> {
          val string = codeBlock.args[a++] as String?
          // Emit null as a literal null: no quotes.
          val literal =
            if (string != null) {
              stringLiteralWithQuotes(
                string,
                isInsideRawString = false,
                isConstantContext = isConstantContext,
              )
            } else {
              "null"
            }
          emit(literal, nonWrapping = true)
        }

        "%P" -> {
          val string =
            codeBlock.args[a++]?.let { arg ->
              if (arg is CodeBlock) {
                arg.toString(this@CodeWriter)
              } else {
                arg as String?
              }
            }
          // Emit null as a literal null: no quotes.
          val literal =
            if (string != null) {
              stringLiteralWithQuotes(
                string,
                isInsideRawString = true,
                isConstantContext = isConstantContext,
              )
            } else {
              "null"
            }
          emit(literal, nonWrapping = true)
        }

        "%T" -> {
          var typeName = codeBlock.args[a++] as TypeName
          if (typeName.isAnnotated) {
            typeName.emitAnnotations(this)
            typeName = typeName.copy(annotations = emptyList())
          }
          // defer "typeName.emit(this)" if next format part will be handled by the default case
          var defer = false
          if (typeName is ClassName && partIterator.hasNext()) {
            if (!codeBlock.formatParts[partIterator.nextIndex()].startsWith("%")) {
              val candidate = typeName
              if (candidate.canonicalName in memberImportNames) {
                check(deferredTypeName == null) { "pending type for static import?!" }
                deferredTypeName = candidate
                defer = true
              }
            }
          }
          if (!defer) {
            typeName.emit(this)
            typeName.emitNullable(this)
          }
        }

        "%M" -> {
          val memberName = codeBlock.args[a++] as MemberName
          memberName.emit(this)
        }

        "%%" -> emit("%")

        "⇥" -> indent()

        "⇤" -> unindent()

        "«" -> {
          check(statementLine == -1) {
            """
            |Can't open a new statement until the current statement is closed (opening « followed
            |by another « without a closing »).
            |Current code block:
            |- Format parts: ${codeBlock.formatParts.map(::escapeCharacterLiterals)}
            |- Arguments: ${codeBlock.args}
            |
            """
              .trimMargin()
          }
          statementLine = 0
        }

        "»" -> {
          check(statementLine != -1) {
            """
            |Can't close a statement that hasn't been opened (closing » is not preceded by an
            |opening «).
            |Current code block:
            |- Format parts: ${codeBlock.formatParts.map(::escapeCharacterLiterals)}
            |- Arguments: ${codeBlock.args}
            |
            """
              .trimMargin()
          }
          if (statementLine > 0) {
            unindent(2) // End a multi-line statement. Decrease the indentation level.
          }
          statementLine = -1
        }

        else -> {
          // Handle deferred type.
          var doBreak = false
          if (deferredTypeName != null) {
            if (part.startsWith(".")) {
              if (emitStaticImportMember(deferredTypeName.canonicalName, part)) {
                // Okay, static import hit and all was emitted, so clean-up and jump to next part.
                deferredTypeName = null
                doBreak = true
              }
            }
            if (!doBreak) {
              deferredTypeName!!.emit(this)
              deferredTypeName.emitNullable(this)
              deferredTypeName = null
            }
          }
          if (!doBreak) {
            emit(part)
          }
        }
      }
    }
    if (ensureTrailingNewline && out.hasPendingSegments) {
      emit("\n")
    }
  }

  private fun emitStaticImportMember(canonical: String, part: String): Boolean {
    val partWithoutLeadingDot = part.substring(1)
    if (partWithoutLeadingDot.isEmpty()) return false
    val first = partWithoutLeadingDot[0]
    if (!Character.isJavaIdentifierStart(first)) return false
    val explicit = imports[canonical + "." + extractMemberName(partWithoutLeadingDot)]
    if (explicit != null) {
      if (explicit.alias != null) {
        val memberName = extractMemberName(partWithoutLeadingDot)
        emit(partWithoutLeadingDot.replaceFirst(memberName, explicit.alias))
      } else {
        emit(partWithoutLeadingDot)
      }
      return true
    }
    return false
  }

  private fun emitLiteral(o: Any?, isConstantContext: Boolean, omitImplicitModifiers: Boolean) {
    when (o) {
      is TypeSpec -> o.emit(this, null)
      is AnnotationSpec -> o.emit(this, inline = true, asParameter = isConstantContext)
      is PropertySpec -> o.emit(this, emptySet())
      is FunSpec ->
        o.emit(
          codeWriter = this,
          enclosingName = null,
          implicitModifiers = if (omitImplicitModifiers) emptySet() else setOf(KModifier.PUBLIC),
          includeKdocTags = true,
        )

      is TypeAliasSpec -> o.emit(this)
      is CodeBlock -> emitCode(o, isConstantContext = isConstantContext)
      else -> emit(o.toString())
    }
  }

  /**
   * Returns the best name to identify `className` with in the current context. This uses the
   * available imports and the current scope to find the shortest name available. It does not honor
   * names visible due to inheritance.
   */
  fun lookupName(className: ClassName): String {
    // Find the shortest suffix of className that resolves to className. This uses both local type
    // names (so `Entry` in `Map` refers to `Map.Entry`). Also uses imports.
    var nameResolved = false
    var c: ClassName? = className
    while (c != null) {
      val alias = imports[c.canonicalName]?.alias
      val simpleName = alias ?: c.simpleName
      val resolved = resolve(simpleName)
      nameResolved = resolved != null

      // We don't care about nullability and type annotations here, as it's irrelevant for imports.
      if (resolved == c.copy(nullable = false, annotations = emptyList())) {
        if (alias == null) {
          referencedNames.add(className.topLevelClassName().simpleName)
        }
        val nestedClassNames =
          className.simpleNames
            .subList(c.simpleNames.size, className.simpleNames.size)
            .joinToString(".")
        return "$simpleName.$nestedClassNames"
      }
      c = c.enclosingClassName()
    }

    // If the name resolved but wasn't a match, we're stuck with the fully qualified name.
    if (nameResolved) {
      return className.canonicalName
    }

    // If the class is in the same package and there's no import alias for that class, we're done.
    if (packageName == className.packageName && imports[className.canonicalName]?.alias == null) {
      referencedNames.add(className.topLevelClassName().simpleName)
      return className.simpleNames.joinToString(".")
    }

    // We'll have to use the fully-qualified name. Mark the type as importable for a future pass.
    if (!kdoc) {
      importableType(className)
    }

    return className.canonicalName
  }

  fun lookupName(memberName: MemberName): String {
    val simpleName = imports[memberName.canonicalName]?.alias ?: memberName.simpleName
    // Match an imported member.
    val importedMembers = importedMembers[simpleName] ?: emptySet()
    val found = memberName in importedMembers
    if (found && !isMethodNameUsedInCurrentContext(simpleName)) {
      return simpleName
    } else if (importedMembers.isNotEmpty() && memberName.enclosingClassName != null) {
      val enclosingClassName = lookupName(memberName.enclosingClassName)
      return "$enclosingClassName.$simpleName"
    } else if (found) {
      return simpleName
    }

    // If the member is in the same package, we're done.
    if (packageName == memberName.packageName && memberName.enclosingClassName == null) {
      referencedNames.add(memberName.simpleName)
      return memberName.simpleName
    }

    // We'll have to use the fully-qualified name.
    // Mark the member as importable for a future pass unless the name clashes with
    // a method in the current context
    if (
      !kdoc && (memberName.isExtension || !isMethodNameUsedInCurrentContext(memberName.simpleName))
    ) {
      importableMember(memberName)
    }

    return memberName.canonicalName
  }

  // TODO(luqasn): also honor superclass members when resolving names.
  private fun isMethodNameUsedInCurrentContext(simpleName: String): Boolean {
    for (it in typeSpecStack.reversed()) {
      if (it.funSpecs.any { it.name == simpleName }) {
        return true
      }
      if (!it.modifiers.contains(KModifier.INNER)) {
        break
      }
    }
    return false
  }

  private fun importableType(className: ClassName) {
    val topLevelClassName = className.topLevelClassName()
    val alias = imports[className.canonicalName]?.alias
    val simpleName = alias ?: topLevelClassName.simpleName
    // Check for name clashes with members.
    if (simpleName !in importableMembers) {
      // Maintain the inner class name if the alias exists.
      val newImportTypes =
        if (alias == null) {
          topLevelClassName
        } else {
          className.copy(nullable = false) as ClassName
        }
      importableTypes[simpleName] = importableTypes.getValue(simpleName) + newImportTypes
    }
  }

  private fun importableMember(memberName: MemberName) {
    val simpleName = imports[memberName.canonicalName]?.alias ?: memberName.simpleName
    // Check for name clashes with types.
    if (memberName.isExtension || simpleName !in importableTypes) {
      importableMembers[simpleName] = importableMembers.getValue(simpleName) + memberName
    }
  }

  /**
   * Returns the class or enum value referenced by `simpleName`, using the current nesting context
   * and imports.
   */
  // TODO(jwilson): also honor superclass members when resolving names.
  private fun resolve(simpleName: String): ClassName? {
    // Match a child of the current (potentially nested) class.
    for (i in typeSpecStack.indices.reversed()) {
      val typeSpec = typeSpecStack[i]
      if (simpleName in typeSpec.nestedTypesSimpleNames) {
        return stackClassName(i, simpleName)
      }
    }

    if (typeSpecStack.size > 0) {
      val typeSpec = typeSpecStack[0]
      if (typeSpec.name == simpleName) {
        // Match the top-level class.
        return ClassName(packageName, simpleName)
      }
      if (typeSpec.isEnum && typeSpec.enumConstants.keys.contains(simpleName)) {
        // Match a top level enum value.
        // Enum values are not proper classes but can still be modeled using ClassName.
        return ClassName(packageName, typeSpec.name!!).nestedClass(simpleName)
      }
    }

    // Match an imported type.
    val importedType = importedTypes[simpleName]
    if (importedType != null) return importedType

    // No match.
    return null
  }

  /** Returns the class named `simpleName` when nested in the class at `stackDepth`. */
  private fun stackClassName(stackDepth: Int, simpleName: String): ClassName {
    var className = ClassName(packageName, typeSpecStack[0].name!!)
    for (i in 1..stackDepth) {
      className = className.nestedClass(typeSpecStack[i].name!!)
    }
    return className.nestedClass(simpleName)
  }

  /**
   * Emits `s` with indentation as required. It's important that all code that writes to
   * [CodeWriter.out] does it through here, since we emit indentation lazily in order to avoid
   * unnecessary trailing whitespace.
   */
  fun emit(s: String, nonWrapping: Boolean = false) = apply {
    val s =
      if (kdoc) {
        // Avoid potential unbalanced nested block comments
        s.replace("/*", "/$ESCAPED_STAR").replace("*/", "$ESCAPED_STAR/")
      } else {
        s
      }
    var first = true
    for (line in s.split('\n')) {
      // Emit a newline character. Make sure blank lines in KDoc & comments look good.
      if (!first) {
        if ((kdoc || comment) && trailingNewline) {
          emitIndentation()
          out.append(if (kdoc) " *" else "//")
        }
        out.newline()
        trailingNewline = true
        if (statementLine != -1) {
          if (statementLine == 0) {
            indent(2) // Begin multiple-line statement. Increase the indentation level.
          }
          statementLine++
        }
      }

      first = false
      if (line.isEmpty()) continue // Don't indent empty lines.

      // Emit indentation and comment prefix if necessary.
      if (trailingNewline) {
        emitIndentation()
        if (kdoc) {
          out.append(" * ")
        } else if (comment) {
          out.append("// ")
        }
      }

      if (nonWrapping) {
        out.appendNonWrapping(line)
      } else {
        out.append(
          line,
          indentLevel = if (kdoc) indentLevel else indentLevel + 2,
          linePrefix = if (kdoc) " * " else "",
        )
      }
      trailingNewline = false
    }
  }

  private fun emitIndentation() {
    for (j in 0..<indentLevel) {
      out.append(indent)
    }
  }

  /**
   * Returns whether a [KModifier.PUBLIC] should be emitted.
   *
   * If [modifiers] contains [KModifier.PUBLIC], this method always returns `true`.
   *
   * Otherwise, this will return `true` when [KModifier.PUBLIC] is one of the [implicitModifiers]
   * and there are no other opposing modifiers (like [KModifier.PROTECTED] etc.) supplied by the
   * consumer in [modifiers].
   */
  private fun shouldEmitPublicModifier(
    modifiers: Set<KModifier>,
    implicitModifiers: Set<KModifier>,
  ): Boolean {
    if (modifiers.contains(KModifier.PUBLIC)) {
      return true
    }

    if (implicitModifiers.contains(KModifier.PUBLIC) && modifiers.contains(KModifier.OVERRIDE)) {
      return false
    }

    if (!implicitModifiers.contains(KModifier.PUBLIC)) {
      return false
    }

    val hasOtherConsumerSpecifiedVisibility =
      modifiers.containsAnyOf(KModifier.PRIVATE, KModifier.INTERNAL, KModifier.PROTECTED)

    return !hasOtherConsumerSpecifiedVisibility
  }

  /**
   * Returns the types that should have been imported for this code. If there were any simple name
   * collisions, import aliases will be generated.
   */
  private fun suggestedTypeImports(): Map<String, Set<ClassName>> {
    return importableTypes.filterKeys { it !in referencedNames }.mapValues { it.value.toSet() }
  }

  /**
   * Returns the members that should have been imported for this code. If there were any simple name
   * collisions, import aliases will be generated.
   */
  private fun suggestedMemberImports(): Map<String, Set<MemberName>> {
    return importableMembers.mapValues { it.value.toSet() }
  }

  /**
   * Perform emitting actions on the current [CodeWriter] using a custom [Appendable]. The
   * [CodeWriter] will continue using the old [Appendable] after this method returns.
   */
  inline fun emitInto(out: Appendable, action: CodeWriter.() -> Unit) {
    val codeWrapper = this
    LineWrapper(out, indent = DEFAULT_INDENT, columnLimit = Int.MAX_VALUE).use { newOut ->
      val oldOut = codeWrapper.out
      codeWrapper.out = newOut
      @Suppress("UNUSED_EXPRESSION", "unused") action()
      codeWrapper.out = oldOut
    }
  }

  override fun close() {
    out.close()
  }

  companion object {
    /**
     * Makes a pass to collect imports by executing [emitStep], and returns an instance of
     * [CodeWriter] pre-initialized with collected imports.
     */
    fun withCollectedImports(
      out: Appendable,
      indent: String,
      memberImports: Map<String, Import>,
      emitStep: (importsCollector: CodeWriter) -> Unit,
    ): CodeWriter {
      // First pass: emit the entire class, just to collect the types we'll need to import.
      val importsCollector =
        CodeWriter(NullAppendable, indent, memberImports, columnLimit = Integer.MAX_VALUE)
      emitStep(importsCollector)
      val generatedImports = mutableMapOf<String, Import>()
      val importedTypes =
        importsCollector
          .suggestedTypeImports()
          .generateImports(
            generatedImports,
            computeCanonicalName = ClassName::canonicalName,
            capitalizeAliases = true,
            referencedNames = importsCollector.referencedNames,
          )
      val importedMembers =
        importsCollector
          .suggestedMemberImports()
          .generateImports(
            generatedImports,
            computeCanonicalName = MemberName::canonicalName,
            capitalizeAliases = false,
            referencedNames = importsCollector.referencedNames,
          )
      importsCollector.close()

      return CodeWriter(
        out = out,
        indent = indent,
        imports = memberImports + generatedImports.filterKeys { it !in memberImports },
        importedTypes = importedTypes.mapValues { it.value.single() },
        importedMembers = importedMembers,
      )
    }

    private fun <T> Map<String, Set<T>>.generateImports(
      generatedImports: MutableMap<String, Import>,
      computeCanonicalName: T.() -> String,
      capitalizeAliases: Boolean,
      referencedNames: Set<String>,
    ): Map<String, Set<T>> {
      val imported = mutableMapOf<String, Set<T>>()
      entries
        // Pre-sorting entries by the number of qualified names to ensure we first process simple
        // names
        // that don't require aliases (qualifiedNames.size == 1), that way we don't run into
        // conflicts when
        // a newly generated alias now clashes with a simple name that didn't originally need an
        // alias.
        .sortedBy { (_, qualifiedNames) -> qualifiedNames.size }
        .forEach { (simpleName, qualifiedNames) ->
          val canonicalNamesToQualifiedNames =
            qualifiedNames.associateBy { it.computeCanonicalName() }
          if (canonicalNamesToQualifiedNames.size == 1 && simpleName !in referencedNames) {
            val canonicalName = canonicalNamesToQualifiedNames.keys.single()
            generatedImports[canonicalName] = Import(canonicalName)

            // For types, qualifiedNames should consist of a single name, for which an import will
            // be generated. For
            // members, there can be more than one qualified name mapping to a single simple name,
            // e.g. overloaded
            // functions declared in the same package. In these cases, a single import will suffice
            // for all of them.
            imported[simpleName] = qualifiedNames
          } else {
            generateImportAliases(
                simpleName,
                canonicalNamesToQualifiedNames,
                capitalizeAliases,
                imported.keys,
              )
              .onEach { (a, qualifiedName) ->
                val alias = a.escapeAsAlias()
                val canonicalName = qualifiedName.computeCanonicalName()
                generatedImports[canonicalName] = Import(canonicalName, alias)

                imported[alias] = setOf(qualifiedName)
              }
          }
        }
      return imported
    }

    private fun <T> generateImportAliases(
      simpleName: String,
      canonicalNamesToQualifiedNames: Map<String, T>,
      capitalizeAliases: Boolean,
      imported: Set<String>,
    ): List<Pair<String, T>> {
      val canonicalNameSegmentsToQualifiedNames =
        canonicalNamesToQualifiedNames.mapKeys { (canonicalName, _) ->
          canonicalName
            .split('.')
            .dropLast(
              1
            ) // Last segment of the canonical name is the simple name, drop it to avoid repetition.
            .filter { it != "Companion" }
            .map { it.replaceFirstChar(Char::uppercaseChar) }
        }
      val aliasNames = mutableMapOf<String, T>()
      var segmentsToUse = 0
      // Iterate until we have unique aliases for all names.
      outer@ while (aliasNames.size != canonicalNamesToQualifiedNames.size) {
        segmentsToUse += 1
        aliasNames.clear()
        for ((segments, qualifiedName) in canonicalNameSegmentsToQualifiedNames) {
          val aliasPrefix =
            segments
              .takeLast(min(segmentsToUse, segments.size))
              .joinToString(separator = "")
              .replaceFirstChar { if (!capitalizeAliases) it.lowercaseChar() else it }
          val aliasSuffix = "_".repeat((segmentsToUse - segments.size).coerceAtLeast(0))
          val aliasName =
            aliasPrefix + simpleName.replaceFirstChar(Char::uppercaseChar) + aliasSuffix
          // If this name has already been imported (e.g. a regular import already exists with this
          // name),
          // continue trying with a greater number of segments.
          if (aliasName in imported) {
            aliasNames.clear()
            continue@outer
          }
          aliasNames[aliasName] = qualifiedName
        }
      }
      return aliasNames.toList()
    }
  }
}
