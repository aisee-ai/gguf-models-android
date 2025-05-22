package ai.aisee.llama.HF;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import okhttp3.OkHttpClient;

public class HFModels {
    private static final OkHttpClient httpClient = new OkHttpClient();

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules(); // auto-register Kotlin/JavaTime modules

    public static HFModelInfo getInfo() {
        return new HFModelInfo(httpClient, mapper);
    }

    public static HFModelTree getTree() {
        return new HFModelTree(httpClient, mapper);
    }

    public static HFModelSearch getSearch() {
        return new HFModelSearch(httpClient, mapper);
    }
}
