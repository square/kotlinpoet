Releasing
=========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. Update `docs/changelog.md` for the impending release.
 3. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 4. `./gradlew clean publish --no-daemon --no-parallel`.
 5. Visit [Sonatype Nexus][sonatype] and ensure there's only one staging repository.
 6. `./gradlew closeAndReleaseRepository`.
 7. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version).
 8. Update `gradle.properties` to the next SNAPSHOT version.
 9. `git commit -am "Prepare next development version."`.
 10. `git push && git push --tags`.

If steps 5-6 fail, drop the Sonatype repo, fix the problem, commit, and start again at step 4.


Prerequisites
-------------

In `~/.gradle/gradle.properties`, set the following:

 * `ORG_GRADLE_PROJECT_mavenCentralUsername` - Sonatype username for releasing to `com.squareup`.
 * `ORG_GRADLE_PROJECT_mavenCentralPassword` - Sonatype password for releasing to `com.squareup`.

 [sonatype]: https://s01.oss.sonatype.org/
