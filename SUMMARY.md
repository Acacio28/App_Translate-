# TranslaTecho — Esplikasaun Kompletu

---

## 1. TranslaTecho ne'é sa?

**TranslaTecho** (App Translate) mak aplikasaun Android ida ne'ebé ajuda tradús testu, verifika gramátika, hakerek ho AI, no xat ho AI. Nia hanesan tradutor + asisténsia hakerek + AI chatbot iha aplikasaun ida de'it.

---

## 2. Lian ne'ebé Suporta (10 Lian)

| Lian | Kódigu | Kódigu API |
|---|---|---|
| Inglês | en | en-US |
| Português | pt | pt-PT |
| Indonéziu | id | id |
| Spanish | es | es |
| French | fr | fr |
| Japanese | ja | ja |
| German | de | de |
| Arabic | ar | ar |
| Chinese | zh | zh |
| **Tetum** | tet | tet |

---

## 3. Ekrã Sira no Funsaun (8 Ekrã)

### 3.1 Splash Screen
**Saida:** Animasaun abertura ho naran "TranslaTecho"
**Proses:** hatudu ~1 segundu, depois ba ekrã tradutor prinsipál

### 3.2 Tradutor (Translator Screen) — Ekrã Prinsipál
**Função núklear:** Tradús testu entre lian rua
**Proses tradusaun:**
```
Uza-na'in hakerek testu → 
API MyMemory (api.mymemory.translated.net) → 
Hetan rezultadu tradusaun
```
**Fitur adisionál:**
- **Deteksaun lian otomátiku:** Uza ML Kit Google nian (on-device) — la presiza internet ba deteksaun. Se falha, uza MyMemory hanesan fallback
- **Voz ba testu:** Pikena botão mikrofone → Android RecognizerIntent → testu sai
- **Testu ba lian (TTS):** Botão volume → Android TextToSpeech lee testu
- **OCR Imajen:** Botão galeria → hili foto → ML Kit Text Recognition → testu husi imajen
- **Undo/Redo:** Rai mudansa testu iha stack, bele volta ba liur
- **Bookmark/Favorite:** Rai tradusaun ba lista favoritu (rai iha Room Database)
- **Alternativas:** Hatudu alternativu tradusaun seluk
- **Switching lian:** Botão ⇄ atu troka lian origem no destino

### 3.3 Kamera (Camera Screen)
**Função:** Buka kamera, fotos → OCR → testu → tradús
**Proses:**
```
Uza-naín loke kamera → 
CameraX hatudu previsualizasaun →
Fotos (shutter) → 
ML Kit Text Recognition fotos testu husi imajen →
Testu tradús ba lian destinatáriu
```
**Fitur:** Flash on/off, hili galeria, moldura viewfinder

### 3.4 Istória (History Screen)
**Função:** Haree fila fali tradusaun hotu ne'ebé halo ona
**Proses:** 
```
Tradusaun hotu rai iha Room Database →
HistoryScreen hatudu lista (foun ba tuan) →
Uza-na'in bele hamoos hotu ka bookmark
```
**Database:** Room — id, sourceText, targetText, sourceLang, targetLang, timestamp, isFavorite

### 3.5 Favoritu (Favorites Screen)
**Função:** Hatudu de'it tradusaun ne'ebé bookmark ona
**Proses:** Filtru husi Room database ne'ebé isFavorite = true

### 3.6 Diksionáriu (Dictionary Screen)
**Função:** Buka signifikadu liafuan
**Proses ba liafuan Inglés:**
```
Hakerek liafuan Inglés →
Free Dictionary API (api.dictionaryapi.dev) →
Hatudu: fonétika, definisaun (max 3), sinónimu, antónimu
```
**Proses ba lian seluk:**
```
Hakerek liafuan →
Gemini 2.5 Flash Lite esplika signifikadu →
Hatudu rezultadu AI
```

### 3.7 Hakerek (Write Screen) — AI Asistente
**Função:** Ajuda hakerek ho gramátika di'ak no estilu profisionál

#### Modo 1: Check Grammar
**Proses:** 
```
Uza-naín hakerek →
LaunchedEffect deteta mudansa →
Halo delay 800ms (debaunce) →
Bolu LanguageTool API (api.languagetool.org/v2/check) →
Lian suporta: en-US, pt-PT, id, es, fr, de, ja, zh, ar, ru, it, nl

Se lian la suporta (hanesan Tetum):
→ Bolu Gemini fallback (grammarViaGemini)

LanguageTool hetan erru → 
Hatudu: liaña mean iha testu sala +
Kartaun erru (testu sala + mensajen + sugestaun korreksaun) +
Testu ne'ebé korrijiu ona ho botão "Use correction"
```

#### Modo 2-4: Formal / Casual / Expand
**Proses:**
```
Uza-naín hili modu (Formal/Casual/Expand) →
Hakerek testu →
Klik botão "✨ Process with AI" →
Bolu Gemini 2.5 Flash Lite ho prompt espesífiku:
  - Formal: "Rewrite...in a professional formal style..."
  - Casual: "Rewrite...in a casual and natural style..."
  - Expand: "Expand...with more complete details..."
Gemini return rezultadu →
Hatudu iha kartaun ho botão "Use this text"
```

**Framework deteksaun lian:**
```
Uza ML Kit Language ID (on-device) →
Se deteta, uza lian ne'ebé deteta →
Uza-naín bele muda manual liu husi language picker
```

### 3.8 Xat AI (Dialogue Screen)
**Função:** Xat ho AI hanesan ChatGPT
**Proses:**
```
Uza-naín hakerek mensajen →
Bolu Gemini 2.5 Flash Lite →
Hatudu mensajen iha bolha:
  - Uza-naín = roxo, liman los
  - AI = mutin, liman karuk
Rai istória xat iha SQLite database →
Auto-scroll ba mensajen foun
```
**Nota:** Ekrã ne'e la iha botão iha navegasaun prinsipál (eksperimentál)

