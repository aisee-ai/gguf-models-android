package ai.aisee.llama.LLaMa;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ModelLoader extends ViewModel {

    private static final String TAG = "ModelLoader";
    private final Context context;
    private final ExecutorService executorService;
    private LLaMa llama;
    private GGUFReader ggufReader;
    private boolean modelLoaded = false;

    private String systemPrompt = "";
    ;


    private boolean inferenceHappened = false ;

//    private String systemPrompt = getString(R.string.systemPrompt);

    public ModelLoader(Context context, String systemPrompt) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.systemPrompt = systemPrompt;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public void copyModelFile(Uri uri, Runnable onComplete, Consumer<String> onError) {
        executorService.execute(() -> {
            long totalStartTime = System.currentTimeMillis();
            if (modelLoaded) {
                onComplete.run();
                return;
            }

            String fileName = "";
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    fileName = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving file name", e);
                onError.accept("Error retrieving file name");
                return;
            }

            if (fileName.isEmpty()) {
                onError.accept("Invalid file name");
                return;
            }

            File destFile = new File(context.getFilesDir(), fileName);

            if (destFile.exists()) {
                Log.d(TAG, "Model file already exists. Skipping copy.");
                loadModel(destFile, onComplete, onError);
                return;
            }

//            showProgressDialog();

            try {
                long copyStart = System.currentTimeMillis();

                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(destFile)) {
                    if (inputStream != null) {
                        FileChannel outChannel = outputStream.getChannel();
                        ReadableByteChannel inChannel = Channels.newChannel(inputStream);
                        outChannel.transferFrom(inChannel, 0, Long.MAX_VALUE);
                    }
                }

                long copyEnd = System.currentTimeMillis();
                Log.d(TAG, "File copy completed in " + (copyEnd - copyStart) + " ms");
            } catch (Exception e) {
                Log.e(TAG, "Error copying file", e);
                onError.accept("Error copying file");
                return;
            }

            loadModel(destFile, onComplete, onError);
        });
    }

    private void loadModel(File modelFile, Runnable onComplete, Consumer<String> onError) {
        llama = new LLaMa();
        ggufReader = new GGUFReader();

        ggufReader.load(modelFile.getAbsolutePath(), new GGUFReader.LoadCallback() {
            @Override
            public void onLoaded() {
                try {
                    long contextSize = ggufReader.getContextSize();
                    String chatTemplate = ggufReader.getChatTemplate();

                    llama.create(
                            modelFile.getAbsolutePath(),
                            0.5f,
                            0.0f,
                            false,
                            2048,
                            chatTemplate,
                            4,
                            true,
                            false
                    );

//                    String systemPromptstart = "If the input is empty, respond with try again.";

                    llama.addSystemPrompt(systemPrompt);
                    modelLoaded = true;
                    onComplete.run();
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing LLaMa after GGUF load", e);
                    onError.accept("Error initializing LLaMa");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading GGUF file", e);
                onError.accept("Error loading GGUF file");
            }
        });
    }

    public void getResponse(
            String prompt,
            Function<String, String> responseTransform,
            Consumer<String> onPartialResponseGenerated,
            Consumer<String> onSuccess,
            Runnable onCancelled,
            Consumer<Exception> onError
    ) {
        if (llama == null) {
            Log.e(TAG, "Model not loaded");
            return;
        }

        if (inferenceHappened){
            llama.close();
            llama = new LLaMa();
//            String systemPrompt = "Please provide a concise and accurate summary of the following text.";
//            String systemPrompt = context.getString(R.string.systemPrompt);

            llama.create(
                    ggufReader.getFilePath(),
                    0.5f,
                    0.0f,
                    false,
                    2048,
                    ggufReader.getChatTemplate(),
                    4,
                    true,
                    false
            );
            llama.addSystemPrompt(systemPrompt);

        }


        try {
            StringBuilder responseBuilder = new StringBuilder();

            llama.getResponse(prompt) // This should return Flowable<String>
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            piece -> {
                                responseBuilder.append(piece);
                                onPartialResponseGenerated.accept(piece); // stream partial chunk
                            },
                            error -> {
                                if (error instanceof CancellationException) {
                                    onCancelled.run();
                                } else {
                                    onError.accept((Exception) error);
                                }
                            },
                            () -> {
                                // All chunks done
                                String fullResponse = responseTransform.apply(responseBuilder.toString());
                                onSuccess.accept(fullResponse);
                                inferenceHappened = true;
                            }
                    );

        } catch (Exception e) {
            Log.d(TAG, "Error getting response", e);
            onError.accept(e);
        }
    }


    public void setSystemPrompt(String prompt) {
        this.systemPrompt = prompt;
    }




    private void showProgressDialog() {
        // Implement progress dialog show logic
    }

    private void hideProgressDialog() {
        // Implement progress dialog hide logic
    }
}
