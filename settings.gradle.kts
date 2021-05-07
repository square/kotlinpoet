pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

include(
    ":kotlinpoet",
    ":interop:kotlinx-metadata:classinspectors:elements",
    ":interop:kotlinx-metadata:classinspectors:reflect",
    ":interop:kotlinx-metadata:core",
    ":interop:kotlinx-metadata:specs",
    ":interop:kotlinx-metadata:specs-tests"
)

// Feature Previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")
