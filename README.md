<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="MIT License"/>
  <img src="https://img.shields.io/badge/Providers-10-blue?style=for-the-badge" alt="10 Providers"/>
</p>

<h1 align="center">🚀 OneLLM</h1>

<p align="center">
  <strong>One SDK. Any LLM. Zero Hassle.</strong>
  <br/>
  <em>A unified Java SDK to call 10+ LLM providers with a single, elegant API</em>
</p>

---

## ✨ Why OneLLM?

Tired of managing different SDKs for each LLM provider? OneLLM solves this by providing:

- 🔌 **Single Interface**: One API for OpenAI, Anthropic, Google, and 7 more providers
- 🎯 **Auto-Routing**: Just specify the model name — we find the right provider
- ⚡ **Streaming**: Real-time token streaming for all providers  
- 🔄 **Async Support**: Non-blocking calls with `CompletableFuture`
- 🛡️ **Resilient**: Built-in retry logic with exponential backoff
- 🪶 **Lightweight**: Only 3 dependencies (Gson, HttpClient5, SLF4J)

---

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>io.onellm</groupId>
    <artifactId>onellm</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.onellm:onellm:1.0.0'
```

### Build from Source

```bash
git clone https://github.com/onellm/onellm-java.git
cd onellm-java
mvn clean install
```

---

## 🚀 Quick Start

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

## 📚 Complete API Reference

### Building OneLLM

```java
// Full configuration with all providers
OneLLM llm = OneLLM.builder()
    // Cloud Providers
    .openai("sk-...")                                    // OpenAI
    .anthropic("sk-ant-...")                             // Anthropic
    .google("AIza...")                                   // Google Gemini
    .azure("key", "my-resource", "gpt4-deployment")      // Azure OpenAI
    .groq("gsk_...")                                     // Groq
    .cerebras("csk-...")                                 // Cerebras
    .xai("xai-...")                                      // xAI (Grok)
    .copilot("ghp_...")                                  // GitHub Copilot
    .openRouter("sk-or-...")                             // OpenRouter
    
    // Local Providers
    .ollama()                                            // Ollama (localhost:11434)
    .ollama("http://192.168.1.100:11434")                // Ollama (custom URL)
    
    // Custom Provider
    .provider(new MyCustomProvider())                    // Your own implementation
    
    .build();
```

---

### LLMRequest - Building Requests

#### Basic Request

```java
LLMRequest request = LLMRequest.builder()
    .model("gpt-4")
    .user("Hello, world!")
    .build();
```

#### With System Prompt

```java
LLMRequest request = LLMRequest.builder()
    .model("claude-3-opus")
    .system("You are a helpful coding assistant. Always provide code examples.")
    .user("How do I read a file in Java?")
    .build();
```

#### Multi-Turn Conversation

```java
LLMRequest request = LLMRequest.builder()
    .model("gpt-4")
    .system("You are a math tutor.")
    .user("What is 2 + 2?")
    .assistant("2 + 2 equals 4.")
    .user("And what is that multiplied by 3?")
    .build();
```

#### With Generation Parameters

```java
LLMRequest request = LLMRequest.builder()
    .model("gemini-pro")
    .user("Write a creative story about a robot.")
    .temperature(0.9)           // Higher = more creative (0.0 - 2.0)
    .maxTokens(1000)            // Maximum response length
    .topP(0.95)                 // Nucleus sampling (0.0 - 1.0)
    .frequencyPenalty(0.5)      // Reduce repetition (-2.0 - 2.0)
    .presencePenalty(0.5)       // Encourage new topics (-2.0 - 2.0)
    .stop("THE END", "---")     // Stop sequences
    .build();
```

#### Using Message Objects

```java
import io.onellm.core.Message;

List<Message> conversation = List.of(
    Message.system("You are a translator. Translate to French."),
    Message.user("Hello, how are you?"),
    Message.assistant("Bonjour, comment allez-vous?"),
    Message.user("What is your name?")
);

LLMRequest request = LLMRequest.builder()
    .model("gpt-4")
    .messages(conversation)
    .build();
```

---

### LLMResponse - Understanding Responses

```java
LLMResponse response = llm.complete(request);

// Core content
String text = response.getContent();           // The generated text
String model = response.getModel();            // Model that was used
String provider = response.getProvider();      // Provider name (e.g., "openai")
String id = response.getId();                  // Request ID
String finishReason = response.getFinishReason(); // "stop", "length", etc.

// Token usage
Usage usage = response.getUsage();
int promptTokens = usage.getPromptTokens();       // Input tokens
int completionTokens = usage.getCompletionTokens(); // Output tokens  
int totalTokens = usage.getTotalTokens();         // Total tokens

