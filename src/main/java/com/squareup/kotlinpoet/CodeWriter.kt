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

/** Sentinel value that indicates that no user-provided package has been set.  */
private val NO_PACKAGE = String()

private fun extractMemberName(part: String): String {
  require(Character.isJavaIdentifierStart(part[0])) { "not an identifier: $part" }
  for (i in 1..part.length) {
    if (!part.substring(0, i).isIdentifier) {
      return part.substring(0, i - 1)
    }
  }
  return part
}

/**
 * Converts a [FileSpec] to a string suitable to both human- and kotlinc-consumption. This honors
 * imports, indentation, and deferred variable names.
 */
internal class CodeWriter constructor(
  out: Appendable,
  private val indent: String = DEFAULT_INDENT,
  private val memberImports: Map<String, Import> = emptyMap(),
  private val importedTypes: Map<String, ClassName> = emptyMap()
) {
  private val out = LineWrapper(out, indent, 100)
  private var indentLevel = 0

  private var kdoc = false
  private var comment = false
  private var packageName = NO_PACKAGE
  private val typeSpecStack = mutableListOf<TypeSpec>()
  private val memberImportClassNames = mutableSetOf<String>()
  private val importableTypes = mutableMapOf<String, ClassName>()
  private val referencedNames = mutableSetOf<String>()
  private var trailingNewline = false

  /**
   * When emitting a statement, this is the line of the statement currently being written. The first
   * line of a statement is indented normally and subsequent wrapped lines are double-indented. This
   * is -1 when the currently-written line isn't part of a statement.
   */
  var statementLine = -1

  init {
    for ((className, _) in memberImports) {
      memberImportClassNames.add(className.substring(0, className.lastIndexOf('.')))
    }
  }

  fun importedTypes() = importedTypes

  fun indent(levels: Int = 1) = apply {
    indentLevel += levels
  }

  fun unindent(levels: Int = 1) = apply {
    require(indentLevel - levels >= 0) { "cannot unindent $levels from $indentLevel" }
    indentLevel -= levels
  }

  fun pushPackage(packageName: String) = apply {
    require(this.packageName === NO_PACKAGE) { "package already set: ${this.packageName}" }
    this.packageName = packageName
  }

  fun popPackage() = apply {
    require(this.packageName !== NO_PACKAGE) { "package already set: ${this.packageName}" }
    this.packageName = NO_PACKAGE
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
      emitCode(kdocCodeBlock)
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
   * be emitted.
   */
  fun emitModifiers(
    modifiers: Set<KModifier>,
    implicitModifiers: Set<KModifier> = emptySet()
  ) {
    if (modifiers.isEmpty()) return
    for (modifier in modifiers.toEnumSet()) {
      if (implicitModifiers.contains(modifier)) continue
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
      if (typeVariable.reified) {
        emit("reified ")
      }
      emitCode("%L", typeVariable.name)
      if (typeVariable.bounds.size == 1) {
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
          if (!firstBound) emit(", ") else emit(" where ")
          emitCode("%L", typeVariable.name)
          emitCode(" : %T", bound)
          firstBound = false
        }
      }
    }
  }

  fun emitCode(s: String) = emitCode(CodeBlock.of(s))

  fun emitCode(format: String, vararg args: Any?) = emitCode(CodeBlock.of(format, *args))

  fun emitCode(codeBlock: CodeBlock) = apply {
    var a = 0
    var deferredTypeName: ClassName? = null // used by "import static" logic
    val partIterator = codeBlock.formatParts.listIterator()
    while (partIterator.hasNext()) {
      val part = partIterator.next()
      when (part) {
        "%L" -> emitLiteral(codeBlock.args[a++])

        "%N" -> emit(codeBlock.args[a++] as String)

        "%S" -> {
          val string = codeBlock.args[a++] as String?
          // Emit null as a literal null: no quotes.
          emit(if (string != null)
            stringLiteralWithQuotes(string) else
            "null")
        }

        "%T" -> {
          var typeName = codeBlock.args[a++] as TypeName
          if (typeName.isAnnotated) {
            typeName.emitAnnotations(this)
            typeName = typeName.withoutAnnotations()
          }
          // defer "typeName.emit(this)" if next format part will be handled by the default case
          var defer = false
          if (typeName is ClassName && partIterator.hasNext()) {
            if (!codeBlock.formatParts[partIterator.nextIndex()].startsWith("%")) {
              val candidate = typeName
              if (memberImportClassNames.contains(candidate.canonicalName)) {
                check(deferredTypeName == null) { "pending type for static import?!" }
                deferredTypeName = candidate
                defer = true
              }
            }
          }
          if (!defer) typeName.emit(this)
          typeName.emitNullable(this)
        }

        "%%" -> emit("%")

        "%>" -> indent()

        "%<" -> unindent()

        "%[" -> {
          check(statementLine == -1) { "statement enter %[ followed by statement enter %[" }
          statementLine = 0
        }

        "%]" -> {
          check(statementLine != -1) { "statement exit %] has no matching statement enter %[" }
          if (statementLine > 0) {
            unindent(2) // End a multi-line statement. Decrease the indentation level.
          }
          statementLine = -1
        }

        "%W" -> out.wrappingSpace(indentLevel + 2)

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
  }

  fun emitWrappingSpace() = apply {
    out.wrappingSpace(indentLevel + 2)
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

  private fun emitLiteral(o: Any?) {
    when (o) {
      is TypeSpec -> o.emit(this, null)
      is AnnotationSpec -> o.emit(this, inline = true, asParameter = true)
      is CodeBlock -> emitCode(o)
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
      val simpleName = alias ?: c.simpleName()
      val resolved = resolve(simpleName)
      nameResolved = resolved != null

      // We don't care about nullability here, as it's irrelevant for imports.
      if (resolved == c.asNonNullable()) {
        if (alias != null) return alias
        val suffixOffset = c.simpleNames().size - 1
        return className.simpleNames().subList(suffixOffset,
            className.simpleNames().size).joinToString(".")
      }
      c = c.enclosingClassName()
    }

    // If the name resolved but wasn't a match, we're stuck with the fully qualified name.
    if (nameResolved) {
      return className.canonicalName
    }

    // If the class is in the same package, we're done.
    if (packageName == className.packageName()) {
      referencedNames.add(className.topLevelClassName().simpleName())
      return className.simpleNames().joinToString(".")
    }

    // We'll have to use the fully-qualified name. Mark the type as importable for a future pass.
    if (!kdoc) {
      importableType(className)
    }

    return className.canonicalName
  }

  private fun importableType(className: ClassName) {
    if (className.packageName().isEmpty()) {
      return
    }
    val topLevelClassName = className.topLevelClassName()
    val simpleName = memberImports[className.canonicalName]?.alias ?: topLevelClassName.simpleName()
    val replaced = importableTypes.put(simpleName, topLevelClassName)
    if (replaced != null) {
      importableTypes.put(simpleName, replaced) // On collision, prefer the first inserted.
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
      for (visibleChild in typeSpec.typeSpecs) {
        if (visibleChild.name == simpleName) {
          return stackClassName(i, simpleName)
        }
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
  fun emit(s: String) = apply {
    var first = true
    for (line in s.split('\n')) {
      // Emit a newline character. Make sure blank lines in KDoc & comments look good.
      if (!first) {
        if ((kdoc || comment) && trailingNewline) {
          emitIndentation()
          out.append(if (kdoc) " *" else "//")
        }
        out.append("\n")
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

      out.append(line)
      trailingNewline = false
    }
  }

  private fun emitIndentation() {
    for (j in 0 until indentLevel) {
      out.append(indent)
    }
  }

  /**
   * Returns the types that should have been imported for this code. If there were any simple name
   * collisions, that type's first use is imported.
   */
  fun suggestedImports(): Map<String, ClassName> {
    return importableTypes.filterKeys { it !in referencedNames }
  }
}
