/**
*******************************************************************************
* Copyright (C) 1996-2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*
*******************************************************************************
*/
/*
 *  @(#) icujniinterface.c	1.2 00/10/11
 *
 * (C) Copyright IBM Corp. 2000 - All Rights Reserved
 *  A JNI wrapper to ICU native converter Interface
 * @author: Ram Viswanadha
 */

#include "ConverterInterface.h"
#include "JNIHelp.h"
#include "AndroidSystemNatives.h"
#include "unicode/utypes.h"   /* Basic ICU data types */
#include "unicode/ucnv.h"     /* C   Converter API    */
#include "unicode/ustring.h"  /* some more string functions*/
#include "unicode/ucnv_cb.h"  /* for callback functions */
#include "unicode/uset.h"     /* for contains function */
#include "ErrorCode.h"
#include <stdlib.h>
#include <string.h>

// BEGIN android-removed
// #define UTF_16BE "UTF-16BE" 
// #define UTF_16 "UTF-16" 
// END android-removed
	 	  
/* Prototype of callback for substituting user settable sub chars */
void  JNI_TO_U_CALLBACK_SUBSTITUTE
 (const void *,UConverterToUnicodeArgs *,const char* ,int32_t ,UConverterCallbackReason ,UErrorCode * );

/**
 * Opens the ICU converter
 * @param env environment handle for JNI 
 * @param jClass handle for the class
 * @param handle buffer to recieve ICU's converter address
 * @param converterName name of the ICU converter
 */
static jlong openConverter (JNIEnv *env, jclass jClass, jstring converterName) {

    UConverter* conv=NULL;
    UErrorCode errorCode = U_ZERO_ERROR;

    const char* cnvName= (const char*) (*env)->GetStringUTFChars(env, converterName,NULL);
    if(cnvName) {
        int count = (*env)->GetStringUTFLength(env,converterName);

        conv = ucnv_open(cnvName,&errorCode);
    }
    (*env)->ReleaseStringUTFChars(env, converterName,cnvName);

    if (icu4jni_error(env, errorCode) != FALSE) {
        return 0;
    }

    return (jlong) conv;
}

/**
 * Closes the ICU converter
 * @param env environment handle for JNI 
 * @param jClass handle for the class
 * @param handle address of ICU converter
 */
static void closeConverter (JNIEnv *env, jclass jClass, jlong handle) {
     
    UConverter* cnv = (UConverter*)(long)handle;
    if(cnv) {
        // BEGIN android-added
        // Free up any contexts created in setCallback[Encode|Decode]()
        UConverterToUCallback toAction;
        UConverterFromUCallback fromAction;
        void * context1 = NULL;
        void * context2 = NULL;
        ucnv_getToUCallBack(cnv, &toAction, &context1);
        ucnv_getFromUCallBack(cnv, &fromAction, &context2);
        // END android-added
        ucnv_close(cnv);
        // BEGIN android-added
        if (context1 != NULL) {
            free(context1);
        }
        if (context2 != NULL) {
            free(context2);
        }
        // END android-added
    }
}

/**
 * Sets the substution mode for from Unicode conversion. Currently only 
 * two modes are supported: substitute or report
 * @param env environment handle for JNI 
 * @param jClass handle for the class
 * @param handle address of ICU converter
 * @param mode the mode to set 
 */
