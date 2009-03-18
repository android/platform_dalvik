/*
 * Copyright (C) 2008 The Android Open Source Project
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

/**
 * Native glue for Java class org.apache.harmony.xnet.provider.jsse.NativeCrypto
 */

#define LOG_TAG "NativeCrypto"

#include <jni.h>
#include <JNIHelp.h>
#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/dsa.h>
#include <openssl/rsa.h>

/**
 * Frees the SSL error state.
 * 
 * OpenSSL keeps an "error stack" per thread, and given that this code
 * can be called from arbitrary threads that we don't keep track of,
 * we err on the side of freeing the error state promptly (instead of,
 * say, at thread death).
 */
static void freeSslErrorState(void) {
    ERR_clear_error();
    ERR_remove_state(0);
}

/**
 * Throws a NullPointerException without any message.
 */
static void throwNullPointerException(JNIEnv* env) {
    jniThrowException(env, "java/lang/NullPointerException", NULL);
}

/**
 * Throws a RuntimeException with a human-readable error message.
 */
static void throwRuntimeException(JNIEnv* env, const char* message) {
    jniThrowException(env, "java/lang/RuntimeException", message);
}

/*
 * Checks this thread's OpenSSL error queue and throws a RuntimeException if
 * necessary.
 * 
 * @return 1 if an exception was thrown, 0 if not.
 */
static int throwExceptionIfNecessary(JNIEnv* env) {
    int error = ERR_get_error();
    int result = 0;

    if (error != 0) {
        char message[50];
        ERR_error_string_n(error, message, sizeof(message));
        LOGD("OpenSSL error %d: %s", error, message);
        throwRuntimeException(env, message);
        result = 1;
    }

    freeSslErrorState();
    return result;
}

/**
 * Converts a Java byte[] to an OpenSSL BIGNUM, allocating the BIGNUM on the
 * fly.
 */
static BIGNUM* arrayToBignum(JNIEnv* env, jbyteArray source) {
    // LOGD("Entering arrayToBignum()");
  
    jbyte* sourceBytes = env->GetByteArrayElements(source, NULL);
    int sourceLength = env->GetArrayLength(source);
    BIGNUM* bignum = BN_bin2bn((unsigned char*) sourceBytes, sourceLength, NULL);
    env->ReleaseByteArrayElements(source, sourceBytes, JNI_ABORT);
    return bignum;
}

static void rsaDestroyKey(JNIEnv* env, jclass clazz, RSA* rsa);

/**
 * private static native int rsaCreatePublicKey(byte[] n, byte[] e);
 */
static RSA* rsaCreatePublicKey(JNIEnv* env, jclass clazz, jbyteArray n, jbyteArray e) {
    // LOGD("Entering rsaCreatePublicKey()");
    
    RSA* rsa = RSA_new();
    
    rsa->n = arrayToBignum(env, n);
    rsa->e = arrayToBignum(env, e);
    
    if (rsa->n == NULL || rsa->e == NULL) {
        rsaDestroyKey(env, clazz, rsa);
        throwRuntimeException(env, "Unable to convert BigInteger to BIGNUM");
        return NULL;
    }
    
    return rsa;
}

/**
 * private static native int rsaCreatePrivateKey(byte[] n, byte[] e, byte[] d, byte[] p, byte[] q);
 */
static RSA* rsaCreatePrivateKey(JNIEnv* env, jclass clazz, jbyteArray n, jbyteArray e, jbyteArray d, jbyteArray p, jbyteArray q) {
    // LOGD("Entering rsaCreatePrivateKey()");
  
    RSA* rsa = RSA_new();
  
    rsa->n = arrayToBignum(env, n);
    rsa->e = arrayToBignum(env, e);
    rsa->d = arrayToBignum(env, d);
    rsa->p = arrayToBignum(env, p);
    rsa->q = arrayToBignum(env, q);

    int check = RSA_check_key(rsa);
    LOGI("RSA_check_key returns %d", check);
    
    if (rsa->n == NULL || rsa->e == NULL || rsa->d == NULL || rsa->p == NULL || rsa->q == NULL) {
        rsaDestroyKey(env, clazz, rsa);
        throwRuntimeException(env, "Unable to convert BigInteger to BIGNUM");
        return NULL;
    }
    
    return rsa;
}

