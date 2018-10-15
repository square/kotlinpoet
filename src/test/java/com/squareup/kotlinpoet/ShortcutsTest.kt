package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.KModifier.INTERNAL
import java.time.Period
import kotlin.test.Test


class ShortcutsTest {

  @Test
  fun fullFileSpec() {
    val classic =
      FileSpec.Builder("com.squareup.kotlinpoet.test", "FooBar")
        .apply {
          addProperty(
            PropertySpec.builder(
              "foo", String::class, INTERNAL
            ).build()
          )

          addType(
            TypeSpec.classBuilder("FooClass")
              .primaryConstructor(FunSpec.constructorBuilder()
                .addCode("println(\"Hello World\")")
                .build())

              .addProperty(PropertySpec.builder("foo", String::class, INTERNAL)
                .initializer("\"the foo\"")
                .mutable(true)
                .build())

              .addProperty(PropertySpec.builder("foobar", String::class)
                .getter(FunSpec.getterBuilder()
                  .addCode("return \"\${foo}bar\"")
                  .build())
                .build())

              .addFunction(FunSpec.builder("dooTheFooBar")
                .returns(Int::class.asTypeName().asNullable())
                .addParameter("howLong", Period::class, INTERNAL)
                .addCode("println(\"\$foobar\")")
                .build())

              .build()
          )

          addType(
            TypeSpec.interfaceBuilder("FooInterface")
              .addFunction(FunSpec.builder("calcMagicNumber")
                .returns(Long::class)
                .addCode("return 42L")
                .build())
              .build()
          )

          addType(
            TypeSpec.objectBuilder("FooObject").build()
          )
        }.build()

    val shortcuts = fileSpec("com.squareup.kotlinpoet.test", "FooBar") {
      propertySpec("foo", String::class, INTERNAL)

      classSpec("FooClass") {
        primaryConstructorSpec {
          addCode("println(\"Hello World\")")
        }

        varPropertySpec("foo", String::class, INTERNAL) {
          initializer("\"the foo\"")
        }

        propertySpec("foobar", String::class) {
          getterSpec { addCode("return \"\${foo}bar\"") }
        }

        funSpec("dooTheFooBar") {
          returns(Int::class.asTypeName().asNullable())
          addParameter("howLong", Period::class, INTERNAL)
          addCode("println(\"\$foobar\")")
        }
      }

      interfaceSpec("FooInterface") {
        funSpec("calcMagicNumber") {
          returns(Long::class)
          addCode("return 42L")
        }
      }

      objectSpec("FooObject") {

      }
    }

    assertThat(shortcuts).isEqualTo(classic)
  }
}