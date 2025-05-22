
# 🦙 AiSee LLaMa Android SDK

An Android SDK for:

* 🔍 Searching and retrieving `.gguf` LLaMA models from **Hugging Face**
* 🧠 Running **on-device inference** with streamed responses

Designed for private, efficient, mobile-friendly AI deployments.

---

## 📦 Installation via JitPack

1. Add JitPack to your root `build.gradle`:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. Add the library to your `build.gradle` (app module):

```gradle
dependencies {
    implementation 'com.github.aisee-ai:gguf-models-android:v1.0.5'
}
```

---

## 🔍 Hugging Face Model Search (`ai.aisee.llama.HF`)

Programmatically search Hugging Face for `.gguf` models.

### ✅ Features

* Query Hugging Face with filters (`author`, `tag`, `search`)
* Automatically fetch and parse model metadata
* Support for pagination, sorting, and tree inspection (e.g., file structure)

### 🧩 HF Package Contents

| File                          | Description                         |
| ----------------------------- | ----------------------------------- |
| `HFModelSearch.java`          | Main API for model search           |
| `HFModelInfo.java`            | Describes individual model metadata |
| `HFModelTree.java`            | Fetches model file tree             |
| `HFModels.java`               | Aggregates API methods              |
| `HFEndpoints.java`            | Central endpoint constants          |
| `ExampleModel.java`           | Sample model structure              |
| `CustomDateDeserializer.java` | Parses `createdAt` timestamps       |
| `CustomDateSerializer.java`   | Serializes timestamps for JSON      |

### 🚀 Usage Example

```java
HFModelSearch modelSearch = new HFModelSearch();

List<HFModelSearch.ModelSearchResult> results = modelSearch.searchModels(
    "llama",                        // query
    "TheBloke",                     // author
    "gguf",                         // tag
    HFModelSearch.ModelSortParam.DOWNLOADS,
    HFModelSearch.ModelSearchDirection.DESCENDING,
    10,
    false,
    false
);

for (HFModelSearch.ModelSearchResult result : results) {
    Log.d("HF", result.modelId + " - " + result.description);
}
```

---

## 🧠 On-Device LLaMA Inference (`ai.aisee.llama.LLaMa`)

Run `.gguf` models with real-time streamed output.

### ✅ Features

* Load `.gguf` model from URI
* Dynamically set system prompts
* Receive partial responses with `LiveData`
* Final callback via `LlamaListener`
* Auto-formatting of `<think>` tags

### 🧩 LLaMa Package Contents

| File                  | Description                                  |
| --------------------- | -------------------------------------------- |
| `ModelInference.java` | Main interface for model interaction         |
| `ModelLoader.java`    | Handles loading `.gguf` model files          |
| `LLaMa.java`          | Core inference logic using native libraries  |
| `GGUFReader.java`     | Utilities for reading `.gguf` model metadata |

### 🚀 Inference Usage

```java
ModelInference model = ModelInference.getInstance(context);

model.setSystemPrompt("You're a helpful assistant.");
model.setListener(response -> Log.d("LLaMa", "Final: " + response));
model.partialResponse.observe(this, partial -> Log.d("LLaMa", "Partial: " + partial));
model.generateResponse("Where did I leave my book?");
```

#### Load a Model:

```java
model.loadModel(modelUri,
    () -> Log.d("Model", "Model loaded successfully."),
    () -> Log.e("Model", "Failed to load model.")
);
```

---

## 📁 Directory Structure

```
ai.aisee.llama
├── HF
│   ├── CustomDateDeserializer.java
│   ├── CustomDateSerializer.java
│   ├── ExampleModel.java
│   ├── HFEndpoints.java
│   ├── HFModelInfo.java
│   ├── HFModelSearch.java
│   ├── HFModelTree.java
│   └── HFModels.java
├── LLaMa
│   ├── GGUFReader.java
│   ├── LLaMa.java
│   ├── ModelInference.java
│   └── ModelLoader.java
```

---

## 🧪 Pro Tips

* Use Hugging Face search to dynamically discover models
* Store downloaded `.gguf` models locally and load via `Uri`
* Combine streamed `LiveData` responses with real-time UI feedback

---

## 📜 License

MIT License © 2025 [AiSee](https://github.com/aisee-ai)
