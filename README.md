<p align="center">
  <img src="https://raw.githubusercontent.com/onellm/onellm-java/main/assets/logo.png" alt="OneLLM Logo" width="180"/>
</p>

<h1 align="center">🚀 OneLLM</h1>

<p align="center">
  <strong>One Interface. Twelve Providers. Your API Keys.</strong>
</p>

<p align="center">
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-providers">Providers</a> •
  <a href="#-rest-api">REST API</a> •
  <a href="#-sdk-usage">SDK Usage</a> •
  <a href="#-configuration">Configuration</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-blue?style=for-the-badge&logo=openjdk" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot 3.2.0"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="MIT License"/>
  <img src="https://img.shields.io/badge/BYOK-Bring%20Your%20Own%20Key-orange?style=for-the-badge" alt="BYOK"/>
</p>

---

**OneLLM** is a unified Java SDK and REST API that provides a single interface for calling **10 different LLM providers**. Users bring their own API keys — just specify the model name and your API key, and OneLLM automatically routes your request to the right provider.

> 🔑 **Bring Your Own Key (BYOK)**: OneLLM doesn't store or require server-side API keys. Users provide their own API keys in each request, making it perfect for multi-tenant applications.

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔌 **12 Providers** | OpenAI, Anthropic, Google Gemini, Azure OpenAI, Groq, Cerebras, Ollama, OpenRouter, xAI, GitHub Copilot, Hugging Face, and **FreeLLM** (Free!) |
| 🔑 **BYOK Model** | Users provide their own API keys — no server-side credential storage |
| 🎯 **Auto-Routing** | Automatically routes requests based on model name |
| 🌊 **Streaming** | Full support for streaming responses via SSE |
| ⚡ **Async** | Non-blocking async completions with `CompletableFuture` |
| 🛡️ **Type-Safe** | Builder pattern with validation for all request parameters |
| 📊 **Usage Tracking** | Token usage and latency metrics in every response |

---

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Run the Server

```bash
# Clone and build
git clone https://github.com/onellm/onellm-java.git
cd onellm

# Run (no API keys needed - users provide their own!)
mvn spring-boot:run
```

The server starts at `http://localhost:8080`

### Make Your First Request

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "YOUR_OPENAI_API_KEY",
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

> **Note**: Replace `YOUR_OPENAI_API_KEY` with your actual API key from the respective provider.

---

## 🔌 Providers

OneLLM supports **10 LLM providers** out of the box:

| Provider | Models | Required Fields |
|----------|--------|-----------------|
| **OpenAI** | `gpt-4`, `gpt-4-turbo`, `gpt-4o`, `gpt-3.5-turbo`, `o1`, `o3`, `chatgpt-*` | `apiKey` |
| **Anthropic** | `claude-3-opus`, `claude-3-sonnet`, `claude-3-haiku`, `claude-3.5-sonnet`, `claude-4-*` | `apiKey` |
| **Google Gemini** | `gemini-pro`, `gemini-ultra`, `gemini-1.5-pro`, `gemini-2.0-flash` | `apiKey` |
| **Azure OpenAI** | Your deployed models | `apiKey`, `azureResourceName`, `azureDeploymentName` |
| **Groq** | `llama-3`, `mixtral`, `gemma` | `apiKey` |
| **Cerebras** | `cerebras-gpt` variants | `apiKey` |
| **Ollama** | Any local model | `baseUrl` (optional, defaults to localhost) |
| **OpenRouter** | 100+ models | `apiKey`, optionally `openRouterSiteName`, `openRouterSiteUrl` |
| **xAI** | `grok-*` models | `apiKey` |
| **GitHub Copilot** | Copilot models | `apiKey` |
| **Hugging Face** | `meta-llama/*`, `mistralai/*`, `Qwen/*`, any HF model | `apiKey` (hf_token) |
| **FreeLLM** 🆓 | `TinyLlama/*`, `Qwen/*`, `microsoft/phi-2` | None (free!) |

### Model Auto-Detection

OneLLM automatically routes to the correct provider based on model name:

```
"gpt-4"           → OpenAI
"claude-3-opus"   → Anthropic
"gemini-1.5-pro"  → Google
"llama-3-70b"     → Groq
"grok-1"          → xAI
```

You can also use explicit provider prefixes:

```
"openai/gpt-4"
"anthropic/claude-3-opus"
"google/gemini-pro"
"azure/my-deployment"
"huggingface/meta-llama/Llama-3.3-70B-Instruct"
"hf/mistralai/Mistral-7B-Instruct-v0.3"
"freellm/TinyLlama/TinyLlama-1.1B-Chat-v1.0"
"free/Qwen/Qwen2.5-0.5B-Instruct"
```

---

## 🌐 REST API

### Base URL

