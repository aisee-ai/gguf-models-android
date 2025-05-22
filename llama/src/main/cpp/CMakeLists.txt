cmake_minimum_required(VERSION 3.10)
project(llama_jni)

# Include LLaMA headers and link the libllama.so library
set(LLAMA_DIR "../../../../llama.cpp")
set(GGML_DIR ${LLAMA_DIR}/ggml)
set(COMMON_DIR ${LLAMA_DIR}/common)

# Define source files for llama and ggufreader
set(LLAMA_SOURCES
        ${GGML_DIR}/src/ggml.c
        ${GGML_DIR}/src/ggml-alloc.c
        ${GGML_DIR}/src/ggml-backend.cpp
        ${GGML_DIR}/src/ggml-threading.cpp
        ${GGML_DIR}/src/ggml-quants.c
        ${GGML_DIR}/src/ggml-backend-reg.cpp
        ${GGML_DIR}/src/ggml-opt.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu.c
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-aarch64.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-quants.c
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-traits.cpp
        ${GGML_DIR}/src/gguf.cpp
        ${LLAMA_DIR}/src/llama.cpp
        ${LLAMA_DIR}/src/llama-vocab.cpp
        ${LLAMA_DIR}/src/llama-grammar.cpp
        ${LLAMA_DIR}/src/llama-sampling.cpp
        ${LLAMA_DIR}/src/llama-context.cpp
        ${LLAMA_DIR}/src/llama-model.cpp
        ${LLAMA_DIR}/src/llama-model-loader.cpp
        ${LLAMA_DIR}/src/llama-impl.cpp
        ${LLAMA_DIR}/src/llama-mmap.cpp
        ${LLAMA_DIR}/src/llama-hparams.cpp
        ${LLAMA_DIR}/src/llama-kv-cache.cpp
        ${LLAMA_DIR}/src/llama-batch.cpp
        ${LLAMA_DIR}/src/llama-arch.cpp
        ${LLAMA_DIR}/src/llama-adapter.cpp
        ${LLAMA_DIR}/src/llama-chat.cpp
        ${LLAMA_DIR}/src/unicode.h
        ${LLAMA_DIR}/src/unicode.cpp
        ${LLAMA_DIR}/src/unicode-data.cpp
        ${COMMON_DIR}/arg.cpp
        ${COMMON_DIR}/base64.hpp
        ${COMMON_DIR}/common.cpp
        ${COMMON_DIR}/console.cpp
        ${COMMON_DIR}/json-schema-to-grammar.cpp
        ${COMMON_DIR}/json.hpp
        ${COMMON_DIR}/log.cpp
        ${COMMON_DIR}/ngram-cache.cpp
        ${COMMON_DIR}/sampling.cpp
        LLMInference.cpp
        llama.cpp
)

set(GGUF_READER_SOURCES
        ${GGML_DIR}/src/ggml.c
        ${GGML_DIR}/src/ggml-alloc.c
        ${GGML_DIR}/src/ggml-backend.cpp
        ${GGML_DIR}/src/ggML-threading.cpp
        ${GGML_DIR}/src/ggml-quants.c
        ${GGML_DIR}/src/ggml-backend-reg.cpp
        ${GGML_DIR}/src/ggml-opt.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu.c
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-aarch64.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-quants.c
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-traits.cpp
        ${GGML_DIR}/src/gguf.cpp
        GGUFReader.cpp
)

# Set the output directory for shared libraries
#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})
# Set the output directory for shared libraries (.so files)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# Define the build function for creating shared libraries
function(build_library target_name cpu_flags)
    add_library(
            ${target_name}
            SHARED
            ${LLAMA_SOURCES}
    )
    target_include_directories(
            ${target_name}
            PUBLIC
            ${GGML_DIR}/include
            ${GGML_DIR}/src
            ${GGML_DIR}/src/ggml-cpu
            ${LLAMA_DIR}/include
            ${COMMON_DIR}
    )
    target_compile_options(
            ${target_name}
            PUBLIC
            -DGGML_USE_CPU -DGGML_USE_CPU_AARCH64 ${cpu_flags} -O3
    )
    target_compile_options(
            ${target_name}
            PUBLIC
            -fvisibility=hidden -fvisibility-inlines-hidden
    )
    target_compile_options(
            ${target_name}
            PUBLIC
            -ffunction-sections -fdata-sections
    )
    target_link_libraries(
            ${target_name}
            android log
    )
    target_link_options(
            ${target_name}
            PRIVATE
            -Wl,--gc-sections -flto
    )
endfunction()

# Build libraries based on architecture
build_library("llama" "")
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    build_library("llama_v8" "-march=armv8-a")
    build_library("llama_v8_2_fp16" "-march=armv8.2-a+fp16")
    build_library("llama_v8_2_fp16_dotprod" "-march=armv8.2-a+fp16+dotprod")
    build_library("llama_v8_4_fp16_dotprod" "-march=armv8.4-a+fp16+dotprod")
    build_library("llama_v8_4_fp16_dotprod_sve" "-march=armv8.4-a+fp16+dotprod+sve")
    build_library("llama_v8_4_fp16_dotprod_i8mm" "-march=armv8.4-a+fp16+dotprod+i8mm")
    build_library("llama_v8_4_fp16_dotprod_i8mm_sve" "-march=armv8.4-a+fp16+dotprod+i8mm+sve")
endif()

# Create GGUFReader shared library
set(TARGET_NAME_GGUF_READER ggufreader)
add_library(${TARGET_NAME_GGUF_READER} SHARED ${GGUF_READER_SOURCES})
target_include_directories(
        ${TARGET_NAME_GGUF_READER}
        PUBLIC
        ${GGML_DIR}/include
        ${GGML_DIR}/src
        ${GGML_DIR}/src/ggml-cpu
)