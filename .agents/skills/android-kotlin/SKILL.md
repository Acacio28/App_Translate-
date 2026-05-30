---
name: android-kotlin
description: Create distinctive, production-grade Android apps using Kotlin and Jetpack Compose. Use this skill when building screens, components, ViewModels, or features for the TranslaTecho translation app. Generates clean, idiomatic Kotlin code following MVVM architecture.
---

This skill guides creation of production-grade Android code for the TranslaTecho app — a multilingual translator supporting Tetun, Portuguese, English, Japanese, Chinese, Korean, and more, built with Kotlin and Jetpack Compose.

The user provides Android requirements: a screen, component, ViewModel, or feature to build or modify. They may include context about the purpose or technical constraints.

## Architecture Thinking

Before coding, understand the context and commit to a CLEAR implementation direction:

- **Purpose**: What feature or problem does this code solve?
- **Layer**: Is this Data (Room, Repository), UI (Compose screen/component), or ViewModel?
- **Constraints**: Must follow MVVM. No business logic in Composables. State only via ViewModel.
- **Integration**: How does this connect to existing code?

**CRITICAL**: Always respect the existing project structure. Never reorganize packages or rename existing files without being explicitly asked.

Then implement working Kotlin code that is:
- Idiomatic Kotlin (use data classes, sealed classes, extension functions where appropriate)
- Follows Jetpack Compose best practices
- MVVM-compliant with clean separation of concerns
- Production-grade with proper error handling

## Project Structure
