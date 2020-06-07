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

import java.io.Closeable

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
  builderAction: CodeWriter.() -> Unit
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
  private val memberImports: Map<String, Import> = emptyMap(),
  val importedTypes: Map<String, ClassName> = emptyMap(),
  val importedMembers: Map<String, MemberName> = emptyMap(),
  columnLimit: Int = 100
) : Closeable {
  private var out = LineWrapper(out, indent, columnLimit)
  private var indentLevel = 0

  private var kdoc = false
  private var comment = false
  private var packageName = NO_PACKAGE
  private val typeSpecStack = mutableListOf<TypeSpec>()
  private val memberImportNames = mutableSetOf<String>()
  private val importableTypes = mutableMapOf<String, ClassName>()
  private val importableMembers = mutableMapOf<String, MemberName>()
  private val referencedNames = mutableSetOf<String>()
  private var trailingNewline = false

  /**
   * When emitting a statement, this is the line of the statement currently being written. The first
   * line of a statement is indented normally and subsequent wrapped lines are double-indented. This
   * is -1 when the currently-written line isn't part of a statement.
   */
  var statementLine = -1

  init {
    for ((memberName, _) in memberImports) {
      val lastDotIndex = memberName.lastIndexOf('.')
      if (lastDotIndex >= 0) {
        memberImportNames.add(memberName.substring(0, lastDotIndex))
      }
    }
  }

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
    implicitModifiers: Set<KModifier> = emptySet()
  ) {
    if (shouldEmitPublicModifier(modifiers, implicitModifiers)) {
      emit(KModifier.PUBLIC.keyword)
      emit(" ")
    }
    for (modifier in modifiers.toEnumSet()) {
      if (modifier in implicitModifiers) continue
      emit(modifier.keyword)
      emit(" ")
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
    ensureTrailingNewline: Boolean = false
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
                escapeDollarSign = true,
                isConstantContext = isConstantContext
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
                escapeDollarSign = false,
                isConstantContext = isConstantContext
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
            |""".trimMargin()
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
            |""".trimMargin()
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
    val explicit = memberImports[canonical + "." + extractMemberName(partWithoutLeadingDot)]
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
      val alias = memberImports[c.canonicalName]?.alias
      val simpleName = alias ?: c.simpleName
      val resolved = resolve(simpleName)
      nameResolved = resolved != null

      // We don't care about nullability and type annotations here, as it's irrelevant for imports.
      if (resolved == c.copy(nullable = false, annotations = emptyList())) {
        if (alias != null) return alias
        val suffixOffset = c.simpleNames.size - 1
        return className.simpleNames.subList(suffixOffset,
            className.simpleNames.size).joinToString(".")
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
    val simpleName = memberImports[memberName.canonicalName]?.alias ?: memberName.simpleName
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

    // We'll have to use the fully-qualified name. Mark the member as importable for a future pass.
    if (!kdoc) {
      importableMember(memberName)
    }

    return memberName.canonicalName
  }

  private fun importableType(className: ClassName) {
    val topLevelClassName = className.topLevelClassName()
    val simpleName = memberImports[className.canonicalName]?.alias ?: topLevelClassName.simpleName
    // Check for name clashes with members.
    if (simpleName !in importableMembers) {
      importableTypes.putIfAbsent(simpleName, topLevelClassName)
    }
  }

  private fun importableMember(memberName: MemberName) {
    if (memberName.packageName.isNotEmpty()) {
      val simpleName = memberImports[memberName.canonicalName]?.alias ?: memberName.simpleName
      // Check for name clashes with types.
      if (simpleName !in importableTypes) {
        val namesake = importableMembers.putIfAbsent(simpleName, memberName)
        if (namesake != null && memberName.enclosingClassName != null) {
          // If there's a name clash and member has an enclosing class, we can mark it as importable
          // and use its resolved name later instead of the FQ member name.
          importableType(memberName.enclosingClassName)
        }
      }
    }
  }

  /**
   * Returns the class referenced by `simpleName`, using the current nesting context and
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

    // Match the top-level class.
    if (typeSpecStack.size > 0 && typeSpecStack[0].name == simpleName) {
      return ClassName(packageName, simpleName)
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
            linePrefix = if (kdoc) " * " else ""
        )
      }
      trailingNewline = false
    }
  }

  private fun emitIndentation() {
    for (j in 0 until indentLevel) {
      out.appendNonWrapping(indent)
    }
  }

  /**
   * Returns whether a [KModifier.PUBLIC] should be emitted.
   *
   * This will return true when [KModifier.PUBLIC] is one of the [implicitModifiers] and
   * there are no other opposing modifiers (like [KModifier.PROTECTED] etc.) supplied by the
   * consumer.
   */
  private fun shouldEmitPublicModifier(
    modifiers: Set<KModifier>,
    implicitModifiers: Set<KModifier>
  ): Boolean {
    if (modifiers.contains(KModifier.PUBLIC)) {
      return true
    }

    if (!implicitModifiers.contains(KModifier.PUBLIC)) {
      return false
    }

    val containsOtherVisibility = modifiers.containsAnyOf(KModifier.PRIVATE, KModifier.INTERNAL, KModifier.PROTECTED)

    return !containsOtherVisibility
  }

  /**
   * Returns the types that should have been imported for this code. If there were any simple name
   * collisions, that type's first use is imported.
   */
  fun suggestedTypeImports(): Map<String, ClassName> {
    return importableTypes.filterKeys { it !in referencedNames }
  }

  /**
   * Returns the members that should have been imported for this code. If there were any simple name
   * collisions, that member's first use is imported.
   */
  fun suggestedMemberImports(): Map<String, MemberName> {
    return importableMembers.filterKeys { it !in referencedNames }
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
}
