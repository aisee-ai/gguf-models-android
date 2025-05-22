package ai.aisee.llama.HF;


public class HFEndpoints {

    private static final String HF_BASE_ENDPOINT = "https://huggingface.co/api/models";

    public static final EndpointProvider getHFModelsListEndpoint = () -> HF_BASE_ENDPOINT;

    public static final ModelEndpointProvider getHFModelTreeEndpoint =
            (modelId) -> HF_BASE_ENDPOINT + "/" + modelId + "/tree/main";

    public static final ModelEndpointProvider getHFModelSpecsEndpoint =
            (modelId) -> HF_BASE_ENDPOINT + "/" + modelId;

    // Functional interfaces
    public interface EndpointProvider {
        String get();
    }

    public interface ModelEndpointProvider {
        String get(String modelId);
    }
}