/**
 * private static native void rsaDestroyKey(int rsa);
 */
static void rsaDestroyKey(JNIEnv* env, jclass clazz, RSA* rsa) {
    // LOGD("Entering rsaDestroyKey()");
    
    if (rsa != NULL) {
        RSA_free(rsa);
    }
}

/**
 * private static native int EVP_PKEY_new_DSA(byte[] p, byte[] q, byte[] g, byte[] pub_key, byte[] priv_key);
 */
static EVP_PKEY* NativeCrypto_EVP_PKEY_new_DSA(JNIEnv* env, jclass clazz, jbyteArray p, jbyteArray q, jbyteArray g, jbyteArray pub_key, jbyteArray priv_key) {
    // LOGD("Entering EVP_PKEY_new_DSA()");
  
    DSA* dsa = DSA_new();
  
    dsa->p = arrayToBignum(env, p);
    dsa->q = arrayToBignum(env, q);
    dsa->g = arrayToBignum(env, g);
    dsa->pub_key = arrayToBignum(env, pub_key);
    
    if (priv_key != NULL) {
        dsa->priv_key = arrayToBignum(env, priv_key);
    }

    if (dsa->p == NULL || dsa->q == NULL || dsa->g == NULL || dsa->pub_key == NULL) {
        DSA_free(dsa);
        throwRuntimeException(env, "Unable to convert BigInteger to BIGNUM");
        return NULL;
    }
    
    EVP_PKEY* pkey = EVP_PKEY_new();
    EVP_PKEY_assign_DSA(pkey, dsa);
    
    return pkey;
}

/**
 * private static native int EVP_PKEY_new_RSA(byte[] n, byte[] e, byte[] d, byte[] p, byte[] q);
 */
static EVP_PKEY* NativeCrypto_EVP_PKEY_new_RSA(JNIEnv* env, jclass clazz, jbyteArray n, jbyteArray e, jbyteArray d, jbyteArray p, jbyteArray q) {
    // LOGD("Entering EVP_PKEY_new_RSA()");
  
    RSA* rsa = RSA_new();
  
    rsa->n = arrayToBignum(env, n);
    rsa->e = arrayToBignum(env, e);
    
    if (d != NULL) {
        rsa->d = arrayToBignum(env, d);
    }
    
    if (p != NULL) {
        rsa->p = arrayToBignum(env, p);
    }
    
    if (q != NULL) {
        rsa->q = arrayToBignum(env, q);
    }

    // int check = RSA_check_key(rsa);
    // LOGI("RSA_check_key returns %d", check);
    
    if (rsa->n == NULL || rsa->e == NULL) {
        RSA_free(rsa);
        throwRuntimeException(env, "Unable to convert BigInteger to BIGNUM");
        return NULL;
    }
    
    EVP_PKEY* pkey = EVP_PKEY_new();
    EVP_PKEY_assign_RSA(pkey, rsa);
    
    return pkey;
}

/**
 * private static native void EVP_PKEY_free(int pkey);
 */
static void NativeCrypto_EVP_PKEY_free(JNIEnv* env, jclass clazz, EVP_PKEY* pkey) {
    // LOGD("Entering EVP_PKEY_free()");

    if (pkey != NULL) {
        EVP_PKEY_free(pkey);
    }
}

/*
 * public static native int EVP_new()
 */
static jint NativeCrypto_EVP_new(JNIEnv* env, jclass clazz) {
    // LOGI("NativeCrypto_EVP_DigestNew");
    
    return (jint)EVP_MD_CTX_create();
}

/*
 * public static native void EVP_free(int)
 */
static void NativeCrypto_EVP_free(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx) {
    // LOGI("NativeCrypto_EVP_DigestFree");
  
    if (ctx != NULL) {
        EVP_MD_CTX_destroy(ctx);
    }
}

/*
 * public static native int EVP_DigestFinal(int, byte[], int)
 */
static jint NativeCrypto_EVP_DigestFinal(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx, jbyteArray hash, jint offset) {
    // LOGI("NativeCrypto_EVP_DigestFinal%x, %x, %d, %d", ctx, hash, offset);
    
    if (ctx == NULL || hash == NULL) {
        throwNullPointerException(env);
        return -1;
    }

    int result = -1;
    
    jbyte* hashBytes = env->GetByteArrayElements(hash, NULL);
    EVP_DigestFinal(ctx, (unsigned char*) (hashBytes + offset), (unsigned int*)&result);
    env->ReleaseByteArrayElements(hash, hashBytes, 0);

    throwExceptionIfNecessary(env);
    
    return result;
}

