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

#define LOG_TAG "OpenSSLSessionImpl"

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
static jfieldID field_session;

static SSL_SESSION *getSslSessionPointer(JNIEnv* env, jobject object) {
    SSL_SESSION* session = (SSL_SESSION *)env->GetIntField(object, field_session);

    return session;
}

/**
 * Throws java.io.IOexception with the provided message.
 */
static void throwIOExceptionStr(JNIEnv* env, const char* message)
{
    jclass exClass = env->FindClass("java/io/IOException");

    if (exClass == NULL) {
        LOGE("Unable to find class java/io/IOException");
    } else {
        env->ThrowNew(exClass, message);
    }
}

/**
 * Gets the peer certificate in the chain and fills a byte array with the
 * information therein.
 */
static jobjectArray org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getpeercertificates(JNIEnv* env,
        jobject object, jint jssl)
{
    SSL_SESSION *ssl_session;
    SSL_CTX* ssl_ctx;
    SSL* ssl;
    STACK_OF(X509) *chain;
    jobjectArray objectArray;

    ssl_session = getSslSessionPointer(env, object);

    ssl_ctx = SSL_CTX_new(SSLv23_client_method());
    ssl = SSL_new(ssl_ctx);

    SSL_set_session(ssl, ssl_session);

    chain = SSL_get_peer_cert_chain(ssl);

    objectArray = getcertificatebytes(env, chain);

    SSL_free(ssl);
    SSL_CTX_free(ssl_ctx);

    return objectArray;
}

/**
 * Serialize the session.
 * See apache mod_ssl
 */
static jbyteArray org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_serialize(JNIEnv* env, jobject object)
{
  SSL_SESSION * ssl_session;
  jbyteArray bytes = NULL;
  jbyte *tmp;
  int size;
  unsigned char *ucp;

  ssl_session = getSslSessionPointer(env, object);
  if (ssl_session == NULL) {
    return NULL;
  }

  // Compute the size of the DER data
  size = i2d_SSL_SESSION(ssl_session, NULL);
  if (size == 0) {
    return NULL;
  }

  bytes = env->NewByteArray(size);
  if (bytes != NULL) {
        tmp = env->GetByteArrayElements(bytes, NULL);
        ucp = (unsigned char *) tmp;
        i2d_SSL_SESSION(ssl_session, &ucp);
        env->ReleaseByteArrayElements(bytes, tmp, 0);
  }

  return bytes;

}

/**
 * Deserialize the session.
 */
static jint org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_deserialize(JNIEnv* env, jobject object, jbyteArray bytes, jint size)
{
  const unsigned char *ucp;
  jbyte *tmp;
  SSL_SESSION *ssl_session;

  if (bytes != NULL) {
        tmp = env->GetByteArrayElements(bytes, NULL);
        ucp = (const unsigned char *) tmp;
        ssl_session = d2i_SSL_SESSION(NULL, &ucp, (long) size);
        env->ReleaseByteArrayElements(bytes, tmp, 0);

        return (jint) ssl_session;
  }
  return 0;
}

/**
 * Gets and returns in a byte array the ID of the actual SSL session.
 */
static jbyteArray org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getid(JNIEnv* env, jobject object)
{
    SSL_SESSION * ssl_session;
    jbyteArray bytes;
    jbyte *tmp;

    ssl_session = getSslSessionPointer(env, object);

    bytes = env->NewByteArray(ssl_session->session_id_length);
    if (bytes != NULL) {
        tmp = env->GetByteArrayElements(bytes, NULL);
        memcpy(tmp, ssl_session->session_id, ssl_session->session_id_length);
        env->ReleaseByteArrayElements(bytes, tmp, 0);
    }

    return bytes;
}

/**
 * Gets and returns in a long integer the creation's time of the
 * actual SSL session.
 */
static jlong org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getcreationtime(JNIEnv* env, jobject object)
{
    SSL_SESSION * ssl_session;

    ssl_session = getSslSessionPointer(env, object);

    // convert the creation time from seconds to milliseconds
    return (jlong)(1000L * ssl_session->time);
}

/**
 * Gets and returns in a string the peer's host's name.
 */
static jstring org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getpeerhost(JNIEnv* env, jobject object)
{
    SSL_CTX *ssl_ctx;
    SSL *ssl;
    SSL_SESSION *ssl_session;
    BIO *bio;
    char* hostname;
    jstring  result;

    ssl_session = getSslSessionPointer(env, object);

    ssl_ctx = SSL_CTX_new(SSLv23_client_method());
    ssl = SSL_new(ssl_ctx);

    SSL_set_session(ssl, ssl_session);

    bio = SSL_get_rbio(ssl);

    hostname = BIO_get_conn_hostname(bio);

    /* Notice: hostname can be NULL */
    result = env->NewStringUTF(hostname);

    SSL_free(ssl);
    SSL_CTX_free(ssl_ctx);

    return result;
}