---

## 4. API no Servisu Ne'ebé Uza

| API | Saida Nia Fó | Presiza Xave? | Nota |
|---|---|---|---|
| **MyMemory** | Tradusaun testu + deteksaun lian | **La** (gratuitu) | Limit 50K karakter/dia, uza email `enzi23dev@gmail.com` |
| **Gemini 2.5 Flash Lite** | AI Formal/Casual/Expand, Diksionáriu lian seluk, Xat AI, Grammar fallback | **Sim** (gratuitu) | Xave husi https://aistudio.google.com/apikey |
| **LanguageTool** | Verifika gramátika + sugestaun korreksaun | **La** (gratuitu) | Suporta 15+ lian |
| **Free Dictionary API** | Definisaun liafuan Inglés | **La** (gratuitu) | Fonétika, sinónimu, antónimu |
| **ML Kit Language ID** | Deteksaun lian (on-device) | **La** (SDK lokál) | Google nia, la presiza internet |
| **ML Kit Text Recognition** | OCR husi imajen/galeria | **La** (SDK lokál) | Google nia, on-device |
| **CameraX** | Previsualizasaun kamera + fotos | **La** (SDK lokál) | Google nia, kamera library |
| **Room** | Rai istória tradusaun | — | Database lokál SQLite |

---

## 5. Arkitetura Aplikasaun

```
┌─────────────────────────────────────────────────┐
│                     UI Layer                     │
│  (Jetpack Compose + Material 3)                 │
│  SplashScreen → TranslatorScreen → Ekrã seluk   │
└──────────────────────┬──────────────────────────┘
                       │ collects StateFlow
┌──────────────────────▼──────────────────────────┐
│              ViewModel Layer                     │
│  TranslatorViewModel (AndroidViewModel)         │
│  - Rai hotu estadu iha TranslatorUiState        │
│  - Gerencia tradisaun, istória, diksionáriu, xat │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Repository Layer                    │
│  TranslateRepository  → MyMemory API            │
│  DictionaryRepository → Free Dictionary API      │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Data Layer                          │
│  Room Database → history_table                  │
│  SQLite → chat_history                          │
│  Network API calls (HttpURLConnection)          │
└─────────────────────────────────────────────────┘
```

**Konseitu importante:**
- **MVVM:** Model-View-ViewModel — separa lójika negósiu husi UI
- **StateFlow:** Rai estadu no atualiza UI otomátiku
- **Single ViewModel:** ViewModel ida de'it ba aplikasaun tomak
- **Manual DI:** La uza dependency injection (Hilt/Dagger)

---

## 6. Permisaun Aplikasaun

| Permisaun | Ba Sa Nia Presiza |
|---|---|
| `INTERNET` | Bolu API MyMemory, Gemini, LanguageTool, Free Dictionary |
| `CAMERA` | Buka kamera ba OCR / fotos |
| `RECORD_AUDIO` | Voz ba testu (mic) |

---

## 7. Stack Teknolojia Kompletu

| Komponente | Naran | Versaun |
|---|---|---|
| Lian | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.11.00 |
| Arkitetura | MVVM + StateFlow | — |
| Database Istória | Room | 2.6.1 |
| Database Xat | SQLite (SQLiteOpenHelper) | — |
| Kamera | CameraX | 1.3.4 |
| Navegasaun | Navigation Compose (deklaradu maibé la uza) | 2.8.3 |
| ML ID Lian | com.google.mlkit:language-id | 17.0.6 |
| ML Tradusaun | com.google.mlkit:translate (deklaradu maibé la uza) | 17.0.1 |
| ML OCR | com.google.mlkit:text-recognition | 16.0.0 |
| Íkone | Material Icons Extended | 1.7.5 |
| Lifecycle | lifecycle-runtime-compose, lifecycle-viewmodel-compose | 2.8.7 |
| Web Service | HttpURLConnection (padraun Android) | — |
| JSON | org.json (padraun Android) | — |
| Build | Gradle + Kotlin DSL | 8.13 |
| AGP | com.android.application | 8.7.2 |
| Min SDK | Android 7.0 Nougat | 24 |
| Target SDK | Android 15 | 35 |

---

## 8. Informasaun Husu GitHub

- **Repositóriu:** https://github.com/Acacio28/App_Translate-
- **Branch prinsipál:** main
- **Oinsá atu kompila:**
  ```bash
  git clone https://github.com/Acacio28/App_Translate-.git
  cd App_Translate
  # Kria local.properties ho xave Gemini:
  # GEMINI_API_KEY=AIzaSy...
  ./gradlew assembleDebug
  # APK iha: app/build/outputs/apk/debug/app-debug.apk
  ```
- **Xave Gemini:** Hetan gratuitu iha https://aistudio.google.com/apikey

---

## 9. Oinsá Esplika ba Emi Seluk (Versaun 30 Segundu)

> "TranslaTecho mak aplikasaun Android tradusaun ne'ebé uza Kotlin no Jetpack Compose. Nia tradús entre 10 lian liu husi MyMemory API ne'ebé gratuitu. Iha mós verifika gramátika automátiku liu husi LanguageTool, asisténsia hakerek ho AI Formal/Casual/Expand liu husi Gemini Google nian, no xat ho AI. Ba deteksaun lian no OCR, nia uza Google ML Kit ne'ebé serbisu iha dispositivu rasik la presiza internet. Nia arkitetura uza MVVM ho ViewModel ida de'it. Presiza xave Gemini gratuitu, ne'ebé tau iha local.properties. Kódigu fonte iha GitHub, bele kompila rasik ka uza apk."
