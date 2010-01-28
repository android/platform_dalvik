/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "OpenSSLServerSocketImpl"

#include <jni.h>
#include <JNIHelp.h>

#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>

#include <openssl/err.h>
#include <openssl/rand.h>
#include <openssl/ssl.h>

#include "org_apache_harmony_xnet_provider_jsse_common.h"

/**
 * Module scope variables initialized during JNI registration.
 */
static jfieldID field_ssl_ctx;

/**
 * Throws java.io.IOexception with the provided message.
 */
static void throwIOExceptionStr(JNIEnv* env, const char* message)
{
    jclass exClass = env->FindClass("java/io/IOException");

    if (exClass == NULL)
    {
        LOGE("Unable to find class java/io/IOException");
    }
    else
    {
        env->ThrowNew(exClass, message);
    }
}

/**
 * Initialization phase of OpenSSL: Loads the Error strings, the crypto algorithms and reset the OpenSSL library
 */
static void org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_initstatic(JNIEnv* env, jobject obj)
{
    SSL_load_error_strings();
    ERR_load_crypto_strings();
    SSL_library_init();
    OpenSSL_add_all_algorithms();
}

/**
 * Initialization phase for a server socket with OpenSSL.  The server's private key and X509 certificate are read and  
 * the Linux /dev/random file is loaded as RNG for the session keys.
 *  
 */
static void org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_init(JNIEnv* env, jobject object,
        jstring privatekey, jstring certificates, jbyteArray seed)
{
    SSL_CTX *ssl_ctx;
    const char *privatekeychar;
    const char *certificateschar;
    EVP_PKEY * privatekeyevp;

    BIO *privatekeybio;
    BIO *certificatesbio;

    // 'seed == null' when no SecureRandom Object is set
    // in the SSLContext.
    if (seed != NULL) {
        jboolean iscopy = JNI_FALSE;
        jbyte* randseed = env->GetByteArrayElements(seed, &iscopy);
        RAND_seed((unsigned char*) randseed, 1024);
        env->ReleaseByteArrayElements(seed, randseed, 0);
    } else {
        RAND_load_file("/dev/urandom", 1024);
    }

    ssl_ctx = SSL_CTX_new(SSLv23_server_method());
    SSL_CTX_set_options(ssl_ctx, SSL_OP_ALL|SSL_OP_NO_SSLv2);

    privatekeychar = env->GetStringUTFChars((jstring)privatekey, NULL);
    privatekeybio = BIO_new_mem_buf((void*)privatekeychar, -1);

    privatekeyevp = PEM_read_bio_PrivateKey(privatekeybio, NULL, 0, NULL);
    env->ReleaseStringUTFChars(privatekey, privatekeychar);

    if (privatekeyevp == NULL) {
        LOGE(ERR_error_string(ERR_get_error(), NULL));
        throwIOExceptionStr(env, "Error parsing the private key");
        return;
    }

    certificateschar = env->GetStringUTFChars((jstring)certificates, NULL);
    certificatesbio = BIO_new_mem_buf((void*)certificateschar, -1);

    X509 * certificatesx509 = PEM_read_bio_X509(certificatesbio, NULL, 0, NULL);
    env->ReleaseStringUTFChars(certificates, certificateschar);

    if (certificatesx509 == NULL) {
        LOGE(ERR_error_string(ERR_get_error(), NULL));
        throwIOExceptionStr(env, "Error parsing the certificates");
        return;
    }

    if (!SSL_CTX_use_certificate(ssl_ctx, certificatesx509)) {
        LOGE(ERR_error_string(ERR_get_error(), NULL));
        throwIOExceptionStr(env, "Error setting the certificates");
        return;
    }

    if (!SSL_CTX_use_PrivateKey(ssl_ctx, privatekeyevp)) {
        LOGE(ERR_error_string(ERR_get_error(), NULL));
        throwIOExceptionStr(env, "Error setting the private key");
        return;
    }

    if (!SSL_CTX_check_private_key(ssl_ctx)) {
        LOGE(ERR_error_string(ERR_get_error(), NULL));
        throwIOExceptionStr(env, "Error checking private key");
        return;
    }

    env->SetIntField(object, field_ssl_ctx, (int)ssl_ctx);
}

/**
 * Loads the desired protocol for the OpenSSL server and enables it.  
 * For example SSL_OP_NO_TLSv1 means do not use TLS v. 1.
 */
static void org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_setenabledprotocols(JNIEnv* env,
        jobject object, jlong protocol)
{
    if (protocol != 0x00000000L) {
        if (protocol & SSL_OP_NO_SSLv3)
            LOGD("SSL_OP_NO_SSLv3 is set");
        if (protocol & SSL_OP_NO_TLSv1)
            LOGD("SSL_OP_NO_TLSv1 is set");

        SSL_CTX* ctx = (SSL_CTX*)env->GetIntField(object, field_ssl_ctx);
        SSL_CTX_set_options((SSL_CTX*)ctx, SSL_OP_ALL|SSL_OP_NO_SSLv2|(long)protocol);
    }
}

/**
 * Loads the ciphers suites that are supported by the OpenSSL server
 * and returns them in a string array.
 */