/**
 * Gets and returns in a string the peer's port name (https, ftp, etc.).
 */
static jstring org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getpeerport(JNIEnv* env, jobject object)
{
    SSL_CTX *ssl_ctx;
    SSL *ssl;
    SSL_SESSION *ssl_session;
    BIO *bio;
    char *port;
    jstring  result;

    ssl_session = getSslSessionPointer(env, object);

    ssl_ctx = SSL_CTX_new(SSLv23_client_method());
    ssl = SSL_new(ssl_ctx);

    SSL_set_session(ssl, ssl_session);

    bio = SSL_get_rbio(ssl);
    port = BIO_get_conn_port(bio);

    /* Notice: port name can be NULL */
    result = env->NewStringUTF(port);

    SSL_free(ssl);
    SSL_CTX_free(ssl_ctx);

    return result;
}

/**
 * Gets and returns in a string the version of the SSL protocol. If it
 * returns the string "unknown" it means that no connection is established.
 */
static jstring org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getprotocol(JNIEnv* env, jobject object)
{
    SSL_CTX *ssl_ctx;
    SSL *ssl;
    SSL_SESSION *ssl_session;
    const char* protocol;
    jstring  result;

    ssl_session = getSslSessionPointer(env, object);

    ssl_ctx = SSL_CTX_new(SSLv23_client_method());
    ssl = SSL_new(ssl_ctx);

    SSL_set_session(ssl, ssl_session);

    protocol = SSL_get_version(ssl);

    result = env->NewStringUTF(protocol);

    SSL_free(ssl);
    SSL_CTX_free(ssl_ctx);

    return result;
}

/**
 * Gets and returns in a string the set of ciphers the actual SSL session uses.
 */
static jstring org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getciphersuite(JNIEnv* env, jobject object)
{
    SSL_CTX *ssl_ctx;
    SSL *ssl;
    SSL_SESSION *ssl_session;

    ssl_session = getSslSessionPointer(env, object);

    ssl_ctx = SSL_CTX_new(SSLv23_client_method());
    ssl = SSL_new(ssl_ctx);

    SSL_set_session(ssl, ssl_session);

    SSL_CIPHER *cipher = SSL_get_current_cipher(ssl);
    jstring result = env->NewStringUTF(SSL_CIPHER_get_name(cipher));

    SSL_free(ssl);
    SSL_CTX_free(ssl_ctx);
    return result;
}

/**
 * Frees the SSL session.
 */
static void org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_free(JNIEnv* env, jobject object, jint session)
{
    LOGD("Freeing OpenSSL session");
    SSL_SESSION* ssl_session;
    ssl_session = (SSL_SESSION*) session;
    SSL_SESSION_free(ssl_session);
}

/**
 * The actual JNI methods' mapping table for the class OpenSSLSessionImpl.
 */
static JNINativeMethod sMethods[] =
{
    {"nativegetid", "()[B", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getid},
    {"nativegetcreationtime", "()J", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getcreationtime},
    {"nativegetpeerhost", "()Ljava/lang/String;", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getpeerhost},
    {"nativegetpeerport", "()Ljava/lang/String;", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getpeerport},
    {"nativegetprotocol", "()Ljava/lang/String;", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getprotocol},
    {"nativegetciphersuite", "()Ljava/lang/String;", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getciphersuite},
    {"nativegetpeercertificates", "()[[B", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_getpeercertificates},
    {"nativefree", "(I)V", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_free},
    {"nativeserialize", "()[B", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_serialize},
    {"nativedeserialize", "([BI)I", (void*)org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl_deserialize}
};

/**
 * Register the native methods with JNI for the class OpenSSLSessionImpl.
 */
extern "C" int register_org_apache_harmony_xnet_provider_jsse_OpenSSLSessionImpl(JNIEnv* env)
{
    int ret;
    jclass clazz;

    clazz = env->FindClass("org/apache/harmony/xnet/provider/jsse/OpenSSLSessionImpl");

    if (clazz == NULL) {
        LOGE("Can't find org/apache/harmony/xnet/provider/jsse/OpenSSLSessionImpl");
        return -1;
    }

    field_session = env->GetFieldID(clazz, "session", "I");

    ret = jniRegisterNativeMethods(env, "org/apache/harmony/xnet/provider/jsse/OpenSSLSessionImpl",
            sMethods, NELEM(sMethods));

    return ret;
}
