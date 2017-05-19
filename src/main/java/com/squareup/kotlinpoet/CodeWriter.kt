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

import com.squareup.kotlinpoet.Util.stringLiteralWithQuotes
import java.io.IOException
import java.util.*
import javax.lang.model.SourceVersion

/** Sentinel value that indicates that no user-provided package has been set.  */
private val NO_PACKAGE = String()

private fun extractMemberName(part: String): String {
  require(Character.isJavaIdentifierStart(part[0])) { "not an identifier: $part" }
  for (i in 1..part.length) {
    if (!SourceVersion.isIdentifier(part.substring(0, i))) {
      return part.substring(0, i - 1)
    }
  }
  return part
}

/**
 * Converts a [KotlinFile] to a string suitable to both human- and javac-consumption. This honors
 * imports, indentation, and deferred variable names.
 */
internal class CodeWriter @JvmOverloads constructor(
    out: Appendable,
    private val indent: String = "  ",
    private val memberImports: Set<String> = emptySet(),
    private val importedTypes: Map<String, ClassName> = emptyMap()) {

  private val out: LineWrapper = LineWrapper(out, indent, 100)
  private var indentLevel: Int = 0

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
    for (signature in memberImports) {
      memberImportClassNames.add(signature.substring(0, signature.lastIndexOf('.')))
    }
  }

  fun importedTypes() = importedTypes

  @JvmOverloads fun indent(levels: Int = 1): CodeWriter {
    indentLevel += levels
    return this
  }

  @JvmOverloads fun unindent(levels: Int = 1): CodeWriter {
    require(indentLevel - levels >= 0) { "cannot unindent $levels from $indentLevel" }
    indentLevel -= levels
    return this
  }

  fun pushPackage(packageName: String): CodeWriter {
    require(this.packageName === NO_PACKAGE) { "package already set: ${this.packageName}" }
    this.packageName = packageName
    return this
  }

  fun popPackage(): CodeWriter {
    require(this.packageName !== NO_PACKAGE) { "package already set: ${this.packageName}" }
    this.packageName = NO_PACKAGE
    return this
  }

  fun pushType(type: TypeSpec): CodeWriter {
    this.typeSpecStack.add(type)
    return this
  }

  fun popType(): CodeWriter {
    this.typeSpecStack.removeAt(typeSpecStack.size - 1)
    return this
  }

  @Throws(IOException::class)
  fun emitComment(codeBlock: CodeBlock) {
    trailingNewline = true // Force the '//' prefix for the comment.
    comment = true
    try {
      emit(codeBlock)
      emit("\n")
    } finally {
      comment = false
    }
  }

  @Throws(IOException::class)
  fun emitKdoc(kdocCodeBlock: CodeBlock) {
    if (kdocCodeBlock.isEmpty()) return

    emit("/**\n")
    kdoc = true
    try {
      emit(kdocCodeBlock)
    } finally {
      kdoc = false
    }
    emit(" */\n")
  }

  @Throws(IOException::class)
  fun emitAnnotations(annotations: List<AnnotationSpec>, inline: Boolean) {
    for (annotationSpec in annotations) {
      annotationSpec.emit(this, inline)
      emit(if (inline) " " else "\n")
    }
  }

  /**
   * Emits `modifiers` in the standard order. Modifiers in `implicitModifiers` will not
   * be emitted.
   *
   * TODO: migrate all callers to [CodeWriter.emitModifiers].
   */
  @Throws(IOException::class)
  @JvmOverloads fun emitJavaModifiers(
      modifiers: Set<KModifier>,
      implicitModifiers: Set<KModifier> = emptySet<KModifier>()) {
    if (modifiers.isEmpty()) return
    for (modifier in EnumSet.copyOf(modifiers)) {
      if (implicitModifiers.contains(modifier)) continue
      emitAndIndent(modifier.name.toLowerCase(Locale.US))
      emitAndIndent(" ")
    }
  }

  /**
   * Emits `modifiers` in the standard order. Modifiers in `implicitModifiers` will not
   * be emitted.
   */
  @Throws(IOException::class)
  @JvmOverloads fun emitModifiers(
      modifiers: Set<KModifier>,
      implicitModifiers: Set<KModifier> = emptySet<KModifier>()) {
    if (modifiers.isEmpty()) return
    for (modifier in EnumSet.copyOf(modifiers)) {
      if (implicitModifiers.contains(modifier)) continue
      emitAndIndent(modifier.name.toLowerCase(Locale.US))
      emitAndIndent(" ")
    }
  }

  /**
   * Emit type variables with their bounds. This should only be used when declaring type variables;
   * everywhere else bounds are omitted.
   */
  @Throws(IOException::class)
  fun emitTypeVariables(typeVariables: List<TypeVariableName>) {
    if (typeVariables.isEmpty()) return

    emit("<")
    var firstTypeVariable = true
    for (typeVariable in typeVariables) {
      if (!firstTypeVariable) emit(", ")
      emit("%L", typeVariable.name)
      var firstBound = true
      for (bound in typeVariable.bounds) {
        emit(if (firstBound) " : %T" else " & %T", bound)
        firstBound = false
      }
      firstTypeVariable = false
    }
    emit(">")
  }

  @Throws(IOException::class)
  fun emit(s: String): CodeWriter = emitAndIndent(s)

  @Throws(IOException::class)
  fun emit(format: String, vararg args: Any?): CodeWriter = emit(CodeBlock.of(format, *args))

  @Throws(IOException::class)
  fun emit(codeBlock: CodeBlock): CodeWriter {
    var a = 0
    var deferredTypeName: ClassName? = null // used by "import static" logic
    val partIterator = codeBlock.formatParts.listIterator()
    while (partIterator.hasNext()) {
      val part = partIterator.next()
      when (part) {
        "%L" -> emitLiteral(codeBlock.args[a++])

        "%N" -> emitAndIndent(codeBlock.args[a++] as String)

        "%S" -> {
          val string = codeBlock.args[a++] as String?
          // Emit null as a literal null: no quotes.
          emitAndIndent(if (string != null)
            stringLiteralWithQuotes(string, indent) else
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
          if (typeName.nullable) emit("?")
        }

        "%%" -> emitAndIndent("%")

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
            emitAndIndent(part)
          }
        }
      }
    }
    return this
  }

  @Throws(IOException::class)
  fun emitWrappingSpace(): CodeWriter {
    out.wrappingSpace(indentLevel + 2)
    return this
  }

  @Throws(IOException::class)
  private fun emitStaticImportMember(canonical: String, part: String): Boolean {
    val partWithoutLeadingDot = part.substring(1)
    if (partWithoutLeadingDot.isEmpty()) return false
    val first = partWithoutLeadingDot[0]
    if (!Character.isJavaIdentifierStart(first)) return false
    val explicit = canonical + "." + extractMemberName(partWithoutLeadingDot)
    val wildcard = canonical + ".*"
    if (memberImports.contains(explicit) || memberImports.contains(wildcard)) {
      emitAndIndent(partWithoutLeadingDot)
      return true
    }
    return false
  }

  @Throws(IOException::class)
  private fun emitLiteral(o: Any?) {
    if (o is TypeSpec) {
      o.emit(this, null)
    } else if (o is AnnotationSpec) {
      o.emit(this, true)
    } else if (o is CodeBlock) {
      emit(o)
    } else {
      emitAndIndent(o.toString())
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
      val resolved = resolve(c.simpleName())
      nameResolved = resolved != null

      if (resolved == c) {
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
    val simpleName = topLevelClassName.simpleName()
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
      return ClassName.get(packageName, simpleName)
    }

    // Match an imported type.
    val importedType = importedTypes[simpleName]
    if (importedType != null) return importedType

    // No match.
    return null
  }

  /** Returns the class named `simpleName` when nested in the class at `stackDepth`.  */
  private fun stackClassName(stackDepth: Int, simpleName: String): ClassName {
    var className = ClassName.get(packageName, typeSpecStack[0].name!!)
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
  @Throws(IOException::class)
  fun emitAndIndent(s: String): CodeWriter {
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
    return this
  }

  @Throws(IOException::class)
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