static jobjectArray org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_getsupportedciphersuites(JNIEnv* env,
        jobject object)
{
    SSL_CTX* ctx;
    SSL* ssl;
    STACK_OF(SSL_CIPHER) *sk;
    jobjectArray ret;
    int i;
    const char *c;

    ctx = SSL_CTX_new(SSLv23_server_method());
    ssl = SSL_new(ctx);
    sk=SSL_get_ciphers(ssl);

    ret= (jobjectArray)env->NewObjectArray(5,
         env->FindClass("java/lang/String"),
         env->NewStringUTF(""));

    i = 0;
    while (SSL_get_cipher_list(ssl,i) != NULL) {
        i++;
    }

    ret = (jobjectArray)env->NewObjectArray(i,
        env->FindClass("java/lang/String"),
        env->NewStringUTF(""));

    for (i=0; ; i++) {
        c=SSL_get_cipher_list(ssl,i);
        if (c == NULL) break;

        env->SetObjectArrayElement(ret,i,env->NewStringUTF(c));
    }

    return ret;
}

/**
 * Loads the ciphers suites that are enabled in the OpenSSL server
 * and returns them in a string array.
 */
static jobjectArray org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_getenabledciphersuites(JNIEnv* env,
        jobject object)
{
    SSL_CTX* ctx;
    SSL* ssl;
    jobjectArray ret;
    int i;
    const char *c;

    ctx = (SSL_CTX*)env->GetIntField(object, field_ssl_ctx);
    ssl = SSL_new(ctx);

    i = 0;
    while (SSL_get_cipher_list(ssl,i) != NULL) {
        i++;
    }

    ret = (jobjectArray)env->NewObjectArray(i,
        env->FindClass("java/lang/String"),
        env->NewStringUTF(""));

    for (i = 0; ; i++) {
        c = SSL_get_cipher_list(ssl,i);
        if (c == NULL) break;

        env->SetObjectArrayElement(ret,i,env->NewStringUTF(c));
    }

    return ret;
}

/**
 * Sets the ciphers suites that are enabled in the OpenSSL server.
 */
static void org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_setenabledciphersuites(JNIEnv* env,
        jobject object, jstring controlstring)
{
    SSL_CTX* ctx;
    const char *str;
    int ret;

    ctx = (SSL_CTX*)env->GetIntField(object, field_ssl_ctx);
    str = env->GetStringUTFChars(controlstring, 0);
    ret = SSL_CTX_set_cipher_list(ctx, str);
    env->ReleaseStringUTFChars(controlstring, str);

    if(ret == 0) {
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exClass, "Illegal cipher suite strings.");
    }    
}

/**
 * Sets  the client's credentials and the depth of theirs verification.
 */
static void org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_nativesetclientauth(JNIEnv* env,
        jobject object, jint value)
{
    SSL_CTX *ssl_ctx = (SSL_CTX *)env->GetIntField(object, field_ssl_ctx);
    SSL_CTX_set_verify(ssl_ctx, (int)value, verify_callback);
}

/**
 * The actual SSL context is reset.
 */
static void org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_nativefree(JNIEnv* env, jobject object)
{
    SSL_CTX *ctx = (SSL_CTX *)env->GetIntField(object, field_ssl_ctx);
    SSL_CTX_free(ctx);
    env->SetIntField(object, field_ssl_ctx, 0);
}

/**
 * The actual JNI methods' mapping table for the class OpenSSLServerSocketImpl.
 */
static JNINativeMethod sMethods[] =
{
    {"nativeinitstatic", "()V", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_initstatic},
    {"nativeinit", "(Ljava/lang/String;Ljava/lang/String;[B)V", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_init},
    {"nativesetenabledprotocols", "(J)V", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_setenabledprotocols},
    {"nativegetsupportedciphersuites", "()[Ljava/lang/String;", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_getsupportedciphersuites},
    {"nativegetenabledciphersuites", "()[Ljava/lang/String;", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_getenabledciphersuites},
    {"nativesetenabledciphersuites", "(Ljava/lang/String;)V", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_setenabledciphersuites},
    {"nativesetclientauth", "(I)V", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_nativesetclientauth},
    {"nativefree", "()V", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl_nativefree}
};

/**
 * Register the native methods with JNI for the class OpenSSLServerSocketImpl.
 */
extern "C" int register_org_apache_harmony_xnet_provider_jsse_OpenSSLServerSocketImpl(JNIEnv* env)
{
    int ret;
    jclass clazz;

    clazz = env->FindClass("org/apache/harmony/xnet/provider/jsse/OpenSSLServerSocketImpl");

    if (clazz == NULL) {
        LOGE("Can't find org/apache/harmony/xnet/provider/jsse/OpenSSLServerSocketImpl");
        return -1;
    }

    ret = jniRegisterNativeMethods(env, "org/apache/harmony/xnet/provider/jsse/OpenSSLServerSocketImpl",
            sMethods, NELEM(sMethods));

    if (ret >= 0) {
        // Note: do these after the registration of native methods, because 
        // there is a static method "initstatic" that's called when the
        // OpenSSLServerSocketImpl class is first loaded, and that required
        // a native method to be associated with it.
        field_ssl_ctx = env->GetFieldID(clazz, "ssl_ctx", "I");
        if (field_ssl_ctx == NULL) {
            LOGE("Can't find OpenSSLServerSocketImpl.ssl_ctx");
            return -1;
        }
    }
    return ret;
}
