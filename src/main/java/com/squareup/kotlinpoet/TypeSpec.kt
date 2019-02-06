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

import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.ANNOTATION
import com.squareup.kotlinpoet.KModifier.COMPANION
import com.squareup.kotlinpoet.KModifier.ENUM
import com.squareup.kotlinpoet.KModifier.EXPECT
import com.squareup.kotlinpoet.KModifier.EXTERNAL
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.KModifier.SEALED
import java.lang.reflect.Type
import kotlin.reflect.KClass

/** A generated class, interface, or enum declaration.  */
class TypeSpec private constructor(builder: TypeSpec.Builder) {
  val kind = builder.kind
  val name = builder.name
  val kdoc = builder.kdoc.build()
  val annotationSpecs = builder.annotationSpecs.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val primaryConstructor = builder.primaryConstructor
  val superclass = builder.superclass
  val superclassConstructorParameters = builder.superclassConstructorParameters.toImmutableList()

  val isEnum = builder.isEnum
  val isAnnotation = builder.isAnnotation
  val isCompanion = builder.isCompanion
  val isAnonymousClass = builder.isAnonymousClass

  /**
   * Map of superinterfaces - entries with a null value represent a regular superinterface (with
   * no delegation), while non-null [CodeBlock] values represent delegates
   * for the corresponding [TypeSpec] interface (key) value
   */
  val superinterfaces = builder.superinterfaces.toImmutableMap()
  val enumConstants = builder.enumConstants.toImmutableMap()
  val propertySpecs = builder.propertySpecs.toImmutableList()
  val initializerBlock = builder.initializerBlock.build()
  val funSpecs = builder.funSpecs.toImmutableList()
  val typeSpecs = builder.typeSpecs.toImmutableList()
  internal val nestedTypesSimpleNames = typeSpecs.map { it.name }.toImmutableSet()

  fun toBuilder(): Builder {
    val builder = Builder(kind, name, *modifiers.toTypedArray())
    builder.kdoc.add(kdoc)
    builder.annotationSpecs += annotationSpecs
    builder.typeVariables += typeVariables
    builder.superclass = superclass
    builder.superclassConstructorParameters += superclassConstructorParameters
    builder.enumConstants += enumConstants
    builder.propertySpecs += propertySpecs
    builder.funSpecs += funSpecs
    builder.typeSpecs += typeSpecs
    builder.initializerBlock.add(initializerBlock)
    builder.superinterfaces.putAll(superinterfaces)
    builder.primaryConstructor = primaryConstructor
    return builder
  }

