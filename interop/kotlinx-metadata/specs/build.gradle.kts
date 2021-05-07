tasks.named<Jar>("jar") {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet.metadata.specs")
  }
}

dependencies {
  api(project(":kotlinpoet"))
  api(project(":interop:kotlinx-metadata:core"))

  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
}