```
http://localhost:8080/api
```

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/chat/completions` | Synchronous chat completion |
| `POST` | `/chat/completions/stream` | Streaming chat completion (SSE) |
| `GET` | `/providers` | List supported providers |
| `GET` | `/health` | Health check |

---

### `POST /api/chat/completions`

Send a chat completion request with your own API key.

**Request Body:**

```json
{
  "apiKey": "sk-your-api-key-here",
  "model": "gpt-4",
  "messages": [
    { "role": "system", "content": "You are a helpful assistant." },
    { "role": "user", "content": "Hello, who are you?" }
  ],
  "temperature": 0.7,
  "maxTokens": 1000,
  "topP": 0.9,
  "frequencyPenalty": 0.0,
  "presencePenalty": 0.0,
  "stop": ["END"],
  "stream": false,
  
  "baseUrl": "https://custom-endpoint.com/v1",
  "azureResourceName": "my-resource",
  "azureDeploymentName": "gpt-4",
  "openRouterSiteName": "MyApp",
  "openRouterSiteUrl": "https://myapp.com"
}
```

**Request Attributes:**

| Field | Type | Required | Description | Valid Range |
|-------|------|----------|-------------|-------------|
| `apiKey` | `string` | ✅ **Yes** | Your API key for the provider | - |
| `model` | `string` | ✅ **Yes** | Model identifier (routes to provider automatically) | - |
| `messages` | `array` | ✅ **Yes** | Array of message objects | Min 1 message |
| `messages[].role` | `string` | ✅ **Yes** | Role: `system`, `user`, or `assistant` | - |
| `messages[].content` | `string` | ✅ **Yes** | Message content | - |
| `temperature` | `number` | ❌ No | Sampling temperature | `0.0` - `2.0` |
| `maxTokens` | `integer` | ❌ No | Maximum tokens to generate | `≥ 1` |
| `topP` | `number` | ❌ No | Nucleus sampling probability | `0.0` - `1.0` |
| `frequencyPenalty` | `number` | ❌ No | Frequency penalty | `-2.0` - `2.0` |
| `presencePenalty` | `number` | ❌ No | Presence penalty | `-2.0` - `2.0` |
| `stop` | `array` | ❌ No | Stop sequences | - |
| `stream` | `boolean` | ❌ No | Enable streaming | - |

**Provider-Specific Fields:**

| Field | Type | Required For | Description |
|-------|------|--------------|-------------|
| `baseUrl` | `string` | Optional | Custom base URL (OpenAI-compatible endpoints) |
| `azureResourceName` | `string` | Azure | Your Azure resource name |
| `azureDeploymentName` | `string` | Azure | Your Azure deployment name |
| `openRouterSiteName` | `string` | Optional | Your app name (for OpenRouter) |
| `openRouterSiteUrl` | `string` | Optional | Your app URL (for OpenRouter) |

**Response:**

```json
{
  "id": "chatcmpl-abc123",
  "model": "gpt-4-0613",
  "content": "Hello! I'm an AI assistant powered by GPT-4...",
  "finishReason": "stop",
  "provider": "openai",
  "latencyMs": 1234,
  "usage": {
    "promptTokens": 25,
    "completionTokens": 45,
    "totalTokens": 70
  }
}
```

---

### `POST /api/chat/completions/stream`

Stream responses using Server-Sent Events (SSE).

**Request:** Same as `/chat/completions`

**Response:** SSE stream with events:

```
event: chunk
data: {"content": "Hello"}

event: chunk
data: {"content": ", I'm"}

event: complete
data: {"id": "...", "model": "gpt-4", "content": "Hello, I'm...", ...}
```

---

### `GET /api/providers`

List all supported providers.

**Response:**

```json
{
  "providers": ["openai", "anthropic", "google", "azure", "groq", "cerebras", "ollama", "openrouter", "xai", "copilot", "huggingface", "freellm"],
  "count": 12
}
```

---

### `GET /api/health`

Health check endpoint.

**Response:**

```json
{
  "status": "ok",
  "service": "OneLLM"
}
```

---

## 📝 API Examples

### OpenAI (GPT-4)

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "sk-your-openai-key",
    "model": "gpt-4",
    "messages": [
      {"role": "system", "content": "You are a helpful coding assistant."},
      {"role": "user", "content": "Write a Python function to reverse a string."}
    ],
    "temperature": 0.5,
    "maxTokens": 500
  }'
```

### Anthropic (Claude)

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "sk-ant-your-anthropic-key",
    "model": "claude-3-opus",
    "messages": [
      {"role": "user", "content": "Explain quantum computing in simple terms."}
    ],
    "maxTokens": 1000
  }'
```

### Google Gemini

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "AIza-your-google-key",
    "model": "gemini-1.5-pro",
    "messages": [
      {"role": "user", "content": "What is the meaning of life?"}
    ]
  }'
```

