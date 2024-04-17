%S for Strings
==============

When emitting code that includes string literals, we can use **`%S`** to emit a **string**, complete
with wrapping quotation marks and escaping. Here's a program that emits 3 methods, each of which
returns its own name:

```kotlin
fun main(args: Array<String>) {
  val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addFunction(whatsMyNameYo("slimShady"))
    .addFunction(whatsMyNameYo("eminem"))
    .addFunction(whatsMyNameYo("marshallMathers"))
    .build()

  val kotlinFile = FileSpec.builder("com.example.helloworld", "HelloWorld")
    .addType(helloWorld)
    .build()

  kotlinFile.writeTo(System.out)
}

private fun whatsMyNameYo(name: String): FunSpec {
  return FunSpec.builder(name)
    .returns(String::class)
    .addStatement("return %S", name)
    .build()
}
```

In this case, using `%S` gives us quotation marks:

```kotlin
class HelloWorld {
  fun slimShady(): String = "slimShady"

  fun eminem(): String = "eminem"

  fun marshallMathers(): String = "marshallMathers"
}
```
