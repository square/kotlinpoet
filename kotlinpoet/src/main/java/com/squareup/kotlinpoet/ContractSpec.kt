package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.ContractEffectType.CALLS
import com.squareup.kotlinpoet.ContractEffectType.RETURNS_CONSTANT
import com.squareup.kotlinpoet.ContractEffectType.RETURNS_NOT_NULL
import kotlin.reflect.KClass

/** Represents a contract of a Kotlin function. */
class ContractSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap()
) : Taggable by tagMap {

  /** Effects of this contract. */
  val effects: List<ContractEffectSpec> = builder.effects.toImmutableList()

  init {
    require(effects.isNotEmpty()) {
      "Contract must have at least one effect!"
    }
  }

  internal fun emit(codeWriter: CodeWriter, function: FunSpec) {
    codeWriter.emitCode("%M·{\n", MemberName("kotlin.contracts", "contract"))
    codeWriter.indent()
    effects.joinEmissions(codeWriter, separator = "\n", postfix = "\n") { writer, effect ->
      effect.emit(writer, function)
    }
    codeWriter.unindent()
    codeWriter.emitCode("}\n")
  }

  private fun <T> List<T>.joinEmissions(
    codeWriter: CodeWriter,
    separator: String = ", ",
    prefix: String = "",
    postfix: String = "",
    emitter: (CodeWriter, T) -> Unit
  ) {
    codeWriter.emitCode(prefix)
    for ((count, element) in this.withIndex()) {
      if (count + 1 > 1) codeWriter.emitCode(separator)
      emitter(codeWriter, element)
    }
    codeWriter.emitCode(postfix)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String = buildCodeString {
    emit(this,
        FunSpec.builder("")
            .apply {
              if (effects.any { it.type == CALLS }) {
                addModifiers(KModifier.INLINE)
              }
            }
            .build()
    )
  }

  fun toBuilder(): Builder {
    val builder = Builder()
    builder.addEffects(effects)
    return builder
  }

  class Builder internal constructor() : Taggable.Builder<Builder> {
    override val tags = mutableMapOf<KClass<*>, Any>()
    val effects = mutableListOf<ContractEffectSpec>()

    fun addEffect(effect: ContractEffectSpec) = apply {
      effects += effect
    }

    fun addEffects(effects: List<ContractEffectSpec>) = apply {
      this.effects += effects
    }

    fun build(): ContractSpec = ContractSpec(this)
  }

  companion object {
    @JvmStatic
    fun builder(): Builder = Builder()
  }
}

/**
 * Represents an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
class ContractEffectSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap()
) : Taggable by tagMap {

  /** Type of the effect. */
  val type = builder.type

  /**
   * Optional number of invocations of the lambda parameter of this function, specified further
   * in the effect expression.
   */
  val invocationKind = builder.invocationKind

  /**
   * Arguments of the effect constructor, i.e. the constant value for the
   * [ContractEffectType.RETURNS_CONSTANT] effect, or the parameter reference for the
   * [ContractEffectType.CALLS] effect.
   */
  val constructorArguments = builder.constructorArguments.toImmutableList()

  /**
   * Conclusion of the effect. If this value is set, the effect represents an implication with this
   * value as the right-hand side.
   */
  val conclusion = builder.conclusion

  init {
    require(!(type == RETURNS_NOT_NULL && constructorArguments.isNotEmpty())) {
      "returnsNotNull effects cannot have constructor arguments but received $constructorArguments"
    }
    require(!(type == RETURNS_NOT_NULL && conclusion == null)) {
      "returnsNotNull effects must have a conclusion"
    }
    require(!(type == RETURNS_CONSTANT && constructorArguments.size > 1)) {
      "returns effects require exactly 0 or 1 constructor argument but received $constructorArguments"
    }
    require(!(type == RETURNS_CONSTANT && conclusion == null)) {
      "returns effects must have a conclusion"
    }
    require(!(type == CALLS && constructorArguments.size != 1)) {
      "callsInPlace requires exactly one constructor argument but received $constructorArguments"
    }
    require(!(type == CALLS && conclusion != null)) {
      "callsInPlace cannot have a conclusion but received $constructorArguments"
    }
  }

  internal fun emit(codeWriter: CodeWriter, function: FunSpec) {
    when (type) {
      RETURNS_CONSTANT -> {
        if (constructorArguments.isNotEmpty()) {
          // returns(value)
          codeWriter.emitCode("returns")
          constructorArguments[0].emit(codeWriter, function)
        } else {
          codeWriter.emitCode("returns()")
        }
      }
      CALLS -> {
        // callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        codeWriter.emitCode("callsInPlace(")
        constructorArguments[0].emit(codeWriter, function, true)
        codeWriter.emitCode(", ")
        requireNotNull(invocationKind).emit(codeWriter)
        codeWriter.emitCode(")")
      }
      RETURNS_NOT_NULL -> {
        codeWriter.emitCode("returnsNotNull()")
      }
    }
    if (conclusion != null) {
      codeWriter.emitCode(" implies ")
      conclusion.emit(codeWriter, function)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String = buildCodeString {
    emit(this,
        FunSpec.builder("")
            .apply {
              if (type == CALLS) {
                addModifiers(KModifier.INLINE)
              }
            }
            .build()
    )
  }

  fun toBuilder(): Builder {
    val builder = Builder(type)
    builder.invocationKind = invocationKind
    builder.conclusion = conclusion
    builder.constructorArguments += constructorArguments
    return builder
  }

  /**
   * TODO Should this just be internal since different types have specific requirements?
   */
  class Builder internal constructor(
    val type: ContractEffectType
  ) : Taggable.Builder<Builder> {
    override val tags = mutableMapOf<KClass<*>, Any>()
    internal var invocationKind: ContractInvocationKind? = null
    internal var conclusion: ContractEffectExpressionSpec? = null
    val constructorArguments = mutableListOf<ContractEffectExpressionSpec>()

    fun invocationKind(invocationKind: ContractInvocationKind?) = apply {
      this.invocationKind = invocationKind
    }

    fun conclusion(conclusion: ContractEffectExpressionSpec?) = apply {
      this.conclusion = conclusion
    }

    fun addConstructorArguments(constructorArguments: ContractEffectExpressionSpec) = apply {
      this.constructorArguments += constructorArguments
    }

    fun addConstructorArguments(constructorArguments: List<ContractEffectExpressionSpec>) = apply {
      this.constructorArguments += constructorArguments
    }

    fun build(): ContractEffectSpec = ContractEffectSpec(this)
  }

  companion object {
    @JvmStatic
    fun calls(
      parameter: ParameterSpec,
      invocationKind: ContractInvocationKind
    ): ContractEffectSpec {
      require(parameter.type is LambdaTypeName) {
        "callsInPlace is only applicable to function parameters. Input was ${parameter.type}"
      }
      return calls(parameter.name, invocationKind)
    }

    @JvmStatic
    fun calls(
      parameterName: String,
      invocationKind: ContractInvocationKind
    ): ContractEffectSpec {
      return ContractEffectSpec.Builder(CALLS)
          .invocationKind(invocationKind)
          .addConstructorArguments(ContractEffectExpressionSpec.parameterReference(parameterName))
          .build()
    }

    @JvmStatic
    fun returns(conclusion: ContractEffectExpressionSpec): ContractEffectSpec {
      return ContractEffectSpec.Builder(RETURNS_CONSTANT)
          .conclusion(conclusion)
          .build()
    }

    @JvmStatic
    fun returnsValue(
      value: CodeBlock,
      conclusion: ContractEffectExpressionSpec
    ): ContractEffectSpec {
      return ContractEffectSpec.Builder(RETURNS_CONSTANT)
          .addConstructorArguments(ContractEffectExpressionSpec.constantValue(value))
          .conclusion(conclusion)
          .build()
    }

    @JvmStatic
    fun returnsNotNull(conclusion: ContractEffectExpressionSpec): ContractEffectSpec {
      return ContractEffectSpec.Builder(RETURNS_NOT_NULL)
          .conclusion(conclusion)
          .build()
    }
  }
}

/**
 * Represents an effect expression, the contents of an effect (a part of the contract of a Kotlin function).
 */
class ContractEffectExpressionSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap()
) : Taggable by tagMap {

  /**
   * Signifies that the corresponding effect expression should be negated to compute the
   * proposition or the conclusion of an effect.
   */
  val isNegated: Boolean = builder.isNegated

  /**
   * Signifies that the corresponding effect expression checks whether a value of some variable is
   * `null`.
   */
  val isNullCheckPredicate: Boolean = builder.isNullCheckPredicate

  /**
   * Optional 1-based index of the value parameter of the function, for effects which assert
   * something about the function parameters. The index 0 means the extension receiver parameter.
   */
  val parameterIndex: Int? = builder.parameterIndex

  /** Constant value used in the effect expression. */
  val constantValue: CodeBlock? = builder.constantValue

  /** Type used as the target of an `is`-expression in the effect expression. */
  val isInstanceType: TypeName? = builder.isInstanceType

  /**
   * Arguments of an `&&`-expression. If this list is non-empty, the resulting effect expression is
   * a conjunction of this expression and elements of the list.
   */
  val andArguments: List<ContractEffectExpressionSpec> = builder.andArguments.toImmutableList()

  /**
   * Arguments of an `||`-expression. If this list is non-empty, the resulting effect expression is
   * a disjunction of this expression and elements of the list.
   */
  val orArguments: List<ContractEffectExpressionSpec> = builder.orArguments.toImmutableList()

  private val instanceOperator = if (isNegated) {
    "!is"
  } else {
    "is"
  }

  private val equalsOperator = if (isNegated) {
    "!="
  } else {
    "=="
  }

  internal fun emit(
    codeWriter: CodeWriter,
    function: FunSpec,
    suppressEnclosingParens: Boolean = false
  ) {
    if (!suppressEnclosingParens) {
      codeWriter.emit("(")
    }
    if (parameterIndex != null) {
      val parameter = if (parameterIndex == 0) {
        "this@${function.name}"
      } else {
        function.parameters[parameterIndex - 1].name
      }
      when {
        isNullCheckPredicate -> {
          codeWriter.emitCode("%L %L null", parameter, equalsOperator)
        }
        constantValue != null -> {
          codeWriter.emitCode("%L %L %L", parameter, equalsOperator, constantValue)
        }
        isInstanceType != null -> {
          codeWriter.emitCode("%L %L %T", parameter, instanceOperator, isInstanceType)
        }
      }

      // TODO and cases are always evaluated before ors in this case. How do we deduce the right order?
      andArguments.forEach {
        codeWriter.emitCode(" && ")
        it.emit(codeWriter, function)
      }
      orArguments.forEach {
        codeWriter.emitCode(" || ")
        it.emit(codeWriter, function)
      }
    } else {
      // Applicable if:
      // - The function receiver is a boolean, values can only be booleans
      // - Used for constant value in returns()
      // - Parameter name for use in callsInPlace()
      codeWriter.emitCode("%L", requireNotNull(constantValue))
    }
    if (!suppressEnclosingParens) {
      codeWriter.emit(")")
    }
  }

  fun toBuilder(): Builder {
    val builder = Builder()
    builder.isNegated = isNegated
    builder.isNullCheckPredicate = isNullCheckPredicate
    builder.parameterIndex = parameterIndex
    builder.constantValue = constantValue
    builder.isInstanceType = isInstanceType
    builder.andArguments += andArguments
    builder.orArguments += orArguments
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String = buildCodeString {
    emit(this, FunSpec.builder("").build()
    )
  }

  infix fun and(andArgument: ContractEffectExpressionSpec): ContractEffectExpressionSpec {
    return toBuilder().addAndArgument(andArgument).build()
  }

  infix fun or(orArgument: ContractEffectExpressionSpec): ContractEffectExpressionSpec {
    return toBuilder().addOrArgument(orArgument).build()
  }

  class Builder internal constructor() : Taggable.Builder<Builder> {
    override val tags = mutableMapOf<KClass<*>, Any>()
    internal var isNegated: Boolean = false
    internal var isNullCheckPredicate: Boolean = false
    internal var parameterIndex: Int? = null
    internal var constantValue: CodeBlock? = null
    internal var isInstanceType: TypeName? = null
    val andArguments = mutableListOf<ContractEffectExpressionSpec>()
    val orArguments = mutableListOf<ContractEffectExpressionSpec>()

    fun isNegated(isNegated: Boolean) = apply {
      this.isNegated = isNegated
    }

    fun isNullCheckPredicate(isNullCheckPredicate: Boolean) = apply {
      this.isNullCheckPredicate = isNullCheckPredicate
    }

    fun parameterIndex(parameterIndex: Int?) = apply {
      this.parameterIndex = parameterIndex
    }

    fun constantValue(constantValue: CodeBlock?) = apply {
      this.constantValue = constantValue
    }

    fun isInstanceType(isInstanceType: TypeName?) = apply {
      this.isInstanceType = isInstanceType
    }

    fun addAndArgument(andArguments: ContractEffectExpressionSpec) = apply {
      this.andArguments += andArguments
    }

    fun addAndArguments(andArguments: List<ContractEffectExpressionSpec>) = apply {
      this.andArguments += andArguments
    }

    fun addOrArgument(orArguments: ContractEffectExpressionSpec) = apply {
      this.orArguments += orArguments
    }

    fun addOrArguments(orArguments: List<ContractEffectExpressionSpec>) = apply {
      this.orArguments += orArguments
    }

    fun build(): ContractEffectExpressionSpec = ContractEffectExpressionSpec(this)
  }

  companion object {

    @JvmStatic
    fun builder(): Builder = Builder()

    /**
     * Constant value expression that's just a simple true/false,
     * for extension functions on Boolean.
     */
    @JvmStatic
    fun constantValue(value: Boolean): ContractEffectExpressionSpec {
      return builder()
          .constantValue(CodeBlock.of(value.toString()))
          .build()
    }

    /** Constant value expression for use in returns() expressions. */
    @JvmStatic
    fun constantValue(value: CodeBlock): ContractEffectExpressionSpec {
      return builder()
          .constantValue(value)
          .build()
    }

    /**
     * Constant value expression. Parameter index is 1-based for function parameters, and 0
     * indicates the target is the function receiver.
     */
    @JvmStatic
    fun constantValue(
      value: CodeBlock,
      parameterIndex: Int,
      isNegated: Boolean = false
    ): ContractEffectExpressionSpec {
      require(parameterIndex >= 0) {
        "parameterIndex must be >= 0"
      }
      require(value.toString().trim() != "null") {
        "use the nullCheck() function for null-checked predicates"
      }
      return builder()
          .constantValue(value)
          .parameterIndex(parameterIndex)
          .isNegated(isNegated)
          .build()
    }

    /**
     * Constant value expression for referencing a parameter, used in `callsInPlace` contracts.
     */
    @JvmStatic
    fun parameterReference(
      parameter: ParameterSpec
    ): ContractEffectExpressionSpec = parameterReference(parameter.name)

    /**
     * Constant value expression for referencing a parameter by name, used in `callsInPlace` contracts.
     */
    @JvmStatic
    fun parameterReference(name: String): ContractEffectExpressionSpec {
      return builder()
          .constantValue(CodeBlock.of(name))
          .build()
    }

    /**
     * Instance type expression. Parameter index is 1-based for function parameters, and 0
     * indicates the target is the function receiver.
     */
    @JvmStatic
    fun isInstance(
      type: TypeName,
      parameterIndex: Int,
      isNegated: Boolean = false
    ): ContractEffectExpressionSpec {
      require(parameterIndex >= 0) {
        "parameterIndex must be >= 0"
      }
      return builder()
          .isInstanceType(type)
          .parameterIndex(parameterIndex)
          .isNegated(isNegated)
          .build()
    }

    /**
     * Null check predicate expression. Parameter index is 1-based for function parameters, and 0
     * indicates the target is the function receiver.
     */
    @JvmStatic
    fun nullCheck(parameterIndex: Int, isNegated: Boolean = false): ContractEffectExpressionSpec {
      require(parameterIndex >= 0) {
        "parameterIndex must be >= 0"
      }
      return builder()
          .isNullCheckPredicate(true)
          .parameterIndex(parameterIndex)
          .isNegated(isNegated)
          .build()
    }
  }
}

/** Type of an effect (a part of the contract of a Kotlin function). */
enum class ContractEffectType {
  RETURNS_CONSTANT,
  CALLS,
  RETURNS_NOT_NULL
}

/** Specifies how many times a function invokes its function parameter in place. */
enum class ContractInvocationKind {
  /** A function parameter will be invoked one time or not invoked at all. */
  AT_MOST_ONCE,

  /** A function parameter will be invoked one or more times. */
  AT_LEAST_ONCE,

  /** A function parameter will be invoked exactly one time. */
  EXACTLY_ONCE,

  /** A function parameter is called in place, but it's unknown how many times it can be called. */
  UNKNOWN;

  internal fun emit(codeWriter: CodeWriter) {
    codeWriter.emitCode("%T", INVOCATION_KIND_CN.nestedClass(name))
  }

  companion object {
    // Hide behind a ClassName lookup for now until Contracts are stable
    private val INVOCATION_KIND_CN = ClassName("kotlin.contracts", "InvocationKind")
  }
}