/*
 * public static native void EVP_DigestInit(int, java.lang.String)
 */
static void NativeCrypto_EVP_DigestInit(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx, jstring algorithm) {
    // LOGI("NativeCrypto_EVP_DigestInit");
    
    if (ctx == NULL || algorithm == NULL) {
        throwNullPointerException(env);
        return;
    }
    
    const char* algorithmChars = env->GetStringUTFChars(algorithm, NULL);
    
    const EVP_MD *digest = EVP_get_digestbynid(OBJ_txt2nid(algorithmChars));
    env->ReleaseStringUTFChars(algorithm, algorithmChars);
    
    if (digest == NULL) {
        throwRuntimeException(env, "Hash algorithm not found");
        return;
    }
    
    EVP_DigestInit(ctx, digest);
    
    throwExceptionIfNecessary(env);
}

/*
 * public static native void EVP_DigestSize(int)
 */
static jint NativeCrypto_EVP_DigestSize(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx) {
    // LOGI("NativeCrypto_EVP_DigestSize");
    
    if (ctx == NULL) {
        throwNullPointerException(env);
        return -1;
    }
    
    int result = EVP_MD_CTX_size(ctx);
    
    throwExceptionIfNecessary(env);
    
    return result;
}

/*
 * public static native void EVP_DigestBlockSize(int)
 */
static jint NativeCrypto_EVP_DigestBlockSize(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx) {
    // LOGI("NativeCrypto_EVP_DigestBlockSize");
    
    if (ctx == NULL) {
        throwNullPointerException(env);
        return -1;
    }
    
    int result = EVP_MD_CTX_block_size(ctx);
    
    throwExceptionIfNecessary(env);
    
    return result;
}

/*
 * public static native void EVP_DigestUpdate(int, byte[], int, int)
 */
static void NativeCrypto_EVP_DigestUpdate(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx, jbyteArray buffer, jint offset, jint length) {
    // LOGI("NativeCrypto_EVP_DigestUpdate %x, %x, %d, %d", ctx, buffer, offset, length);
    
    if (ctx == NULL || buffer == NULL) {
        throwNullPointerException(env);
        return;
    }
  
    jbyte* bufferBytes = env->GetByteArrayElements(buffer, NULL);
    EVP_DigestUpdate(ctx, (unsigned char*) (bufferBytes + offset), length);
    env->ReleaseByteArrayElements(buffer, bufferBytes, JNI_ABORT);
  
    throwExceptionIfNecessary(env);
}

/*
 * public static native void EVP_VerifyInit(int, java.lang.String)
 */
static void NativeCrypto_EVP_VerifyInit(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx, jstring algorithm) {
    // LOGI("NativeCrypto_EVP_VerifyInit");
    
    if (ctx == NULL || algorithm == NULL) {
        throwNullPointerException(env);
        return;
    }
    
    const char* algorithmChars = env->GetStringUTFChars(algorithm, NULL);
    
    const EVP_MD *digest = EVP_get_digestbynid(OBJ_txt2nid(algorithmChars));
    env->ReleaseStringUTFChars(algorithm, algorithmChars);
    
    if (digest == NULL) {
        throwRuntimeException(env, "Hash algorithm not found");
        return;
    }
    
    EVP_VerifyInit(ctx, digest);
    
    throwExceptionIfNecessary(env);
}

/*
 * public static native void EVP_VerifyUpdate(int, byte[], int, int)
 */
static void NativeCrypto_EVP_VerifyUpdate(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx, jbyteArray buffer, jint offset, jint length) {
    // LOGI("NativeCrypto_EVP_VerifyUpdate %x, %x, %d, %d", ctx, buffer, offset, length);
    
    if (ctx == NULL || buffer == NULL) {
        throwNullPointerException(env);
        return;
    }
  
    jbyte* bufferBytes = env->GetByteArrayElements(buffer, NULL);
    EVP_VerifyUpdate(ctx, (unsigned char*) (bufferBytes + offset), length);
    env->ReleaseByteArrayElements(buffer, bufferBytes, JNI_ABORT);
  
    throwExceptionIfNecessary(env);
}