### Azure OpenAI

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "your-azure-api-key",
    "model": "azure/my-gpt4-deployment",
    "azureResourceName": "my-azure-resource",
    "azureDeploymentName": "my-gpt4-deployment",
    "messages": [
      {"role": "user", "content": "Hello from Azure!"}
    ]
  }'
```

### Groq (Fast Inference)

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "gsk_your-groq-key",
    "model": "llama-3-70b",
    "messages": [
      {"role": "user", "content": "Write a haiku about coding."}
    ]
  }'
```

### OpenRouter (100+ Models)

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "sk-or-your-openrouter-key",
    "model": "openrouter/anthropic/claude-3-opus",
    "openRouterSiteName": "MyApp",
    "openRouterSiteUrl": "https://myapp.com",
    "messages": [
      {"role": "user", "content": "Hello via OpenRouter!"}
    ]
  }'
```

### xAI (Grok)

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "xai-your-xai-key",
    "model": "grok-1",
    "messages": [
      {"role": "user", "content": "Tell me a joke."}
    ]
  }'
```

### Ollama (Local Models)

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "not-required",
    "model": "ollama/llama2",
    "baseUrl": "http://localhost:11434",
    "messages": [
      {"role": "user", "content": "Hello from local Ollama!"}
    ]
  }'
```

### Hugging Face

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "hf_your-huggingface-token",
    "model": "huggingface/meta-llama/Llama-3.3-70B-Instruct",
    "messages": [
      {"role": "user", "content": "Hello from Hugging Face!"}
    ],
    "maxTokens": 500
  }'
```

### Streaming Example

```bash
curl -X POST http://localhost:8080/api/chat/completions/stream \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "sk-your-openai-key",
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "Count from 1 to 10 slowly."}]
  }'
```

### FreeLLM (Free - No API Key!)

```bash
curl -X POST http://localhost:8080/api/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "freellm/TinyLlama/TinyLlama-1.1B-Chat-v1.0",
    "messages": [
      {"role": "user", "content": "Hello! What can you help me with?"}
    ],
    "maxTokens": 256
  }'
```

> **Note**: FreeLLM is completely free - no API key required! It's hosted on Hugging Face Spaces with no rate limiting or billing. Perfect for testing and development.

**Recommended FreeLLM Models:**
| Model | Size | Speed | Quality |
|-------|------|-------|---------|
| `TinyLlama/TinyLlama-1.1B-Chat-v1.0` | 1.1B | ⚡⚡⚡ | ⭐⭐ |
| `Qwen/Qwen2.5-0.5B-Instruct` | 0.5B | ⚡⚡⚡ | ⭐⭐ |
| `Qwen/Qwen2.5-1.5B-Instruct` | 1.5B | ⚡⚡ | ⭐⭐⭐ |
| `microsoft/phi-2` | 2.7B | ⚡⚡ | ⭐⭐⭐⭐ |

### Streaming Example

```bash
curl -X POST http://localhost:8080/api/chat/completions/stream \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "sk-your-openai-key",
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "Count from 1 to 10 slowly."}]
  }'
```

---

## 💻 JavaScript/TypeScript Client

```typescript
// Basic request
const response = await fetch('http://localhost:8080/api/chat/completions', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    apiKey: 'sk-your-api-key',
    model: 'gpt-4',
    messages: [
      { role: 'user', content: 'Hello!' }
    ]
  })
});

const data = await response.json();
console.log(data.content);

// Streaming request
const eventSource = new EventSource('http://localhost:8080/api/chat/completions/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    apiKey: 'sk-your-api-key',
    model: 'gpt-4',
    messages: [{ role: 'user', content: 'Write a story' }]
  })
});

eventSource.addEventListener('chunk', (e) => {
  const data = JSON.parse(e.data);
  process.stdout.write(data.content);
});

eventSource.addEventListener('complete', (e) => {
  console.log('\nDone!');
  eventSource.close();
});
```

---

## 📦 SDK Usage

Use OneLLM programmatically in your Java application:

### Basic Usage

```java
import io.onellm.OneLLM;
import io.onellm.core.*;

// Build the client with your API keys
OneLLM llm = OneLLM.builder()
    .openai("sk-your-openai-key")
    .anthropic("sk-ant-your-anthropic-key")
    .google("AIza-your-google-key")
    .build();

// Send a completion request
LLMResponse response = llm.complete(
    LLMRequest.builder()
        .model("gpt-4")
        .system("You are a helpful assistant.")
        .user("Explain quantum computing in simple terms.")
        .temperature(0.7)
        .maxTokens(500)
        .build()
);

System.out.println(response.getContent());
System.out.println("Provider: " + response.getProvider());
System.out.println("Latency: " + response.getLatencyMs() + "ms");
```

