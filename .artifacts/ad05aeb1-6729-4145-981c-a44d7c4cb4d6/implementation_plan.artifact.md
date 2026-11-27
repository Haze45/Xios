# Fix "Failed to resolve: androidx.compose.ui:ui-test-junit4"

The project uses the Jetpack Compose Bill of Materials (BOM) to manage versions of Compose libraries. However, the BOM is currently only applied to the `implementation` configuration. This causes dependencies in other configurations (like `androidTestImplementation` and `debugImplementation`) to fail resolution because they lack version information.

## Proposed Changes

### [app]

#### [MODIFY] [build.gradle.kts](file:///D:/Programming/Mobile_Development/Music/app/build.gradle.kts)

- Add `androidTestImplementation(platform(libs.androidx.compose.bom))` to ensure test dependencies use the BOM versions.
- Add `debugImplementation(platform(libs.androidx.compose.bom))` to ensure debug dependencies (like tooling and manifest) use the BOM versions.

## Verification Plan

### Automated Tests
- Run Gradle sync to verify that the dependency resolution error is resolved.
- Run `./gradlew assembleDebug` to ensure the project builds successfully.

### Manual Verification
- Verify that the IDE no longer shows resolution errors for `ui-test-junit4`.
