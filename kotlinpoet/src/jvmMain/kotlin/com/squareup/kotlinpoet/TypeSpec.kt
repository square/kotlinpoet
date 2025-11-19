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

import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.ANNOTATION
import com.squareup.kotlinpoet.KModifier.COMPANION
import com.squareup.kotlinpoet.KModifier.ENUM
import com.squareup.kotlinpoet.KModifier.EXPECT
import com.squareup.kotlinpoet.KModifier.EXTERNAL
import com.squareup.kotlinpoet.KModifier.FUN
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PROTECTED
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.KModifier.SEALED
import com.squareup.kotlinpoet.KModifier.VALUE
import java.lang.IllegalArgumentException
import java.lang.reflect.Type
import javax.lang.model.element.Element
import kotlin.DeprecationLevel.ERROR
import kotlin.reflect.KClass

/** A generated class, interface, or enum declaration. */
@OptIn(ExperimentalKotlinPoetApi::class)
public class TypeSpec
private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap(),
  private val delegateOriginatingElements: OriginatingElementsHolder =
    builder.originatingElements
      .plus(builder.typeSpecs.flatMap(TypeSpec::originatingElements))
      .buildOriginatingElements(),
  private val contextReceivers: ContextReceivers = builder.buildContextReceivers(),
) :
  Taggable by tagMap,
  OriginatingElementsHolder by delegateOriginatingElements,
  ContextReceivable by contextReceivers,
  Annotatable,
  Documentable,
  TypeSpecHolder,
  MemberSpecHolder {
  public val kind: Kind = builder.kind
  public val name: String? = builder.name
  override val kdoc: CodeBlock = builder.kdoc.build()
  override val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  public val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()
  public val typeVariables: List<TypeVariableName> = builder.typeVariables.toImmutableList()
  public val primaryConstructor: FunSpec? = builder.primaryConstructor
  public val superclass: TypeName = builder.superclass
  public val superclassConstructorParameters: List<CodeBlock> =
    builder.superclassConstructorParameters.toImmutableList()

  public val isEnum: Boolean = builder.isEnum
  public val isAnnotation: Boolean = builder.isAnnotation
  public val isCompanion: Boolean = builder.isCompanion
  public val isAnonymousClass: Boolean = builder.isAnonymousClass
  public val isFunctionalInterface: Boolean = builder.isFunInterface

  /**
   * Map of superinterfaces - entries with a null value represent a regular superinterface (with no
   * delegation), while non-null [CodeBlock] values represent delegates for the corresponding
   * [TypeSpec] interface (key) value
   */
  public val superinterfaces: Map<TypeName, CodeBlock?> = builder.superinterfaces.toImmutableMap()
  public val enumConstants: Map<String, TypeSpec> = builder.enumConstants.toImmutableMap()
  override val propertySpecs: List<PropertySpec> = builder.propertySpecs.toImmutableList()
  public val initializerBlock: CodeBlock = builder.initializerBlock.build()
  public val initializerIndex: Int = builder.initializerIndex
  override val funSpecs: List<FunSpec> = builder.funSpecs.toImmutableList()
  public override val typeSpecs: List<TypeSpec> = builder.typeSpecs.toImmutableList()
  public val typeAliasSpecs: List<TypeAliasSpec> = builder.typeAliasSpecs.toImmutableList()
  internal val nestedTypesSimpleNames = typeSpecs.map { it.name }.toImmutableSet()

  @Deprecated("Use annotations property", ReplaceWith("annotations"), ERROR)
  public val annotationSpecs: List<AnnotationSpec>
    get() = annotations

  @JvmOverloads
  public fun toBuilder(kind: Kind = this.kind, name: String? = this.name): Builder {
    val builder = Builder(kind, name)
    builder.modifiers += modifiers
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.typeVariables += typeVariables
    builder.superclass = superclass
    builder.superclassConstructorParameters += superclassConstructorParameters
    builder.enumConstants += enumConstants
    builder.propertySpecs += propertySpecs
    builder.funSpecs += funSpecs
    builder.typeSpecs += typeSpecs
    builder.typeAliasSpecs += typeAliasSpecs
    builder.initializerBlock.add(initializerBlock)
    builder.initializerIndex = initializerIndex
    builder.superinterfaces.putAll(superinterfaces)
    builder.primaryConstructor = primaryConstructor
    builder.tags += tagMap.tags
    builder.originatingElements += originatingElements
    builder.contextReceiverTypes += contextReceiverTypes
    return builder
  }

  internal fun emit(
    codeWriter: CodeWriter,
    enumName: String?,
    implicitModifiers: Set<KModifier> = emptySet(),
    isNestedExternal: Boolean = false,
  ) {
    // Types.
    val areNestedExternal = EXTERNAL in modifiers || isNestedExternal

    // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
    // it back afterwards when this type is complete.
    val previousStatementLine = codeWriter.statementLine
    codeWriter.statementLine = -1

    val constructorProperties: Map<String, PropertySpec> = constructorProperties()
    val superclassConstructorParametersBlock = superclassConstructorParameters.joinToCode()

    try {
      if (enumName != null) {
        codeWriter.emitKdoc(kdocWithConstructorDocs())
        codeWriter.emitAnnotations(annotations, false)
        codeWriter.emitCode("%N", enumName)
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
        val supertype =
          if (superclass != ANY) {
            if (!areNestedExternal && !modifiers.contains(EXPECT)) {
              listOf(CodeBlock.of("%T(%L)", superclass, superclassConstructorParametersBlock))
            } else {
              listOf(CodeBlock.of("%T", superclass))
            }
          } else {
            listOf()
          }

        val allSuperTypes =
          supertype +
            superinterfaces.entries.map { (type, init) ->
              if (init == null) {
                CodeBlock.of("%T", type)
              } else {
                CodeBlock.of("%T by %L", type, init)
              }
            }

        if (allSuperTypes.isNotEmpty()) {
          codeWriter.emitCode(allSuperTypes.joinToCode(prefix = " : "))
        }
        if (hasNoBody) {
          codeWriter.emit(" {\n}")
          return
        }
        codeWriter.emit(" {\n")
      } else {
        codeWriter.emitKdoc(kdocWithConstructorDocs())
        codeWriter.emitContextReceivers(contextReceiverTypes, suffix = "\n")
        codeWriter.emitAnnotations(annotations, false)
        codeWriter.emitModifiers(
          modifiers,
          if (isNestedExternal) setOf(PUBLIC, EXTERNAL) else setOf(PUBLIC),
        )
        codeWriter.emit(kind.declarationKeyword)
        if (name != null) {
          codeWriter.emitCode(" %N", this)
        }
        codeWriter.emitTypeVariables(typeVariables)

        var wrapSupertypes = false
        primaryConstructor?.let {
          codeWriter.pushType(this) // avoid name collisions when emitting primary constructor
          val emittedAnnotations = it.annotations.isNotEmpty()
          val useKeyword = it.annotations.isNotEmpty() || it.modifiers.isNotEmpty()

          if (it.annotations.isNotEmpty()) {
            codeWriter.emit(" ")
            codeWriter.emitAnnotations(it.annotations, true)
          }

          if (it.modifiers.isNotEmpty()) {
            if (!emittedAnnotations) codeWriter.emit(" ")
            codeWriter.emitModifiers(it.modifiers)
          }

          if (useKeyword) {
            codeWriter.emit("constructor")
          }

          it.parameters.emit(codeWriter, forceNewLines = true) { param ->
            wrapSupertypes = true

            val property = constructorProperties[param.name]
            if (property != null) {
              property.emit(
                codeWriter,
                setOf(PUBLIC),
                withInitializer = false,
                inline = true,
                inlineAnnotations = false,
              )
              param.emitDefaultValue(codeWriter)
            } else {
              param.emit(codeWriter, inlineAnnotations = false)
            }
          }

          codeWriter.popType()
        }

        val types =
          listOf(superclass)
            .filter { it != ANY }
            .map {
              if (primaryConstructor != null || funSpecs.none(FunSpec::isConstructor)) {
                if (!areNestedExternal && !modifiers.contains(EXPECT)) {
                  CodeBlock.of("%T(%L)", it, superclassConstructorParametersBlock)
                } else {
                  CodeBlock.of("%T", it)
                }
              } else {
                CodeBlock.of("%T", it)
              }
            }
        val superTypes =
          types +
            superinterfaces.entries.map { (type, init) ->
              if (init == null) CodeBlock.of("%T", type) else CodeBlock.of("%T by %L", type, init)
            }

        if (superTypes.isNotEmpty()) {
          val separator = if (wrapSupertypes) ",\n    " else ",♢"
          codeWriter.emitCode(superTypes.joinToCode(separator = separator, prefix = " : "))
        }

        codeWriter.emitWhereBlock(typeVariables)

        if (hasNoBody) {
          codeWriter.emit("\n")
          return // Avoid unnecessary braces "{}".
        }
        codeWriter.emit(" {\n")
      }

      codeWriter.pushType(this)
      codeWriter.indent()
      var firstMember = true
      for ((key, value) in enumConstants.entries) {
        if (!firstMember) codeWriter.emit("\n")
        value.emit(codeWriter, key)
        codeWriter.emit(",")
        firstMember = false
      }
      if (isEnum) {
        if (!firstMember) {
          codeWriter.emit("\n")
        }
        if (
          propertySpecs.isNotEmpty() ||
            funSpecs.isNotEmpty() ||
            typeSpecs.isNotEmpty() ||
            initializerBlock.isNotEmpty()
        ) {
          codeWriter.emit(";\n")
        }
      }

      val cachedHasInitializer = hasInitializer
      var initializerEmitted = false
      fun possiblyEmitInitializer() {
        if (initializerEmitted) return
        initializerEmitted = true
        if (cachedHasInitializer) {
          if (!firstMember) codeWriter.emit("\n")
          codeWriter.emitCode(initializerBlock)
          firstMember = false
        }
      }

      // Properties and initializer block.
      for ((index, propertySpec) in propertySpecs.withIndex()) {
        // Initializer block.
        if (index == initializerIndex) {
          possiblyEmitInitializer()
        }
        if (constructorProperties.containsKey(propertySpec.name)) {
          continue
        }
        if (!firstMember) codeWriter.emit("\n")
        propertySpec.emit(codeWriter, kind.implicitPropertyModifiers(modifiers))
        firstMember = false
      }

      // One last try in case the initializer index is after all properties
      possiblyEmitInitializer()

      if (primaryConstructor != null && primaryConstructor.body.isNotEmpty()) {
        codeWriter.emit("init {\n")
        codeWriter.indent()
        codeWriter.emitCode(primaryConstructor.body)
        codeWriter.unindent()
        codeWriter.emit("}\n")
      }

      // Constructors.
      for (funSpec in funSpecs) {
        if (!funSpec.isConstructor) continue
        if (!firstMember) codeWriter.emit("\n")
        funSpec.emit(
          codeWriter,
          name,
          kind.implicitFunctionModifiers(modifiers + implicitModifiers),
          false,
        )
        firstMember = false
      }

      // Functions.
      for (funSpec in funSpecs) {
        if (funSpec.isConstructor) continue
        if (!firstMember) codeWriter.emit("\n")
        funSpec.emit(
          codeWriter,
          name,
          kind.implicitFunctionModifiers(modifiers + implicitModifiers),
          true,
        )
        firstMember = false
      }

      for (typeSpec in typeSpecs) {
        if (!firstMember) codeWriter.emit("\n")
        typeSpec.emit(
          codeWriter,
          null,
          kind.implicitTypeModifiers(modifiers + implicitModifiers),
          isNestedExternal = areNestedExternal,
        )
        firstMember = false
      }

      for (typeAliasSpec in typeAliasSpecs) {
        if (!firstMember) codeWriter.emit("\n")
        typeAliasSpec.emit(codeWriter)
        firstMember = false
      }

      codeWriter.unindent()
      codeWriter.popType()

      codeWriter.emit("}")
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

    // Properties added after the initializer are not permitted to be inlined into the constructor
    // due to ordering concerns.
    val range =
      if (hasInitializer) {
        0..<initializerIndex
      } else {
        propertySpecs.indices
      }
    val result: MutableMap<String, PropertySpec> = LinkedHashMap()
    for (propertyIndex in range) {
      val property = propertySpecs[propertyIndex]
      if (property.getter != null || property.setter != null) continue
      val parameter = primaryConstructor.parameter(property.name) ?: continue
      if (parameter.type != property.type) continue
      if (!isPropertyInitializerConstructorParameter(property, parameter)) {
        continue
      }

      result[property.name] = property.fromPrimaryConstructorParameter(parameter)
    }
    return result
  }

  /** Returns true if the property can be declared inline as a constructor parameter */
  private fun isPropertyInitializerConstructorParameter(
    property: PropertySpec,
    parameter: ParameterSpec,
  ): Boolean {
    val parameterName = CodeBlock.of("%N", parameter).toString()
    val initializerCode = property.initializer.toString().escapeIfNecessary(validate = false)
    return parameterName == initializerCode
  }

  /**
   * Returns KDoc comments including those of the primary constructor and its parameters.
   *
   * Parameters' KDocs, if present, will always be printed in the type header.
   */
  private fun kdocWithConstructorDocs(): CodeBlock {
    val classKdoc = kdoc.ensureEndsWithNewLine()
    val constructorKdoc = buildCodeBlock {
      if (primaryConstructor != null) {
        if (primaryConstructor.kdoc.isNotEmpty()) {
          add("@constructor %L", primaryConstructor.kdoc.ensureEndsWithNewLine())
        }
        primaryConstructor.parameters.forEach { parameter ->
          if (parameter.kdoc.isNotEmpty()) {
            add("@param %L %L", parameter.name, parameter.kdoc.ensureEndsWithNewLine())
          }
        }
      }
    }
    return listOf(classKdoc, constructorKdoc)
      .filter(CodeBlock::isNotEmpty)
      .joinToCode(separator = "\n")
  }

  private val hasInitializer: Boolean
    get() = initializerIndex != -1 && initializerBlock.isNotEmpty()

  private val hasNoBody: Boolean
    get() {
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
        typeSpecs.isEmpty() &&
        typeAliasSpecs.isEmpty()
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int = toString().hashCode()

  override fun toString(): String = buildCodeString { emit(this, null) }

  public enum class Kind(
    internal val declarationKeyword: String,
    internal val defaultImplicitPropertyModifiers: Set<KModifier>,
    internal val defaultImplicitFunctionModifiers: Set<KModifier>,
    internal val defaultImplicitTypeModifiers: Set<KModifier>,
  ) {
    CLASS("class", setOf(PUBLIC), setOf(PUBLIC), setOf()),
    OBJECT("object", setOf(PUBLIC), setOf(PUBLIC), setOf()),
    INTERFACE("interface", setOf(PUBLIC, ABSTRACT), setOf(PUBLIC, ABSTRACT), setOf());

    internal fun implicitPropertyModifiers(modifiers: Set<KModifier>): Set<KModifier> {
      return defaultImplicitPropertyModifiers +
        when {
          ANNOTATION in modifiers -> emptySet()
          EXPECT in modifiers -> setOf(EXPECT)
          EXTERNAL in modifiers -> setOf(EXTERNAL)
          else -> emptySet()
        }
    }

    internal fun implicitFunctionModifiers(modifiers: Set<KModifier> = setOf()): Set<KModifier> {
      return defaultImplicitFunctionModifiers +
        when {
          EXPECT in modifiers -> setOf(EXPECT)
          EXTERNAL in modifiers -> setOf(EXTERNAL)
          else -> emptySet()
        }
    }

    internal fun implicitTypeModifiers(modifiers: Set<KModifier> = setOf()): Set<KModifier> {
      return defaultImplicitTypeModifiers +
        when {
          EXPECT in modifiers -> setOf(EXPECT)
          EXTERNAL in modifiers -> setOf(EXTERNAL)
          else -> emptySet()
        }
    }
  }

  public class Builder
  internal constructor(
    internal var kind: Kind,
    internal val name: String?,
    vararg modifiers: KModifier,
  ) :
    Taggable.Builder<Builder>,
    OriginatingElementsHolder.Builder<Builder>,
    ContextReceivable.Builder<Builder>,
    Annotatable.Builder<Builder>,
    Documentable.Builder<Builder>,
    TypeSpecHolder.Builder<Builder>,
    MemberSpecHolder.Builder<Builder> {
    internal var primaryConstructor: FunSpec? = null
    internal var superclass: TypeName = ANY
    internal val initializerBlock = CodeBlock.builder()
    public var initializerIndex: Int = -1
    internal val isAnonymousClass
      get() = name == null && kind == Kind.CLASS

    internal val isExternal
      get() = EXTERNAL in modifiers

    internal val isEnum
      get() = kind == Kind.CLASS && ENUM in modifiers

    internal val isAnnotation
      get() = kind == Kind.CLASS && ANNOTATION in modifiers

    internal val isCompanion
      get() = kind == Kind.OBJECT && COMPANION in modifiers

    internal val isInlineOrValClass
      get() = kind == Kind.CLASS && (INLINE in modifiers || VALUE in modifiers)

    internal val isSimpleClass
      get() = kind == Kind.CLASS && !isEnum && !isAnnotation

    internal val isFunInterface
      get() = kind == Kind.INTERFACE && FUN in modifiers

    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()
    override val kdoc: CodeBlock.Builder = CodeBlock.builder()
    override val originatingElements: MutableList<Element> = mutableListOf()
    override val annotations: MutableList<AnnotationSpec> = mutableListOf()

    @ExperimentalKotlinPoetApi
    override val contextReceiverTypes: MutableList<TypeName> = mutableListOf()
    public val modifiers: MutableSet<KModifier> = mutableSetOf(*modifiers)
    public val superinterfaces: MutableMap<TypeName, CodeBlock?> = mutableMapOf()
    public val enumConstants: MutableMap<String, TypeSpec> = mutableMapOf()
    public val typeVariables: MutableList<TypeVariableName> = mutableListOf()
    public val superclassConstructorParameters: MutableList<CodeBlock> = mutableListOf()
    public val propertySpecs: MutableList<PropertySpec> = mutableListOf()
    public val funSpecs: MutableList<FunSpec> = mutableListOf()
    public val typeSpecs: MutableList<TypeSpec> = mutableListOf()
    public val typeAliasSpecs: MutableList<TypeAliasSpec> = mutableListOf()

    @Deprecated("Use annotations property", ReplaceWith("annotations"), ERROR)
    public val annotationSpecs: MutableList<AnnotationSpec>
      get() = annotations

    public fun addModifiers(vararg modifiers: KModifier): Builder = apply {
      check(!isAnonymousClass) { "forbidden on anonymous types." }
      this.modifiers += modifiers
    }

    public fun addModifiers(modifiers: Iterable<KModifier>): Builder = apply {
      check(!isAnonymousClass) { "forbidden on anonymous types." }
      this.modifiers += modifiers
    }

    public fun addTypeVariables(typeVariables: Iterable<TypeVariableName>): Builder = apply {
      this.typeVariables += typeVariables
    }

    public fun addTypeVariable(typeVariable: TypeVariableName): Builder = apply {
      typeVariables += typeVariable
    }

    public fun primaryConstructor(primaryConstructor: FunSpec?): Builder = apply {
      check(kind == Kind.CLASS) { "$kind can't have a primary constructor" }
      if (primaryConstructor != null) {
        require(primaryConstructor.isConstructor) {
          "expected a constructor but was ${primaryConstructor.name}"
        }

        if (isInlineOrValClass) {
          check(primaryConstructor.parameters.size == 1) {
            "value/inline classes must have 1 parameter in constructor"
          }
        }

        require(
          primaryConstructor.delegateConstructor == null &&
            primaryConstructor.delegateConstructorArguments.isEmpty()
        ) {
          "primary constructor can't delegate to other constructors"
        }
      }
      this.primaryConstructor = primaryConstructor
    }

    public fun superclass(superclass: TypeName): Builder = apply {
      checkCanHaveSuperclass()
      check(this.superclass === ANY) { "superclass already set to ${this.superclass}" }
      this.superclass = superclass
    }

    private fun checkCanHaveSuperclass() {
      check(isSimpleClass || kind == Kind.OBJECT) {
        "only classes can have super classes, not $kind"
      }
      check(!isInlineOrValClass) { "value/inline classes cannot have super classes" }
    }

    private fun checkCanHaveInitializerBlocks() {
      check(isSimpleClass || isEnum || kind == Kind.OBJECT) {
        "$kind can't have initializer blocks"
      }
      check(EXPECT !in modifiers) { "expect $kind can't have initializer blocks" }
    }

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    public fun superclass(superclass: Type): Builder = superclass(superclass.asTypeName())

    public fun superclass(superclass: KClass<*>): Builder = superclass(superclass.asTypeName())

    public fun addSuperclassConstructorParameter(format: String, vararg args: Any): Builder =
      apply {
        addSuperclassConstructorParameter(CodeBlock.of(format, *args))
      }

    public fun addSuperclassConstructorParameter(codeBlock: CodeBlock): Builder = apply {
      checkCanHaveSuperclass()
      this.superclassConstructorParameters += codeBlock
    }

    public fun addSuperinterfaces(superinterfaces: Iterable<TypeName>): Builder = apply {
      this.superinterfaces.putAll(superinterfaces.map { it to null })
    }

    public fun addSuperinterface(
      superinterface: TypeName,
      delegate: CodeBlock = CodeBlock.EMPTY,
    ): Builder = apply {
      if (delegate.isEmpty()) {
        this.superinterfaces[superinterface] = null
      } else {
        require(isSimpleClass || kind == Kind.OBJECT) {
          "delegation only allowed for classes and objects (found $kind '$name')"
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

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    public fun addSuperinterface(
      superinterface: Type,
      delegate: CodeBlock = CodeBlock.EMPTY,
    ): Builder = addSuperinterface(superinterface.asTypeName(), delegate)

    public fun addSuperinterface(
      superinterface: KClass<*>,
      delegate: CodeBlock = CodeBlock.EMPTY,
    ): Builder = addSuperinterface(superinterface.asTypeName(), delegate)

    public fun addSuperinterface(
      superinterface: KClass<*>,
      constructorParameterName: String,
    ): Builder = addSuperinterface(superinterface.asTypeName(), constructorParameterName)

    public fun addSuperinterface(superinterface: TypeName, constructorParameter: String): Builder =
      apply {
        requireNotNull(primaryConstructor) {
          "delegating to constructor parameter requires not-null constructor"
        }
        val parameter = primaryConstructor?.parameter(constructorParameter)
        requireNotNull(parameter) {
          "no such constructor parameter '$constructorParameter' to delegate to for type '$name'"
        }
        addSuperinterface(superinterface, CodeBlock.of(constructorParameter))
      }

    @JvmOverloads
    public fun addEnumConstant(
      name: String,
      typeSpec: TypeSpec = anonymousClassBuilder().build(),
    ): Builder = apply {
      require(name != "name" && name != "ordinal") {
        "constant with name \"$name\" conflicts with a supertype member with the same name"
      }
      enumConstants[name] = typeSpec
    }

    override fun addProperty(propertySpec: PropertySpec): Builder = apply {
      if (EXPECT in modifiers) {
        require(propertySpec.initializer == null) {
          "properties in expect classes can't have initializers"
        }
        require(propertySpec.getter == null && propertySpec.setter == null) {
          "properties in expect classes can't have getters and setters"
        }
      }
      if (isEnum) {
        require(propertySpec.name != "name" && propertySpec.name != "ordinal") {
          "${propertySpec.name} is a final supertype member and can't be redeclared or overridden"
        }
      }
      propertySpecs += propertySpec
    }

    public fun addInitializerBlock(block: CodeBlock): Builder = apply {
      checkCanHaveInitializerBlocks()
      // Set index to however many properties we have
      // All properties added after this point are declared as such, including any that initialize
      // to a constructor param.
      initializerIndex = propertySpecs.size
      initializerBlock.add("init {\n").indent().add(block).unindent().add("}\n")
    }

    override fun addFunction(funSpec: FunSpec): Builder = apply { funSpecs += funSpec }

    override fun addType(typeSpec: TypeSpec): Builder = apply { typeSpecs += typeSpec }

    public fun addTypeAlias(typeAliasSpec: TypeAliasSpec): Builder = apply {
      typeAliasSpecs += typeAliasSpec
    }

    @ExperimentalKotlinPoetApi
    override fun contextReceivers(receiverTypes: Iterable<TypeName>): Builder = apply {
      check(isSimpleClass) { "contextReceivers can only be applied on simple classes" }
      contextReceiverTypes += receiverTypes
    }

    // region Overrides for binary compatibility
    @Suppress("RedundantOverride")
    override fun addAnnotation(annotationSpec: AnnotationSpec): Builder =
      super.addAnnotation(annotationSpec)

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

    @Suppress("RedundantOverride")
    override fun addKdoc(format: String, vararg args: Any): Builder = super.addKdoc(format, *args)

    @Suppress("RedundantOverride")
    override fun addKdoc(block: CodeBlock): Builder = super.addKdoc(block)

    @Suppress("RedundantOverride")
    override fun addTypes(typeSpecs: Iterable<TypeSpec>): Builder = super.addTypes(typeSpecs)

    @Suppress("RedundantOverride")
    override fun addProperties(propertySpecs: Iterable<PropertySpec>): Builder =
      super.addProperties(propertySpecs)

    @Suppress("RedundantOverride")
    override fun addProperty(name: String, type: TypeName, vararg modifiers: KModifier): Builder =
      super.addProperty(name, type, *modifiers)

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    override fun addProperty(name: String, type: Type, vararg modifiers: KModifier): Builder =
      super.addProperty(name, type, *modifiers)

    @Suppress("RedundantOverride")
    override fun addProperty(name: String, type: KClass<*>, vararg modifiers: KModifier): Builder =
      super.addProperty(name, type, *modifiers)

    @Suppress("RedundantOverride")
    override fun addProperty(
      name: String,
      type: TypeName,
      modifiers: Iterable<KModifier>,
    ): Builder = super.addProperty(name, type, modifiers)

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    override fun addProperty(name: String, type: Type, modifiers: Iterable<KModifier>): Builder =
      super.addProperty(name, type, modifiers)

    @Suppress("RedundantOverride")
    override fun addProperty(
      name: String,
      type: KClass<*>,
      modifiers: Iterable<KModifier>,
    ): Builder = super.addProperty(name, type, modifiers)

    @Suppress("RedundantOverride")
    override fun addFunctions(funSpecs: Iterable<FunSpec>): Builder = super.addFunctions(funSpecs)

    // endregion

    public fun build(): TypeSpec {
      if (enumConstants.isNotEmpty()) {
        check(isEnum) { "$name is not an enum and cannot have enum constants" }
      }

      if (superclassConstructorParameters.isNotEmpty()) {
        checkCanHaveSuperclass()

        check(!isExternal) { "delegated constructor call in external class is not allowed" }
      }
      check(!(isExternal && funSpecs.any { it.delegateConstructor != null })) {
        "delegated constructor call in external class is not allowed"
      }

      check(!(isAnonymousClass && typeVariables.isNotEmpty())) {
        "typevariables are forbidden on anonymous types"
      }

      val isAbstract =
        ABSTRACT in modifiers || SEALED in modifiers || kind == Kind.INTERFACE || isEnum
      for (funSpec in funSpecs) {
        require(isAbstract || ABSTRACT !in funSpec.modifiers) {
          "non-abstract type $name cannot declare abstract function ${funSpec.name}"
        }
        when {
          kind == Kind.INTERFACE -> {
            requireNoneOf(funSpec.modifiers, INTERNAL, PROTECTED)
            requireNoneOrOneOf(funSpec.modifiers, ABSTRACT, PRIVATE)
          }
          isAnnotation -> {
            throw IllegalArgumentException(
              "annotation class $name cannot declare member function ${funSpec.name}"
            )
          }
          EXPECT in modifiers ->
            require(funSpec.body.isEmpty()) { "functions in expect classes can't have bodies" }
        }
      }

      for (propertySpec in propertySpecs) {
        require(isAbstract || ABSTRACT !in propertySpec.modifiers) {
          "non-abstract type $name cannot declare abstract property ${propertySpec.name}"
        }
        if (propertySpec.contextParameters.isNotEmpty()) {
          if (
            ABSTRACT !in kind.implicitPropertyModifiers(modifiers) &&
              ABSTRACT !in propertySpec.modifiers
          ) {
            val errors = buildList {
              if (propertySpec.getter == null) {
                add("non-abstract properties with context parameters require a ${FunSpec.GETTER}")
              }
              if (propertySpec.mutable && propertySpec.setter == null) {
                add(
                  "non-abstract mutable properties with context parameters require a ${FunSpec.SETTER}"
                )
              }
            }
            if (errors.isNotEmpty()) {
              throw IllegalArgumentException(errors.joinToString(", "))
            }
          }
        }
        if (propertySpec.contextReceiverTypes.isNotEmpty()) {
          if (
            ABSTRACT !in kind.implicitPropertyModifiers(modifiers) &&
              ABSTRACT !in propertySpec.modifiers
          ) {
            val errors = buildList {
              if (propertySpec.getter == null) {
                add("non-abstract properties with context receivers require a ${FunSpec.GETTER}")
              }
              if (propertySpec.mutable && propertySpec.setter == null) {
                add(
                  "non-abstract mutable properties with context receivers require a ${FunSpec.SETTER}"
                )
              }
            }
            if (errors.isNotEmpty()) {
              throw IllegalArgumentException(errors.joinToString(", "))
            }
          }
        }
      }

      if (isAnnotation) {
        primaryConstructor?.let {
          requireNoneOf(it.modifiers, INTERNAL, PROTECTED, PRIVATE, ABSTRACT)
        }
      }

      if (primaryConstructor == null) {
        require(funSpecs.none { it.isConstructor } || superclassConstructorParameters.isEmpty()) {
          "types without a primary constructor cannot specify secondary constructors and " +
            "superclass constructor parameters"
        }
      }

      if (isInlineOrValClass) {
        primaryConstructor?.let {
          check(it.parameters.size == 1) {
            "value/inline classes must have 1 parameter in constructor"
          }
        }

        check(propertySpecs.size > 0) { "value/inline classes must have at least 1 property" }

        val constructorParamName = primaryConstructor?.parameters?.firstOrNull()?.name
        constructorParamName?.let { paramName ->
          val underlyingProperty = propertySpecs.find { it.name == paramName }
          requireNotNull(underlyingProperty) {
            "value/inline classes must have a single read-only (val) property parameter."
          }
          check(!underlyingProperty.mutable) {
            "value/inline classes must have a single read-only (val) property parameter."
          }
        }
        check(superclass == Any::class.asTypeName()) {
          "value/inline classes cannot have super classes"
        }
      }

      if (isFunInterface) {
        // Note: Functional interfaces can contain any number of non-abstract functions.
        val abstractFunSpecs = funSpecs.filter { ABSTRACT in it.modifiers }

        if (superinterfaces.isEmpty()) {
          check(abstractFunSpecs.size == 1) {
            "Functional interfaces must have exactly one abstract function. Contained " +
              "${abstractFunSpecs.size}: ${abstractFunSpecs.map { it.name }}"
          }
        }
      }

      when (typeSpecs.count { it.isCompanion }) {
        0 -> Unit
        1 -> {
          require(isSimpleClass || kind == Kind.INTERFACE || isEnum || isAnnotation) {
            "$kind types can't have a companion object"
          }
        }
        else -> {
          throw IllegalArgumentException(
            "Multiple companion objects are present but only one is allowed."
          )
        }
      }

      return TypeSpec(this)
    }
  }

  public companion object {
    @JvmStatic public fun classBuilder(name: String): Builder = Builder(Kind.CLASS, name)

    @JvmStatic
    public fun classBuilder(className: ClassName): Builder = classBuilder(className.simpleName)

    @Deprecated(
      "Use classBuilder() instead. This function will be removed in KotlinPoet 2.0.",
      ReplaceWith("TypeSpec.classBuilder(name).addModifiers(KModifier.EXPECT)"),
    )
    @JvmStatic
    public fun expectClassBuilder(name: String): Builder = Builder(Kind.CLASS, name, EXPECT)

    @Deprecated(
      "Use classBuilder() instead. This function will be removed in KotlinPoet 2.0.",
      ReplaceWith("TypeSpec.classBuilder(className).addModifiers(KModifier.EXPECT)"),
    )
    @JvmStatic
    public fun expectClassBuilder(className: ClassName): Builder =
      classBuilder(className.simpleName).addModifiers(EXPECT)

    @Deprecated(
      "Use classBuilder() instead. This function will be removed in KotlinPoet 2.0.",
      ReplaceWith("TypeSpec.classBuilder(name).addModifiers(KModifier.VALUE)"),
    )
    @JvmStatic
    public fun valueClassBuilder(name: String): Builder = Builder(Kind.CLASS, name, VALUE)

    @JvmStatic public fun objectBuilder(name: String): Builder = Builder(Kind.OBJECT, name)

    @JvmStatic
    public fun objectBuilder(className: ClassName): Builder = objectBuilder(className.simpleName)

    @JvmStatic
    @JvmOverloads
    public fun companionObjectBuilder(name: String? = null): Builder =
      Builder(Kind.OBJECT, name, COMPANION)

    @JvmStatic public fun interfaceBuilder(name: String): Builder = Builder(Kind.INTERFACE, name)

    @JvmStatic
    public fun interfaceBuilder(className: ClassName): Builder =
      interfaceBuilder(className.simpleName)

    @JvmStatic
    public fun funInterfaceBuilder(name: String): Builder = Builder(Kind.INTERFACE, name, FUN)

    @JvmStatic
    public fun funInterfaceBuilder(className: ClassName): Builder =
      funInterfaceBuilder(className.simpleName)

    @JvmStatic public fun enumBuilder(name: String): Builder = Builder(Kind.CLASS, name, ENUM)

    @JvmStatic
    public fun enumBuilder(className: ClassName): Builder = enumBuilder(className.simpleName)

    @JvmStatic public fun anonymousClassBuilder(): Builder = Builder(Kind.CLASS, null)

    @JvmStatic
    public fun annotationBuilder(name: String): Builder = Builder(Kind.CLASS, name, ANNOTATION)

    @JvmStatic
    public fun annotationBuilder(className: ClassName): Builder =
      annotationBuilder(className.simpleName)
  }
}
