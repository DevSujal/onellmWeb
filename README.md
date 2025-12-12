<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot 3.2"/>
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="MIT License"/>
  <img src="https://img.shields.io/badge/Providers-10-blue?style=for-the-badge" alt="10 Providers"/>
</p>

<h1 align="center">🚀 OneLLM</h1>

<p align="center">
  <strong>One SDK. Any LLM. Zero Hassle.</strong>
  <br/>
  <em>A unified Java SDK & REST API to call 10+ LLM providers with a single interface</em>
</p>

---

## ✨ Why OneLLM?

Tired of managing different SDKs for each LLM provider? OneLLM solves this by providing:

- 🔌 **Single Interface**: One API for OpenAI, Anthropic, Google, and 7 more providers
- 🌐 **REST API**: Deploy as a Spring Boot microservice with instant HTTP access
- 🎯 **Auto-Routing**: Just specify the model name — we find the right provider
- ⚡ **Streaming**: Real-time token streaming (SSE for REST, callbacks for SDK)  
- 🔄 **Async Support**: Non-blocking calls with `CompletableFuture`
- 🛡️ **Resilient**: Built-in retry logic with exponential backoff
- 🪶 **Production Ready**: Spring Boot 3.2 with configuration via environment variables

---

## 🚀 Two Ways to Use OneLLM

### Option 1: REST API (Spring Boot)

Deploy OneLLM as a microservice and call it via HTTP from any language.

```bash
# Start the server
mvn spring-boot:run

# Call the API
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

### Option 2: Java SDK

Use OneLLM directly in your Java application.

```java
OneLLM llm = OneLLM.builder()
    .openai("sk-...")
    .anthropic("sk-ant-...")
    .build();

LLMResponse response = llm.complete(
    LLMRequest.builder()
        .model("gpt-4")
        .user("Hello!")
        .build()
);
```

---

## 🌐 REST API Reference

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/chat/completions` | POST | Synchronous chat completion |
| `/api/chat/completions/stream` | POST | Streaming via Server-Sent Events |
| `/api/providers` | GET | List configured providers |
| `/api/health` | GET | Health check |

### Chat Completion Request

```json
POST /api/chat/completions
Content-Type: application/json

{
  "model": "gpt-4",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "What is Java?"}
  ],
  "temperature": 0.7,
  "maxTokens": 1000
}
```

### Chat Completion Response

```json
{
  "id": "chatcmpl-...",
  "model": "gpt-4",
  "content": "Java is a high-level, object-oriented programming language...",
  "finishReason": "stop",
  "provider": "openai",
  "latencyMs": 1234,
  "usage": {
    "promptTokens": 25,
    "completionTokens": 150,
    "totalTokens": 175
  }
}
```

### Streaming (Server-Sent Events)

```bash
curl -X POST http://localhost:8080/api/chat/completions/stream \
  -H "Content-Type: application/json" \
  -d '{"model": "gpt-4", "messages": [{"role": "user", "content": "Write a poem"}]}'
```

Response stream:
```
event: chunk
data: {"content": "In "}

event: chunk
data: {"content": "the "}

event: chunk
data: {"content": "realm "}

event: complete
data: {"id": "...", "model": "gpt-4", "content": "In the realm...", ...}
```

---

## ⚙️ Configuration

### Environment Variables

Set API keys via environment variables:

```bash
# Windows
set ONELLM_OPENAI_API_KEY=sk-...
set ONELLM_ANTHROPIC_API_KEY=sk-ant-...
set ONELLM_GOOGLE_API_KEY=AIza...
set ONELLM_GROQ_API_KEY=gsk_...

# Linux/Mac
export ONELLM_OPENAI_API_KEY=sk-...
export ONELLM_ANTHROPIC_API_KEY=sk-ant-...
```

### application.properties

```properties
# Server
server.port=8080

# Providers (automatically enabled when API key is set)
onellm.openai.api-key=${ONELLM_OPENAI_API_KEY:}
onellm.anthropic.api-key=${ONELLM_ANTHROPIC_API_KEY:}
onellm.google.api-key=${ONELLM_GOOGLE_API_KEY:}
onellm.groq.api-key=${ONELLM_GROQ_API_KEY:}

# Ollama (enabled by default for local development)
onellm.ollama.enabled=true
onellm.ollama.base-url=http://localhost:11434
```

### All Supported Environment Variables

| Variable | Provider |
|----------|----------|
| `ONELLM_OPENAI_API_KEY` | OpenAI |
| `ONELLM_ANTHROPIC_API_KEY` | Anthropic |
| `ONELLM_GOOGLE_API_KEY` | Google Gemini |
| `ONELLM_AZURE_API_KEY` | Azure OpenAI |
| `ONELLM_GROQ_API_KEY` | Groq |
| `ONELLM_CEREBRAS_API_KEY` | Cerebras |
| `ONELLM_OPENROUTER_API_KEY` | OpenRouter |
| `ONELLM_XAI_API_KEY` | xAI (Grok) |
| `ONELLM_COPILOT_API_KEY` | GitHub Copilot |

---

## 📦 Installation

### Run as Spring Boot Application

```bash
git clone https://github.com/DevSujal/onellmWeb.git
cd onellmWeb

# Set your API keys
export ONELLM_OPENAI_API_KEY=sk-...

# Run
mvn spring-boot:run
```

### Build Executable JAR

```bash
mvn clean package -DskipTests
java -jar target/onellm-1.0.0.jar
```

### Use as Library (Maven)

