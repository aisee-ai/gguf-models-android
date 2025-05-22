package ai.aisee.llama.LLaMa;

/*
 * Copyright (C) 2025 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GGUFReader {

    static {
        System.loadLibrary("ggufreader");
    }

    private long nativeHandle = 0L;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String modelPath = null;

    public interface LoadCallback {
        void onLoaded();
        void onError(Exception e);
    }

    public void load(final String modelPath, final LoadCallback callback) {
        this.modelPath = modelPath;
        executor.execute(() -> {
            try {
                nativeHandle = getGGUFContextNativeHandle(modelPath);
                if (callback != null) callback.onLoaded();
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public String getFilePath() {
        return modelPath;
    }


    public Long getContextSize() {
        if (nativeHandle == 0L) {
            throw new IllegalStateException("Use GGUFReader.loadAsync() to initialize the reader");
        }
        long contextSize = getContextSize(nativeHandle);
        return contextSize == -1L ? null : contextSize;
    }

    public String getChatTemplate() {
        if (nativeHandle == 0L) {
            throw new IllegalStateException("Use GGUFReader.loadAsync() to initialize the reader");
        }
        String chatTemplate = getChatTemplate(nativeHandle);
        return chatTemplate.isEmpty() ? null : chatTemplate;
    }

    // Native method declarations
    private native long getGGUFContextNativeHandle(String modelPath);
    private native long getContextSize(long nativeHandle);
    private native String getChatTemplate(long nativeHandle);
}