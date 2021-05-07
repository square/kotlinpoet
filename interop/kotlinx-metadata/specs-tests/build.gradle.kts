import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
  freeCompilerArgs = listOf("-Xjvm-default=all")
}

dependencies {
  testImplementation(project(":kotlinpoet"))
  testImplementation(project(":interop:kotlinx-metadata:core"))
  testImplementation(project(":interop:kotlinx-metadata:specs"))
  testImplementation(project(":interop:kotlinx-metadata:classinspectors:elements"))
  testImplementation(project(":interop:kotlinx-metadata:classinspectors:reflect"))
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.compile.testing)
  testImplementation(libs.kt.compile.testing)
}