### Streaming

```java
llm.streamComplete(
    LLMRequest.builder()
        .model("claude-3-opus")
        .user("Write a story about a robot learning to cook.")
        .build(),
    new StreamHandler() {
        @Override
        public void onChunk(String chunk) {
            System.out.print(chunk);
        }
        
        @Override
        public void onComplete(LLMResponse response) {
            System.out.println("\n\nDone! Tokens: " + response.getUsage().getTotalTokens());
        }
        
        @Override
        public void onError(Throwable error) {
            System.err.println("Error: " + error.getMessage());
        }
    }
);
```

### Builder Methods

```java
OneLLM llm = OneLLM.builder()
    .openai("sk-...")                              // OpenAI
    .openai("sk-...", "https://custom-url.com")    // Custom base URL
    .anthropic("sk-ant-...")                       // Anthropic
    .google("AIza...")                             // Google Gemini
    .azure("api-key", "resource", "deployment")    // Azure OpenAI
    .groq("gsk_...")                               // Groq
    .cerebras("cbs-...")                           // Cerebras
    .ollama()                                      // Ollama (localhost)
    .ollama("http://custom-host:11434")            // Ollama (custom)
    .openRouter("or-...")                          // OpenRouter
    .openRouter("or-...", "MySite", "https://...")  // OpenRouter with site
    .xai("xai-...")                                // xAI
    .copilot("token")                              // GitHub Copilot
    .huggingface("hf_...")                         // Hugging Face
    .huggingface("hf_...", "https://endpoint")     // Hugging Face (dedicated endpoint)
    .freellm()                                     // FreeLLM (free, no API key!)
    .freellm("https://custom-freellm")             // FreeLLM (custom host)
    .provider(myCustomProvider)                    // Custom provider
    .build();
```

---

## 🛡️ Error Handling

OneLLM provides structured error responses:

```json
{
  "error": true,
  "message": "API key is required",
  "timestamp": "2024-12-12T14:30:00Z",
  "type": "validation_error",
  "fields": {
    "apiKey": "API key is required"
  }
}
```

### Error Types

| Type | HTTP Status | Description |
|------|-------------|-------------|
| `validation_error` | 400 | Invalid request parameters (e.g., missing API key) |
| `model_not_found` | 404 | No provider supports the model |
| `provider_not_configured` | 503 | Provider not configured |
| `authentication_error` | 401 | Invalid API key |
| `rate_limit_error` | 429 | Rate limit exceeded |
| `server_error` | 502 | Provider server error |
| `internal_error` | 500 | Unexpected server error |

---

## 🏗️ Project Structure

```
onellm/
├── src/main/java/io/onellm/
│   ├── OneLLM.java              # SDK entry point
│   ├── OneLLMApplication.java   # Spring Boot application
│   ├── config/
│   │   └── LLMConfig.java       # Spring configuration
│   ├── controller/
│   │   └── ChatController.java  # REST API endpoints
│   ├── service/
│   │   └── ProviderFactory.java # Dynamic provider creation
│   ├── core/
│   │   ├── LLMProvider.java     # Provider interface
│   │   ├── LLMRequest.java      # Request model
│   │   ├── LLMResponse.java     # Response model
│   │   ├── Message.java         # Chat message
│   │   ├── StreamHandler.java   # Streaming callback
│   │   └── Usage.java           # Token usage
│   ├── dto/
│   │   ├── ChatCompletionRequest.java
│   │   ├── ChatCompletionResponse.java
│   │   └── MessageDTO.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── LLMException.java
│   │   ├── ModelNotFoundException.java
│   │   └── ProviderNotConfiguredException.java
│   ├── providers/
│   │   ├── BaseProvider.java
│   │   ├── OpenAIProvider.java
│   │   ├── AnthropicProvider.java
│   │   ├── GoogleProvider.java
│   │   ├── AzureOpenAIProvider.java
│   │   ├── GroqProvider.java
│   │   ├── CerebrasProvider.java
│   │   ├── OllamaProvider.java
│   │   ├── OpenRouterProvider.java
│   │   ├── XAIProvider.java
│   │   ├── CopilotProvider.java
│   │   ├── HuggingFaceProvider.java
│   │   └── FreeLLMProvider.java
│   └── util/
│       └── HttpClientWrapper.java
└── pom.xml
```

---

## 🔒 Security Notes

- **API keys are never stored** on the server
- Each request is processed independently with the provided credentials
- Use HTTPS in production to encrypt API keys in transit
- Consider implementing rate limiting for production deployments

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

Built with ❤️ using:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Apache HttpClient 5](https://hc.apache.org/httpcomponents-client-5.3.x/)
- [Gson](https://github.com/google/gson)

---

<p align="center">
  Made with ☕ by the OneLLM Team
</p>
