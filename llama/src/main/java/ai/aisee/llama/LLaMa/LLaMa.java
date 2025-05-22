package ai.aisee.llama.LLaMa;


import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;

public class LLaMa {
    static {
        String logTag = LLaMa.class.getSimpleName();

        String cpuFeatures = getCPUFeatures();
        boolean hasFp16 = cpuFeatures.contains("fp16") || cpuFeatures.contains("fphp");
        boolean hasDotProd = cpuFeatures.contains("dotprod") || cpuFeatures.contains("asimddp");
        boolean hasSve = cpuFeatures.contains("sve");
        boolean hasI8mm = cpuFeatures.contains("i8mm");
        boolean isAtLeastArmV82 =
                cpuFeatures.contains("asimd") && cpuFeatures.contains("crc32") && cpuFeatures.contains("aes");
        boolean isAtLeastArmV84 = cpuFeatures.contains("dcpop") && cpuFeatures.contains("uscat");

        Log.d(logTag, "CPU features: " + cpuFeatures);
        Log.d(logTag, "- hasFp16: " + hasFp16);
        Log.d(logTag, "- hasDotProd: " + hasDotProd);
        Log.d(logTag, "- hasSve: " + hasSve);
        Log.d(logTag, "- hasI8mm: " + hasI8mm);
        Log.d(logTag, "- isAtLeastArmV82: " + isAtLeastArmV82);
        Log.d(logTag, "- isAtLeastArmV84: " + isAtLeastArmV84);

        if (supportsArm64V8a()) {
            if (isAtLeastArmV84 && hasSve && hasI8mm && hasFp16 && hasDotProd) {
                Log.d(logTag, "Loading libllama_v8_4_fp16_dotprod_i8mm_sve.so");
                System.loadLibrary("llama_v8_4_fp16_dotprod_i8mm_sve");
            } else if (isAtLeastArmV84 && hasSve && hasFp16 && hasDotProd) {
                Log.d(logTag, "Loading libllama_v8_4_fp16_dotprod_sve.so");
                System.loadLibrary("llama_v8_4_fp16_dotprod_sve");
            } else if (isAtLeastArmV84 && hasI8mm && hasFp16 && hasDotProd) {
                Log.d(logTag, "Loading libllama_v8_4_fp16_dotprod_i8mm.so");
                System.loadLibrary("llama_v8_4_fp16_dotprod_i8mm");
            } else if (isAtLeastArmV84 && hasFp16 && hasDotProd) {
                Log.d(logTag, "Loading libllama_v8_4_fp16_dotprod.so");
                System.loadLibrary("llama_v8_4_fp16_dotprod");
            } else if (isAtLeastArmV82 && hasFp16 && hasDotProd) {
                Log.d(logTag, "Loading libllama_v8_2_fp16_dotprod.so");
                System.loadLibrary("llama_v8_2_fp16_dotprod");
            } else if (isAtLeastArmV82 && hasFp16) {
                Log.d(logTag, "Loading libllama_v8_2_fp16.so");
                System.loadLibrary("llama_v8_2_fp16");
            } else {
                Log.d(logTag, "Loading libllama_v8.so");
                System.loadLibrary("llama_v8");
            }
        } else {
            Log.d(logTag, "Loading default libllama.so");
            System.loadLibrary("llama");
        }
    }

    private long nativePtr = 0L;

    private static String getCPUFeatures() {
        try {
            File file = new File("/proc/cpuinfo");
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            return content.substring(content.indexOf("Features") + 9).split("\\n")[0].trim();
        } catch (FileNotFoundException e) {
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean supportsArm64V8a() {
        return Build.SUPPORTED_ABIS[0].equals("arm64-v8a");
    }

    public boolean create(String modelPath, float minP, float temperature, boolean storeChats, long contextSize,
                          String chatTemplate, int nThreads, boolean useMmap, boolean useMlock) {
        nativePtr = loadModel(modelPath, minP, temperature, storeChats, contextSize, chatTemplate, nThreads, useMmap, useMlock);
        return nativePtr != 0L;
    }

    public void addUserMessage(String message) {
        verifyHandle();
        addChatMessage(nativePtr, message, "user");
    }

    public void addSystemPrompt(String prompt) {
        verifyHandle();
        addChatMessage(nativePtr, prompt, "system");
    }

    public void addAssistantMessage(String message) {
        verifyHandle();
        addChatMessage(nativePtr, message, "assistant");
    }

    public float getResponseGenerationSpeed() {
        verifyHandle();
        return getResponseGenerationSpeed(nativePtr);
    }

    public int getContextLengthUsed() {
        verifyHandle();
        return getContextSizeUsed(nativePtr);
    }

    public Flowable<String> getResponse(String query) {
        return Flowable.create(emitter -> {
            verifyHandle();  // your setup
            startCompletion(nativePtr, query);  // start generation

            try {
                String piece = completionLoop(nativePtr);
                while (!"[EOG]".equals(piece) && !emitter.isCancelled()) {
                    emitter.onNext(piece);  // emit each chunk like Flow
                    piece = completionLoop(nativePtr);
                }
                emitter.onComplete();  // signal end of stream
            } catch (Throwable t) {
                emitter.onError(t);  // handle any error
            } finally {
                stopCompletion(nativePtr);  // your cleanup
            }
        }, BackpressureStrategy.BUFFER);  // buffers emitted items if consumer is slow
    }

    public void close() {
        if (nativePtr != 0L) {
            close(nativePtr);
            nativePtr = 0L;
        }
    }

    private void verifyHandle() {
        if (nativePtr == 0L) {
            throw new IllegalStateException("Model is not loaded. Use create() to load the model");
        }
    }

    private native long loadModel(String modelPath, float minP, float temperature, boolean storeChats, long contextSize,
                                  String chatTemplate, int nThreads, boolean useMmap, boolean useMlock);

    private native void addChatMessage(long modelPtr, String message, String role);

    private native float getResponseGenerationSpeed(long modelPtr);

    private native int getContextSizeUsed(long modelPtr);

    private native void close(long modelPtr);

    private native void startCompletion(long modelPtr, String prompt);

    private native String completionLoop(long modelPtr);

    private native void stopCompletion(long modelPtr);
}