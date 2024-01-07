Code Block Format Strings
=========================

Code blocks may specify the values for their placeholders in a few ways. Only one style may be used
for each operation on a code block.

## Relative Arguments

Pass an argument value for each placeholder in the format string to `CodeBlock.add()`. In each
example, we generate code to say "I ate 3 tacos"

```kotlin
CodeBlock.builder().add("I ate %L %L", 3, "tacos")
```

## Positional Arguments

Place an integer index (1-based) before the placeholder in the format string to specify which
argument to use.

```kotlin
CodeBlock.builder().add("I ate %2L %1L", "tacos", 3)
```

## Named Arguments

Use the syntax `%argumentName:X` where `X` is the format character and call `CodeBlock.addNamed()`
with a map containing all argument keys in the format string. Argument names use characters in
`a-z`, `A-Z`, `0-9`, and `_`, and must start with a lowercase character.

```kotlin
val map = LinkedHashMap<String, Any>()
map += "food" to "tacos"
map += "count" to 3
CodeBlock.builder().addNamed("I ate %count:L %food:L", map)
```
