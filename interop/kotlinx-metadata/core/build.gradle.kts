tasks.named<Jar>("jar") {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet.km")
  }
}

dependencies {
  api(libs.kotlinx.metadata)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
}
