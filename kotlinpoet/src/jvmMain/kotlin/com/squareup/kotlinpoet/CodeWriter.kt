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

/** Sentinel value that indicates that no user-provided package has been set.  */
private val NO_PACKAGE = String()

internal val NULLABLE_ANY = ANY.copy(nullable = true)

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
  CodeWriter(stringBuilder, columnLimit = Integer.MAX_VALUE).use {
    it.builderAction()
  }
  return stringBuilder.toString()
}

internal fun buildCodeString(
  codeWriter: CodeWriter,
  builderAction: CodeWriter.() -> Unit,
): String {
  val stringBuilder = StringBuilder()
  codeWriter.emitInto(stringBuilder, builderAction)
  return stringBuilder.toString()
}

/**
 * Converts a [FileSpec] to a string suitable to both human- and kotlinc-consumption. This honors
 * imports, indentation, and deferred variable names.
 */
internal class CodeWriter constructor(
  out: Appendable,
  private val indent: String = DEFAULT_INDENT,
  imports: Map<String, Import> = emptyMap(),
  private val importedTypes: Map<String, ClassName> = emptyMap(),
  private val importedMembers: Map<String, MemberName> = emptyMap(),
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
  private val importableMembers = mutableMapOf<String, List<MemberName>>().withDefault { emptyList() }
  private val referencedNames = mutableSetOf<String>()
  private var trailingNewline = false

  val imports = imports.also {
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

  fun indent(levels: Int = 1) = apply {
    indentLevel += levels
  }

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

  fun pushType(type: TypeSpec) = apply {
    this.typeSpecStack.add(type)
  }

  fun popType() = apply {
    this.typeSpecStack.removeAt(typeSpecStack.size - 1)
  }

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
   * Emits `modifiers` in the standard order. Modifiers in `implicitModifiers` will not
   * be emitted except for [KModifier.PUBLIC]
   */
  fun emitModifiers(
    modifiers: Set<KModifier>,
    implicitModifiers: Set<KModifier> = emptySet(),
  ) {
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

  /**
   * Emits the `context` block for [contextReceivers].
   */
  fun emitContextReceivers(contextReceivers: List<TypeName>, suffix: String = "") {
    if (contextReceivers.isNotEmpty()) {
      val receivers = contextReceivers
        .map { CodeBlock.of("%T", it) }
        .joinToCode(prefix = "context(", suffix = ")")
      emitCode(receivers)
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
  ) = apply {
    var a = 0
    var deferredTypeName: ClassName? = null // used by "import static" logic
    val partIterator = codeBlock.formatParts.listIterator()
    while (partIterator.hasNext()) {
      when (val part = partIterator.next()) {
        "%L" -> emitLiteral(codeBlock.args[a++], isConstantContext)

        "%N" -> emit(codeBlock.args[a++] as String)

        "%S" -> {
          val string = codeBlock.args[a++] as String?
          // Emit null as a literal null: no quotes.
          val literal = if (string != null) {
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
          val string = codeBlock.args[a++]?.let { arg ->
            if (arg is CodeBlock) {
              arg.toString(this@CodeWriter)
            } else {
              arg as String?
            }
          }
          // Emit null as a literal null: no quotes.
          val literal = if (string != null) {
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
          if (!defer) typeName.emit(this)
          typeName.emitNullable(this)
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
            """.trimMargin()
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
            """.trimMargin()
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

  private fun emitLiteral(o: Any?, isConstantContext: Boolean) {
    when (o) {
      is TypeSpec -> o.emit(this, null)
      is AnnotationSpec -> o.emit(this, inline = true, asParameter = isConstantContext)
      is PropertySpec -> o.emit(this, emptySet())
      is FunSpec -> o.emit(
        codeWriter = this,
        enclosingName = null,
        implicitModifiers = setOf(KModifier.PUBLIC),
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
        val nestedClassNames = className.simpleNames.subList(
          c.simpleNames.size,
          className.simpleNames.size,
        ).joinToString(".")
        return "$simpleName.$nestedClassNames"
      }
      c = c.enclosingClassName()
    }

    // If the name resolved but wasn't a match, we're stuck with the fully qualified name.
    if (nameResolved) {
      return className.canonicalName
    }

    // If the class is in the same package, we're done.
    if (packageName == className.packageName) {
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
    val importedMember = importedMembers[simpleName]
    if (importedMember == memberName) {
      return simpleName
    } else if (importedMember != null && memberName.enclosingClassName != null) {
      val enclosingClassName = lookupName(memberName.enclosingClassName)
      return "$enclosingClassName.$simpleName"
    }

    // If the member is in the same package, we're done.
    if (packageName == memberName.packageName && memberName.enclosingClassName == null) {
      referencedNames.add(memberName.simpleName)
      return memberName.simpleName
    }

    // We'll have to use the fully-qualified name.
    // Mark the member as importable for a future pass unless the name clashes with
    // a method in the current context
    if (!kdoc && (
        memberName.isExtension ||
          !isMethodNameUsedInCurrentContext(memberName.simpleName)
        )
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
    val simpleName = imports[className.canonicalName]?.alias ?: topLevelClassName.simpleName
    // Check for name clashes with members.
    if (simpleName !in importableMembers) {
      importableTypes[simpleName] = importableTypes.getValue(simpleName) + topLevelClassName
    }
  }

  private fun importableMember(memberName: MemberName) {
    if (memberName.packageName.isNotEmpty()) {
      val simpleName = imports[memberName.canonicalName]?.alias ?: memberName.simpleName
      // Check for name clashes with types.
      if (simpleName !in importableTypes) {
        importableMembers[simpleName] = importableMembers.getValue(simpleName) + memberName
      }
    }
  }

  /**
   * Returns the class or enum value referenced by `simpleName`, using the current nesting context and
   * imports.
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

  /** Returns the class named `simpleName` when nested in the class at `stackDepth`.  */
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
    var first = true
    for (line in s.split('\n')) {
      // Emit a newline character. Make sure blank lines in KDoc & comments look good.
      if (!first) {
        if ((kdoc || comment) && trailingNewline) {
          emitIndentation()
          out.appendNonWrapping(if (kdoc) " *" else "//")
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
          out.appendNonWrapping(" * ")
        } else if (comment) {
          out.appendNonWrapping("// ")
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
      out.appendNonWrapping(indent)
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
    return importableMembers.filterKeys { it !in referencedNames }.mapValues { it.value.toSet() }
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
      action()
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
      val importsCollector = CodeWriter(
        NullAppendable,
        indent,
        memberImports,
        columnLimit = Integer.MAX_VALUE,
      )
      emitStep(importsCollector)
      val generatedImports = mutableMapOf<String, Import>()
      val suggestedTypeImports = importsCollector.suggestedTypeImports()
        .generateImports(
          generatedImports,
          canonicalName = ClassName::canonicalName,
          capitalizeAliases = true,
        )
      val suggestedMemberImports = importsCollector.suggestedMemberImports()
        .generateImports(
          generatedImports,
          canonicalName = MemberName::canonicalName,
          capitalizeAliases = false,
        )
      importsCollector.close()

      return CodeWriter(
        out,
        indent,
        memberImports + generatedImports.filterKeys { it !in memberImports },
        suggestedTypeImports,
        suggestedMemberImports,
      )
    }

    private fun <T> Map<String, Set<T>>.generateImports(
      generatedImports: MutableMap<String, Import>,
      canonicalName: T.() -> String,
      capitalizeAliases: Boolean,
    ): Map<String, T> {
      return flatMap { (simpleName, qualifiedNames) ->
        if (qualifiedNames.size == 1) {
          listOf(simpleName to qualifiedNames.first()).also {
            val canonicalName = qualifiedNames.first().canonicalName()
            generatedImports[canonicalName] = Import(canonicalName)
          }
        } else {
          generateImportAliases(simpleName, qualifiedNames, canonicalName, capitalizeAliases)
            .onEach { (alias, qualifiedName) ->
              val canonicalName = qualifiedName.canonicalName()
              generatedImports[canonicalName] = Import(canonicalName, alias)
            }
        }
      }.toMap()
    }

    private fun <T> generateImportAliases(
      simpleName: String,
      qualifiedNames: Set<T>,
      canonicalName: T.() -> String,
      capitalizeAliases: Boolean,
    ): List<Pair<String, T>> {
      val canonicalNameSegments = qualifiedNames.associateWith { qualifiedName ->
        qualifiedName.canonicalName().split('.')
          .dropLast(1) // Last segment of the canonical name is the simple name, drop it to avoid repetition.
          .filter { it != "Companion" }
          .map { it.replaceFirstChar(Char::uppercaseChar) }
      }
      val aliasNames = mutableMapOf<String, T>()
      var segmentsToUse = 0
      // Iterate until we have unique aliases for all names.
      while (aliasNames.size != qualifiedNames.size) {
        segmentsToUse += 1
        aliasNames.clear()
        for ((qualifiedName, segments) in canonicalNameSegments) {
          val aliasPrefix = segments.takeLast(min(segmentsToUse, segments.size))
            .joinToString(separator = "")
            .replaceFirstChar { if (!capitalizeAliases) it.lowercaseChar() else it }
          val aliasName = aliasPrefix + simpleName.replaceFirstChar(Char::uppercaseChar)
          aliasNames[aliasName] = qualifiedName
        }
      }
      return aliasNames.toList()
    }
  }
}
