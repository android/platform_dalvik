/*
 * Copyright (C) 2006 The Android Open Source Project
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

#define LOG_TAG "InetAddress"

#include "JNIHelp.h"
#include "utils/Log.h"
#include "jni.h"

#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <netdb.h>
#include <errno.h>

#include <cutils/properties.h>
#include <cutils/adb_networking.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/socket.h>


static jclass byteArrayClass = NULL;

static jstring InetAddress_gethostname(JNIEnv* env, jobject obj)
{
    char name[256];
    int r = gethostname(name, 256);
    if (r == 0) {
        return env->NewStringUTF(name);
    } else {
        return NULL;
    }
}

static void throwNullPointerException(JNIEnv* env)
{
    const char* className = "java/lang/NullPointerException";

    jclass exClass = env->FindClass(className);

    if (exClass == NULL) {
        LOGE("Unable to find class %s", className);
    } else {
        env->ThrowNew(exClass, NULL);
    }
}

static void logIpString(struct addrinfo* ai, const char* name)
{
    char ipString[INET6_ADDRSTRLEN];
    int result = getnameinfo(ai->ai_addr, ai->ai_addrlen, ipString,
                             sizeof(ipString), NULL, 0, NI_NUMERICHOST);
    if (result == 0) {
        LOGD("%s: %s (family %d, proto %d)", name, ipString, ai->ai_family,
             ai->ai_protocol);
    } else {
        LOGE("%s: getnameinfo: %s", name, gai_strerror(result));
    }
}

static jobjectArray getAllByNameUsingAdb(JNIEnv* env, const char* name)
{
    struct in_addr outaddr;
    jobjectArray addressArray = NULL;
    jbyteArray byteArray;

#if 0
    LOGI("ADB networking: -gethostbyname err %d addr 0x%08x %u.%u.%u.%u",
            err, (unsigned int)outaddr.a.s_addr,
            outaddr.j[0],outaddr.j[1],
            outaddr.j[2],outaddr.j[3]);
#endif

    if (adb_networking_gethostbyname(name, &outaddr) >= 0) {
        addressArray = env->NewObjectArray(1, byteArrayClass, NULL);
        byteArray = env->NewByteArray(4);
        if (addressArray && byteArray) {
            env->SetByteArrayRegion(byteArray, 0, 4, (jbyte*) &outaddr.s_addr);
            env->SetObjectArrayElement(addressArray, 1, byteArray);
        }
    }
    return addressArray;
}

static jobjectArray getAllByNameUsingDns(JNIEnv* env, const char* name, 
                                         jboolean preferIPv4Stack)
{
    struct addrinfo hints, *addressList = NULL, *addrInfo;
    jobjectArray addressArray = NULL;

    memset(&hints, 0, sizeof(hints));
    /*
     * IPv4 only for now until the socket code supports IPv6; otherwise, the
     * resolver will create two separate requests, one for IPv4 and one,
     * currently unnecessary, for IPv6.
     */
    hints.ai_family = AF_INET;
    /*
     * If we don't specify a socket type, every address will appear twice, once
     * for SOCK_STREAM and one for SOCK_DGRAM. Since we do not return the family
     * anyway, just pick one.
     */
    hints.ai_socktype = SOCK_STREAM;

    int result = getaddrinfo(name, NULL, &hints, &addressList);
    if (result == 0 && addressList) {
        // Count results so we know how to size the output array.
        int addressCount = 0;
        for (addrInfo = addressList; addrInfo; addrInfo = addrInfo->ai_next) {
            if (addrInfo->ai_family == AF_INET ||
                addrInfo->ai_family == AF_INET6) {
                addressCount++;
            }
        }

        // Prepare output array.
        addressArray = env->NewObjectArray(addressCount, byteArrayClass, NULL);
        if (addressArray == NULL) {
            // Appropriate exception will be thrown.
            LOGE("getAllByNameUsingDns: could not allocate output array");
            freeaddrinfo(addrInfo);
            return NULL;
        }

        // Examine returned addresses one by one, save them in the output array.
        int index = 0;
        for (addrInfo = addressList; addrInfo; addrInfo = addrInfo->ai_next) {
            struct sockaddr* address = addrInfo->ai_addr;
            size_t addressLength = 0;
            void* rawAddress;

            switch (addrInfo->ai_family) {
                // Find the raw address length and start pointer.
                case AF_INET6:
                    addressLength = 16;
                    rawAddress =
                        &((struct sockaddr_in6*) address)->sin6_addr.s6_addr;
                    logIpString(addrInfo, name);
                    break;
                case AF_INET:
                    addressLength = 4;
                    rawAddress =
                        &((struct sockaddr_in*) address)->sin_addr.s_addr;
                    logIpString(addrInfo, name);
                    break;
                default:
                    // Unknown address family. Skip this address.
                    LOGE("getAllByNameUsingDns: Unknown address family %d",
                         addrInfo->ai_family);
                    continue;
            }

            // Convert each IP address into a Java byte array.
            jbyteArray bytearray = env->NewByteArray(addressLength);
            if (bytearray == NULL) {
                // Out of memory error will be thrown on return.
                LOGE("getAllByNameUsingDns: Can't allocate %d-byte array",
                     addressLength);
                addressArray = NULL;
                break;
            }
            env->SetByteArrayRegion(bytearray, 0, addressLength,
                                    (jbyte*) rawAddress);
            env->SetObjectArrayElement(addressArray, index, bytearray);
            env->DeleteLocalRef(bytearray);
            index++;
        }
    } else if (result == EAI_SYSTEM && errno == EACCES) {
        /* No permission to use network */
        jniThrowException(
            env, "java/lang/SecurityException",
            "Permission denied (maybe missing INTERNET permission)");
    } else {
        // Do nothing. Return value will be null and the caller will throw an
        // UnknownHostExeption.
    }

    if (addressList) {
        freeaddrinfo(addressList);
    }

    return addressArray;
}

