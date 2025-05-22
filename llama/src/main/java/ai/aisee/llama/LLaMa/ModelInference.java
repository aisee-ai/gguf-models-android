package ai.aisee.llama.LLaMa;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelInference {
    private static ModelInference instance;
    private final ModelLoader modelLoader;


    private static final String TAG = "ModelInference";

    private static final Pattern findThinkTagRegex = Pattern.compile("<think>(.*?)</think>");
    private final MutableLiveData<String> _partialResponse = new MutableLiveData<>();
    public LiveData<String> partialResponse = _partialResponse;

    private ModelInference(Context context) {
        modelLoader = new ModelLoader(context,"");
    }
    private String lastFinalResponse = null;

    private LlamaListener listener;

    public void setListener(LlamaListener llamaListener) {
        listener = llamaListener;
    }

    public static synchronized ModelInference getInstance(Context context) {
        if (instance == null) {
            instance = new ModelInference(context.getApplicationContext());
        }
        return instance;
    }

    public void setSystemPrompt(String systemPrompt) {
        modelLoader.setSystemPrompt(systemPrompt);
    }

    public void loadModel(Uri modelUri, Runnable onSuccess, Runnable onFailure) {
        modelLoader.copyModelFile(
                modelUri,
                () -> {
                    Log.d(TAG, "Model copied and loaded successfully!");
                    if (onSuccess != null) onSuccess.run();  // callback on main thread
                },
                error -> {
                    Log.e(TAG, "Model loading error: " + error);
                    if (onFailure != null) onFailure.run();  // callback on main thread
                }
        );
    }

    public void generateResponse(String prompt) {
        Log.d("Started","MI");
        modelLoader.getResponse(
                prompt,
                piece -> {
                    Matcher matcher = findThinkTagRegex.matcher(piece);
                    StringBuffer sb = new StringBuffer();
                    while (matcher.find()) {
                        matcher.appendReplacement(sb, "<blockquote>" + Matcher.quoteReplacement(matcher.group(1)) + "</blockquote>");
                    }
                    matcher.appendTail(sb);
                    return sb.toString();
                },
                partial -> {
                    Log.d("LLaMa", "Partial response: " + partial);
                    _partialResponse.postValue(partial);
                },
                finalResponse -> {
                    Log.d("LLaMa", "Final response: " + finalResponse);
                    lastFinalResponse = finalResponse;
                    if (listener != null) {
                        listener.onLlamaComplete(finalResponse);
                    }
//                    listener.onLlamaComplete(finalResponse);
                    // You can post final response to another LiveData if needed
                },
                () -> {
                    Log.d("LLaMa", "Response generation cancelled.");
                },
                error -> {
                    Log.e("LLaMa", "Error generating response", error);
                }
        );
    }

    public interface LlamaListener {
        void onLlamaComplete(String response);
    }




//    public void generateResponseAsync(String prompt, java.util.function.Consumer<String> onSuccess, java.util.function.Consumer<String> onError) {
//        modelLoader.getResponseAsync(prompt, onSuccess, onError);
//    }
}