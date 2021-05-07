package com.squareup.kotlinpoet

import com.google.testing.compile.CompilationRule
import org.junit.Rule
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.test.Ignore

@Ignore("Not clear this test is useful to retain in the Kotlin world")
class TypesTest : AbstractTypesTest() {
  @JvmField @Rule val compilation = CompilationRule()

  override val elements: Elements
    get() = compilation.elements

  override val types: Types
    get() = compilation.types
}