jobjectArray InetAddress_getallbyname(JNIEnv* env, jobject obj,
                                      jstring javaName,
                                      jboolean preferIPv4Stack)
{
    if (javaName == NULL) {
        throwNullPointerException(env);
        return NULL;
    }

    const char* name = env->GetStringUTFChars(javaName, NULL);
    jobjectArray out = NULL;

    char useAdbNetworkingProperty[PROPERTY_VALUE_MAX];
    char adbConnected[PROPERTY_VALUE_MAX];
    property_get("android.net.use-adb-networking",
            useAdbNetworkingProperty, "");
    property_get("adb.connected",
            adbConnected, "");

    // Any non-empty string value for use-adb-networking is considered "set"
    if ((strlen(useAdbNetworkingProperty) > 0)
            && (strlen(adbConnected) > 0) ) {
        out = getAllByNameUsingAdb(env, name);
    } else {
        out = getAllByNameUsingDns(env, name, preferIPv4Stack);
    }

    if (!out) {
        LOGI("Unknown host %s, throwing UnknownHostException", name);
        jniThrowException(env, "java/net/UnknownHostException", name);
    }
    env->ReleaseStringUTFChars(javaName, name);
    return out;
}


static jstring InetAddress_gethostbyaddr(JNIEnv* env, jobject obj,
                                         jbyteArray javaAddress)
{
    if (javaAddress == NULL) {
        throwNullPointerException(env);
        return NULL;
    }

    size_t addrlen = env->GetArrayLength(javaAddress);
    jbyte* rawAddress = env->GetByteArrayElements(javaAddress, NULL);
    if (rawAddress == NULL) {
        throwNullPointerException(env);
        return NULL;
    }

    // Convert the raw address bytes into a socket address structure.
    struct sockaddr_storage ss;
    struct sockaddr_in *sin = (struct sockaddr_in *) &ss;
    struct sockaddr_in6 *sin6 = (struct sockaddr_in6 *) &ss;
    size_t socklen;
    switch (addrlen) {
        case 4:
            socklen = sizeof(struct sockaddr_in);
            memset(sin, 0, sizeof(struct sockaddr_in));
            sin->sin_family = AF_INET;
            memcpy(&sin->sin_addr.s_addr, rawAddress, 4);
            break;
        case 16:
            socklen = sizeof(struct sockaddr_in6);
            memset(sin6, 0, sizeof(struct sockaddr_in6));
            sin6->sin6_family = AF_INET6;
            memcpy(&sin6->sin6_addr.s6_addr, rawAddress, 4);
            break;
        default:
            jniThrowException(env, "java/net/UnknownHostException",
                                   "Invalid address length");
            return NULL;
    }


    // Convert the socket address structure to an IP string for logging.
    int ret;
    char ipstr[INET6_ADDRSTRLEN];
    ret = getnameinfo((struct sockaddr *) &ss, socklen, ipstr, sizeof(ipstr),
                      NULL, 0, NI_NUMERICHOST);
    if (ret) {
        LOGE("gethostbyaddr: getnameinfo: %s", gai_strerror(ret));
        return NULL;
    }

    // Look up the IP address from the socket address structure.
    jstring result = NULL;
    char name[NI_MAXHOST];
    ret = getnameinfo((struct sockaddr *) &ss, socklen, name, sizeof(name),
                      NULL, 0, 0);
    if (ret == 0) {
        LOGI("gethostbyaddr: getnameinfo: %s = %s", ipstr, name);
        result = env->NewStringUTF(name);
    } else {
        LOGE("gethostbyaddr: getnameinfo: unknown host %s: %s", ipstr,
             gai_strerror(ret));
    }

    return result;
}

/*
 * JNI registration
 */
static JNINativeMethod gMethods[] = {
    /* name, signature, funcPtr */
    { "gethostbyaddr",    "([B)Ljava/lang/String;",
      (void*) InetAddress_gethostbyaddr },
    { "getallbyname",     "(Ljava/lang/String;Z)[[B",
      (void*) InetAddress_getallbyname },
    { "gethostname",      "()Ljava/lang/String;",
      (void*) InetAddress_gethostname  },
};

extern "C" int register_java_net_InetAddress(JNIEnv* env)
{
    jclass tempClass = env->FindClass("[B");
    if (tempClass) {
        byteArrayClass = (jclass) env->NewGlobalRef(tempClass);
    }
    if (!byteArrayClass) {
        LOGE("register_java_net_InetAddress: cannot allocate byte array class");
        return -1;
    }
    return jniRegisterNativeMethods(env, "java/net/InetAddress",
                gMethods, NELEM(gMethods));
}