```xml
<dependency>
    <groupId>io.onellm</groupId>
    <artifactId>onellm</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 🔌 Supported Providers

| Provider | Status | Models | Auto-Route Prefixes |
|----------|--------|--------|---------------------|
| **OpenAI** | ✅ | GPT-4, GPT-4o, GPT-3.5, o1, o3 | `gpt-4`, `gpt-3.5`, `o1`, `o3` |
| **Anthropic** | ✅ | Claude 3/4 Opus, Sonnet, Haiku | `claude-3`, `claude-4` |
| **Google** | ✅ | Gemini Pro, Ultra, Flash | `gemini` |
| **Azure OpenAI** | ✅ | Azure-hosted GPT models | `azure/` |
| **Groq** | ✅ | LLaMA, Mixtral (fast inference) | `llama`, `mixtral`, `groq/` |
| **Cerebras** | ✅ | LLaMA (ultra-fast inference) | `cerebras/` |
| **Ollama** | ✅ | Any local model | `ollama/`, `local/` |
| **OpenRouter** | ✅ | 100+ models from all providers | Any `org/model` format |
| **xAI** | ✅ | Grok-1, Grok-2 | `grok`, `xai/` |
| **Copilot** | ✅ | GitHub Copilot models | `copilot`, `github/` |

---

## 📚 SDK Quick Start

```java
import io.onellm.OneLLM;
import io.onellm.core.LLMRequest;
import io.onellm.core.LLMResponse;

public class QuickStart {
    public static void main(String[] args) {
        // 1. Build OneLLM with your API keys
        OneLLM llm = OneLLM.builder()
            .openai(System.getenv("OPENAI_API_KEY"))
            .anthropic(System.getenv("ANTHROPIC_API_KEY"))
            .google(System.getenv("GOOGLE_API_KEY"))
            .build();
        
        // 2. Make a request - model name auto-routes to correct provider!
        LLMResponse response = llm.complete(
            LLMRequest.builder()
                .model("gpt-4")           // → Routes to OpenAI
                .user("What is Java?")
                .build()
        );
        
        // 3. Use the response
        System.out.println(response.getContent());
        System.out.println("Tokens: " + response.getUsage().getTotalTokens());
        System.out.println("Latency: " + response.getLatencyMs() + "ms");
    }
}
```

### Streaming with SDK

```java
llm.streamComplete(
    LLMRequest.builder()
        .model("gpt-4")
        .user("Write a poem")
        .stream(true)
        .build(),
    new StreamHandler() {
        @Override
        public void onChunk(String chunk) {
            System.out.print(chunk);
        }
        
        @Override
        public void onComplete(LLMResponse response) {
            System.out.println("\nDone! Latency: " + response.getLatencyMs() + "ms");
        }
        
        @Override
        public void onError(Throwable error) {
            System.err.println("Error: " + error.getMessage());
        }
    }
);
```

### Async with SDK

```java
CompletableFuture<LLMResponse> future = llm.completeAsync(request);

future.thenAccept(response -> {
    System.out.println("Got: " + response.getContent());
}).exceptionally(error -> {
    System.err.println("Error: " + error.getMessage());
    return null;
});
```

---

## 🛠️ Building LLMRequest

```java
LLMRequest request = LLMRequest.builder()
    .model("gpt-4")
    .system("You are a helpful assistant.")
    .user("Explain quantum computing")
    .temperature(0.7)        // 0.0 - 2.0
    .maxTokens(1000)
    .topP(0.95)
    .frequencyPenalty(0.5)
    .presencePenalty(0.5)
    .stop("THE END")
    .build();
```

---

## ⚠️ Error Handling

### REST API Errors

```json
{
  "error": true,
  "message": "Model 'unknown-model' not found",
  "timestamp": "2024-12-12T19:00:00Z",
  "type": "model_not_found"
}
```

| HTTP Status | Error Type |
|-------------|------------|
| 400 | Validation error, bad request |
| 401 | Authentication error |
| 404 | Model not found |
| 429 | Rate limit exceeded |
| 502 | Provider server error |
| 503 | Provider not configured |

### SDK Exception Handling

```java
try {
    LLMResponse response = llm.complete(request);
} catch (ModelNotFoundException e) {
    System.err.println("Unknown model: " + e.getModel());
} catch (LLMException e) {
    if (e.isRateLimitError()) {
        // Wait and retry
    } else if (e.isAuthenticationError()) {
        // Check API key
    }
}
```

---

## 📊 Model Comparison

| Model | Provider | Speed | Quality | Context | Best For |
|-------|----------|-------|---------|---------|----------|
| `gpt-4` | OpenAI | ⭐⭐ | ⭐⭐⭐⭐⭐ | 8K | Complex reasoning |
| `gpt-4o` | OpenAI | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 128K | Multimodal |
| `claude-3-opus` | Anthropic | ⭐⭐ | ⭐⭐⭐⭐⭐ | 200K | Analysis, writing |
| `claude-3.5-sonnet` | Anthropic | ⭐⭐⭐ | ⭐⭐⭐⭐ | 200K | Coding |
| `gemini-1.5-pro` | Google | ⭐⭐⭐ | ⭐⭐⭐⭐ | 1M | Very long context |
| `llama-3.1-70b` | Groq | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 128K | Fast inference |
| `grok-2` | xAI | ⭐⭐⭐ | ⭐⭐⭐⭐ | 32K | Real-time info |

---

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

<p align="center">
  Made with ❤️ for the Java community
  <br/>
  <strong>One SDK to rule them all.</strong>
</p>
