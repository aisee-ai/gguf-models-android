package ai.aisee.llama.HF;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class HFModelInfo {
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public HFModelInfo(OkHttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public ModelInfo getModelInfo(String modelId) throws IOException {
        String url = HFEndpoints.getHFModelSpecsEndpoint.get(modelId);
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Invalid model ID: " + response.code());
            }
            return mapper.readValue(response.body().string(), ModelInfo.class);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelInfo {
        @JsonProperty("_id")
        public String _id;

        public String id;

        public String modelId;

        public String author;

        public boolean isPrivate;

        public boolean disabled;

        public List<String> tags;

        @JsonAlias("downloads")
        public long numDownloads;

        @JsonAlias("likes")
        public long numLikes;

        @JsonProperty("lastModified")
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CustomDateDeserializer.class)
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = CustomDateSerializer.class)
        public LocalDateTime lastModified;

        @JsonProperty("createdAt")
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CustomDateDeserializer.class)
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = CustomDateSerializer.class)
        public LocalDateTime createdAt;
    }
}
