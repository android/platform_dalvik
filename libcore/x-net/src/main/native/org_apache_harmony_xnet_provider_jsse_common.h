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

#ifndef org_apache_harmony_xnet_provider_jsse_common_h
#define org_apache_harmony_xnet_provider_jsse_common_h

#include <openssl/err.h>
#include <openssl/rand.h>
#include <openssl/ssl.h>

#include <stdio.h>

/**
 * Structure to hold together useful JNI variables.
 */
typedef struct {
    JNIEnv* env;
    jobject object;
} mydata_t;

/**
 * Gives an array back containing all the X509 certificate's bytes.
 */
static jobjectArray getcertificatebytes(JNIEnv* env,
        const STACK_OF(X509) *chain)
{
    BUF_MEM *bptr;
    int count, i;
    jbyteArray bytes;
    jobjectArray joa;

    if (chain == NULL) {
        // Chain can be NULL if the associated cipher doesn't do certs.
        return NULL;
    }
    
    count = sk_X509_num(chain);

    if (count > 0) {
        joa = env->NewObjectArray(count, env->FindClass("[B"), NULL);

        if (joa == NULL) {
            return NULL;
        }

        BIO *bio = BIO_new(BIO_s_mem());

        // LOGD("Start fetching the certificates");
        for (i = 0; i < count; i++) {
            X509 *cert = sk_X509_value(chain, i);

            BIO_reset(bio);
            PEM_write_bio_X509(bio, cert);

            BIO_get_mem_ptr(bio, &bptr);
            bytes = env->NewByteArray(bptr->length);

            if (bytes == NULL) {
                /*
                 * Indicate an error by resetting joa to NULL. It will
                 * eventually get gc'ed.
                 */
                joa = NULL;
                break;
            } else {
                jbyte *tmp = env->GetByteArrayElements(bytes, NULL);
                memcpy(tmp, bptr->data, bptr->length);
                env->ReleaseByteArrayElements(bytes, tmp, 0);
                env->SetObjectArrayElement(joa, i, bytes);
            }
        }

        // LOGD("Certificate fetching complete");
        BIO_free(bio);
        return joa;
    } else {
        return NULL;
    }
}

/**
 * Verify the X509 certificate.
 */
static int verify_callback(int preverify_ok, X509_STORE_CTX *x509_store_ctx)
{
    SSL *ssl;
    mydata_t *mydata;
    jclass cls;

    jobjectArray objectArray;

    /* Get the correct index to the SSLobject stored into X509_STORE_CTX. */
    ssl = (SSL*)X509_STORE_CTX_get_ex_data(x509_store_ctx, SSL_get_ex_data_X509_STORE_CTX_idx());

    mydata = (mydata_t*)SSL_get_app_data(ssl);

    cls = mydata->env->GetObjectClass(mydata->object);

    jmethodID methodID = mydata->env->GetMethodID(cls, "verify_callback", "([[B)I");

    objectArray = getcertificatebytes(mydata->env, x509_store_ctx->untrusted);

    mydata->env->CallIntMethod(mydata->object, methodID, objectArray);

    return 1;
}

#endif
