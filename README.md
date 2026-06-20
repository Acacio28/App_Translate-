# TranslaTecho

---

## English

TranslaTecho is a modern Android translation app built with **Kotlin** and **Jetpack Compose**. It provides fast text translation, camera OCR, dictionary lookup, grammar checking, and AI-powered writing assistance in a clean Material 3 interface.

### Features

- **Translator** — Text translation via MyMemory API (free, 50K chars/day), 10 languages, auto language detection (ML Kit), voice input, text-to-speech, image OCR, clipboard, undo/redo
- **Camera** — Live OCR using CameraX + ML Kit Text Recognition, flash toggle, gallery picker
- **History** — Room database, chronological list, clear all
- **Write** — Grammar check via LanguageTool API (red underline + corrections), Formal/Casual/Expand modes via Gemini 2.5 Flash Lite
- **Dictionary** — English word lookup via Free Dictionary API (phonetics, definitions, synonyms, antonyms)
- **Dialogue** — AI chat via Gemini 2.5 Flash Lite with persistent SQLite history

### Tech Stack

| Layer | Tech |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM with StateFlow |
| Database | Room 2.6.1 + SQLite |
| Camera | CameraX 1.3.4 |
| ML | Google ML Kit |
| APIs | MyMemory, Gemini 2.5 Flash Lite, LanguageTool, Free Dictionary |
| Build | Gradle 8.13, AGP 8.7.2, Kotlin DSL |

### Getting Started

1. Clone: `git clone https://github.com/Akashio28/App_Translate-.git`
2. Open in Android Studio, let Gradle sync.
3. Add to `local.properties`: `GEMINI_API_KEY=your_key` (get free at https://aistudio.google.com/apikey)
4. Build: `./gradlew assembleDebug`
5. APK at `app/build/outputs/apk/debug/app-debug.apk`

### Project Structure

```
app/src/main/java/com/example/app_translate/
├── MainActivity.kt
├── viewmodel/          TranslatorViewModel.kt
├── data/model/         Language, DictionaryEntry
├── data/repository/    TranslateRepository, DictionaryRepository
├── data/local/         AppDatabase, HistoryDao, HistoryEntity, DatabaseHelper
└── ui/
    ├── screen/         Translator, Camera, History, Write, Dictionary, Dialogue
    ├── components/     BottomNavigationBar, InputSection, OutputSection, LanguagePickerDialog
    └── theme/          Color, Theme, Type
```

---

## Tetum

TranslaTecho mak aplikasaun Android ida ne'ebé dezenvolve ho **Kotlin** no **Jetpack Compose** hodi ajuda uza-na'in sira tradús testu, halo OCR ho kamera, buka liafuan iha diksionáriu, verifika gramática, no hetan asisténsia hakerek ho AI — hotu ho interface Material 3 ne'ebé moos no modernu.

### Funsaun Sira

- **Tradutor** — Tradús testu liu husi MyMemory API (gratuitu, 50K karakter loron-loron), suporta lian 10, deteksaun lian automátiku (ML Kit), voz ba testu, testu ba lian, OCR husi imajen, clipboard, undo/redo
- **Kamera** — OCR langsung husi kamera (CameraX + ML Kit), kontrolu flash, fotos husi galeria
- **Istória** — Database Room, lista tuir ordem, hamoos hotu
- **Hakerek** — Verifika gramática via LanguageTool API (hatudu sala ho linha mean + sugestaun koreksaun), modo Formal/Kazuál/Expande liu husi Gemini 2.5 Flash Lite
- **Diksionáriu** — Buka liafuan Inglés liu husi Free Dictionary API (fonétika, definisaun, sinónimu, antónimu)
- **Diólogu** — Xat ho AI liu husi Gemini 2.5 Flash Lite, rai mensajen iha SQLite

### Teknolojia ne'ebé Uza

| Kamada | Teknolojia |
|---|---|
| Lian | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Arkitetura | MVVM ho StateFlow |
| Database | Room 2.6.1 + SQLite |
| Kamera | CameraX 1.3.4 |
| ML | Google ML Kit |
| API | MyMemory, Gemini 2.5 Flash Lite, LanguageTool, Free Dictionary |
| Build | Gradle 8.13, AGP 8.7.2, Kotlin DSL |

### Oinsá atu Hahú

1. Klona: `git clone https://github.com/Akashio28/App_Translate-.git`
2. Loke iha Android Studio, husik Gradle sincroniza.
3. Tau iha `local.properties`: `GEMINI_API_KEY=your_key` (hetan gratuitu iha https://aistudio.google.com/apikey)
4. Build: `./gradlew assembleDebug`
5. APK iha `app/build/outputs/apk/debug/app-debug.apk`

### Estrutura Projetu

```
app/src/main/java/com/example/app_translate/
├── MainActivity.kt
├── viewmodel/          TranslatorViewModel.kt
├── data/model/         Language, DictionaryEntry
├── data/repository/    TranslateRepository, DictionaryRepository
├── data/local/         AppDatabase, HistoryDao, HistoryEntity, DatabaseHelper
└── ui/
    ├── screen/         Translator, Camera, History, Write, Dictionary, Dialogue
    ├── components/     BottomNavigationBar, InputSection, OutputSection, LanguagePickerDialog
    └── theme/          Color, Theme, Type
```
