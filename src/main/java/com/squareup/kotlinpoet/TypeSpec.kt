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

import com.squareup.kotlinpoet.KModifier.PUBLIC
import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Type
import javax.lang.model.SourceVersion
import kotlin.reflect.KClass

/** A generated class, interface, or enum declaration.  */
class TypeSpec private constructor(builder: TypeSpec.Builder) {
  val kind = builder.kind
  val name = builder.name
  val anonymousTypeArguments = builder.anonymousTypeArguments
  val kdoc = builder.kdoc.build()
  val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()
  val typeVariables: List<TypeVariableName> = builder.typeVariables.toImmutableList()
  val companionObject: TypeSpec? = builder.companionObject
  val primaryConstructor = builder.primaryConstructor
  val superclass = builder.superclass
  val superinterfaces: List<TypeName> = builder.superinterfaces.toImmutableList()
  val enumConstants: Map<String, TypeSpec> = builder.enumConstants.toImmutableMap()
  val propertySpecs: List<PropertySpec> = builder.propertySpecs.toImmutableList()
  val initializerBlock = builder.initializerBlock.build()
  val funSpecs: List<FunSpec> = builder.funSpecs.toImmutableList()
  val typeSpecs: List<TypeSpec> = builder.typeSpecs.toImmutableList()

