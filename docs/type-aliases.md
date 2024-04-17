Type Aliases
============

KotlinPoet provides API for creating Type Aliases, which supports simple class names, parameterized
types and lambdas:

```kotlin
val k = TypeVariableName("K")
val t = TypeVariableName("T")

val fileTable = Map::class.asClassName()
  .parameterizedBy(k, Set::class.parameterizedBy(File::class))

val predicate = LambdaTypeName.get(
  parameters = arrayOf(t),
  returnType = Boolean::class.asClassName()
)
val helloWorld = FileSpec.builder("com.example", "HelloWorld")
  .addTypeAlias(TypeAliasSpec.builder("Word", String::class).build())
  .addTypeAlias(
    TypeAliasSpec.builder("FileTable", fileTable)
      .addTypeVariable(k)
      .build()
  )
  .addTypeAlias(
    TypeAliasSpec.builder("Predicate", predicate)
      .addTypeVariable(t)
      .build()
  )
  .build()
```

Which generates the following:

```kotlin
package com.example

import java.io.File
import kotlin.Boolean
import kotlin.String
import kotlin.collections.Map
import kotlin.collections.Set

typealias Word = String

typealias FileTable<K> = Map<K, Set<File>>

typealias Predicate<T> = (T) -> Boolean
```
