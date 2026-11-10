#include <jni.h>
#include <cstdint>
#include <limits>

extern "C" bool plusgram_text_process(const uint16_t*, size_t, bool, void*,
                                     void (*)(void*, uint32_t, const uint16_t*, size_t));
struct Result {
    JNIEnv* env;
    jobjectArray values;
};

static void receive(void* context, uint32_t index, const uint16_t* data, size_t size) {
    auto* result = static_cast<Result*>(context);
    if (result->env->ExceptionCheck()) return;
    if (size > static_cast<size_t>(std::numeric_limits<jsize>::max())) {
        jclass error = result->env->FindClass("java/lang/OutOfMemoryError");
        if (error) {
            result->env->ThrowNew(error, "Rust text result exceeds Java string limit");
            result->env->DeleteLocalRef(error);
        }
        return;
    }
    jstring value = result->env->NewString(reinterpret_cast<const jchar*>(data), static_cast<jsize>(size));
    if (value) {
        result->env->SetObjectArrayElement(result->values, static_cast<jsize>(index), value);
        result->env->DeleteLocalRef(value);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_plusgram_app_ai_network_ReasoningFilter_nativeProcess(
    JNIEnv* env, jclass, jstring input, jboolean inReasoning) {
    if (!input) return nullptr;
    const jsize length = env->GetStringLength(input);
    const jchar* chars = env->GetStringChars(input, nullptr);
    if (!chars) return nullptr;
    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) {
        env->ReleaseStringChars(input, chars);
        return nullptr;
    }
    jobjectArray values = env->NewObjectArray(4, stringClass, nullptr);
    env->DeleteLocalRef(stringClass);
    if (!values) {
        env->ReleaseStringChars(input, chars);
        return nullptr;
    }
    Result result{env, values};
    const bool success = plusgram_text_process(
        reinterpret_cast<const uint16_t*>(chars), static_cast<size_t>(length),
        inReasoning == JNI_TRUE, &result, receive);
    env->ReleaseStringChars(input, chars);
    if (!success || env->ExceptionCheck()) {
        env->DeleteLocalRef(values);
        return nullptr;
    }
    return values;
}