// Performance
long latencyMs = response.getLatencyMs();      // Request duration in ms
```

---

### Completion Methods

#### Synchronous Completion

```java
// Blocks until response is ready
LLMResponse response = llm.complete(request);
System.out.println(response.getContent());
```

#### Asynchronous Completion

```java
// Non-blocking with CompletableFuture
CompletableFuture<LLMResponse> future = llm.completeAsync(request);

// Option 1: Callback
future.thenAccept(response -> {
    System.out.println("Got response: " + response.getContent());
}).exceptionally(error -> {
    System.err.println("Error: " + error.getMessage());
    return null;
});

// Option 2: Wait with timeout
LLMResponse response = future.get(30, TimeUnit.SECONDS);

// Option 3: Combine multiple requests
CompletableFuture<LLMResponse> gpt4 = llm.completeAsync(gptRequest);
CompletableFuture<LLMResponse> claude = llm.completeAsync(claudeRequest);

CompletableFuture.allOf(gpt4, claude).join();
System.out.println("GPT-4: " + gpt4.get().getContent());
System.out.println("Claude: " + claude.get().getContent());
```

#### Streaming Completion

```java
import io.onellm.core.StreamHandler;

llm.streamComplete(
    LLMRequest.builder()
        .model("gpt-4")
        .user("Write a poem about Java programming")
        .stream(true)
        .build(),
    new StreamHandler() {
        @Override
        public void onChunk(String chunk) {
            // Called for each token as it arrives
            System.out.print(chunk);
        }
        
        @Override
        public void onComplete(LLMResponse response) {
            // Called when stream ends
            System.out.println("\n\nDone! Latency: " + response.getLatencyMs() + "ms");
        }
        
        @Override
        public void onError(Throwable error) {
            // Called if an error occurs
            System.err.println("Stream error: " + error.getMessage());
        }
    }
);
```

---

## 🔧 Provider-Specific Examples

### OpenAI

```java
OneLLM llm = OneLLM.builder()
    .openai(System.getenv("OPENAI_API_KEY"))
    .build();

// GPT-4
LLMResponse gpt4 = llm.complete(LLMRequest.builder()
    .model("gpt-4")
    .user("Explain quantum computing")
    .build());

// GPT-4 Turbo
LLMResponse gpt4Turbo = llm.complete(LLMRequest.builder()
    .model("gpt-4-turbo")
    .user("Summarize this article...")
    .build());

// GPT-4o (Omni)
LLMResponse gpt4o = llm.complete(LLMRequest.builder()
    .model("gpt-4o")
    .user("What's in this image?")
    .build());

// o1 Reasoning Model
LLMResponse o1 = llm.complete(LLMRequest.builder()
    .model("o1-preview")
    .user("Solve this complex math problem...")
    .build());
```

### Anthropic (Claude)

```java
OneLLM llm = OneLLM.builder()
    .anthropic(System.getenv("ANTHROPIC_API_KEY"))
    .build();

// Claude 3 Opus (most capable)
LLMResponse opus = llm.complete(LLMRequest.builder()
    .model("claude-3-opus-20240229")
    .system("You are an expert programmer.")
    .user("Review this code for bugs...")
    .maxTokens(4096)
    .build());

// Claude 3.5 Sonnet (balanced)
LLMResponse sonnet = llm.complete(LLMRequest.builder()
    .model("claude-3-5-sonnet-20241022")
    .user("Write a Python script to...")
    .build());

// Claude 3 Haiku (fastest)
LLMResponse haiku = llm.complete(LLMRequest.builder()
    .model("claude-3-haiku-20240307")
    .user("Quick question: what is 2+2?")
    .build());
```

### Google (Gemini)

```java
OneLLM llm = OneLLM.builder()
    .google(System.getenv("GOOGLE_API_KEY"))
    .build();

// Gemini Pro
LLMResponse geminiPro = llm.complete(LLMRequest.builder()
    .model("gemini-pro")
    .user("Explain machine learning")
    .build());

// Gemini 1.5 Pro (long context)
LLMResponse gemini15 = llm.complete(LLMRequest.builder()
    .model("gemini-1.5-pro")
    .user("Analyze this long document...")
    .build());

// Gemini 2.0 Flash (fast)
LLMResponse flash = llm.complete(LLMRequest.builder()
    .model("gemini-2.0-flash-exp")
    .user("Quick summary please")
    .build());
```

### Azure OpenAI

```java
OneLLM llm = OneLLM.builder()
    .azure(
        System.getenv("AZURE_OPENAI_KEY"),
        "my-openai-resource",    // Your Azure resource name
        "gpt4-deployment"        // Your deployment name
    )
    .build();