  fun toBuilder(): Builder {
    val builder = Builder(kind, name, anonymousTypeArguments)
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.superclass = superclass
    builder.superinterfaces += superinterfaces
    builder.enumConstants += enumConstants
    builder.propertySpecs += propertySpecs
    builder.funSpecs += funSpecs
    builder.typeSpecs += typeSpecs
    builder.initializerBlock.add(initializerBlock)
    return builder
  }

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter, enumName: String?) {
    // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
    // it back afterwards when this type is complete.
    val previousStatementLine = codeWriter.statementLine
    codeWriter.statementLine = -1

    try {
      if (enumName != null) {
        codeWriter.emitKdoc(kdoc)
        codeWriter.emitAnnotations(annotations, false)
        codeWriter.emitCode("%L", enumName)
        if (anonymousTypeArguments!!.formatParts.isNotEmpty()) {
          codeWriter.emit("(")
          codeWriter.emitCode(anonymousTypeArguments)
          codeWriter.emit(")")
        }
        if (propertySpecs.isEmpty() && funSpecs.isEmpty() && typeSpecs.isEmpty()) {
          return  // Avoid unnecessary braces "{}".
        }
        codeWriter.emit(" {\n")
      } else if (anonymousTypeArguments != null) {
        val supertype = if (superinterfaces.isNotEmpty()) superinterfaces[0] else superclass
        codeWriter.emitCode("object : %T(", supertype)
        codeWriter.emitCode(anonymousTypeArguments)
        codeWriter.emit(") {\n")
      } else {
        codeWriter.emitKdoc(kdoc)
        codeWriter.emitAnnotations(annotations, false)
        codeWriter.emitModifiers(modifiers, setOf(PUBLIC))
        codeWriter.emit(kind.declarationKeyword)
        if (kind != Kind.COMPANION) {
          codeWriter.emitCode(" %L", name)
        }
        codeWriter.emitTypeVariables(typeVariables)

        primaryConstructor?.emitParameterList(codeWriter)

        val extendsTypes: List<TypeName>
        val implementsTypes: List<TypeName>
        if (kind == Kind.INTERFACE) {
          extendsTypes = superinterfaces
          implementsTypes = emptyList()
        } else {
          extendsTypes = if (superclass == ANY) emptyList() else listOf(superclass)
          implementsTypes = superinterfaces
        }

        (extendsTypes + implementsTypes).forEachIndexed { i, typeName ->
          codeWriter.emit(if (i == 0)  " :" else ",")
          codeWriter.emitCode(" %T", typeName)
        }

        codeWriter.emit(" {\n")
      }

      codeWriter.pushType(this)
      codeWriter.indent()
      var firstMember = true
      val i = enumConstants.entries.iterator()
      while (i.hasNext()) {
        val enumConstant = i.next()
        if (!firstMember) codeWriter.emit("\n")
        enumConstant.value
            .emit(codeWriter, enumConstant.key)
        firstMember = false
        if (i.hasNext()) {
          codeWriter.emit(",\n")
        } else if (propertySpecs.isNotEmpty() || funSpecs.isNotEmpty() || typeSpecs.isNotEmpty()) {
          codeWriter.emit(";\n")
        } else {
          codeWriter.emit("\n")
        }
      }

      // Non-static properties.
      for (propertySpec in propertySpecs) {
        if (!firstMember) codeWriter.emit("\n")
        propertySpec.emit(codeWriter, kind.implicitPropertyModifiers)
        firstMember = false
      }

      if (primaryConstructor != null && !primaryConstructor.code.isEmpty()) {
        codeWriter.emit("init {\n")
        codeWriter.indent()
        codeWriter.emitCode(primaryConstructor.code)
        codeWriter.unindent()
        codeWriter.emit("}\n")
      }

      // Initializer block.
      if (!initializerBlock.isEmpty()) {
        if (!firstMember) codeWriter.emit("\n")
        codeWriter.emitCode(initializerBlock)
        firstMember = false
      }

      // Constructors.
      for (funSpec in funSpecs) {
        if (!funSpec.isConstructor) continue
        if (!firstMember) codeWriter.emit("\n")
        funSpec.emit(codeWriter, name!!, kind.implicitFunctionModifiers)
        firstMember = false
      }

      // Functions (static and non-static).
      for (funSpec in funSpecs) {
        if (funSpec.isConstructor) continue
        if (!firstMember) codeWriter.emit("\n")
        funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers)
        firstMember = false
      }

      // Types.
      for (typeSpec in typeSpecs) {
        if (!firstMember) codeWriter.emit("\n")
        typeSpec.emit(codeWriter, null)
        firstMember = false
      }

      companionObject?.let {
        it.emit(codeWriter, null)
      }

      codeWriter.unindent()
      codeWriter.popType()

      codeWriter.emit("}")
      if (enumName == null && anonymousTypeArguments == null) {
        codeWriter.emit("\n") // If this type isn't also a value, include a trailing newline.
      }
    } finally {
      codeWriter.statementLine = previousStatementLine
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int {
    return toString().hashCode()
  }

  override fun toString(): String {
    val out = StringWriter()
    try {
      val codeWriter = CodeWriter(out)
      emit(codeWriter, null)
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }
  }

  enum class Kind(
      internal val declarationKeyword: String,
      internal val implicitPropertyModifiers: Set<KModifier>,
      internal val implicitFunctionModifiers: Set<KModifier>) {
    CLASS(
        "class",
        setOf(KModifier.PUBLIC),
        setOf(KModifier.PUBLIC)),

    OBJECT(
        "object",
        setOf(KModifier.PUBLIC),
        setOf(KModifier.PUBLIC)),

    COMPANION(
        "companion object",
        setOf(KModifier.PUBLIC),
        setOf(KModifier.PUBLIC)),

    INTERFACE(
        "interface",
        setOf(KModifier.PUBLIC),
        setOf(KModifier.PUBLIC, KModifier.ABSTRACT)),

    ENUM(
        "enum class",
        setOf(KModifier.PUBLIC),
        setOf(KModifier.PUBLIC)),

    ANNOTATION(
        "annotation class",
        emptySet(),
        setOf(KModifier.PUBLIC, KModifier.ABSTRACT));
  }

  class Builder internal constructor(
      internal val kind: Kind,
      internal val name: String?,
      internal val anonymousTypeArguments: CodeBlock?) {
    internal val kdoc = CodeBlock.builder()
    internal val annotations = mutableListOf<AnnotationSpec>()
    internal val modifiers = mutableListOf<KModifier>()
    internal val typeVariables = mutableListOf<TypeVariableName>()
    internal var primaryConstructor : FunSpec? = null
    internal var companionObject : TypeSpec? = null
    internal var superclass: TypeName = ANY
    internal val superinterfaces = mutableListOf<TypeName>()
    internal val enumConstants = mutableMapOf<String, TypeSpec>()
    internal val propertySpecs = mutableListOf<PropertySpec>()
    internal val initializerBlock = CodeBlock.builder()
    internal val funSpecs = mutableListOf<FunSpec>()
    internal val typeSpecs = mutableListOf<TypeSpec>()

    init {
      require(name == null || SourceVersion.isName(name)) { "not a valid name: $name" }
    }

    fun addKdoc(format: String, vararg args: Any): Builder {
      kdoc.add(format, *args)
      return this
    }

    fun addKdoc(block: CodeBlock): Builder {
      kdoc.add(block)
      return this
    }

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder {
      annotations += annotationSpecs
      return this
    }

    fun addAnnotation(annotationSpec: AnnotationSpec): Builder {
      annotations += annotationSpec
      return this
    }

    fun addAnnotation(annotation: ClassName)
        = addAnnotation(AnnotationSpec.builder(annotation).build())

    fun addAnnotation(annotation: Class<*>) = addAnnotation(ClassName.get(annotation))

    fun addAnnotation(annotation: KClass<*>) = addAnnotation(ClassName.get(annotation))

    fun addModifiers(vararg modifiers: KModifier): Builder {
      check(anonymousTypeArguments == null) { "forbidden on anonymous types." }
      this.modifiers += modifiers
      return this
    }

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>): Builder {
      check(anonymousTypeArguments == null) { "forbidden on anonymous types." }
      this.typeVariables += typeVariables
      return this
    }

    fun addTypeVariable(typeVariable: TypeVariableName): Builder {
      check(anonymousTypeArguments == null) { "forbidden on anonymous types." }
      typeVariables += typeVariable
      return this
    }

    fun companionObject(companionObject: TypeSpec): Builder {
      check(kind == Kind.CLASS || kind == Kind.INTERFACE) { "$kind can't have a companion object" }
      require(companionObject.kind == Kind.COMPANION) { "expected a companion object class but was $kind "}
      this.companionObject = companionObject
      return this
    }

    fun primaryConstructor(primaryConstructor: FunSpec?): Builder {
      check(kind == Kind.CLASS || kind == Kind.ENUM) { "$kind can't have initializer blocks" }
      if (primaryConstructor != null) {
        require (primaryConstructor.isConstructor) {
          "expected a constructor but was ${primaryConstructor.name}"
        }
      }
      this.primaryConstructor = primaryConstructor
      return this
    }

    fun superclass(superclass: TypeName): Builder {
      check(kind == Kind.CLASS || kind == Kind.OBJECT || kind == Kind.COMPANION) {
        "only classes can have super classes, not $kind"
      }
      check(this.superclass === ANY) { "superclass already set to ${this.superclass}" }
      this.superclass = superclass
      return this
    }

    fun superclass(superclass: Type): Builder {
      return superclass(TypeName.get(superclass))
    }

    fun addSuperinterfaces(superinterfaces: Iterable<TypeName>): Builder {
      this.superinterfaces += superinterfaces
      return this
    }

    fun addSuperinterface(superinterface: TypeName): Builder {
      superinterfaces += superinterface
      return this
    }

    fun addSuperinterface(superinterface: Type)
        = addSuperinterface(TypeName.get(superinterface))

    fun addSuperinterface(superinterface: KClass<*>)
        = addSuperinterface(TypeName.get(superinterface))

    @JvmOverloads fun addEnumConstant(
        name: String,
        typeSpec: TypeSpec = anonymousClassBuilder("").build()): Builder {
      check(kind == Kind.ENUM) { "${this.name} is not enum" }
      require(typeSpec.anonymousTypeArguments != null) {
          "enum constants must have anonymous type arguments" }
      require(SourceVersion.isName(name)) { "not a valid enum constant: $name" }
      enumConstants.put(name, typeSpec)
      return this
    }

    fun addProperties(propertySpecs: Iterable<PropertySpec>): Builder {
      this.propertySpecs += propertySpecs
      return this
    }

    fun addProperty(propertySpec: PropertySpec): Builder {
      check(kind == Kind.CLASS || kind == Kind.ENUM) {
        "cannot add property $propertySpec to $kind"
      }
      propertySpecs += propertySpec
      return this
    }

    fun addProperty(name: String, type: TypeName, vararg modifiers: KModifier)
        = addProperty(PropertySpec.builder(name, type, *modifiers).build())

    fun addProperty(name: String, type: Type, vararg modifiers: KModifier)
        = addProperty(name, TypeName.get(type), *modifiers)

    fun addProperty(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = addProperty(name, TypeName.get(type), *modifiers)

    fun addInitializerBlock(block: CodeBlock): Builder {
      check(kind == Kind.CLASS || kind == Kind.OBJECT || kind == Kind.ENUM) { "$kind can't have initializer blocks" }
      initializerBlock.add("init {\n")
          .indent()
          .add(block)
          .unindent()
          .add("}\n")
      return this
    }

    fun addFunctions(funSpecs: Iterable<FunSpec>): Builder {
      this.funSpecs += funSpecs
      return this
    }

    fun addFun(funSpec: FunSpec): Builder {
      if (kind == Kind.INTERFACE) {
        requireExactlyOneOf(funSpec.modifiers, KModifier.ABSTRACT)
        requireExactlyOneOf(funSpec.modifiers, KModifier.PUBLIC, KModifier.PRIVATE)
      } else if (kind == Kind.ANNOTATION) {
        check(funSpec.modifiers == kind.implicitFunctionModifiers) {
            "$kind $name.${funSpec.name} requires modifiers ${kind.implicitFunctionModifiers}" }
      }
      if (kind != Kind.ANNOTATION) {
        check(funSpec.defaultValue == null) {
          "$kind $name.${funSpec.name} cannot have a default value" }
      }
      funSpecs += funSpec
      return this
    }

    fun addTypes(typeSpecs: Iterable<TypeSpec>): Builder {
      this.typeSpecs += typeSpecs
      return this
    }

    fun addType(typeSpec: TypeSpec): Builder {
      typeSpecs += typeSpec
      return this
    }

    fun build(): TypeSpec {
      require(kind != Kind.ENUM || enumConstants.isNotEmpty()) {
          "at least one enum constant is required for $name" }

      val isAbstract = modifiers.contains(KModifier.ABSTRACT) || kind != Kind.CLASS
      for (funSpec in funSpecs) {
        require(isAbstract || !funSpec.modifiers.contains(KModifier.ABSTRACT)) {
            "non-abstract type $name cannot declare abstract function ${funSpec.name}" }
      }

      val superclassIsAny = superclass == ANY
      val interestingSupertypeCount = (if (superclassIsAny) 0 else 1) + superinterfaces.size
      require(anonymousTypeArguments == null || interestingSupertypeCount <= 1) {
          "anonymous type has too many supertypes" }

      return TypeSpec(this)
    }
  }

  companion object {
    @JvmStatic fun classBuilder(name: String) = Builder(Kind.CLASS, name, null)

    @JvmStatic fun classBuilder(className: ClassName) = classBuilder(className.simpleName())

    @JvmStatic fun objectBuilder(name: String) = Builder(Kind.OBJECT, name, null)

    @JvmStatic fun objectBuilder(className: ClassName) = objectBuilder(className.simpleName())

    @JvmStatic fun companionObjectBuilder() = Builder(Kind.COMPANION, null, null)

    @JvmStatic fun interfaceBuilder(name: String) = Builder(Kind.INTERFACE, name, null)

    @JvmStatic fun interfaceBuilder(className: ClassName) = interfaceBuilder(className.simpleName())

    @JvmStatic fun enumBuilder(name: String) = Builder(Kind.ENUM, name, null)

    @JvmStatic fun enumBuilder(className: ClassName) = enumBuilder(className.simpleName())

    @JvmStatic fun anonymousClassBuilder(typeArgumentsFormat: String, vararg args: Any): Builder {
      return Builder(Kind.CLASS, null, CodeBlock.builder()
          .add(typeArgumentsFormat, *args)
          .build())
    }

    @JvmStatic fun annotationBuilder(name: String) = Builder(Kind.ANNOTATION, name, null)

    @JvmStatic fun annotationBuilder(className: ClassName)
        = annotationBuilder(className.simpleName())
  }
}
