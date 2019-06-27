package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ContractInvocationKind.AT_MOST_ONCE
import com.squareup.kotlinpoet.ContractInvocationKind.EXACTLY_ONCE
import org.junit.Test
import kotlin.test.fail

class ContractSpecTest {

  @Test
  fun calls() {
    val param = ParameterSpec.builder(
        "body",
        LambdaTypeName.get(parameters = *arrayOf(STRING), returnType = STRING)
    ).build()
    val contractSpec = ContractSpec.builder()
        .addEffect(ContractEffectSpec.calls(param, EXACTLY_ONCE))
        .build()
    val function = FunSpec.builder("test")
        .addModifiers(KModifier.INLINE)
        .addParameter(param)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      inline fun test(body: (kotlin.String) -> kotlin.String) {
        kotlin.contracts.contract {
          callsInPlace(body, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
        }
      }
    """.trimIndent())
  }

  @Test
  fun callsTestMissingInline() {
    val param = ParameterSpec.builder(
        "body",
        LambdaTypeName.get(parameters = *arrayOf(STRING), returnType = STRING)
    ).build()
    val contractSpec = ContractSpec.builder()
        .addEffect(ContractEffectSpec.calls(param, EXACTLY_ONCE))
        .build()
    try {
      FunSpec.builder("test")
          .addParameter(param)
          .contract(contractSpec)
          .build()
      fail()
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessageThat().contains("functions with callsInPlace effects must be inline")
    }
  }

  @Test
  fun callsTestParamNotFunction() {
    val param = ParameterSpec.builder("body", STRING).build()
    try {
      ContractEffectSpec.calls(param, EXACTLY_ONCE)
      fail()
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessageThat().contains(
          "callsInPlace is only applicable to function parameters")
    }
  }

  @Test
  fun returnsNotNullTest() {
    val param = ParameterSpec.builder("param", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returnsNotNull(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .returns(UNIT.copy(nullable = true))
        .addParameter(param)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param: kotlin.String?): kotlin.Unit? {
        kotlin.contracts.contract {
          returnsNotNull() implies (param != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun returnsNotNullWithReceiver() {
    val param = ParameterSpec.builder("param", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returnsNotNull(
                conclusion = ContractEffectExpressionSpec.nullCheck(0, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .receiver(STRING.copy(nullable = true))
        .addParameter(param)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun kotlin.String?.test(param: kotlin.String?) {
        kotlin.contracts.contract {
          returnsNotNull() implies (this@test != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun returnsTest() {
    val param = ParameterSpec.builder("param", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .returns(UNIT.copy(nullable = true))
        .addParameter(param)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param: kotlin.String?): kotlin.Unit? {
        kotlin.contracts.contract {
          returns() implies (param != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun instanceTest() {
    val param = ParameterSpec.builder("param", ANY)
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.isInstance(STRING, 1, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .returns(UNIT.copy(nullable = true))
        .addParameter(param)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param: kotlin.String?): kotlin.Unit? {
        kotlin.contracts.contract {
          returns() implies (param != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun returnsValueTest() {
    val param = ParameterSpec.builder("param", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returnsValue(
                value = CodeBlock.of("1"),
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .returns(INT)
        .addParameter(param)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param: kotlin.String?): kotlin.Int {
        kotlin.contracts.contract {
          returns(1) implies (param != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun booleanConclusion() {
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.constantValue(true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .receiver(BOOLEAN)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun kotlin.Boolean.test() {
        kotlin.contracts.contract {
          returns() implies (true)
        }
      }
    """.trimIndent())
  }

  @Test
  fun multipleEffects() {
    val lambda = LambdaTypeName.get(parameters = *arrayOf(STRING), returnType = STRING)
    val param1 = ParameterSpec.builder(
        "body1",
        lambda
    ).build()
    val param2 = ParameterSpec.builder(
        "body2",
        lambda
    ).build()

    val effect1 = ContractEffectSpec.calls(param1, EXACTLY_ONCE)
    val effect2 = ContractEffectSpec.calls(param2, AT_MOST_ONCE)

    val contractSpec = ContractSpec.builder()
        .addEffect(effect1)
        .addEffect(effect2)
        .build()
    val function = FunSpec.builder("test")
        .addModifiers(KModifier.INLINE)
        .addParameter(param1)
        .addParameter(param2)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
        inline fun test(body1: (kotlin.String) -> kotlin.String, body2: (kotlin.String) -> kotlin.String) {
          kotlin.contracts.contract {
            callsInPlace(body1, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
            callsInPlace(body2, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
          }
        }
    """.trimIndent())
  }

  @Test
  fun andArguments() {
    val param1 = ParameterSpec.builder("param1", STRING.copy(nullable = true))
        .build()
    val param2 = ParameterSpec.builder("param2", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
                    .and(ContractEffectExpressionSpec.nullCheck(2, true))
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .addParameter(param1)
        .addParameter(param2)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param1: kotlin.String?, param2: kotlin.String?) {
        kotlin.contracts.contract {
          returns() implies (param1 != null && (param2 != null))
        }
      }
    """.trimIndent())
  }

  @Test
  fun orArguments() {
    val param1 = ParameterSpec.builder("param1", STRING.copy(nullable = true))
        .build()
    val param2 = ParameterSpec.builder("param2", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
                    .or(ContractEffectExpressionSpec.nullCheck(2, true))
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .addParameter(param1)
        .addParameter(param2)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param1: kotlin.String?, param2: kotlin.String?) {
        kotlin.contracts.contract {
          returns() implies (param1 != null || (param2 != null))
        }
      }
    """.trimIndent())
  }

  @Test
  fun andOrArguments() {
    val param1 = ParameterSpec.builder("param1", STRING.copy(nullable = true))
        .build()
    val param2 = ParameterSpec.builder("param2", STRING.copy(nullable = true))
        .build()
    val param3 = ParameterSpec.builder("param3", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
                    .or(ContractEffectExpressionSpec.nullCheck(2, true))
                    .and(ContractEffectExpressionSpec.nullCheck(3, true))
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .addParameter(param1)
        .addParameter(param2)
        .addParameter(param3)
        .contract(contractSpec)
        .build()

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(
        param1: kotlin.String?,
        param2: kotlin.String?,
        param3: kotlin.String?
      ) {
        kotlin.contracts.contract {
          returns() implies (param1 != null && (param3 != null) || (param2 != null))
        }
      }
    """.trimIndent())
  }

  // TODO more error cases
  // TODO complex
}
