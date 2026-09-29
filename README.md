# LLM#1 — Universal AI Chat Client & Local API Gateway


<p align="center">
  <b>A state-of-the-art, privacy-first mobile AI workstation for Android.</b><br>
  Connect to 10+ cloud AI providers, run completely offline with local PC models (Ollama / LM Studio), parse any document or spreadsheet natively, ground answers with live web search, and turn your Android device into an on-device OpenAI-compatible REST server and desktop web gateway.
</p>

</div>

---

## 📑 Table of Contents

- [Executive Overview](#-executive-overview)
- [Key Features at a Glance](#-key-features-at-a-glance)
- [A to Z Comprehensive Feature Guide](#-a-to-z-comprehensive-feature-guide)
  - [A — AI Providers & Model Registry](#a--ai-providers--model-registry)
  - [B — Backup & Restore](#b--backup--restore)
  - [C — Chat Sessions & Incognito Mode](#c--chat-sessions--incognito-mode)
  - [D — Document & File Attachment Engine](#d--document--file-attachment-engine)
  - [E — Endpoints & Custom OpenAI Proxies](#e--endpoints--custom-openai-proxies)
  - [F — Formatting & Markdown Engine](#f--formatting--markdown-engine)
  - [G — Grounding with Live Web Search](#g--grounding-with-live-web-search)
  - [H — HTTP LocalHost Server & Service](#h--http-localhost-server--service)
  - [I — IP Access Control & Whitelisting](#i--ip-access-control--whitelisting)
  - [J — JSON & Markdown Chat Export](#j--json--markdown-chat-export)
  - [K — Key Security & Passcode Protection](#k--key-security--passcode-protection)
  - [L — Local Desktop Web Client](#l--local-desktop-web-client)
  - [M — Multimodal Vision & Image Processing](#m--multimodal-vision--image-processing)
  - [N — Network Architecture & OpenAI REST Standard](#n--network-architecture--openai-rest-standard)
  - [O — Offline & Air-Gapped Operation](#o--offline--air-gapped-operation)
  - [P — Personas & System Prompt Engineering](#p--personas--system-prompt-engineering)
  - [Q — QR Code Quick Connect](#q--qr-code-quick-connect)
  - [R — Room Database Architecture & Migrations](#r--room-database-architecture--migrations)
  - [S — Speech & Voice Interaction (STT & TTS)](#s--speech--voice-interaction-stt--tts)
  - [T — Token Usage Statistics & Counter](#t--token-usage-statistics--counter)
  - [U — UI/UX Design System (Material Design 3)](#u--uiux-design-system-material-design-3)
  - [V — Vision & Image Attachments](#v--vision--image-attachments)
  - [W — Web Search Engine Management](#w--web-search-engine-management)
  - [X — XML, ODF & Office Parsing Subsystem](#x--xml-odf--office-parsing-subsystem)
  - [Y — Yield & Adaptive Context Window Limiter](#y--yield--adaptive-context-window-limiter)
  - [Z — Zero Cloud Tracking Policy](#z--zero-cloud-tracking-policy)
- [System Architecture](#-system-architecture)
- [Technology Stack](#-technology-stack)
- [Project Directory Structure](#-project-directory-structure)
- [LocalHost Gateway REST API Reference](#-localhost-gateway-rest-api-reference)
- [Getting Started & Installation](#-getting-started--installation)
- [Configuration & Environment Variables](#-configuration--environment-variables)
- [Security & Network Policies](#-security--network-policies)
- [Troubleshooting & FAQ](#-troubleshooting--faq)
- [Contributing & License](#-contributing--license)

---

## 🚀 Executive Overview

**LLM#1** is an all-in-one Android application designed for developers, researchers, writers, and power users who demand total flexibility and privacy in their AI workflows. Unlike walled-garden client apps that tie you to a single vendor or upload your conversation telemetry to third-party tracking servers, **LLM#1 is 100% client-driven, zero-tracking, and air-gap capable**.

### Core Pillars

1. **Provider Agnostic:** Connect directly to official endpoints from OpenRouter, OpenAI, Google Gemini, Groq, DeepSeek, Mistral, Anthropic Claude, Perplexity AI, or run local instances like Ollama and LM Studio.
2. **On-Device HTTP Web Server:** The app embeds a full multi-threaded HTTP server (`LocalHostServer`) running as an Android Foreground Service. It serves a responsive desktop web client and exposes standard `/v1/chat/completions` REST endpoints over your local Wi-Fi.
3. **Universal Native File Ingestion:** Attach PDFs, Microsoft Word (`.docx`), Excel spreadsheets (`.xlsx`), PowerPoint decks (`.pptx`), OpenDocument files (`.odt`, `.ods`, `.odp`), CSVs, source code, and images without third-party cloud converters.
4. **Real-Time Web Search Augmentation:** Ground any query with live, up-to-the-minute web results from Google, Bing, DuckDuckGo, Yahoo, Brave, or SearXNG before the prompt reaches the LLM.
5. **Rock-Solid Local Persistence:** Built on Android Jetpack Room with automatic schema migrations, encrypted/private local storage, and full JSON backup/restore.

---

## ⚡ Key Features at a Glance

| Feature | Description |
| :--- | :--- |
| **Multi-Provider Catalog** | 10+ preset providers with instant latency testing, model dropdowns, and documentation links. |
| **Local Desktop Web App** | Access your phone's LLM from any PC browser on the same Wi-Fi network via an embedded single-page app. |
| **OpenAI Proxy (`/v1`)** | Use your phone as an OpenAI-compatible API endpoint for desktop IDEs, Cursor, Continue.dev, or scripts. |
| **Native Document Parsing** | Zero-dependency extraction for Word, Excel, PowerPoint, PDF, RTF, CSV, Code, and Archives. |
| **Live Web Grounding** | Automatic web search query generation, live snippet extraction, and citation injection. |
| **Voice & Speech Engine** | Hands-free voice dictation (STT) and native Text-to-Speech (TTS) with rate control (0.5x–2.0x). |
| **Rich Markdown UI** | Headings, blockquotes, scrollable spreadsheet tables, syntax code blocks with 1-tap copy, and image downloads. |
| **Security & Privacy** | Passcode protection (PIN), IP subnet whitelisting, ephemeral Incognito sessions, and air-gapped support. |
| **Data Sovereignty** | 100% on-device SQLite database; export or import complete backups in standard JSON format. |

---

## 📖 A to Z Comprehensive Feature Guide

### A — AI Providers & Model Registry
LLM#1 integrates with any provider compliant with the OpenAI REST schema, plus tailored presets for frontier and open-source models:
* **OpenRouter:** Access 100+ models with a single unified key (`meta-llama/llama-3.3-70b-instruct`, `anthropic/claude-3.5-sonnet`, `google/gemini-2.0-flash-exp:free`, `deepseek/deepseek-r1`).
* **OpenAI:** Official GPT-4o, GPT-4o-mini, o1-mini, and GPT-4-turbo models.
* **Google Gemini:** Direct OpenAI-compatible endpoint (`generativelanguage.googleapis.com/v1beta/openai`) with high-speed models (`gemini-1.5-flash`, `gemini-1.5-pro`).
* **Groq:** Ultra low-latency inference on custom LPUs (`llama-3.3-70b-versatile`, `mixtral-8x7b-32768`).
* **DeepSeek:** Frontier coding and mathematical reasoning (`deepseek-chat`, `deepseek-reasoner`).
* **Mistral AI:** High-efficiency European models (`mistral-small-latest`, `codestral-latest`).
* **Anthropic (Claude):** Direct Claude 3.5 Sonnet, Haiku, and Opus models.
* **Perplexity AI:** Search-augmented reasoning models (`sonar-pro`, `sonar`).
* **Ollama (Local):** Zero-config connection to offline models running on your local machine (`http://10.0.2.2:11434/v1` or local LAN IP).
* **LM Studio (Local):** Local desktop LLM server support (`http://10.0.2.2:1234/v1`).
* **Custom Endpoints:** Input any custom host URL, port, authorization header, and model tag.

### B — Backup & Restore
* **Full Database Export:** Creates an export containing all chat sessions, messages, API configs, active personas, system prompts, web search settings, and token counters.
* **Non-Destructive Restoration:** Seamlessly inspects imported JSON, validates schema versions, and reinstates history into Room SQLite with immediate UI synchronization.
* **Storage Independence:** Backups can be saved to local storage, shared via Android Intent, or transferred between devices.

### C — Chat Sessions & Incognito Mode
* **Multi-Session Drawer:** Organize conversations into distinct sessions with automatic semantic titling based on the initial query.
* **Session Lifecycle:** Rename, switch, or delete individual sessions with swipe and long-press actions.
* **Incognito Mode:** Toggle the top-bar Incognito switch to enter a transient state. In Incognito mode:
  * Messages are processed and rendered in memory.
  * No queries, responses, or attachments are written to the SQLite database.
  * History is instantly wiped upon exiting Incognito mode.

### D — Document & File Attachment Engine
Powered by `FileAttachmentHelper.kt`, LLM#1 features a native multi-format extraction engine operating on `Dispatchers.IO`:
* **PDF:** Rendered via Android's native `PdfRenderer` for first-page visual previews, accompanied by ISO-8859-1 byte stream extraction of `BT...ET` and `Tj/TJ` text blocks.
* **Microsoft Office (.docx, .pptx, .xlsx):** Pure-Kotlin `ZipInputStream` parser that decompresses OpenXML containers to extract `word/document.xml`, `ppt/slides/slide*.xml`, and `xl/worksheets/sheet*.xml` without bulky external Java libraries.
* **OpenDocument (.odt, .ods, .odp):** Parses OASIS `content.xml` packages and formats tabular spreadsheets directly into markdown tables.
* **Tabular Data (.csv, .tsv):** Handles quoted delimiters, extracts header rows, and constructs clean Markdown tables.
* **Code & Markup:** Kotlin, Java, Python, JavaScript, TypeScript, C/C++, Rust, Go, SQL, HTML, CSS, JSON, YAML, TOML, Markdown, and shell scripts automatically wrapped in language-tagged code fences.
* **Archives (.zip, .jar, .tar, .apk):** Inspects archive table-of-contents, file sizes, and extracts embedded `README.md` files.

### E — Endpoints & Custom OpenAI Proxies
* Define and store multiple custom endpoints under the **Endpoints** tab.
* Toggle between development proxies, enterprise internal AI gateways (e.g., vLLM, LiteLLM, FastChat), and cloud providers.
* Perform one-tap live connection and latency ping tests directly from the interface.

### F — Formatting & Markdown Engine
Built from scratch in `MarkdownFormatter.kt` using Jetpack Compose `AnnotatedString` and custom composables:
* **Inline Tokens:** Supports `**bold**`, `*italic*`, `~~strikethrough~~`, `` `inline code` ``, and hyperlinks.
* **Escaped Character Handling:** Upgraded lookbehind regex engine ensures escaped asterisks (`\*`) render cleanly without false formatting triggers.
* **HTML Linebreak Translation:** Converts AI-generated `<br>`, `<br/>`, and `<br />` tags into native visual line breaks.
* **Scrollable Spreadsheet Tables:** Renders markdown tables in responsive, horizontally-scrollable containers with alternating header styling, eliminating clipping on mobile screens.
* **Syntax Code Blocks:** Distinct code container with monospace typography, language pill, and a one-tap copy-to-clipboard button with haptic feedback.
* **Embedded Image Downloader:** Automatic detection of markdown images (`![alt](url)`) with Coil asynchronous rendering and an instant save-to-gallery button.

### G — Grounding with Live Web Search
Augment your LLM with real-time world knowledge via `WebSearchService.kt`:
* **Active Engines:** Choose from Google, Microsoft Bing, DuckDuckGo, Yahoo, Brave Search, or private SearXNG instances.
* **Fallback Architecture:** If an engine encounters rate limits or CAPTCHAs, queries gracefully fall back to live DuckDuckGo HTML extraction to ensure uninterrupted results.
* **Grounding Context:** Search snippets, page titles, and destination URLs are injected into a dedicated `[Live Web Search Results]` prompt block before query dispatch.

### H — HTTP LocalHost Server & Service
Turn your mobile device into an on-demand AI server for your local area network:
* **Architecture:** Built on raw Java `ServerSocket` driven by Kotlin Coroutines on `Dispatchers.IO`.
* **Android Service:** Managed by `LocalHostService`, running as an Android Foreground Service with a persistent notification (`FOREGROUND_SERVICE_DATA_SYNC`) to prevent OS sleep.
* **Live Network Inspection:** Automatically determines active Wi-Fi, Hotspot, and Loopback IP addresses (`127.0.0.1`).
* **Request Inspector:** Real-time log buffer detailing HTTP method, request path, status code, client IP, and latency in milliseconds.

### I — IP Access Control & Whitelisting
* Configure CIDR or specific IP address restrictions in **Settings > Local Host**.
* Support for exact IP matches (e.g., `192.168.1.45`) and wildcard subnet masks (e.g., `192.168.1.*`).
* Automatically drops unauthorized LAN connections with HTTP 403 Forbidden.

### J — JSON & Markdown Chat Export
* **JSON Export:** Full export of session messages with role tags, unix timestamps, attachment names, and token metadata.
* **Markdown Export:** Clean, human-readable document containing styled speaker headings, blockquoted metadata, and horizontal rules, ready for Obsidian, Notion, or GitHub.

### K — Key Security & Passcode Protection
* **Local-Only Key Storage:** API credentials are stored strictly in private app storage (`app_prefs.xml`) and never transmitted to any telemetry or proxy backend.
* **PIN Authentication:** Secure the LocalHost Web UI and API routes with a 4-digit PIN passcode. Web clients must supply the passcode or pass an `X-Passcode` HTTP header.

### L — Local Desktop Web Client
When the LocalHost server is active, visiting `http://<phone-ip>:8080` from your laptop, tablet, or desktop browser opens a built-in single-page web app:
* Complete desktop chat interface with real-time responses.
* Remote model selection: Switch the active model on your phone directly from your desktop.
* Session management: Create, browse, and delete mobile chat sessions.
* File & Document drag-and-drop attachment upload.
* Dark / Light mode synchronized with modern CSS variables.

### M — Multimodal Vision & Image Processing
* Select pictures via the Android Photo Picker (`PickVisualMedia`).
* High-resolution images are scaled to a maximum dimension of 1024px and compressed to JPEG (85% quality) to conserve device RAM and API tokens.
* Encoded into standard OpenAI multimodal format (`image_url: { url: "data:image/jpeg;base64,..." }`) for seamless consumption by GPT-4o, Claude 3.5, and Gemini.

### N — Network Architecture & OpenAI REST Standard
The local HTTP server exposes two industry-standard endpoints:
* `GET /v1/models`: Returns JSON array of all configured models formatted to OpenAI specification.
* `POST /v1/chat/completions`: Accepts standard OpenAI JSON payloads, passes them through your configured mobile credentials, and returns completion responses.

### O — Offline & Air-Gapped Operation
* Connect directly to Ollama or LM Studio running on your local network (e.g., `http://192.168.1.100:11434/v1`).
* Enjoy complete AI chat and document analysis without an active internet connection.

### P — Personas & System Prompt Engineering
Switch system instructions on the fly:
1. **General Assistant:** Comprehensive, thoroughly reasoned, structured markdown responses.
2. **Code Architect:** Principal software engineer persona delivering production-ready, clean, idiomatic code without fluff.
3. **Concise & Direct:** Ultra-efficient, high-density answers without greetings or conversational filler.
4. **Research Analyst:** Scholarly, objective, and empirical breakdowns with multi-viewpoint analysis.
5. **Creative Writer:** Imaginative prose, evocative sensory details, and narrative pacing.
6. **Custom Persona:** Full user-defined system prompt textarea.

### Q — QR Code Quick Connect
* On-device raster generator (`QrCodeGenerator.kt`) renders a crisp QR code bitmap directly in the LocalHost settings panel.
* Point your laptop or tablet camera at the phone screen to immediately open the Web Client without typing IP addresses.

### R — Room Database Architecture & Migrations
Built with Room 2.6+ with explicit migrations:
* `chat_sessions`: Manages session metadata, auto-generated titles, and creation/update timestamps.
* `chat_messages`: Stores message body, role (`user` vs `assistant`), image URIs, and attachment payload metadata.
* `api_configs`: Stores endpoint URLs, model identifiers, API keys, and active state flags.

### S — Speech & Voice Interaction (STT & TTS)
* **Speech-to-Text (STT):** Dictate queries hands-free using Android's native `SpeechRecognizer` (`android.permission.RECORD_AUDIO`).
* **Text-to-Speech (TTS):** Integrated Android `TextToSpeech` engine.
  * Strips markdown syntax, code fences, and links before reading for clean voice output.
  * Adjustable speech rate from 0.5x to 2.0x.
  * Optional Auto-Speak mode to read incoming AI responses automatically.

### T — Token Usage Statistics & Counter
* Real-time tracking of token consumption across prompt and completion phases.
* Cumulative statistics showing:
  * Total Prompt Tokens
  * Total Completion Tokens
  * Total API Requests
  * Last Request Token Count
* One-tap counter reset in **Settings > General**.

### U — UI/UX Design System (Material Design 3)
* Fully compliant with modern **Material Design 3 (M3)** principles.
* Dynamic theming supporting system dark and light modes.
* Edge-to-edge layout with full support for Android WindowInsets (`safeDrawing`, `statusBars`, `ime`).
* Smooth animated transitions and responsive input bars with auto-resizing text fields.

### V — Vision & Image Attachments
* In-line visual cards displaying attached images and generated charts.
* Pinch-to-zoom and full-screen preview support.
* Direct download action saving processed images to the device's public media store.

### W — Web Search Engine Management
* Full CRUD management for custom search engines under **Settings > Search**.
* Customize search URL templates using `{query}` substitution tags.
* Individual toggles to enable or disable web search grounding per session.

### X — XML, ODF & Office Parsing Subsystem
* Custom XML streaming helpers unescape standard entities (`&amp;`, `&lt;`, `&gt;`, `&quot;`, `&#124;`).
* Extracts footnotes, headings, bullet lists, and table structures directly from Microsoft Word XML schemas (`<w:t>`, `<w:p>`, `<w:tr>`, `<w:tc>`).

### Y — Yield & Adaptive Context Window Limiter
* Configure context history limits (6, 12, 20, 40 messages, or unlimited).
* Prevents token limit overflow and reduces API cost on large, continuous chat threads by automatically slicing earlier turns while preserving active system prompts.

### Z — Zero Cloud Tracking Policy
* No analytics SDKs (Firebase Analytics, Mixpanel, Segment, etc.).
* No advertising IDs or background telemetry pings.
* All network traffic flows strictly between your Android device and your explicitly chosen AI endpoints.

---

## 🏗 System Architecture

The following diagram illustrates the complete data flow and architectural layers of LLM#1:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│                                                                        │
│   ┌───────────────────────────┐      ┌─────────────────────────────┐   │
│   │   ChatScreen (Compose)    │      │   SettingsScreen (Compose)  │   │
│   │  - MarkdownFormatter      │      │  - Providers & Endpoints    │   │
│   │  - VoiceManager (STT/TTS) │      │  - Search Engine Manager    │   │
│   │  - Attachment Engine      │      │  - LocalHost Dashboard      │   │
│   └─────────────┬─────────────┘      └──────────────┬──────────────┘   │
│                 │                                   │                  │
│                 └─────────────────┬─────────────────┘                  │
│                                   ▼                                    │
│                       ┌───────────────────────┐                        │
│                       │     MainViewModel     │                        │
│                       └───────────┬───────────┘                        │
└───────────────────────────────────┼────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼────────────────────────────────────┐
│                          DATA LAYER                                    │
│                                   ▼                                    │
│                       ┌───────────────────────┐                        │
│                       │     AppRepository     │                        │
│                       └─────┬───────────┬─────┘                        │
│                             │           │                              │
│              ┌──────────────┘           └──────────────┐               │
│              ▼                                         ▼               │
│   ┌─────────────────────┐                   ┌─────────────────────┐    │
│   │  AppDatabase (Room) │                   │  SharedPreferences  │    │
│   │  - chat_sessions    │                   │  - App settings     │    │
│   │  - chat_messages    │                   │  - Token counters   │    │
│   │  - api_configs      │                   │  - Custom personas  │    │
│   └─────────────────────┘                   └─────────────────────┘    │
└────────────────────────────────────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                    NETWORK & SERVER SUBSYSTEM                          │
│                                                                        │
│   ┌─────────────────────────┐             ┌────────────────────────┐   │
│   │   NetworkModule / API   │             │   LocalHostServer      │   │
│   │  - OpenAiApiService     │             │  - Foreground Service  │   │
│   │  - WebSearchService     │             │  - Desktop Web Client  │   │
│   │  - OkHttp / Retrofit    │             │  - OpenAI /v1 Proxy    │   │
│   └───────────┬─────────────┘             └───────────┬────────────┘   │
└───────────────┼───────────────────────────────────────┼────────────────┘
                ▼                                       ▼
     ┌───────────────────────┐             ┌─────────────────────────┐
     │  Cloud / Local LLMs   │             │  LAN Desktop Clients    │
     │  (OpenAI, Gemini,     │             │  (Browser, Cursor IDE,  │
     │   Ollama, Groq, etc.) │             │   Continue, Python)     │
     └───────────────────────┘             └─────────────────────────┘
```

---

## 🛠 Technology Stack

* **Core Language:** Kotlin 2.0+
* **UI Framework:** Jetpack Compose with Material Design 3 (M3)
* **Architecture:** Model-View-ViewModel (MVVM) + Unidirectional Data Flow (StateFlow)
* **Local Persistence:** Android Jetpack Room 2.6+ with KSP code generation
* **HTTP Client:** OkHttp 4.12+ with Retrofit 2.11+ and Moshi Kotlin
* **Image Loading:** Coil Compose
* **Document Processing:** Pure-Kotlin streaming ZIP, XML, and Android `PdfRenderer`
* **Speech Services:** Android `SpeechRecognizer` (STT) and `TextToSpeech` (TTS)
* **Barcode Generation:** Custom matrix rasterizer (PNG compression)
* **Server Infrastructure:** Kotlin Coroutine-based `ServerSocket` HTTP/1.1 engine running in a Foreground Service

---

## 📂 Project Directory Structure

```
app/src/main/
├── AndroidManifest.xml                  # Permissions, Activities & Services
├── java/com/example/
│   ├── MainActivity.kt                  # Root Activity, DB init & Compose NavHost
│   ├── data/
│   │   ├── AiPersona.kt                 # System prompt personas & templates
│   │   ├── ApiConfig.kt                 # Room entity for API endpoints
│   │   ├── AppDao.kt                    # Room Data Access Object
│   │   ├── AppDatabase.kt               # Room database definition & migrations
│   │   ├── AppRepository.kt             # Central data repository & preferences
│   │   ├── BackupRestoreHelper.kt       # JSON backup & restore engine
│   │   ├── ChatMessage.kt               # Room entity for chat messages
│   │   ├── ChatSession.kt               # Room entity for chat sessions
│   │   ├── ProviderModels.kt            # Provider catalog & preset registries
│   │   ├── SearchEngine.kt              # Search engine model & templates
│   │   └── TokenUsageStats.kt           # Token analytics data model
│   ├── network/
│   │   ├── NetworkModule.kt             # Dynamic Retrofit / OkHttp factories
│   │   ├── OpenAiApiService.kt          # OpenAI REST contract definition
│   │   └── WebSearchService.kt          # Multi-engine web search scraper & API
│   ├── server/
│   │   ├── LocalHostManager.kt          # Server state, IP detection & log buffer
│   │   ├── LocalHostServer.kt           # Embedded HTTP server implementation
│   │   ├── LocalHostService.kt          # Android Foreground Service wrapper
│   │   ├── LocalHostWebPage.kt          # Embedded Desktop HTML/CSS/JS Single-Page App
│   │   ├── QrCodeGenerator.kt           # QR code generation engine
│   │   └── ServerLogEntry.kt            # HTTP request log data model
│   └── ui/
│       ├── screens/
│       │   ├── AiProviderBadge.kt       # Visual provider badge component
│       │   ├── ChatScreen.kt            # Primary chat conversation interface
│       │   ├── LocalHostTabContent.kt   # LocalHost dashboard tab composable
│       │   ├── ProviderModelDialog.kt   # Provider configuration dialog
│       │   ├── SearchServicesTabContent.kt # Search services tab composable
│       │   └── SettingsScreen.kt        # Multi-tab settings & provider manager
│       ├── theme/
│       │   ├── Color.kt                 # Material 3 color palettes
│       │   ├── Theme.kt                 # Dynamic Theme definition
│       │   └── Type.kt                  # Typography definitions
│       ├── util/
│       │   ├── FileAttachmentHelper.kt  # Multi-format document parser
│       │   ├── ImageDownloadHelper.kt   # Gallery image saving helper
│       │   ├── ImageHelper.kt           # Bitmap scaling & compression helper
│       │   ├── MarkdownFormatter.kt     # Jetpack Compose markdown formatter
│       │   └── VoiceManager.kt          # SpeechRecognizer & TextToSpeech manager
│       └── viewmodel/
│           ├── MainViewModel.kt         # Central UI ViewModel
│           └── MainViewModelFactory.kt  # ViewModel dependency injection factory
└── res/
    ├── drawable/                        # Vector drawables & icons
    ├── mipmap/                          # App launcher icons
    ├── values/
    │   ├── strings.xml                  # App strings
    │   └── styles.xml                   # Theme resources
    └── xml/                             # Backup & data extraction rules
```

---

## 🌐 LocalHost Gateway REST API Reference

When the LocalHost Server is running (default port `8080`), it exposes the following endpoints to clients on the local network:

### General & Telemetry Endpoints

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/` or `/index.html` | Serves the full single-page Desktop Web Chat Client | No |
| `GET` | `/api/status` | Returns JSON status: server state, IP, port, active model, request count | No |
| `GET` | `/api/qr` | Generates a PNG QR code encoding the server URL | No |
| `GET` | `/api/logs` | Returns JSON array of the last 50 HTTP requests with latency | No |
| `POST` | `/api/logs/clear` | Clears the in-memory server log buffer | No |
| `POST` | `/api/auth/verify` | Validates client PIN passcode: `{"passcode":"1234"}` | No |

### AI & Session Endpoints

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/models` | Lists all configured models on the phone | If PIN enabled |
| `POST` | `/api/models/select` | Sets active model: `{"id": 2}` | If PIN enabled |
| `GET` | `/api/sessions` | Lists all chat sessions with timestamps | If PIN enabled |
| `POST` | `/api/sessions` | Creates a new session: `{"title": "Research"}` | If PIN enabled |
| `DELETE` | `/api/sessions?id={id}` | Deletes a specified session | If PIN enabled |
| `GET` | `/api/messages?sessionId={id}` | Returns all messages for a session | If PIN enabled |
| `GET` | `/api/export?sessionId={id}&format=md` | Exports chat as Markdown (`format=md`) or JSON (`format=json`) | If PIN enabled |
| `POST` | `/api/chat` | Main web chat endpoint with document context and web search | If PIN enabled |

### OpenAI Standard REST API (`/v1`)

| Method | Endpoint | Description | Payload Format |
| :--- | :--- | :--- | :--- |
| `GET` | `/v1/models` | OpenAI-compatible list of available models | Standard OpenAI JSON |
| `POST` | `/v1/chat/completions` | OpenAI-compatible chat completion proxy | Standard OpenAI JSON |

#### Example: Using LLM#1 with `curl` from your PC
```bash
curl http://192.168.1.105:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "X-Passcode: 1234" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [
      {"role": "user", "content": "Explain quantum computing in simple terms."}
    ]
  }'
```

---

## 💻 Getting Started & Installation

### Prerequisites
* **Android Studio:** Ladybug (2024.2.1+) or newer
* **JDK:** Version 17 or Version 21
* **Android SDK:** Compile SDK 36, Minimum SDK 24 (Android 7.0 Nougat)
* **Device / Emulator:** Any ARM64 or x86_64 device running Android 7.0 or higher

### Building from Source

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/your-username/openllm-android.git
   cd openllm-android
   ```

2. **Configure Environment Variables:**
   Copy `.env.example` to `.env` in the root project directory:
   ```bash
   cp .env.example .env
   ```
   Add your Gemini or OpenRouter API keys if you wish to pre-configure defaults:
   ```env
   GEMINI_API_KEY=your_actual_api_key_here
   ```

3. **Open and Sync in Android Studio:**
   * Open Android Studio and select **Open**.
   * Navigate to the project folder and click **OK**.
   * Allow Gradle to download dependencies and sync the project catalog.

4. **Run on Device or Emulator:**
   * Connect your physical Android device via USB with USB Debugging enabled, or launch an Android Virtual Device (AVD).
   * Click **Run 'app'** (`Shift + F10`).

---

## 🔒 Security & Network Policies

### Cleartext Traffic Policy
Because LLM#1 contains an embedded local HTTP server for local network communication (`http://192.168.x.x:8080`) and connects to local development endpoints (e.g., `http://10.0.2.2:11434` for Ollama), `android:usesCleartextTraffic="true"` is declared in `AndroidManifest.xml`. All external cloud API requests (OpenAI, Gemini, OpenRouter, Anthropic) strictly use encrypted TLS (`https://`).

### Zero-Permission Storage Ingestion
In full compliance with Google Play Developer Program policies:
* Media files are ingested via the privacy-respecting Android Photo Picker (`ActivityResultContracts.PickVisualMedia()`) which requires **zero storage permissions**.
* Documents are opened via the system Storage Access Framework (`ActivityResultContracts.OpenDocument()`).
* Legacy storage permissions are restricted to `maxSdkVersion="28"`.

---

## ❓ Troubleshooting & FAQ

#### 1. Cannot connect to LocalHost server from PC browser?
* **Same Network Check:** Ensure your computer and your Android device are connected to the exact same Wi-Fi network (or that your PC is connected to the phone's portable hotspot).
* **AP Isolation:** Some public or corporate Wi-Fi routers enable "AP Isolation" or "Client Isolation", which blocks connected devices from communicating with one another. If this occurs, enable your phone's personal hotspot and connect your PC to it.
* **Firewall / Port:** Verify the server is started (green indicator in the app) and check that port `8080` is not blocked.

#### 2. How to connect to Ollama on my PC from the Android Emulator?
* Use the special Android emulator loopback alias: `http://10.0.2.2:11434/v1`.
* Ensure Ollama is configured to listen on all interfaces by setting the environment variable `OLLAMA_HOST=0.0.0.0` on your host computer.

#### 3. Why are long files truncated?
* Extremely large files (over 80,000 characters) are safely truncated to prevent exceeding the LLM's maximum token context window and to safeguard device memory. You can adjust the context window limit in **Settings > General**.

---

## 📄 Contributing & License

Contributions are welcome! Please feel free to submit pull requests, report issues, or suggest new features.

```
Copyright 2026 LLM#1 Project Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
