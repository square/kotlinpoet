package com.squareup.kotlinpoet.compile

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.testutil.TestScript
import com.squareup.kotlinpoet.typeNameOf
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.test.assertTrue
import org.junit.Test

class ParameterWithSpaceCompileTest {
  @Test
  fun parameterWithSpace() {
    val builder = StringBuilder()

    @OptIn(ExperimentalStdlibApi::class)
    FileSpec.builder("a", "b")
      .addFunction(FunSpec.builder("foo").apply {
        addParameter("aaa bbb", typeNameOf<(Int) -> String>())
        val arg = mutableListOf<String>()
        addStatement(
          StringBuilder().apply {
            repeat(10) {
              append("%N($it) + ")
              arg += "aaa bbb"
            }
            append("%N(100)")
            arg += "aaa bbb"
          }.toString(),
          *arg.toTypedArray()
        )
      }.build())
      .build()
      .writeTo(builder)

    val compileConfig = createJvmCompilationConfigurationFromTemplate<TestScript> {
      jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
      }
    }

    val res = BasicJvmScriptingHost().eval(
      builder.toString().replace("public", "").toScriptSource("test.kts"),
      compileConfig,
      null
    )

    println(res.reports.joinToString("\n") { it.render() })
    assertTrue { res is ResultWithDiagnostics.Success }
  }
}