/*
 * public static native void EVP_VerifyFinal(int, byte[], int, int, int)
 */
static int NativeCrypto_EVP_VerifyFinal(JNIEnv* env, jclass clazz, EVP_MD_CTX* ctx, jbyteArray buffer, jint offset, jint length, EVP_PKEY* pkey) {
    // LOGI("NativeCrypto_EVP_VerifyFinal %x, %x, %d, %d %x", ctx, buffer, offset, length, pkey);
    
    if (ctx == NULL || buffer == NULL || pkey == NULL) {
        throwNullPointerException(env);
        return -1;
    }
  
    jbyte* bufferBytes = env->GetByteArrayElements(buffer, NULL);
    int result = EVP_VerifyFinal(ctx, (unsigned char*) (bufferBytes + offset), length, pkey);
    env->ReleaseByteArrayElements(buffer, bufferBytes, JNI_ABORT);
  
    throwExceptionIfNecessary(env);
    
    return result;
}

/*
 * Defines the mapping from Java methods and their signatures
 * to native functions. Order is (1) Java name, (2) signature,
 * (3) pointer to C function.
 */
static JNINativeMethod methods[] = {
/*
    { "dsaCreatePublicKey",  "([B[B[B[B)I",   (void*)dsaCreatePublicKey  },
    { "dsaCreatePrivateKey", "([B[B[B[B[B)I", (void*)dsaCreatePrivateKey },
    { "dsaDestroyKey",       "(I)V",          (void*)dsaDestroyKey },
    { "rsaCreatePublicKey",  "([B[B)I",       (void*)rsaCreatePublicKey  },
    { "rsaCreatePrivateKey", "([B[B[B[B[B)I", (void*)rsaCreatePrivateKey },
    { "rsaDestroyKey",       "(I)V",          (void*)rsaDestroyKey },
*/    
    { "EVP_PKEY_new_DSA",    "([B[B[B[B[B)I", (void*)NativeCrypto_EVP_PKEY_new_DSA },
    { "EVP_PKEY_new_RSA",    "([B[B[B[B[B)I", (void*)NativeCrypto_EVP_PKEY_new_RSA },
    { "EVP_PKEY_free",       "(I)V",          (void*)NativeCrypto_EVP_PKEY_free },
    { "EVP_new",             "()I",           (void*)NativeCrypto_EVP_new },
    { "EVP_free",            "(I)V",          (void*)NativeCrypto_EVP_free },
    { "EVP_DigestFinal",     "(I[BI)I",       (void*)NativeCrypto_EVP_DigestFinal },
    { "EVP_DigestInit",      "(ILjava/lang/String;)V", (void*)NativeCrypto_EVP_DigestInit },
    { "EVP_DigestBlockSize", "(I)I",          (void*)NativeCrypto_EVP_DigestBlockSize },
    { "EVP_DigestSize",      "(I)I",          (void*)NativeCrypto_EVP_DigestSize },
    { "EVP_DigestUpdate",    "(I[BII)V",      (void*)NativeCrypto_EVP_DigestUpdate },
    { "EVP_VerifyInit",      "(ILjava/lang/String;)V", (void*)NativeCrypto_EVP_VerifyInit },
    { "EVP_VerifyUpdate",    "(I[BII)V",      (void*)NativeCrypto_EVP_VerifyUpdate },
    { "EVP_VerifyFinal",     "(I[BIII)I",     (void*)NativeCrypto_EVP_VerifyFinal }
};

/*
 * Peforms the actual registration of the native methods.
 * Also looks up the fields that belong to the class (if
 * any) and stores the field IDs. Simply remove what you
 * don't need.
 */
extern "C" int register_org_apache_harmony_xnet_provider_jsse_NativeCrypto(JNIEnv* env) {
    int result;
    result = jniRegisterNativeMethods(env, "org/apache/harmony/xnet/provider/jsse/NativeCrypto", methods, NELEM(methods));
    if (result == -1) {
        return -1;
    }

    jclass clazz;
    clazz = env->FindClass("org/apache/harmony/xnet/provider/jsse/NativeCrypto");
    if (clazz == NULL) {
        return -1;
    }

    return 0;
}