// Use with azure/ prefix or deployment name
LLMResponse response = llm.complete(LLMRequest.builder()
    .model("azure/gpt-4")
    .user("Hello from Azure!")
    .build());
```

### Groq (Fast Inference)

```java
OneLLM llm = OneLLM.builder()
    .groq(System.getenv("GROQ_API_KEY"))
    .build();

// LLaMA 3.1 70B - blazing fast!
LLMResponse llama = llm.complete(LLMRequest.builder()
    .model("llama-3.1-70b-versatile")
    .user("Write a function to sort an array")
    .build());

// Mixtral 8x7B
LLMResponse mixtral = llm.complete(LLMRequest.builder()
    .model("mixtral-8x7b-32768")
    .user("Explain transformers in ML")
    .build());

// Gemma 7B
LLMResponse gemma = llm.complete(LLMRequest.builder()
    .model("gemma-7b-it")
    .user("Hello!")
    .build());

System.out.println("Groq latency: " + llama.getLatencyMs() + "ms"); // Usually <500ms!
```

### Cerebras (Ultra-Fast Inference)

```java
OneLLM llm = OneLLM.builder()
    .cerebras(System.getenv("CEREBRAS_API_KEY"))
    .build();

// LLaMA 3.1 70B on Cerebras hardware
LLMResponse response = llm.complete(LLMRequest.builder()
    .model("llama3.1-70b")
    .user("Generate code to parse JSON")
    .build());

// With prefix
LLMResponse response2 = llm.complete(LLMRequest.builder()
    .model("cerebras/llama3.1-8b")
    .user("Quick question...")
    .build());
```

### Ollama (Local Models)

```java
// Default: localhost:11434
OneLLM llm = OneLLM.builder()
    .ollama()
    .build();

// Or custom URL
OneLLM llmRemote = OneLLM.builder()
    .ollama("http://192.168.1.100:11434")
    .build();

// Use any local model
LLMResponse llama = llm.complete(LLMRequest.builder()
    .model("ollama/llama2")
    .user("Hello!")
    .build());

LLMResponse mistral = llm.complete(LLMRequest.builder()
    .model("local/mistral")
    .user("Write a haiku")
    .build());

LLMResponse codellama = llm.complete(LLMRequest.builder()
    .model("ollama/codellama")
    .user("Write a Python function")
    .build());
```

### OpenRouter (100+ Models)

```java
OneLLM llm = OneLLM.builder()
    .openRouter(System.getenv("OPENROUTER_API_KEY"))
    .build();

// Access ANY model through OpenRouter
LLMResponse gpt4 = llm.complete(LLMRequest.builder()
    .model("openai/gpt-4-turbo")
    .user("Hello!")
    .build());

LLMResponse claude = llm.complete(LLMRequest.builder()
    .model("anthropic/claude-3-opus")
    .user("Hello!")
    .build());

LLMResponse llama = llm.complete(LLMRequest.builder()
    .model("meta-llama/llama-3.1-405b-instruct")
    .user("Hello!")
    .build());

LLMResponse deepseek = llm.complete(LLMRequest.builder()
    .model("deepseek/deepseek-chat")
    .user("Hello!")
    .build());
```

### xAI (Grok)

```java
OneLLM llm = OneLLM.builder()
    .xai(System.getenv("XAI_API_KEY"))
    .build();

// Grok-2
LLMResponse grok2 = llm.complete(LLMRequest.builder()
    .model("grok-2")
    .user("What's happening in the world today?")
    .build());

// Grok Beta
LLMResponse grokBeta = llm.complete(LLMRequest.builder()
    .model("grok-beta")
    .user("Tell me a joke")
    .build());
```

### GitHub Copilot

```java
OneLLM llm = OneLLM.builder()
    .copilot(System.getenv("GITHUB_TOKEN"))
    .build();

LLMResponse response = llm.complete(LLMRequest.builder()
    .model("copilot/gpt-4")
    .system("You are a code assistant.")
    .user("Write a React component for a login form")
    .build());