  internal fun emit(codeWriter: CodeWriter, enumName: String?, isNestedExternal: Boolean = false) {
    // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
    // it back afterwards when this type is complete.
    val previousStatementLine = codeWriter.statementLine
    codeWriter.statementLine = -1

    val constructorProperties: Map<String, PropertySpec> = constructorProperties()
    val superclassConstructorParametersBlock = superclassConstructorParameters.joinToCode()

    try {
      if (enumName != null) {
        codeWriter.emitKdoc(kdocWithConstructorParameters())
        codeWriter.emitAnnotations(annotationSpecs, false)
        codeWriter.emitCode("%L", enumName)
        if (superclassConstructorParametersBlock.isNotEmpty()) {
          codeWriter.emit("(")
          codeWriter.emitCode(superclassConstructorParametersBlock)
          codeWriter.emit(")")
        }
        if (hasNoBody) {
          return // Avoid unnecessary braces "{}".
        }
        codeWriter.emit(" {\n")
      } else if (isAnonymousClass) {

        codeWriter.emitCode("object")
        val supertype = if (superclass != ANY) {
          listOf(CodeBlock.of(" %T(%L)", superclass, superclassConstructorParametersBlock))
        } else {
          listOf()
        }

        val allSuperTypes = supertype + if (superinterfaces.isNotEmpty())
          superinterfaces.keys.map { CodeBlock.of(" %T", it) } else
          emptyList()

        if (allSuperTypes.isNotEmpty()) {
          codeWriter.emitCode(" :")
          codeWriter.emitCode(allSuperTypes.joinToCode(","))
        }
        if (hasNoBody) {
          codeWriter.emit(" {\n}")
          return
        }
        codeWriter.emit(" {\n")
      } else {
        codeWriter.emitKdoc(kdocWithConstructorParameters())
        codeWriter.emitAnnotations(annotationSpecs, false)
        codeWriter.emitModifiers(modifiers,
            if (isNestedExternal) setOf(PUBLIC, EXTERNAL) else setOf(PUBLIC))
        codeWriter.emit(kind.declarationKeyword)
        if (name != null) {
          codeWriter.emitCode(" %L", name.escapeIfNecessary())
        }
        codeWriter.emitTypeVariables(typeVariables)

        primaryConstructor?.let {
          codeWriter.pushType(this) // avoid name collisions when emitting primary constructor
          var useKeyword = false
          var emittedAnnotations = false

          if (it.annotations.isNotEmpty()) {
            codeWriter.emit(" ")
            codeWriter.emitAnnotations(it.annotations, true)
            useKeyword = true
            emittedAnnotations = true
          }

          if (it.modifiers.isNotEmpty()) {
            if (!emittedAnnotations) codeWriter.emit(" ")
            codeWriter.emitModifiers(it.modifiers)
            useKeyword = true
          }

          if (useKeyword) {
            codeWriter.emit("constructor")
          }

          val forceNewLines = it.parameters
              .any { param -> constructorProperties[param.name]?.kdoc?.isNotEmpty() == true }

          it.parameters.emit(codeWriter, forceNewLines = forceNewLines) { param ->
            val property = constructorProperties[param.name]
            if (property != null) {
              property.emit(codeWriter, setOf(PUBLIC), withInitializer = false, inline = true)
              param.emitDefaultValue(codeWriter)
            } else {
              param.emit(codeWriter)
            }
          }
          codeWriter.popType()
        }

        val types = listOf(superclass).filter { it != ANY }.map {
          if (primaryConstructor != null || funSpecs.none(FunSpec::isConstructor)) {
            CodeBlock.of("%T(%L)", it, superclassConstructorParametersBlock)
          } else {
            CodeBlock.of("%T", it)
          }
        }
        val superTypes = types + superinterfaces.entries.map { (type, init) ->
            if (init == null)  CodeBlock.of("%T", type) else CodeBlock.of("%T by $init", type)
        }

        if (superTypes.isNotEmpty()) {
          codeWriter.emitCode(superTypes.joinToCode(separator = ", ", prefix = " : "))
        }

        codeWriter.emitWhereBlock(typeVariables)

        if (hasNoBody) {
          codeWriter.emit("\n")
          return // Avoid unnecessary braces "{}".
        }
        if (!isAnnotation) {
          codeWriter.emit(" {\n")
        }
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

      // Properties.
      for (propertySpec in propertySpecs) {
        if (constructorProperties.containsKey(propertySpec.name)) {
          continue
        }
        if (!firstMember) codeWriter.emit("\n")
        propertySpec.emit(codeWriter, kind.implicitPropertyModifiers(modifiers))
        firstMember = false
      }

      if (primaryConstructor != null && primaryConstructor.body.isNotEmpty()) {
        codeWriter.emit("init {\n")
        codeWriter.indent()
        codeWriter.emitCode(primaryConstructor.body)
        codeWriter.unindent()
        codeWriter.emit("}\n")
      }

      // Initializer block.
      if (initializerBlock.isNotEmpty()) {
        if (!firstMember) codeWriter.emit("\n")
        codeWriter.emitCode(initializerBlock)
        firstMember = false
      }

      // Constructors.
      for (funSpec in funSpecs) {
        if (!funSpec.isConstructor) continue
        if (!firstMember) codeWriter.emit("\n")
        funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers(modifiers), false)
        firstMember = false
      }

      // Functions.
      for (funSpec in funSpecs) {
        if (funSpec.isConstructor) continue
        if (!firstMember) codeWriter.emit("\n")
        funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers(modifiers), true)
        firstMember = false
      }

      // Types.
      val areNestedExternal = EXTERNAL in modifiers || isNestedExternal

      for (typeSpec in typeSpecs) {
        if (!firstMember) codeWriter.emit("\n")
        typeSpec.emit(codeWriter, null, isNestedExternal = areNestedExternal)
        firstMember = false
      }

      codeWriter.unindent()
      codeWriter.popType()

      if (!isAnnotation) {
        codeWriter.emit("}")
      }
      if (enumName == null && !isAnonymousClass) {
        codeWriter.emit("\n") // If this type isn't also a value, include a trailing newline.
      }
    } finally {
      codeWriter.statementLine = previousStatementLine
    }
  }

  /** Returns the properties that can be declared inline as constructor parameters. */
  private fun constructorProperties(): Map<String, PropertySpec> {
    if (primaryConstructor == null) return emptyMap()

    val result: MutableMap<String, PropertySpec> = LinkedHashMap()
    for (property in propertySpecs) {
      val parameter = primaryConstructor.parameter(property.name) ?: continue
      if (parameter.type != property.type) continue
      if (CodeBlock.of("%N", parameter) != CodeBlock.of("%N", property.initializer.toString()))
        continue

      result[property.name] = property.fromPrimaryConstructorParameter(parameter)
    }
    return result
  }

  /** Returns KDoc comments including those of primary constructor parameters. */
  private fun kdocWithConstructorParameters(): CodeBlock {
    if (primaryConstructor == null || primaryConstructor.parameters.isEmpty()) {
      return kdoc.ensureEndsWithNewLine()
    }

    val constructorProperties = constructorProperties()

    return with(kdoc.ensureEndsWithNewLine().toBuilder()) {
      primaryConstructor.parameters.forEachIndexed { index, parameterSpec ->
        val kdoc = parameterSpec.kdoc.takeUnless { it.isEmpty() }
                ?: constructorProperties[parameterSpec.name]?.kdoc?.takeUnless { it.isEmpty() }
        if (kdoc != null) {
          if (isNotEmpty() && index == 0) add("\n")
          val isProperty = parameterSpec.name in constructorProperties
          val tag = if (isProperty) "@property" else "@param"
          add("$tag %L %L", parameterSpec.name, kdoc.ensureEndsWithNewLine())
        }
      }
      build()
    }
  }

  private val hasNoBody: Boolean
    get() {
      if (isAnnotation) {
        return true
      }
      if (propertySpecs.isNotEmpty()) {
        val constructorProperties = constructorProperties()
        for (propertySpec in propertySpecs) {
          if (!constructorProperties.containsKey(propertySpec.name)) {
            return false
          }
        }
      }
      return enumConstants.isEmpty() &&
          initializerBlock.isEmpty() &&
          (primaryConstructor?.body?.isEmpty() ?: true) &&
          funSpecs.isEmpty() &&
          typeSpecs.isEmpty()
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildCodeString { emit(this, null) }

  enum class Kind(
    internal val declarationKeyword: String,
    internal val defaultImplicitPropertyModifiers: Set<KModifier>,
    internal val defaultImplicitFunctionModifiers: Set<KModifier>
  ) {
    CLASS("class", setOf(PUBLIC), setOf(PUBLIC)),
    OBJECT("object", setOf(PUBLIC), setOf(PUBLIC)),
    INTERFACE("interface", setOf(PUBLIC), setOf(PUBLIC, ABSTRACT));

    internal fun implicitPropertyModifiers(modifiers: Set<KModifier>): Set<KModifier> {
      return defaultImplicitPropertyModifiers + when {
        ANNOTATION in modifiers -> emptySet()
        EXPECT in modifiers -> setOf(EXPECT)
        EXTERNAL in modifiers -> setOf(EXTERNAL)
        else -> emptySet()
      }
    }

    internal fun implicitFunctionModifiers(modifiers: Set<KModifier> = setOf()): Set<KModifier> {
      return defaultImplicitFunctionModifiers + when {
        ANNOTATION in modifiers -> setOf(ABSTRACT)
        EXPECT in modifiers -> setOf(EXPECT)
        EXTERNAL in modifiers -> setOf(EXTERNAL)
        else -> emptySet()
      }
    }
  }

  class Builder internal constructor(
    internal var kind: Kind,
    internal val name: String?,
    vararg modifiers: KModifier
  ) {
    internal val kdoc = CodeBlock.builder()
    internal var primaryConstructor: FunSpec? = null
    internal var superclass: TypeName = ANY
    internal val initializerBlock = CodeBlock.builder()
    internal val isAnonymousClass get() = name == null && kind == Kind.CLASS
    internal val isEnum get() = kind == Kind.CLASS && ENUM in modifiers
    internal val isAnnotation get() = kind == Kind.CLASS && ANNOTATION in modifiers
    internal val isCompanion get() = kind == Kind.OBJECT && COMPANION in modifiers
    internal val isInlineClass get() = kind == Kind.CLASS && INLINE in modifiers
    internal val isSimpleClass get() = kind == Kind.CLASS && !isEnum && !isAnnotation

    val modifiers = mutableSetOf(*modifiers)
    val superinterfaces = mutableMapOf<TypeName, CodeBlock?>()
    val enumConstants = mutableMapOf<String, TypeSpec>()
    val annotationSpecs = mutableListOf<AnnotationSpec>()
    val typeVariables = mutableListOf<TypeVariableName>()
    val superclassConstructorParameters = mutableListOf<CodeBlock>()
    val propertySpecs = mutableListOf<PropertySpec>()
    val funSpecs = mutableListOf<FunSpec>()
    val typeSpecs = mutableListOf<TypeSpec>()

    init {
      require(name == null || name.isName) { "not a valid name: $name" }
    }

    fun addKdoc(format: String, vararg args: Any) = apply {
      kdoc.add(format, *args)
    }

    fun addKdoc(block: CodeBlock) = apply {
      kdoc.add(block)
    }

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>) = apply {
      this.annotationSpecs += annotationSpecs
    }

    fun addAnnotation(annotationSpec: AnnotationSpec) = apply {
      annotationSpecs += annotationSpec
    }

    fun addAnnotation(annotation: ClassName)
        = addAnnotation(AnnotationSpec.builder(annotation).build())

    fun addAnnotation(annotation: Class<*>) = addAnnotation(annotation.asClassName())

    fun addAnnotation(annotation: KClass<*>) = addAnnotation(annotation.asClassName())

    fun addModifiers(vararg modifiers: KModifier) = apply {
      check(!isAnonymousClass) { "forbidden on anonymous types." }
      this.modifiers += modifiers
    }

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeVariableName) = apply {
      typeVariables += typeVariable
    }

    fun primaryConstructor(primaryConstructor: FunSpec?) = apply {
      check(kind == Kind.CLASS) {
        "$kind can't have initializer blocks"
      }
      if (primaryConstructor != null) {
        require(primaryConstructor.isConstructor) {
          "expected a constructor but was ${primaryConstructor.name}"
        }

        if (isInlineClass) {
          check(primaryConstructor.parameters.size == 1) {
            "Inline classes must have 1 parameter in constructor"
          }
        }
      }
      this.primaryConstructor = primaryConstructor
    }

    fun superclass(superclass: TypeName) = apply {
      checkCanHaveSuperclass()
      check(this.superclass === ANY) { "superclass already set to ${this.superclass}" }
      this.superclass = superclass
    }

    private fun checkCanHaveSuperclass() {
      check(isSimpleClass || kind == Kind.OBJECT) {
        "only classes can have super classes, not $kind"
      }
      check(!isInlineClass) {
        "Inline classes cannot have super classes"
      }
    }

    private fun checkCanHaveInitializerBlocks() {
      check(isSimpleClass || isEnum || kind == Kind.OBJECT) {
        "$kind can't have initializer blocks"
      }
      check(!isInlineClass) {
        "Inline classes can't have initializer blocks"
      }
      check(EXPECT !in modifiers) {
        "expect $kind can't have initializer blocks"
      }
    }

    fun superclass(superclass: Type) = superclass(superclass.asTypeName())

    fun superclass(superclass: KClass<*>) = superclass(superclass.asTypeName())

    fun addSuperclassConstructorParameter(format: String, vararg args: Any) = apply {
      addSuperclassConstructorParameter(CodeBlock.of(format, *args))
    }

    fun addSuperclassConstructorParameter(codeBlock: CodeBlock) = apply {
      checkCanHaveSuperclass()
      this.superclassConstructorParameters += codeBlock
    }

    fun addSuperinterfaces(superinterfaces: Iterable<TypeName>) = apply {
      this.superinterfaces.putAll(superinterfaces.map { it to null })
    }

    fun addSuperinterface(superinterface: TypeName, delegate: CodeBlock = CodeBlock.EMPTY) = apply {
      if (delegate.isEmpty()) {
        this.superinterfaces[superinterface] = null
      } else {
        require(isSimpleClass) {
          "delegation only allowed for classes (found $kind '$name')"
        }
        require(!superinterface.isNullable) {
          "expected non-nullable type but was '${superinterface.copy(nullable = false)}'"
        }
        require(this.superinterfaces[superinterface] == null) {
          "'$name' can not delegate to $superinterface by $delegate with existing declaration by " +
              "${this.superinterfaces[superinterface]}"
        }
        this.superinterfaces[superinterface] = delegate
      }
    }

    fun addSuperinterface(superinterface: Type, delegate: CodeBlock = CodeBlock.EMPTY)
        = addSuperinterface(superinterface.asTypeName(), delegate)

    fun addSuperinterface(superinterface: KClass<*>, delegate: CodeBlock = CodeBlock.EMPTY)
        = addSuperinterface(superinterface.asTypeName(), delegate)

    fun addSuperinterface(superinterface: KClass<*>, constructorParameterName: String) =
        addSuperinterface(superinterface.asTypeName(), constructorParameterName)

    fun addSuperinterface(superinterface: TypeName, constructorParameter: String) = apply {
      requireNotNull(primaryConstructor) {
        "delegating to constructor parameter requires not-null constructor"
      }
      val parameter = primaryConstructor?.parameter(constructorParameter)
      requireNotNull(parameter) {
        "no such constructor parameter '$constructorParameter' to delegate to for type '$name'"
      }
      addSuperinterface(superinterface, CodeBlock.of(constructorParameter))
    }

    @JvmOverloads fun addEnumConstant(
      name: String,
      typeSpec: TypeSpec = anonymousClassBuilder().build()
    ) = apply {
      enumConstants[name] = typeSpec
    }

    fun addProperties(propertySpecs: Iterable<PropertySpec>) = apply {
      propertySpecs.map(this::addProperty)
    }

    fun addProperty(propertySpec: PropertySpec) = apply {
      if (EXPECT in modifiers) {
        require(propertySpec.initializer == null) {
          "properties in expect classes can't have initializers"
        }
        require(propertySpec.getter == null && propertySpec.setter == null) {
          "properties in expect classes can't have getters and setters"
        }
      }
      propertySpecs += propertySpec
    }

    fun addProperty(name: String, type: TypeName, vararg modifiers: KModifier)
        = addProperty(PropertySpec.builder(name, type, *modifiers).build())

    fun addProperty(name: String, type: Type, vararg modifiers: KModifier)
        = addProperty(name, type.asTypeName(), *modifiers)

    fun addProperty(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = addProperty(name, type.asTypeName(), *modifiers)

    fun addInitializerBlock(block: CodeBlock) = apply {
      checkCanHaveInitializerBlocks()
      initializerBlock.add("init {\n")
          .indent()
          .add(block)
          .unindent()
          .add("}\n")
    }

    fun addFunctions(funSpecs: Iterable<FunSpec>) = apply {
      funSpecs.forEach { addFunction(it) }
    }

    fun addFunction(funSpec: FunSpec) = apply {
      funSpecs += funSpec
    }

    fun addTypes(typeSpecs: Iterable<TypeSpec>) = apply {
      this.typeSpecs += typeSpecs
    }

    fun addType(typeSpec: TypeSpec) = apply {
      typeSpecs += typeSpec
    }

    fun build(): TypeSpec {
      if (enumConstants.isNotEmpty()) {
        check(isEnum) { "${this.name} is not enum and cannot have enum constants" }
        for (it in enumConstants.keys) {
          require(it.isName) { "not a valid enum constant: $name" }
        }
      }

      if (superclassConstructorParameters.isNotEmpty()) {
        checkCanHaveSuperclass()
      }

      check(!(isAnonymousClass && typeVariables.isNotEmpty())) {
        "typevariables are forbidden on anonymous types"
      }

      val isAbstract = ABSTRACT in modifiers || SEALED in modifiers || kind != Kind.CLASS || !isSimpleClass
      for (funSpec in funSpecs) {
        require(isAbstract || ABSTRACT !in funSpec.modifiers) {
          "non-abstract type $name cannot declare abstract function ${funSpec.name}"
        }
        when {
          kind == Kind.INTERFACE -> {
            requireNoneOf(funSpec.modifiers, KModifier.INTERNAL, KModifier.PROTECTED)
            requireNoneOrOneOf(funSpec.modifiers, KModifier.ABSTRACT, KModifier.PRIVATE)
          }
          isAnnotation -> require(funSpec.modifiers == kind.implicitFunctionModifiers(modifiers)) {
            "annotation class $name.${funSpec.name} " +
                "requires modifiers ${kind.implicitFunctionModifiers(modifiers)}"
          }
          EXPECT in modifiers -> require(funSpec.body.isEmpty()) {
            "functions in expect classes can't have bodies"
          }
        }
      }

      if (primaryConstructor == null) {
        require(funSpecs.none { it.isConstructor } || superclassConstructorParameters.isEmpty()) {
          "types without a primary constructor cannot specify secondary constructors and superclass constructor parameters"
        }
      }

      if (isInlineClass) {
        primaryConstructor?.let {
          check(it.parameters.size == 1) {
            "Inline classes must have 1 parameter in constructor"
          }
          check(PRIVATE !in it.modifiers && INTERNAL !in it.modifiers) {
            "Inline classes must have a public primary constructor"
          }
        }

        check(propertySpecs.size > 0) {
          "Inline classes must have at least 1 property"
        }

        val constructorParamName = primaryConstructor?.parameters?.firstOrNull()?.name
        constructorParamName?.let { paramName ->
          val underlyingProperty = propertySpecs.find { it.name == paramName }
          requireNotNull(underlyingProperty) {
            "Inline classes must have a single read-only (val) property parameter."
          }
          check(!underlyingProperty.mutable) {
            "Inline classes must have a single read-only (val) property parameter."
          }
        }
        check(initializerBlock.isEmpty()) {
          "Inline classes can't have initializer blocks"
        }
        check(superclass == Any::class.asTypeName()) {
          "Inline classes cannot have super classes"
        }
      }

      val companionObjectsCount = typeSpecs.count { it.isCompanion }
      when (companionObjectsCount) {
        0 -> Unit
        1 -> {
          require(isSimpleClass || kind == Kind.INTERFACE || isEnum) {
            "$kind types can't have a companion object"
          }
        }
        else -> {
          throw IllegalArgumentException("Multiple companion objects are present but only one is allowed.")
        }
      }

      return TypeSpec(this)
    }
  }

  companion object {
    @JvmStatic fun classBuilder(name: String) = Builder(Kind.CLASS, name)

    @JvmStatic fun classBuilder(className: ClassName) = classBuilder(className.simpleName)

    @JvmStatic fun expectClassBuilder(name: String) = Builder(Kind.CLASS, name, EXPECT)

    @JvmStatic fun expectClassBuilder(className: ClassName) = expectClassBuilder(className.simpleName)

    @JvmStatic fun objectBuilder(name: String) = Builder(Kind.OBJECT, name)

    @JvmStatic fun objectBuilder(className: ClassName) = objectBuilder(className.simpleName)

    @JvmStatic @JvmOverloads fun companionObjectBuilder(name: String? = null) =
        Builder(Kind.OBJECT, name, COMPANION)

    @JvmStatic fun interfaceBuilder(name: String) = Builder(Kind.INTERFACE, name)

    @JvmStatic fun interfaceBuilder(className: ClassName) = interfaceBuilder(className.simpleName)

    @JvmStatic fun enumBuilder(name: String) = Builder(Kind.CLASS, name, ENUM)

    @JvmStatic fun enumBuilder(className: ClassName) = enumBuilder(className.simpleName)

    @JvmStatic fun anonymousClassBuilder(): Builder {
      return Builder(Kind.CLASS, null)
    }

    @JvmStatic fun annotationBuilder(name: String) = Builder(Kind.CLASS, name, ANNOTATION)

    @JvmStatic fun annotationBuilder(className: ClassName)
        = annotationBuilder(className.simpleName)
  }
}