static jint setSubstitutionModeCharToByte (JNIEnv *env, jclass jClass, jlong handle, jboolean mode) {
    
    UConverter* conv = (UConverter*)(long)handle;
    UErrorCode errorCode =U_ZERO_ERROR;

    if(conv) {
        
        UConverterFromUCallback fromUOldAction ;
        void* fromUOldContext;
        void* fromUNewContext=NULL;
        if(mode) {

            ucnv_setFromUCallBack(conv,
               UCNV_FROM_U_CALLBACK_SUBSTITUTE,
               fromUNewContext,
               &fromUOldAction,
               (const void**)&fromUOldContext,
               &errorCode);

        }
        else{

            ucnv_setFromUCallBack(conv,
               UCNV_FROM_U_CALLBACK_STOP,
               fromUNewContext,
               &fromUOldAction,
               (const void**)&fromUOldContext,
               &errorCode);
         
        }
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}
/**
 * Sets the substution mode for to Unicode conversion. Currently only 
 * two modes are supported: substitute or report
 * @param env environment handle for JNI 
 * @param jClass handle for the class
 * @param handle address of ICU converter
 * @param mode the mode to set 
 */
static jint setSubstitutionModeByteToChar (JNIEnv *env, jclass jClass, jlong handle, jboolean mode) {
    
    UConverter* conv = (UConverter*)handle;
    UErrorCode errorCode =U_ZERO_ERROR;

    if(conv) {
        
        UConverterToUCallback toUOldAction ;
        void* toUOldContext;
        void* toUNewContext=NULL;
        if(mode) {

            ucnv_setToUCallBack(conv,
               UCNV_TO_U_CALLBACK_SUBSTITUTE,
               toUNewContext,
               &toUOldAction,
               (const void**)&toUOldContext,
               &errorCode);

        }
        else{

            ucnv_setToUCallBack(conv,
               UCNV_TO_U_CALLBACK_STOP,
               toUNewContext,
               &toUOldAction,
               (const void**)&toUOldContext,
               &errorCode);
         
        }
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}
/**
 * Converts a buffer of Unicode code units to target encoding 
 * @param env environment handle for JNI 
 * @param jClass handle for the class
 * @param handle address of ICU converter
 * @param source buffer of Unicode chars to convert 
 * @param sourceEnd limit of the source buffer
 * @param target buffer to recieve the converted bytes
 * @param targetEnd the limit of the target buffer
 * @param data buffer to recieve state of the current conversion
 * @param flush boolean that specifies end of source input
 */
static jint convertCharToByte(JNIEnv *env, jclass jClass, jlong handle,  jcharArray source,  jint sourceEnd, jbyteArray target, jint targetEnd, jintArray data, jboolean flush) {
    

    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        jint* myData = (jint*) (*env)->GetPrimitiveArrayCritical(env,data,NULL);
        if(myData) {
            jint* sourceOffset = &myData[0];
            jint* targetOffset = &myData[1];
            const jchar* uSource =(jchar*) (*env)->GetPrimitiveArrayCritical(env,source, NULL);
            if(uSource) {
                jbyte* uTarget=(jbyte*) (*env)->GetPrimitiveArrayCritical(env,target,NULL);
                if(uTarget) {
                    const jchar* mySource = uSource+ *sourceOffset;
                    const UChar* mySourceLimit= uSource+sourceEnd;
                    char* cTarget=uTarget+ *targetOffset;
                    const char* cTargetLimit=uTarget+targetEnd;
                    
                    ucnv_fromUnicode( cnv , &cTarget, cTargetLimit,&mySource,
                                    mySourceLimit,NULL,(UBool) flush, &errorCode);

                    *sourceOffset = (jint) (mySource - uSource)-*sourceOffset;
                    *targetOffset = (jint) ((jbyte*)cTarget - uTarget)- *targetOffset;
                    if(U_FAILURE(errorCode)) {
                        (*env)->ReleasePrimitiveArrayCritical(env,target,uTarget,0);
                        (*env)->ReleasePrimitiveArrayCritical(env,source,(jchar*)uSource,0);
                        (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
                        return errorCode;
                    }
                }else{
                    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
                }
                (*env)->ReleasePrimitiveArrayCritical(env,target,uTarget,0);
            }else{
                    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
            (*env)->ReleasePrimitiveArrayCritical(env,source,(jchar*)uSource,0); 
        }else{
                    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}

static jint encode(JNIEnv *env, jclass jClass, jlong handle, jcharArray source, jint sourceEnd, jbyteArray target, jint targetEnd, jintArray data, jboolean flush) {
   
    UErrorCode ec = convertCharToByte(env,jClass,handle,source,sourceEnd, target,targetEnd,data,flush);
    UConverter* cnv = (UConverter*)handle;
    jint* myData = (jint*) (*env)->GetPrimitiveArrayCritical(env,data,NULL);

    if(cnv && myData) {
        
       UErrorCode errorCode = U_ZERO_ERROR;
       myData[3] = ucnv_fromUCountPending(cnv, &errorCode);

       if(ec == U_ILLEGAL_CHAR_FOUND || ec == U_INVALID_CHAR_FOUND) {
            int8_t count =32;
            UChar invalidUChars[32];
            ucnv_getInvalidUChars(cnv,invalidUChars,&count,&errorCode);

            if(U_SUCCESS(errorCode)) {
                myData[2] = count;
            }	  
        }
    }
    (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
    return ec;
}

/**
 * Converts a buffer of encoded bytes to Unicode code units
 * @param env environment handle for JNI 
 * @param jClass handle for the class
 * @param handle address of ICU converter
 * @param source buffer of Unicode chars to convert 
 * @param sourceEnd limit of the source buffer
 * @param target buffer to recieve the converted bytes
 * @param targetEnd the limit of the target buffer
 * @param data buffer to recieve state of the current conversion
 * @param flush boolean that specifies end of source input
 */
static jint convertByteToChar(JNIEnv *env, jclass jClass, jlong handle, jbyteArray source, jint sourceEnd, jcharArray target, jint targetEnd, jintArray data, jboolean flush) {

    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        jint* myData = (jint*) (*env)->GetPrimitiveArrayCritical(env,data,NULL);
        if(myData) {
            jint* sourceOffset = &myData[0];
            jint* targetOffset = &myData[1];

            const jbyte* uSource =(jbyte*) (*env)->GetPrimitiveArrayCritical(env,source, NULL);
            if(uSource) {
                jchar* uTarget=(jchar*) (*env)->GetPrimitiveArrayCritical(env,target,NULL);
                if(uTarget) {
                    const jbyte* mySource = uSource+ *sourceOffset;
                    const char* mySourceLimit= uSource+sourceEnd;
                    UChar* cTarget=uTarget+ *targetOffset;
                    const UChar* cTargetLimit=uTarget+targetEnd;
                    
                    ucnv_toUnicode( cnv , &cTarget, cTargetLimit,(const char**)&mySource,
                                   mySourceLimit,NULL,(UBool) flush, &errorCode);
                
                    *sourceOffset = mySource - uSource - *sourceOffset  ;
                    *targetOffset = cTarget - uTarget - *targetOffset;
                    if(U_FAILURE(errorCode)) {
                        (*env)->ReleasePrimitiveArrayCritical(env,target,uTarget,0);
                        (*env)->ReleasePrimitiveArrayCritical(env,source,(jchar*)uSource,0);
                        (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
                        return errorCode;
                    }
                }else{
                    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
                }
                (*env)->ReleasePrimitiveArrayCritical(env,target,uTarget,0);
            }else{
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
            (*env)->ReleasePrimitiveArrayCritical(env,source,(jchar*)uSource,0); 
        }else{
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}

static jint decode(JNIEnv *env, jclass jClass, jlong handle, jbyteArray source, jint sourceEnd, jcharArray target, jint targetEnd, jintArray data, jboolean flush) {

    jint ec = convertByteToChar(env, jClass,handle,source,sourceEnd, target,targetEnd,data,flush);

    jint* myData = (jint*) (*env)->GetPrimitiveArrayCritical(env,data,NULL);
    UConverter* cnv = (UConverter*)handle;

    if(myData && cnv) {
        UErrorCode errorCode = U_ZERO_ERROR;
        myData[3] = ucnv_toUCountPending(cnv, &errorCode);

        if(ec == U_ILLEGAL_CHAR_FOUND || ec == U_INVALID_CHAR_FOUND ) {
            char invalidChars[32] = {'\0'};
            int8_t len = 32;
            ucnv_getInvalidChars(cnv,invalidChars,&len,&errorCode);
            
            if(U_SUCCESS(errorCode)) {
                myData[2] = len;
            }	  
        }
    }
    (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
    return ec;
}
static void resetByteToChar(JNIEnv *env, jclass jClass, jlong handle) {

    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        ucnv_resetToUnicode(cnv);
    }
}

static void resetCharToByte(JNIEnv *env, jclass jClass, jlong handle) {

    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        ucnv_resetFromUnicode(cnv);
    }

}

static jint countInvalidBytes (JNIEnv *env, jclass jClass, jlong handle, jintArray length) {

    UConverter* cnv = (UConverter*)handle;
    UErrorCode errorCode = U_ZERO_ERROR;
    if(cnv) {
        char invalidChars[32];

        jint* len = (jint*) (*env)->GetPrimitiveArrayCritical(env,length, NULL);
        if(len) {
            ucnv_getInvalidChars(cnv,invalidChars,(int8_t*)len,&errorCode);
        }
        (*env)->ReleasePrimitiveArrayCritical(env,length,(jint*)len,0);
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;

}


static jint countInvalidChars(JNIEnv *env, jclass jClass, jlong handle, jintArray length) {

    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    UChar invalidUChars[32];
    if(cnv) {
        jint* len = (jint*) (*env)->GetPrimitiveArrayCritical(env,length, NULL);
        if(len) {
            ucnv_getInvalidUChars(cnv,invalidUChars,(int8_t*)len,&errorCode);
        }
        (*env)->ReleasePrimitiveArrayCritical(env,length,(jint*)len,0);
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;

}

static jint getMaxBytesPerChar(JNIEnv *env, jclass jClass, jlong handle) {

    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        return (jint)ucnv_getMaxCharSize(cnv);
    }
    return -1;
}

static jint getMinBytesPerChar(JNIEnv *env, jclass jClass, jlong handle) {

    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        return (jint)ucnv_getMinCharSize(cnv);
    }
    return -1;
}
static jfloat getAveBytesPerChar(JNIEnv *env, jclass jClass, jlong handle) {

    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
         jfloat max = (jfloat)ucnv_getMaxCharSize(cnv);
         jfloat min = (jfloat)ucnv_getMinCharSize(cnv);
         return (jfloat) ( (max+min)/2 );
    }
    return -1;
}
static jint flushByteToChar(JNIEnv *env, jclass jClass,jlong handle, jcharArray target, jint targetEnd, jintArray data) {

    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        jbyte source ='\0';
        jint* myData = (jint*) (*env)->GetPrimitiveArrayCritical(env,data,NULL);
        if(myData) {
            jint* targetOffset = &myData[1];
            jchar* uTarget=(jchar*) (*env)->GetPrimitiveArrayCritical(env,target,NULL);
            if(uTarget) {
                const jbyte* mySource =&source;
                const char* mySourceLimit=&source;
                UChar* cTarget=uTarget+ *targetOffset;
                const UChar* cTargetLimit=uTarget+targetEnd;

                ucnv_toUnicode( cnv , &cTarget, cTargetLimit,(const char**)&mySource,
                               mySourceLimit,NULL,TRUE, &errorCode);


                *targetOffset = (jint) ((jchar*)cTarget - uTarget)- *targetOffset;
                if(U_FAILURE(errorCode)) {
                    (*env)->ReleasePrimitiveArrayCritical(env,target,uTarget,0);
                    (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
                    return errorCode;
                }
            }else{
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
            (*env)->ReleasePrimitiveArrayCritical(env,target,uTarget,0);

        }else{
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}

static jint flushCharToByte (JNIEnv *env, jclass jClass, jlong handle, jbyteArray target, jint targetEnd, jintArray data) {
          
    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    jchar source = '\0';
    if(cnv) {
        jint* myData = (jint*) (*env)->GetPrimitiveArrayCritical(env,data,NULL);
        if(myData) {
            jint* targetOffset = &myData[1];
            jbyte* uTarget=(jbyte*) (*env)->GetPrimitiveArrayCritical(env,target,NULL);
            if(uTarget) {
                const jchar* mySource = &source;
                const UChar* mySourceLimit= &source;
                char* cTarget=uTarget+ *targetOffset;
                const char* cTargetLimit=uTarget+targetEnd;

                ucnv_fromUnicode( cnv , &cTarget, cTargetLimit,&mySource,
                                  mySourceLimit,NULL,TRUE, &errorCode);
            

                *targetOffset = (jint) ((jbyte*)cTarget - uTarget)- *targetOffset;
                if(U_FAILURE(errorCode)) {
                    (*env)->ReleasePrimitiveArrayCritical(env,target,uTarget,0);
                
                    (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
                    return errorCode;
                }
            }else{
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
            (*env)->ReleasePrimitiveArrayCritical(env,target,uTarget,0);
        }else{
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        (*env)->ReleasePrimitiveArrayCritical(env,data,(jint*)myData,0);
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}

void toChars(const UChar* us, char* cs, int32_t length) {
    UChar u;
    while(length>0) {
        u=*us++;
        *cs++=(char)u;
        --length;
    }
}
static jint setSubstitutionBytes(JNIEnv *env, jclass jClass, jlong handle, jbyteArray subChars, jint length) {

    UConverter* cnv = (UConverter*) handle;
    UErrorCode errorCode = U_ZERO_ERROR;
    if(cnv) {
        jbyte* u_subChars = (*env)->GetPrimitiveArrayCritical(env,subChars,NULL);
        if(u_subChars) {
             char* mySubChars= (char*)malloc(sizeof(char)*length);
             toChars((UChar*)u_subChars,&mySubChars[0],length);
             ucnv_setSubstChars(cnv,mySubChars, (char)length,&errorCode);
             if(U_FAILURE(errorCode)) {
                (*env)->ReleasePrimitiveArrayCritical(env,subChars,mySubChars,0);
                return errorCode;
             }
             free(mySubChars);
        }
        else{   
           errorCode =  U_ILLEGAL_ARGUMENT_ERROR;
        }
        (*env)->ReleasePrimitiveArrayCritical(env,subChars,u_subChars,0); 
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}


#define VALUE_STRING_LENGTH 32

typedef struct{
    int length;
    UChar subChars[256];
    UBool stopOnIllegal;
}SubCharStruct;


static UErrorCode 
setToUCallbackSubs(UConverter* cnv,UChar* subChars, int32_t length,UBool stopOnIllegal ) {
    SubCharStruct* substitutionCharS = (SubCharStruct*) malloc(sizeof(SubCharStruct));
    UErrorCode errorCode = U_ZERO_ERROR;
    if(substitutionCharS) {
       UConverterToUCallback toUOldAction;
       void* toUOldContext=NULL;
       void* toUNewContext=NULL ;
       if(subChars) {
            u_strncpy(substitutionCharS->subChars,subChars,length);
       }else{
           substitutionCharS->subChars[length++] =0xFFFD;
       }
       substitutionCharS->subChars[length]=0;
       substitutionCharS->length = length;
       substitutionCharS->stopOnIllegal = stopOnIllegal;
       toUNewContext = substitutionCharS;

       ucnv_setToUCallBack(cnv,
           JNI_TO_U_CALLBACK_SUBSTITUTE,
           toUNewContext,
           &toUOldAction,
           (const void**)&toUOldContext,
           &errorCode);

       if(toUOldContext) {
           SubCharStruct* temp = (SubCharStruct*) toUOldContext;
           free(temp);
       }

       return errorCode;
    }
    return U_MEMORY_ALLOCATION_ERROR;
}
static jint setSubstitutionChars(JNIEnv *env, jclass jClass, jlong handle, jcharArray subChars, jint length) {

    UErrorCode errorCode = U_ZERO_ERROR;
    UConverter* cnv = (UConverter*) handle;
    jchar* u_subChars=NULL;
    if(cnv) {
        if(subChars) {
            int len = (*env)->GetArrayLength(env,subChars);
            u_subChars = (*env)->GetPrimitiveArrayCritical(env,subChars,NULL);
            if(u_subChars) {
               errorCode =  setToUCallbackSubs(cnv,u_subChars,len,FALSE);
            }else{
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
            (*env)->ReleasePrimitiveArrayCritical(env,subChars,u_subChars,0);
            return errorCode;
        }
    }
    return U_ILLEGAL_ARGUMENT_ERROR;
}


void  JNI_TO_U_CALLBACK_SUBSTITUTE( const void *context, UConverterToUnicodeArgs *toArgs, const char* codeUnits, int32_t length, UConverterCallbackReason reason, UErrorCode * err) {

    if(context) {
        SubCharStruct* temp = (SubCharStruct*)context;
        if( temp) {
            if(temp->stopOnIllegal==FALSE) {
                if (reason > UCNV_IRREGULAR) {
                    return;
                }
                /* reset the error */
                *err = U_ZERO_ERROR;
                ucnv_cbToUWriteUChars(toArgs,temp->subChars ,temp->length , 0, err);
            }else{
                if(reason != UCNV_UNASSIGNED) {
                    /* the caller must have set 
                     * the error code accordingly
                     */
                    return;
                }else{
                    *err = U_ZERO_ERROR;
                    ucnv_cbToUWriteUChars(toArgs,temp->subChars ,temp->length , 0, err);
                    return;
                }
            }
        }
    }
    return;
}

static jboolean canEncode(JNIEnv *env, jclass jClass, jlong handle, jint codeUnit) {
    
    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        UChar source[3];
        UChar *mySource=source;
        const UChar* sourceLimit = (codeUnit<0x010000) ? &source[1] : &source[2];
        char target[5];
        char *myTarget = target;
        const char* targetLimit = &target[4];
        int i=0;
        UTF_APPEND_CHAR(&source[0],i,2,codeUnit);

        ucnv_fromUnicode(cnv,&myTarget,targetLimit, 
                         (const UChar**)&mySource, 
                         sourceLimit,NULL, TRUE,&errorCode);

        if(U_SUCCESS(errorCode)) {
            return (jboolean)TRUE;
        }
    }
    return (jboolean)FALSE;
}


static jboolean canDecode(JNIEnv *env, jclass jClass, jlong handle, jbyteArray source) {
    
    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        jint len = (*env)->GetArrayLength(env,source);    
        jbyte* cSource =(jbyte*) (*env)->GetPrimitiveArrayCritical(env,source, NULL);
        if(cSource) {
            const jbyte* cSourceLimit = cSource+len;

            /* Assume that we need at most twice the length of source */
            UChar* target = (UChar*) malloc(sizeof(UChar)* (len<<1));
            UChar* targetLimit = target + (len<<1);
            if(target) {
                ucnv_toUnicode(cnv,&target,targetLimit, 
                               (const char**)&cSource, 
                               cSourceLimit,NULL, TRUE,&errorCode);

                if(U_SUCCESS(errorCode)) {
                    free(target);
                    (*env)->ReleasePrimitiveArrayCritical(env,source,cSource,0);        
                    return (jboolean)TRUE;
                }
            }
            free(target);
        }
        (*env)->ReleasePrimitiveArrayCritical(env,source,cSource,0);        
    }
    return (jboolean)FALSE;
}

static jint countAvailable(JNIEnv *env, jclass jClass) {
    return ucnv_countAvailable();
}

int32_t copyString(char* dest, int32_t destCapacity, int32_t startIndex,
           const char* src, UErrorCode* status) {
    int32_t srcLen = 0, i=0;
    if(U_FAILURE(*status)) {
        return 0;
    }
    if(dest == NULL || src == NULL || destCapacity < startIndex) { 
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    srcLen = strlen(src);
    if(srcLen >= destCapacity) {
        *status = U_BUFFER_OVERFLOW_ERROR;
        return 0;
    }
    for(i=0; i < srcLen; i++) {
        dest[startIndex++] = src[i];
    }
    /* null terminate the buffer */
    dest[startIndex] = 0; /* no bounds check already made sure that we have enough room */
    return startIndex;
}

int32_t getJavaCanonicalName1(const char* icuCanonicalName, 
                     char* canonicalName, int32_t capacity, 
                     UErrorCode* status) {
 /*
 If a charset listed in the IANA Charset Registry is supported by an implementation 
 of the Java platform then its canonical name must be the name listed in the registry. 
 Many charsets are given more than one name in the registry, in which case the registry 
 identifies one of the names as MIME-preferred. If a charset has more than one registry 
 name then its canonical name must be the MIME-preferred name and the other names in 
 the registry must be valid aliases. If a supported charset is not listed in the IANA 
 registry then its canonical name must begin with one of the strings "X-" or "x-".
 */
    int32_t retLen = 0;
    const char* cName = NULL;
    /* find out the alias with MIME tag */
    if((cName =ucnv_getStandardName(icuCanonicalName, "MIME", status)) !=  NULL) {
        retLen = copyString(canonicalName, capacity, 0, cName, status);
        /* find out the alias with IANA tag */
    }else if((cName =ucnv_getStandardName(icuCanonicalName, "IANA", status)) !=  NULL) {
        retLen = copyString(canonicalName, capacity, 0, cName, status);
    }else {
        /*  
            check to see if an alias already exists with x- prefix, if yes then 
            make that the canonical name
        */
        int32_t aliasNum = ucnv_countAliases(icuCanonicalName,status);
        int32_t i=0;
        const char* name;
        for(i=0;i<aliasNum;i++) {
            name = ucnv_getAlias(icuCanonicalName,(uint16_t)i, status);
            if(name != NULL && name[0]=='x' && name[1]=='-') {
                retLen = copyString(canonicalName, capacity, 0, name, status);
                break;
            }
        }
        /* last resort just append x- to any of the alias and 
            make it the canonical name */
        if(retLen == 0 && U_SUCCESS(*status)) {
            name = ucnv_getStandardName(icuCanonicalName, "UTR22", status);
            if(name == NULL && strchr(icuCanonicalName, ',')!= NULL) {
                name = ucnv_getAlias(icuCanonicalName, 1, status);
                if(*status == U_INDEX_OUTOFBOUNDS_ERROR) {
                    *status = U_ZERO_ERROR;
                }
            }
            /* if there is no UTR22 canonical name .. then just return itself*/
            if(name == NULL) {                
                name = icuCanonicalName;
            }
            if(capacity >= 2) {
                strcpy(canonicalName,"x-");
            }
            retLen = copyString(canonicalName, capacity, 2, name, status);
        }
    }
    return retLen;
}

static jobjectArray getAvailable(JNIEnv *env, jclass jClass) {
   
    jobjectArray ret;
    int32_t i = 0;
    int32_t num = ucnv_countAvailable();
    UErrorCode error = U_ZERO_ERROR;
    const char* name =NULL;
    char canonicalName[256]={0};
    ret= (jobjectArray)(*env)->NewObjectArray( env,num,
                                               (*env)->FindClass(env,"java/lang/String"),
                                               (*env)->NewStringUTF(env,""));

    for(i=0;i<num;i++) {
        name = ucnv_getAvailableName(i);
        getJavaCanonicalName1(name, canonicalName, 256, &error);   
#if DEBUG
        if(U_FAILURE(error)) {
            printf("An error occurred retrieving index %i. Error: %s. \n", i, u_errorName(error));
        }

        printf("canonical name for %s\n", canonicalName);
#endif
        // BEGIN android-changed
        jstring canonName = (*env)->NewStringUTF(env,canonicalName);
        (*env)->SetObjectArrayElement(env,ret,i,canonName);
        (*env)->DeleteLocalRef(env, canonName);
        // END android-changed
        /*printf("canonical name : %s  at %i\n", name,i); */
        canonicalName[0]='\0';/* nul terminate */
    }
    return (ret);
}

static jint countAliases(JNIEnv *env, jclass jClass,jstring enc) {
    
    UErrorCode error = U_ZERO_ERROR;
    jint num =0;
    const char* encName = (*env)->GetStringUTFChars(env,enc,NULL);
    
    if(encName) {
        num = ucnv_countAliases(encName,&error);
    }
    
    (*env)->ReleaseStringUTFChars(env,enc,encName);

    return num;
}


static jobjectArray getAliases(JNIEnv *env, jclass jClass, jstring enc) {

    jobjectArray ret=NULL;
    int32_t aliasNum = 0;
    UErrorCode error = U_ZERO_ERROR;
    const char* encName = (*env)->GetStringUTFChars(env,enc,NULL);
    int i=0;
    int j=0;
    const char* aliasArray[50];
    // BEGIN android-removed
    // int32_t utf16AliasNum = 0; 
    // END android-removed

    
    if(encName) {
        const char* myEncName = encName;
        aliasNum = ucnv_countAliases(myEncName,&error);

        // BEGIN android-removed
        // /* special case for UTF-16. In java UTF-16 is always BE*/ 
        // if(strcmp(myEncName, UTF_16BE)==0) { 
        //     utf16AliasNum=ucnv_countAliases(UTF_16,&error); 
        // }
        // END android-removed

        if(aliasNum==0 && encName[0] == 0x78 /*x*/ && encName[1]== 0x2d /*-*/) {
            myEncName = encName+2;
            aliasNum = ucnv_countAliases(myEncName,&error);
        }
        if(U_SUCCESS(error)) {
            for(i=0,j=0;i<aliasNum;i++) {
                const char* name = ucnv_getAlias(myEncName,(uint16_t)i,&error);
                if(strchr(name,'+')==0 && strchr(name,',')==0) {
                    aliasArray[j++]= name;
                }
            }

            // BEGIN android-removed
            // if(utf16AliasNum>0) {
            //     for(i=0;i<utf16AliasNum;i++) {
            //         const char* name = ucnv_getAlias(UTF_16,(uint16_t)i,&error);
            //         if(strchr(name,'+')==0 && strchr(name,',')==0) {
            //             aliasArray[j++]= name;
            //         }
            //     }
            // }
            // END android-removed

            ret =  (jobjectArray)(*env)->NewObjectArray(env,j,
                                                        (*env)->FindClass(env,"java/lang/String"),
                                                        (*env)->NewStringUTF(env,""));
            for(;--j>=0;) {
                 // BEGIN android-changed
                 jstring alias = (*env)->NewStringUTF(env, aliasArray[j]);
                 (*env)->SetObjectArrayElement(env, ret, j, alias);
                 (*env)->DeleteLocalRef(env, alias);
                 // END android-changed
            }
        }            
    }
    (*env)->ReleaseStringUTFChars(env,enc,encName);

    return (ret);
}

static jstring getCanonicalName(JNIEnv *env, jclass jClass,jstring enc) {

    UErrorCode error = U_ZERO_ERROR;
    const char* encName = (*env)->GetStringUTFChars(env,enc,NULL);
    const char* canonicalName = "";
    // BEGIN android-changed
    jstring ret = NULL;
    if(encName) {
        canonicalName = ucnv_getAlias(encName,0,&error);
        if(canonicalName !=NULL && strstr(canonicalName,",")!=0) {
            canonicalName = ucnv_getAlias(canonicalName,1,&error);
        }
        ret = ((*env)->NewStringUTF(env, canonicalName));
        (*env)->ReleaseStringUTFChars(env,enc,encName);
    }
    // END android-changed
    return ret;
}

static jstring getICUCanonicalName(JNIEnv *env, jclass jClass, jstring enc) {

    UErrorCode error = U_ZERO_ERROR;
    const char* encName = (*env)->GetStringUTFChars(env,enc,NULL);
    const char* canonicalName = NULL;
    jstring ret = NULL;
    if(encName) {
        // BEGIN android-removed
        // if(strcmp(encName,"UTF-16")==0) {
        //     ret = ((*env)->NewStringUTF(env,UTF_16BE));
        // }else
        // END android-removed
        if((canonicalName = ucnv_getCanonicalName(encName, "MIME", &error))!=NULL) {
            ret = ((*env)->NewStringUTF(env, canonicalName));
        }else if((canonicalName = ucnv_getCanonicalName(encName, "IANA", &error))!=NULL) {
            ret = ((*env)->NewStringUTF(env, canonicalName));
        }else if((canonicalName = ucnv_getCanonicalName(encName, "", &error))!=NULL) {
            ret = ((*env)->NewStringUTF(env, canonicalName));
        }else if((canonicalName =  ucnv_getAlias(encName, 0, &error)) != NULL) {
            /* we have some aliases in the form x-blah .. match those first */
            ret = ((*env)->NewStringUTF(env, canonicalName));
        }else if( ret ==NULL && strstr(encName, "x-") == encName) {
            /* check if the converter can be opened with the encName given */
            UConverter* conv = NULL;
            error = U_ZERO_ERROR;
            conv = ucnv_open(encName+2, &error);
            if(conv!=NULL) {
                ret = ((*env)->NewStringUTF(env, encName+2));
            }else{
                /* unsupported encoding */
                ret = ((*env)->NewStringUTF(env, ""));
            }
            ucnv_close(conv);
        }else{
            /* unsupported encoding */
           ret = ((*env)->NewStringUTF(env, ""));
        }
    }
    (*env)->ReleaseStringUTFChars(env,enc,encName);
    return ret;
}

static jstring getJavaCanonicalName2(JNIEnv *env, jclass jClass, jstring icuCanonName) {
 /*
 If a charset listed in the IANA Charset Registry is supported by an implementation 
 of the Java platform then its canonical name must be the name listed in the registry. 
 Many charsets are given more than one name in the registry, in which case the registry 
 identifies one of the names as MIME-preferred. If a charset has more than one registry 
 name then its canonical name must be the MIME-preferred name and the other names in 
 the registry must be valid aliases. If a supported charset is not listed in the IANA 
 registry then its canonical name must begin with one of the strings "X-" or "x-".
 */
    UErrorCode error = U_ZERO_ERROR;
    const char* icuCanonicalName = (*env)->GetStringUTFChars(env,icuCanonName,NULL);
    char cName[UCNV_MAX_CONVERTER_NAME_LENGTH] = {0};
    jstring ret;
    if(icuCanonicalName && icuCanonicalName[0] != 0) {
        getJavaCanonicalName1(icuCanonicalName, cName, UCNV_MAX_CONVERTER_NAME_LENGTH, &error);
    }
    ret = ((*env)->NewStringUTF(env, cName));
    (*env)->ReleaseStringUTFChars(env,icuCanonName,icuCanonicalName);
    return ret;
}

#define SUBS_ARRAY_CAPACITY 256
typedef struct{
    int length;
    char subChars[SUBS_ARRAY_CAPACITY];
    UConverterFromUCallback onUnmappableInput;
    UConverterFromUCallback onMalformedInput;
}EncoderCallbackContext;

void CHARSET_ENCODER_CALLBACK(const void *context,
                  UConverterFromUnicodeArgs *fromArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
                  UErrorCode * status) {   
    if(context) {
        EncoderCallbackContext* ctx = (EncoderCallbackContext*)context;
        
        if(ctx) {
            UConverterFromUCallback realCB = NULL;
            switch(reason) {
                case UCNV_UNASSIGNED:
                    realCB = ctx->onUnmappableInput;
                    break;
                case UCNV_ILLEGAL:/*malformed input*/
                case UCNV_IRREGULAR:/*malformed input*/
                    realCB = ctx->onMalformedInput;
                    break;
                /*
                case UCNV_RESET:
                    ucnv_resetToUnicode(args->converter);
                    break;
                case UCNV_CLOSE:
                    ucnv_close(args->converter);
                    break;
                case UCNV_CLONE:
                    ucnv_clone(args->clone);
               */
                default:
                    *status = U_ILLEGAL_ARGUMENT_ERROR;
                    return;
            }
            if(realCB==NULL) {
                *status = U_INTERNAL_PROGRAM_ERROR;
            }
            realCB(context, fromArgs, codeUnits, length, codePoint, reason, status);
        }
    }      
}

void JNI_FROM_U_CALLBACK_SUBSTITUTE_ENCODER(const void *context,
                                        UConverterFromUnicodeArgs *fromArgs,
                                        const UChar* codeUnits,
                                        int32_t length,
                                        UChar32 codePoint,
                                        UConverterCallbackReason reason,
                                        UErrorCode * err) {
    if(context) {
        EncoderCallbackContext* temp = (EncoderCallbackContext*)context;
        *err = U_ZERO_ERROR;
        ucnv_cbFromUWriteBytes(fromArgs,temp->subChars ,temp->length , 0, err);
    }
    return;
}

UConverterFromUCallback getFromUCallback(int32_t mode) {
    switch(mode) {
        default: /* falls through */
        case com_ibm_icu4jni_converters_NativeConverter_STOP_CALLBACK:
            return UCNV_FROM_U_CALLBACK_STOP;
        case com_ibm_icu4jni_converters_NativeConverter_SKIP_CALLBACK:
            return UCNV_FROM_U_CALLBACK_SKIP ;
        case com_ibm_icu4jni_converters_NativeConverter_SUBSTITUTE_CALLBACK:
            return JNI_FROM_U_CALLBACK_SUBSTITUTE_ENCODER;
    }
}

static jint setCallbackEncode(JNIEnv *env, jclass jClass, jlong handle, jint onMalformedInput, jint onUnmappableInput, jbyteArray subChars, jint length) {

    UConverter* conv = (UConverter*)handle;
    UErrorCode errorCode =U_ZERO_ERROR;

    if(conv) {
        
        UConverterFromUCallback fromUOldAction = NULL;
        void* fromUOldContext = NULL;
        EncoderCallbackContext* fromUNewContext=NULL;
        UConverterFromUCallback fromUNewAction=NULL;
        jbyte* sub = (jbyte*) (*env)->GetPrimitiveArrayCritical(env,subChars, NULL);
        ucnv_getFromUCallBack(conv, &fromUOldAction, &fromUOldContext);

        /* fromUOldContext can only be DecodeCallbackContext since
           the converter created is private data for the decoder
           and callbacks can only be set via this method!
        */
        if(fromUOldContext==NULL) {
            fromUNewContext = (EncoderCallbackContext*) malloc(sizeof(EncoderCallbackContext));
            fromUNewAction = CHARSET_ENCODER_CALLBACK;
        }else{
            fromUNewContext = fromUOldContext;
            fromUNewAction = fromUOldAction;
            fromUOldAction = NULL;
            fromUOldContext = NULL;
        }
        fromUNewContext->onMalformedInput = getFromUCallback(onMalformedInput);
        fromUNewContext->onUnmappableInput = getFromUCallback(onUnmappableInput);
        // BEGIN android-changed
        if(sub!=NULL) {
            fromUNewContext->length = length;
            strncpy(fromUNewContext->subChars, sub, length);
            (*env)->ReleasePrimitiveArrayCritical(env,subChars, sub, 0);
        }else{
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        // END android-changed

        ucnv_setFromUCallBack(conv,
           fromUNewAction,
           fromUNewContext,
           &fromUOldAction,
           (const void**)&fromUOldContext,
           &errorCode);


        return errorCode;
    }
    return U_ILLEGAL_ARGUMENT_ERROR;
}
                                                                  
typedef struct{
    int length;
    UChar subUChars[256];
    UConverterToUCallback onUnmappableInput;
    UConverterToUCallback onMalformedInput;
}DecoderCallbackContext;

void JNI_TO_U_CALLBACK_SUBSTITUTE_DECODER(const void *context,
                                    UConverterToUnicodeArgs *toArgs,
                                    const char* codeUnits,
                                    int32_t length,
                                    UConverterCallbackReason reason,
                                    UErrorCode * err) {
    if(context) {
        DecoderCallbackContext* temp = (DecoderCallbackContext*)context;
        *err = U_ZERO_ERROR;
        ucnv_cbToUWriteUChars(toArgs,temp->subUChars ,temp->length , 0, err);
    }
    return;
}

UConverterToUCallback getToUCallback(int32_t mode) {
    switch(mode) {
        default: /* falls through */
        case com_ibm_icu4jni_converters_NativeConverter_STOP_CALLBACK:
            return UCNV_TO_U_CALLBACK_STOP;
        case com_ibm_icu4jni_converters_NativeConverter_SKIP_CALLBACK:
            return UCNV_TO_U_CALLBACK_SKIP ;
        case com_ibm_icu4jni_converters_NativeConverter_SUBSTITUTE_CALLBACK:
            return JNI_TO_U_CALLBACK_SUBSTITUTE_DECODER;
    }
}

void  CHARSET_DECODER_CALLBACK(const void *context,
                               UConverterToUnicodeArgs *args, 
                               const char* codeUnits, 
                               int32_t length,
                               UConverterCallbackReason reason,
                               UErrorCode *status ) {
   
    if(context) {
        DecoderCallbackContext* ctx = (DecoderCallbackContext*)context;
        
        if(ctx) {
            UConverterToUCallback realCB = NULL;
            switch(reason) {
                case UCNV_UNASSIGNED:
                    realCB = ctx->onUnmappableInput;
                    break;
                case UCNV_ILLEGAL:/*malformed input*/
                case UCNV_IRREGULAR:/*malformed input*/
                    realCB = ctx->onMalformedInput;
                    break;
                /*
                case UCNV_RESET:
                    ucnv_resetToUnicode(args->converter);
                    break;
                case UCNV_CLOSE:
                    ucnv_close(args->converter);
                    break;
                case UCNV_CLONE:
                    ucnv_clone(args->clone);
               */
                default:
                    *status = U_ILLEGAL_ARGUMENT_ERROR;
                    return;
            }
            if(realCB==NULL) {
                *status = U_INTERNAL_PROGRAM_ERROR;
            }
            realCB(context, args, codeUnits, length, reason, status);
        }
    }      
}

static jint setCallbackDecode(JNIEnv *env, jclass jClass, jlong handle, jint onMalformedInput, jint onUnmappableInput, jcharArray subChars, jint length) {
    
    UConverter* conv = (UConverter*)handle;
    UErrorCode errorCode =U_ZERO_ERROR;
    if(conv) {
        
        UConverterToUCallback toUOldAction ;
        void* toUOldContext;
        DecoderCallbackContext* toUNewContext = NULL;
        UConverterToUCallback toUNewAction = NULL;
        jchar* sub = (jchar*) (*env)->GetPrimitiveArrayCritical(env,subChars, NULL);
    
        ucnv_getToUCallBack(conv, &toUOldAction, &toUOldContext);

        /* toUOldContext can only be DecodeCallbackContext since
           the converter created is private data for the decoder
           and callbacks can only be set via this method!
        */
        if(toUOldContext==NULL) {
            toUNewContext = (DecoderCallbackContext*) malloc(sizeof(DecoderCallbackContext));
            toUNewAction = CHARSET_DECODER_CALLBACK;
        }else{
            toUNewContext = toUOldContext;
            toUNewAction = toUOldAction;
            toUOldAction = NULL;
            toUOldContext = NULL;
        }
        toUNewContext->onMalformedInput = getToUCallback(onMalformedInput);
        toUNewContext->onUnmappableInput = getToUCallback(onUnmappableInput);
        // BEGIN android-changed
        if(sub!=NULL) {
            toUNewContext->length = length;
            u_strncpy(toUNewContext->subUChars, sub, length);
            (*env)->ReleasePrimitiveArrayCritical(env,subChars, sub, 0);
        }else{
            errorCode =  U_ILLEGAL_ARGUMENT_ERROR;
        }
        // END android-changed
        ucnv_setToUCallBack(conv,
           toUNewAction,
           toUNewContext,
           &toUOldAction,
           (const void**)&toUOldContext,
           &errorCode);

        return errorCode;
    }
    return U_ILLEGAL_ARGUMENT_ERROR;
}

static jlong safeClone(JNIEnv *env, jclass jClass, jlong src) {

    UErrorCode status = U_ZERO_ERROR;

    jint buffersize = U_CNV_SAFECLONE_BUFFERSIZE;

    UConverter* conv=NULL;
    UErrorCode errorCode = U_ZERO_ERROR;
    UConverter* source = (UConverter*) src;

    if(source) {
        conv = ucnv_safeClone(source, NULL, &buffersize, &errorCode);
    }

    if (icu4jni_error(env, errorCode) != FALSE) {
        return NULL;
    }

    return conv;
}

static jint getMaxCharsPerByte(JNIEnv *env, jclass jClass, jlong handle) {
    /*
     * currently we know that max number of chars per byte is 2
     */
    return 2;
}

static jfloat getAveCharsPerByte(JNIEnv *env, jclass jClass, jlong handle) {
    jfloat ret = 0;
    ret = (jfloat)( 1/(jfloat)getMaxBytesPerChar(env, jClass, handle));
    return ret;
}

void toUChars(const char* cs, UChar* us, int32_t length) {
    char c;
    while(length>0) {
        c=*cs++;
        *us++=(char)c;
        --length;
    }
}

static jbyteArray getSubstitutionBytes(JNIEnv *env, jclass jClass, jlong handle) {

    const UConverter * cnv = (const UConverter *) handle;
    UErrorCode status = U_ZERO_ERROR;
    char subBytes[10];
    int8_t len =(char)10;
    jbyteArray arr;
    if(cnv) {
        ucnv_getSubstChars(cnv,subBytes,&len,&status);
        if(U_SUCCESS(status)) {
            arr = ((*env)->NewByteArray(env, len));
            if(arr) {
                (*env)->SetByteArrayRegion(env,arr,0,len,(jbyte*)subBytes);
            }
            return arr;
        }
    }
    return ((*env)->NewByteArray(env, 0));
}

static jboolean contains( JNIEnv *env, jclass jClass, jlong handle1, jlong handle2) {
    UErrorCode status = U_ZERO_ERROR;
    const UConverter * cnv1 = (const UConverter *) handle1;
    const UConverter * cnv2 = (const UConverter *) handle2;
    USet* set1;
    USet* set2;
    UBool bRet = 0;
    
    if(cnv1 != NULL && cnv2 != NULL) {
	    /* open charset 1 */
        set1 = uset_open(1, 2);
        ucnv_getUnicodeSet(cnv1, set1, UCNV_ROUNDTRIP_SET, &status);

        if(U_SUCCESS(status)) {
            /* open charset 2 */
            status = U_ZERO_ERROR;
            set2 = uset_open(1, 2);
            ucnv_getUnicodeSet(cnv2, set2, UCNV_ROUNDTRIP_SET, &status);

            /* contains?      */
            if(U_SUCCESS(status)) {
                bRet = uset_containsAll(set1, set2);
	            uset_close(set2);
            }
            uset_close(set1);
        }
    }
	return bRet;
}

/*
 * JNI registration
 */
static JNINativeMethod gMethods[] = {
    /* name, signature, funcPtr */
    { "convertByteToChar", "(J[BI[CI[IZ)I", (void*) convertByteToChar },
    { "decode", "(J[BI[CI[IZ)I", (void*) decode },
    { "convertCharToByte", "(J[CI[BI[IZ)I", (void*) convertCharToByte },
    { "encode", "(J[CI[BI[IZ)I", (void*) encode },
    { "flushCharToByte", "(J[BI[I)I", (void*) flushCharToByte },
    { "flushByteToChar", "(J[CI[I)I", (void*) flushByteToChar },
    { "openConverter", "(Ljava/lang/String;)J", (void*) openConverter },
    { "resetByteToChar", "(J)V", (void*) resetByteToChar },
    { "resetCharToByte", "(J)V", (void*) resetCharToByte },
    { "closeConverter", "(J)V", (void*) closeConverter },
    { "setSubstitutionChars", "(J[CI)I", (void*) setSubstitutionChars },
    { "setSubstitutionBytes", "(J[BI)I", (void*) setSubstitutionBytes },
    { "setSubstitutionModeCharToByte", "(JZ)I", (void*) setSubstitutionModeCharToByte },
    { "setSubstitutionModeByteToChar", "(JZ)I", (void*) setSubstitutionModeByteToChar },
    { "countInvalidBytes", "(J[I)I", (void*) countInvalidBytes },
    { "countInvalidChars", "(J[I)I", (void*) countInvalidChars },
    { "getMaxBytesPerChar", "(J)I", (void*) getMaxBytesPerChar },
    { "getMinBytesPerChar", "(J)I", (void*) getMinBytesPerChar },
    { "getAveBytesPerChar", "(J)F", (void*) getAveBytesPerChar },
    { "getMaxCharsPerByte", "(J)I", (void*) getMaxCharsPerByte },
    { "getAveCharsPerByte", "(J)F", (void*) getAveCharsPerByte },
    { "contains", "(JJ)Z", (void*) contains },
    { "getSubstitutionBytes", "(J)[B", (void*) getSubstitutionBytes },
    { "canEncode", "(JI)Z", (void*) canEncode },
    { "canDecode", "(J[B)Z", (void*) canDecode },
    { "countAvailable", "()I", (void*) countAvailable },
    { "getAvailable", "()[Ljava/lang/String;", (void*) getAvailable },
    { "countAliases", "(Ljava/lang/String;)I", (void*) countAliases },
    { "getAliases", "(Ljava/lang/String;)[Ljava/lang/String;", (void*) getAliases },
    { "getCanonicalName", "(Ljava/lang/String;)Ljava/lang/String;", (void*) getCanonicalName },
    { "getICUCanonicalName", "(Ljava/lang/String;)Ljava/lang/String;", (void*) getICUCanonicalName },
    { "getJavaCanonicalName", "(Ljava/lang/String;)Ljava/lang/String;", (void*) getJavaCanonicalName2 },
    { "setCallbackDecode", "(JII[CI)I", (void*) setCallbackDecode },
    { "setCallbackEncode", "(JII[BI)I", (void*) setCallbackEncode },
    { "safeClone", "(J)J", (void*) safeClone }
};

int register_com_ibm_icu4jni_converters_NativeConverter(JNIEnv *_env) {
    return jniRegisterNativeMethods(_env, "com/ibm/icu4jni/charset/NativeConverter",
                gMethods, NELEM(gMethods));
}


