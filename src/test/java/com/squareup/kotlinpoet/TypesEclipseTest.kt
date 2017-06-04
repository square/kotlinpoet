/*
 * Copyright (C) 2014 Google, Inc.
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

import com.google.common.base.Charsets.UTF_8
import com.google.common.collect.ImmutableSet
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.JUnit4
import org.junit.runners.model.Statement
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject

@Ignore("Not clear this test is useful to retain in the Kotlin world")
class TypesEclipseTest : AbstractTypesTest() {
  /**
   * A [JUnit4] [Rule] that executes tests such that a instances of [Elements] and [Types] are
   * available during execution.
   *
   * To use this rule in a test, just add the following field:
   *
   * ```
   *   public CompilationRule compilationRule = new CompilationRule();
   * ```
   *
   * @author Gregory Kick
   */
  class CompilationRule : TestRule {
    private var elements: Elements? = null
    private var types: Types? = null

    override fun apply(base: Statement, description: Description): Statement {
      return object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
          val thrown = AtomicReference<Throwable>()
          val successful = compile(listOf(object : AbstractProcessor() {
            override fun getSupportedSourceVersion() = SourceVersion.latest()

            override fun getSupportedAnnotationTypes() = setOf("*")

            @Synchronized override fun init(processingEnv: ProcessingEnvironment) {
              super.init(processingEnv)
              elements = processingEnv.elementUtils
              types = processingEnv.typeUtils
            }

            override fun process(
                annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
              // just run the test on the last round after compilation is over
              if (roundEnv.processingOver()) {
                try {
                  base.evaluate()
                } catch (e: Throwable) {
                  thrown.set(e)
                }
              }
              return false
            }
          }))
          check(successful)
          val t = thrown.get()
          if (t != null) {
            throw t
          }
        }
      }
    }

    /**
     * Returns the [Elements] instance associated with the current execution of the rule.
     *
     * @throws IllegalStateException if this method is invoked outside the execution of the rule.
     */
    fun getElements() = elements!!

    /**
     * Returns the [Types] instance associated with the current execution of the rule.
     *
     * @throws IllegalStateException if this method is invoked outside the execution of the rule.
     */
    fun getTypes() = types!!

    private fun compile(processors: Iterable<Processor>): Boolean {
      val compiler = EclipseCompiler()
      val diagnosticCollector = DiagnosticCollector<JavaFileObject>()
      val fileManager = compiler.getStandardFileManager(
          diagnosticCollector, Locale.getDefault(), UTF_8)
      val task = compiler.getTask(null,
          fileManager,
          diagnosticCollector,
          ImmutableSet.of<String>(),
          ImmutableSet.of(TypesEclipseTest::class.java.canonicalName),
          ImmutableSet.of<JavaFileObject>())
      task.setProcessors(processors)
      return task.call()!!
    }
  }

  @JvmField @Rule val compilation = CompilationRule()

  override val elements: Elements
    get() = compilation.getElements()

  override val types: Types
    get() = compilation.getTypes()
}
