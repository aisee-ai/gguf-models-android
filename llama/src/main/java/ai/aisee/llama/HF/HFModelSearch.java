package ai.aisee.llama.HF;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class HFModelSearch {

    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private String pageURL;

    public HFModelSearch(OkHttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
        this.pageURL = HFEndpoints.getHFModelsListEndpoint.get();
    }

    public List<ModelSearchResult> searchModels(
            String query,
            String author,
            String filter,
            ModelSortParam sort,
            ModelSearchDirection direction,
            int limit,
            boolean full,
            boolean config
    ) throws IOException {

        String urlToUse = pageURL.equals(HFEndpoints.getHFModelsListEndpoint.get()) ? buildUrl(query, filter,author, sort, direction, limit, full, config) : pageURL;

        Request request = new Request.Builder().url(urlToUse).get().build();
        try (Response response = client.newCall(request).execute()) {
            Log.d("HF_DEBUG", "Sending HF API request...");
            Log.d("HF_DEBUG", "Request URL: " + request.url());

            if (!response.isSuccessful()) {
                Log.d("HF_DEBUG", "Response failed with code: " + response.code());
                return Collections.emptyList();
            }

            ResponseBody body = response.body();
            if (body == null) {
                Log.d("HF_RAW_RESPONSE", "Response body is null");
                return Collections.emptyList();
            }

            String rawJson = body.string();
            Log.d("HF_RAW_RESPONSE", rawJson);

            return mapper.readValue(rawJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, ModelSearchResult.class));

        }
    }

    private String buildUrl(String query, String filter,String author, ModelSortParam sort, ModelSearchDirection direction, int limit, boolean full, boolean config) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(HFEndpoints.getHFModelsListEndpoint.get())).newBuilder();
        urlBuilder.addQueryParameter("search", query);
        urlBuilder.addQueryParameter("tag", "gguf");
         urlBuilder.addQueryParameter("author", author);
//        urlBuilder.addQueryParameter("filter", filter);
//        urlBuilder.addQueryParameter("sort", sort.value);
////        urlBuilder.addQueryParameter("direction", String.valueOf(direction.value));
        urlBuilder.addQueryParameter("limit", String.valueOf(limit));
//        urlBuilder.addQueryParameter("full", String.valueOf(full));
//        urlBuilder.addQueryParameter("config", String.valueOf(config));
        return urlBuilder.build().toString();
    }

    private Map<String, String> parseLinkHeader(String header) {
        Pattern pattern = Pattern.compile("<([^>]+)>;\\s+rel=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(header);
        Map<String, String> links = new HashMap<>();
        while (matcher.find()) {
            links.put(matcher.group(2), matcher.group(1));
        }
        return links;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelSearchResult {

        @JsonProperty("_id")
        public String _id;

        public String id;

        @JsonAlias("likes")
        public int numLikes;

        @JsonAlias("downloads")
        public int numDownloads;

        @JsonAlias("private")
        public boolean isPrivate;

        public List<String> tags;

        @JsonProperty("createdAt")
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CustomDateDeserializer.class)
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = CustomDateSerializer.class)
        public LocalDateTime createdAt;

        public String modelId;

        @JsonProperty("description")
        public String description;

        @JsonProperty("author")
        public String author;
    }


    public enum ModelSortParam {
        NONE(""),
        DOWNLOADS("downloads"),
        AUTHOR("author");

        public final String value;

        ModelSortParam(String value) {
            this.value = value;
        }
    }

    public enum ModelSearchDirection {
        ASCENDING(1),
        DESCENDING(-1);

        public final int value;

        ModelSearchDirection(int value) {
            this.value = value;
        }
    }
}
