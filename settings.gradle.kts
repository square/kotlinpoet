// TODO(egorand): Remove once gradle-maven-publish-plugin:0.8.0 is published
pluginManagement {
  repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    gradlePluginPortal()
  }
  resolutionStrategy {
    eachPlugin {  
      if (requested.id.namespace == "com.vanniktech.maven") {
        useModule("com.vanniktech:gradle-maven-publish-plugin:${requested.version}")
      }
    }
  }
}