```

---

## 🔄 Multi-Provider Patterns

### Fallback Chain

```java
public LLMResponse completeWithFallback(LLMRequest request) {
    String[] models = {"gpt-4", "claude-3-opus", "gemini-pro"};
    
    for (String model : models) {
        try {
            LLMRequest fallbackRequest = LLMRequest.builder()
                .model(model)
                .messages(request.getMessages())
                .build();
            return llm.complete(fallbackRequest);
        } catch (LLMException e) {
            System.out.println("Failed with " + model + ", trying next...");
        }
    }
    throw new RuntimeException("All providers failed");
}
```

### Parallel Comparison

```java
public void compareProviders(String prompt) {
    List<String> models = List.of(
        "gpt-4", "claude-3-opus", "gemini-pro", "llama-3.1-70b"
    );
    
    List<CompletableFuture<LLMResponse>> futures = models.stream()
        .map(model -> llm.completeAsync(
            LLMRequest.builder()
                .model(model)
                .user(prompt)
                .build()
        ))
        .collect(Collectors.toList());
    
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    for (int i = 0; i < models.size(); i++) {
        LLMResponse response = futures.get(i).join();
        System.out.printf("%s (%dms): %s%n",
            models.get(i),
            response.getLatencyMs(),
            response.getContent().substring(0, 100) + "..."
        );
    }
}
```

### Cost-Optimized Routing

```java
public LLMResponse smartRoute(LLMRequest request, boolean needsHighQuality) {
    String model;
    
    if (needsHighQuality) {
        model = "gpt-4";  // Best quality
    } else if (request.getMessages().size() > 10) {
        model = "gemini-1.5-pro";  // Long context
    } else {
        model = "llama-3.1-70b";  // Fast and cheap via Groq
    }
    
    return llm.complete(
        LLMRequest.builder()
            .model(model)
            .messages(request.getMessages())
            .build()
    );
}
```

---

## ⚠️ Error Handling

```java
import io.onellm.exception.*;

try {
    LLMResponse response = llm.complete(request);
    System.out.println(response.getContent());
    
} catch (ModelNotFoundException e) {
    // Unknown model name
    System.err.println("Unknown model: " + e.getModel());
    System.err.println("Configured providers: " + llm.listProviders());
    
} catch (ProviderNotConfiguredException e) {
    // Provider API key not set
    System.err.println("Please configure: " + e.getProviderName());
    
} catch (LLMException e) {
    // API errors
    if (e.isRateLimitError()) {
        System.err.println("Rate limited! Wait and retry...");
        Thread.sleep(60000);  // Wait 1 minute
        
    } else if (e.isAuthenticationError()) {
        System.err.println("Invalid API key for: " + e.getProvider());
        
    } else if (e.isServerError()) {
        System.err.println("Provider " + e.getProvider() + " is having issues");
        
    } else {
        System.err.println("API Error [" + e.getStatusCode() + "]: " + e.getMessage());
    }
}
```

---

## 🛠️ Advanced Configuration

### Custom HTTP Settings

```java
// Create a custom HTTP client wrapper (if needed in future)
// Currently uses sensible defaults:
// - 30s connect timeout
// - 60s read timeout  
// - 3 retries with exponential backoff
// - Connection pooling (100 max, 20 per route)
```

### Environment Variables Pattern

```java
public class LLMConfig {
    public static OneLLM create() {
        OneLLM.Builder builder = OneLLM.builder();
        
        // Add providers based on available env vars
        String openaiKey = System.getenv("OPENAI_API_KEY");
        if (openaiKey != null) builder.openai(openaiKey);
        
        String anthropicKey = System.getenv("ANTHROPIC_API_KEY");
        if (anthropicKey != null) builder.anthropic(anthropicKey);
        
        String googleKey = System.getenv("GOOGLE_API_KEY");
        if (googleKey != null) builder.google(googleKey);
        
        String groqKey = System.getenv("GROQ_API_KEY");
        if (groqKey != null) builder.groq(groqKey);
        
        String cerebrasKey = System.getenv("CEREBRAS_API_KEY");
        if (cerebrasKey != null) builder.cerebras(cerebrasKey);
        
        // Always available
        builder.ollama();
        
        return builder.build();
    }
}
```

---

## 📊 Model Comparison Table

| Model | Provider | Speed | Quality | Context | Best For |
|-------|----------|-------|---------|---------|----------|
| `gpt-4` | OpenAI | ⭐⭐ | ⭐⭐⭐⭐⭐ | 8K | Complex reasoning |
| `gpt-4-turbo` | OpenAI | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 128K | Long documents |
| `gpt-4o` | OpenAI | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 128K | Multimodal |
| `claude-3-opus` | Anthropic | ⭐⭐ | ⭐⭐⭐⭐⭐ | 200K | Analysis, writing |
| `claude-3.5-sonnet` | Anthropic | ⭐⭐⭐ | ⭐⭐⭐⭐ | 200K | Coding |
| `gemini-pro` | Google | ⭐⭐⭐ | ⭐⭐⭐⭐ | 32K | General purpose |
| `gemini-1.5-pro` | Google | ⭐⭐⭐ | ⭐⭐⭐⭐ | 1M | Very long context |
| `llama-3.1-70b` | Groq | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 128K | Fast inference |
| `llama3.1-70b` | Cerebras | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8K | Ultra-fast |
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
