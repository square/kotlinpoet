%P for String Templates
=======================

`%S` also handles the escaping of dollar signs (`$`), to avoid inadvertent creation of string
templates, which may fail to compile in generated code:

```kotlin
val stringWithADollar = "Your total is " + "$" + "50"
val funSpec = FunSpec.builder("printTotal")
  .returns(String::class)
  .addStatement("return %S", stringWithADollar)
  .build()
```

produces:

```kotlin
fun printTotal(): String = "Your total is ${'$'}50"
```

If you need to generate string templates, use `%P`, which doesn't escape dollars:

```kotlin
val amount = 50
val stringWithADollar = "Your total is " + "$" + "amount"
val funSpec = FunSpec.builder("printTotal")
  .returns(String::class)
  .addStatement("return %P", stringWithADollar)
  .build()
```

produces:

```kotlin
fun printTotal(): String = "Your total is $amount"
```

You can also use `CodeBlock`s as arguments to `%P`, which is handy when you need to reference
importable types or members inside the string template:

```kotlin
val file = FileSpec.builder("com.example", "Digits")
  .addFunction(
    FunSpec.builder("print")
      .addParameter("digits", IntArray::class)
      .addStatement("println(%P)", buildCodeBlock {
        val contentToString = MemberName("kotlin.collections", "contentToString")
        add("These are the digits: \${digits.%M()}", contentToString)
      })
      .build()
  )
  .build()
println(file)
```

The snippet above will produce the following output, handling the imports properly:

```kotlin
package com.example

import kotlin.IntArray
import kotlin.collections.contentToString

fun print(digits: IntArray) {
  println("""These are the digits: ${digits.contentToString()}""")
}
```
