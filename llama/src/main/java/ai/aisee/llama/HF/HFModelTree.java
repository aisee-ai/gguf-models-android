package ai.aisee.llama.HF;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HFModelTree {

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public HFModelTree(OkHttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public List<HFModelFile> getModelFileTree(String modelId) throws IOException {
        String url = HFEndpoints.getHFModelTreeEndpoint.get(modelId);
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Invalid model ID: " + response.code());
            }

            String json = response.body().string();
            return mapper.readValue(json, new TypeReference<List<HFModelFile>>() {});
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HFModelFile {
        @JsonProperty("type")
        public String type;

        @JsonProperty("oid")
        public String oid;

        @JsonProperty("size")
        public long size;

        @JsonProperty("path")
        public String path;
    }
}