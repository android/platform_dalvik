/*
 * Copyright 2006 The Android Open Source Project
 *
 * Internal native functions.  All of the functions defined here make
 * direct use of VM functions or data structures, so they can't be written
 * with JNI and shouldn't really be in a shared library.
 *
 * All functions here either complete quickly or are used to enter a wait
 * state, so we don't set the thread status to THREAD_NATIVE when executing
 * these methods.  This means that the GC will wait for these functions
 * to finish.  DO NOT perform long operations or blocking I/O in here.
 *
 * In some cases we're following the division of labor defined by GNU
 * ClassPath, e.g. java.lang.Thread has "Thread" and "VMThread", with
 * the VM-specific behavior isolated in VMThread.
 */

#include "JNIHelp.h"
#include "AndroidSystemNatives.h"
#include "unicode/unum.h"
#include "unicode/numfmt.h"
#include "unicode/decimfmt.h"
#include "unicode/fmtable.h"
#include "unicode/ustring.h"
#include "digitlst.h"
#include "ErrorCode.h"
#include <stdlib.h>
#include <string.h>
#include "cutils/log.h"

#define LOG_TAG "DecimalFormatInterface"

static UBool icuError(JNIEnv *env, UErrorCode errorcode)
{
    const char *emsg = u_errorName(errorcode);
    jclass  exception;

    if (U_FAILURE(errorcode)) {// errorcode > U_ZERO_ERROR && errorcode < U_ERROR_LIMIT) {
        switch (errorcode) {
            case U_ILLEGAL_ARGUMENT_ERROR :
                exception = env->FindClass("java/lang/IllegalArgumentException");
                break;
            case U_INDEX_OUTOFBOUNDS_ERROR :
            case U_BUFFER_OVERFLOW_ERROR :
                exception = env->FindClass("java/lang/ArrayIndexOutOfBoundsException");
                break;
            case U_UNSUPPORTED_ERROR :
                exception = env->FindClass("java/lang/UnsupportedOperationException");
                break;
            default :
                exception = env->FindClass("java/lang/RuntimeException");
        }

        return (env->ThrowNew(exception, emsg) != 0);
    }
    return 0;
}

static jint openDecimalFormatImpl(JNIEnv *env, jclass clazz, jstring locale,
        jstring pattern) {

    // the errorcode returned by unum_open
    UErrorCode status = U_ZERO_ERROR;

    // prepare the pattern string for the call to unum_open
    const UChar *pattChars = env->GetStringChars(pattern, NULL);
    int pattLen = env->GetStringLength(pattern);

    // prepare the locale string for the call to unum_open
    const char *localeChars = env->GetStringUTFChars(locale, NULL);

    // open a default type number format
    UNumberFormat *fmt = unum_open(UNUM_PATTERN_DECIMAL, pattChars, pattLen,
            localeChars, NULL, &status);

    // release the allocated strings
    env->ReleaseStringChars(pattern, pattChars);
    env->ReleaseStringUTFChars(locale, localeChars);

    // check for an error
    if ( icuError(env, status) != FALSE) {
        return 0;
    }

    // return the handle to the number format
    return (long) fmt;
}

static void closeDecimalFormatImpl(JNIEnv *env, jclass clazz, jint addr) {

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    // close this number format
    unum_close(fmt);
}

static void setSymbol(JNIEnv *env, jclass clazz, jint addr, jint symbol,
        jstring text) {

    // the errorcode returned by unum_setSymbol
    UErrorCode status = U_ZERO_ERROR;

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    // prepare the symbol string for the call to unum_setSymbol
    const UChar *textChars = env->GetStringChars(text, NULL);
    int textLen = env->GetStringLength(text);

    // set the symbol
    unum_setSymbol(fmt, (UNumberFormatSymbol) symbol, textChars, textLen,
            &status);

    // release previously allocated space
    env->ReleaseStringChars(text, textChars);

    // check if an error occured
    icuError(env, status);
}

static jstring getSymbol(JNIEnv *env, jclass clazz, jint addr, jint symbol) {

    uint32_t resultlength, reslenneeded;

    // the errorcode returned by unum_getSymbol
    UErrorCode status = U_ZERO_ERROR;

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    UChar* result = NULL;
    resultlength=0;

    // find out how long the result will be
    reslenneeded=unum_getSymbol(fmt, (UNumberFormatSymbol) symbol, result,
            resultlength, &status);

    result = NULL;
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        resultlength=reslenneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        reslenneeded=unum_getSymbol(fmt, (UNumberFormatSymbol) symbol, result,
                resultlength, &status);
    }
    if (icuError(env, status) != FALSE) {
        return NULL;
    }

    jstring res = env->NewString(result, reslenneeded);

    free(result);

    return res;
}

static void setAttribute(JNIEnv *env, jclass clazz, jint addr, jint symbol,
        jint value) {

    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    unum_setAttribute(fmt, (UNumberFormatAttribute) symbol, value);
}

static jint getAttribute(JNIEnv *env, jclass clazz, jint addr, jint symbol) {

    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    int res = unum_getAttribute(fmt, (UNumberFormatAttribute) symbol);

    return res;
}

static void setTextAttribute(JNIEnv *env, jclass clazz, jint addr, jint symbol,
        jstring text) {

    // the errorcode returned by unum_setTextAttribute
    UErrorCode status = U_ZERO_ERROR;

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    const UChar *textChars = env->GetStringChars(text, NULL);
    int textLen = env->GetStringLength(text);

    unum_setTextAttribute(fmt, (UNumberFormatTextAttribute) symbol, textChars,
            textLen, &status);

    env->ReleaseStringChars(text, textChars);

    icuError(env, status);
}

static jstring getTextAttribute(JNIEnv *env, jclass clazz, jint addr,
        jint symbol) {

    uint32_t resultlength, reslenneeded;

    // the errorcode returned by unum_getTextAttribute
    UErrorCode status = U_ZERO_ERROR;

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    UChar* result = NULL;
    resultlength=0;

    // find out how long the result will be
    reslenneeded=unum_getTextAttribute(fmt, (UNumberFormatTextAttribute) symbol,
            result, resultlength, &status);

    result = NULL;
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        resultlength=reslenneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        reslenneeded=unum_getTextAttribute(fmt,
                (UNumberFormatTextAttribute) symbol, result, resultlength,
                &status);
    }
    if (icuError(env, status) != FALSE) {
        return NULL;
    }

    jstring res = env->NewString(result, reslenneeded);

    free(result);

    return res;
}

static void applyPatternImpl(JNIEnv *env, jclass clazz, jint addr,
        jboolean localized, jstring pattern) {

    // the errorcode returned by unum_applyPattern
    UErrorCode status = U_ZERO_ERROR;

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    const UChar *pattChars = env->GetStringChars(pattern, NULL);
    int pattLen = env->GetStringLength(pattern);

    unum_applyPattern(fmt, localized, pattChars, pattLen, NULL, &status);

    env->ReleaseStringChars(pattern, pattChars);

    icuError(env, status);
}

static jstring toPatternImpl(JNIEnv *env, jclass clazz, jint addr,
        jboolean localized) {

    uint32_t resultlength, reslenneeded;

    // the errorcode returned by unum_toPattern
    UErrorCode status = U_ZERO_ERROR;

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    UChar* result = NULL;
    resultlength=0;

    // find out how long the result will be
    reslenneeded=unum_toPattern(fmt, localized, result, resultlength, &status);

    result = NULL;
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        resultlength=reslenneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        reslenneeded=unum_toPattern(fmt, localized, result, resultlength,
                &status);
    }
    if (icuError(env, status) != FALSE) {
        return NULL;
    }

    jstring res = env->NewString(result, reslenneeded);

    free(result);

    return res;
}

static jstring formatLong(JNIEnv *env, jclass clazz, jint addr, jlong value,
        jobject field, jstring fieldType, jobject attributes) {

    const char * fieldPositionClassName = "java/text/FieldPosition";
    const char * stringBufferClassName = "java/lang/StringBuffer";
    jclass fieldPositionClass = env->FindClass(fieldPositionClassName);
    jclass stringBufferClass = env->FindClass(stringBufferClassName);
    jmethodID setBeginIndexMethodID = env->GetMethodID(fieldPositionClass,
            "setBeginIndex", "(I)V");
    jmethodID setEndIndexMethodID = env->GetMethodID(fieldPositionClass,
            "setEndIndex", "(I)V");
    jmethodID appendMethodID = env->GetMethodID(stringBufferClass,
            "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

    const char * fieldName = NULL;

    if(fieldType != NULL) {
        fieldName = env->GetStringUTFChars(fieldType, NULL);
    }

    uint32_t reslenneeded;
    int64_t val = value;
    UChar *result = NULL;

    FieldPosition fp;
    fp.setField(FieldPosition::DONT_CARE);

    UErrorCode status = U_ZERO_ERROR;

    DecimalFormat::AttrBuffer attrBuffer = NULL;
    attrBuffer = (DecimalFormat::AttrBuffer) malloc(sizeof(*attrBuffer));
    attrBuffer->bufferSize = 128;
    attrBuffer->buffer = (char *) malloc(129 * sizeof(char));
    attrBuffer->buffer[0] = '\0';

    DecimalFormat *fmt = (DecimalFormat *)(int)addr;

    UnicodeString *res = new UnicodeString();

    fmt->format(val, *res, fp, attrBuffer);

    reslenneeded = res->extract(NULL, 0, status);

    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;

        result = (UChar*)malloc(sizeof(UChar) * (reslenneeded + 1));

        res->extract(result, reslenneeded + 1, status);
    }
    if (icuError(env, status) != FALSE) {
        free(attrBuffer->buffer);
        free(attrBuffer);
        free(result);
        delete(res);
        return NULL;
    }

    int attrLength = 0;

    attrLength = (strlen(attrBuffer->buffer) + 1 );

    if(strlen(attrBuffer->buffer) > 0) {

        // check if we want to get all attributes
        if(attributes != NULL) {
            jstring attrString = env->NewStringUTF(attrBuffer->buffer + 1);  // cut off the leading ';'
            env->CallObjectMethod(attributes, appendMethodID, attrString);
        }

        // check if we want one special attribute returned in the given FieldPos
        if(fieldName != NULL && field != NULL) {
            const char *delimiter = ";";
            int begin;
            int end;
            char * resattr;
            resattr = strtok(attrBuffer->buffer, delimiter);

            while(resattr != NULL && strcmp(resattr, fieldName) != 0) {
                resattr = strtok(NULL, delimiter);
            }

            if(resattr != NULL && strcmp(resattr, fieldName) == 0) {
                resattr = strtok(NULL, delimiter);
                begin = (int) strtol(resattr, NULL, 10);
                resattr = strtok(NULL, delimiter);
                end = (int) strtol(resattr, NULL, 10);

                env->CallVoidMethod(field, setBeginIndexMethodID, (jint) begin);
                env->CallVoidMethod(field, setEndIndexMethodID, (jint) end);
            }
        }
    }

    if(fieldType != NULL) {
        env->ReleaseStringUTFChars(fieldType, fieldName);
    }

    jstring resulting = env->NewString(result, reslenneeded);

    free(attrBuffer->buffer);
    free(attrBuffer);
    free(result);
    delete(res);

    return resulting;
}

static jstring formatDouble(JNIEnv *env, jclass clazz, jint addr, jdouble value,
        jobject field, jstring fieldType, jobject attributes) {

    const char * fieldPositionClassName = "java/text/FieldPosition";
    const char * stringBufferClassName = "java/lang/StringBuffer";
    jclass fieldPositionClass = env->FindClass(fieldPositionClassName);
    jclass stringBufferClass = env->FindClass(stringBufferClassName);
    jmethodID setBeginIndexMethodID = env->GetMethodID(fieldPositionClass,
            "setBeginIndex", "(I)V");
    jmethodID setEndIndexMethodID = env->GetMethodID(fieldPositionClass,
            "setEndIndex", "(I)V");
    jmethodID appendMethodID = env->GetMethodID(stringBufferClass,
            "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

    const char * fieldName = NULL;

    if(fieldType != NULL) {
        fieldName = env->GetStringUTFChars(fieldType, NULL);
    }

    uint32_t reslenneeded;
    double val = value;
    UChar *result = NULL;

    FieldPosition fp;
    fp.setField(FieldPosition::DONT_CARE);

    UErrorCode status = U_ZERO_ERROR;

    DecimalFormat::AttrBuffer attrBuffer = NULL;
    attrBuffer = (DecimalFormat::AttrBuffer) malloc(sizeof(*attrBuffer));
    attrBuffer->bufferSize = 128;
    attrBuffer->buffer = (char *) malloc(129 * sizeof(char));
    attrBuffer->buffer[0] = '\0';

    DecimalFormat *fmt = (DecimalFormat *)(int)addr;

    UnicodeString *res = new UnicodeString();

    fmt->format(val, *res, fp, attrBuffer);

    reslenneeded = res->extract(NULL, 0, status);

    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;

        result = (UChar*)malloc(sizeof(UChar) * (reslenneeded + 1));

        res->extract(result, reslenneeded + 1, status);

    }
    if (icuError(env, status) != FALSE) {
        free(attrBuffer->buffer);
        free(attrBuffer);
        free(result);
        delete(res);
        return NULL;
    }

    int attrLength = 0;

    attrLength = (strlen(attrBuffer->buffer) + 1 );

    if(strlen(attrBuffer->buffer) > 0) {

        // check if we want to get all attributes
        if(attributes != NULL) {
            jstring attrString = env->NewStringUTF(attrBuffer->buffer + 1);  // cut off the leading ';'
            env->CallObjectMethod(attributes, appendMethodID, attrString);
        }

        // check if we want one special attribute returned in the given FieldPos
        if(fieldName != NULL && field != NULL) {
            const char *delimiter = ";";
            int begin;
            int end;
            char * resattr;
            resattr = strtok(attrBuffer->buffer, delimiter);

            while(resattr != NULL && strcmp(resattr, fieldName) != 0) {
                resattr = strtok(NULL, delimiter);
            }

            if(resattr != NULL && strcmp(resattr, fieldName) == 0) {
                resattr = strtok(NULL, delimiter);
                begin = (int) strtol(resattr, NULL, 10);
                resattr = strtok(NULL, delimiter);
                end = (int) strtol(resattr, NULL, 10);

                env->CallVoidMethod(field, setBeginIndexMethodID, (jint) begin);
                env->CallVoidMethod(field, setEndIndexMethodID, (jint) end);
            }
        }
    }

    if(fieldType != NULL) {
        env->ReleaseStringUTFChars(fieldType, fieldName);
    }

    jstring resulting = env->NewString(result, reslenneeded);

    free(attrBuffer->buffer);
    free(attrBuffer);
    free(result);
    delete(res);

    return resulting;
}

static jstring formatDigitList(JNIEnv *env, jclass clazz, jint addr, jstring value,
        jobject field, jstring fieldType, jobject attributes, jint scale) {

    // const char * valueUTF = env->GetStringUTFChars(value, NULL);
    // LOGI("ENTER formatDigitList: %s, scale: %d", valueUTF, scale);
    // env->ReleaseStringUTFChars(value, valueUTF);

    if (scale < 0) {
        icuError(env, U_ILLEGAL_ARGUMENT_ERROR);
        return NULL;
    }

    const char * fieldName = NULL;
    if(fieldType != NULL) {
        fieldName = env->GetStringUTFChars(fieldType, NULL);
    }

    uint32_t reslenneeded;

    // prepare digit list

    const char *valueChars = env->GetStringUTFChars(value, NULL);

    bool isInteger = (scale == 0);
    bool isPositive = (*valueChars != '-');

    // skip the '-' if the number is negative
    const char *digits = (isPositive ? valueChars : valueChars + 1);
    int length = strlen(digits);

    // The length of our digit list buffer must be the actual string length + 3,
    // because ICU will append some additional characters at the head and at the
    // tail of the string, in order to keep strtod() happy:
    //
    // - The sign "+" or "-" is appended at the head
    // - The exponent "e" and the "\0" terminator is appended at the tail
    //
    // In retrospect, the changes to ICU's DigitList that were necessary for
    // big numbers look a bit hacky. It would make sense to rework all this
    // once ICU 4.x has been integrated into Android. Ideally, big number
    // support would make it into ICU itself, so we don't need our private
    // fix anymore.
    DigitList digitList(length + 3);
    digitList.fCount = length;
    strcpy(digitList.fDigits, digits);
    env->ReleaseStringUTFChars(value, valueChars);

    digitList.fDecimalAt = digitList.fCount - scale;
    digitList.fIsPositive = isPositive;
    digitList.fRoundingMode = DecimalFormat::kRoundHalfUp;

    UChar *result = NULL;

    FieldPosition fp;
    fp.setField(FieldPosition::DONT_CARE);
    fp.setBeginIndex(0);
    fp.setEndIndex(0);

    UErrorCode status = U_ZERO_ERROR;

    DecimalFormat::AttributeBuffer *attrBuffer = NULL;
    attrBuffer = (DecimalFormat::AttributeBuffer *) calloc(sizeof(DecimalFormat::AttributeBuffer), 1);
    attrBuffer->bufferSize = 128;
    attrBuffer->buffer = (char *) calloc(129 * sizeof(char), 1);

    DecimalFormat *fmt = (DecimalFormat *)(int)addr;

    UnicodeString res;

    fmt->subformat(res, fp, attrBuffer, digitList, isInteger);

    reslenneeded = res.extract(NULL, 0, status);

    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;

        result = (UChar*)malloc(sizeof(UChar) * (reslenneeded + 1));

        res.extract(result, reslenneeded + 1, status);

        if (icuError(env, status) != FALSE) {
            if(fieldType != NULL) {
                env->ReleaseStringUTFChars(fieldType, fieldName);
            }
            free(result);
            free(attrBuffer->buffer);
            free(attrBuffer);
            return NULL;
        }

    } else {
        if(fieldType != NULL) {
            env->ReleaseStringUTFChars(fieldType, fieldName);
        }
        free(attrBuffer->buffer);
        free(attrBuffer);
        return NULL;
    }

    int attrLength = (strlen(attrBuffer->buffer) + 1 );

    if(attrLength > 1) {

        // check if we want to get all attributes
        if(attributes != NULL) {
            // prepare the classes and method ids
            const char * stringBufferClassName = "java/lang/StringBuffer";
            jclass stringBufferClass = env->FindClass(stringBufferClassName);
            jmethodID appendMethodID = env->GetMethodID(stringBufferClass,
                    "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

            jstring attrString = env->NewStringUTF(attrBuffer->buffer + 1);  // cut off the leading ';'
            env->CallObjectMethod(attributes, appendMethodID, attrString);
        }

        // check if we want one special attribute returned in the given FieldPos
        if(fieldName != NULL && field != NULL) {
            const char *delimiter = ";";
            int begin;
            int end;
            char * resattr;
            resattr = strtok(attrBuffer->buffer, delimiter);

            while(resattr != NULL && strcmp(resattr, fieldName) != 0) {
                resattr = strtok(NULL, delimiter);
            }

            if(resattr != NULL && strcmp(resattr, fieldName) == 0) {

                // prepare the classes and method ids
                const char * fieldPositionClassName =
                        "java/text/FieldPosition";
                jclass fieldPositionClass = env->FindClass(
                        fieldPositionClassName);
                jmethodID setBeginIndexMethodID = env->GetMethodID(
                        fieldPositionClass, "setBeginIndex", "(I)V");
                jmethodID setEndIndexMethodID = env->GetMethodID(
                       fieldPositionClass, "setEndIndex", "(I)V");


                resattr = strtok(NULL, delimiter);
                begin = (int) strtol(resattr, NULL, 10);
                resattr = strtok(NULL, delimiter);
                end = (int) strtol(resattr, NULL, 10);

                env->CallVoidMethod(field, setBeginIndexMethodID, (jint) begin);
                env->CallVoidMethod(field, setEndIndexMethodID, (jint) end);
            }
        }
    }

    if(fieldType != NULL) {
        env->ReleaseStringUTFChars(fieldType, fieldName);
    }

    jstring resulting = env->NewString(result, reslenneeded);

    free(attrBuffer->buffer);
    free(attrBuffer);
    free(result);
    // const char * resultUTF = env->GetStringUTFChars(resulting, NULL);
    // LOGI("RETURN formatDigitList: %s", resultUTF);
    // env->ReleaseStringUTFChars(resulting, resultUTF);

    return resulting;
}

static jobject parse(JNIEnv *env, jclass clazz, jint addr, jstring text,
        jobject position) {

    const char * textUTF = env->GetStringUTFChars(text, NULL);
    env->ReleaseStringUTFChars(text, textUTF);

    const char * parsePositionClassName = "java/text/ParsePosition";
    const char * longClassName = "java/lang/Long";
    const char * doubleClassName = "java/lang/Double";
    const char * bigDecimalClassName = "java/math/BigDecimal";
    const char * bigIntegerClassName = "java/math/BigInteger";

    UErrorCode status = U_ZERO_ERROR;

    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    jchar *str = (UChar *)env->GetStringChars(text, NULL);
    int strlength = env->GetStringLength(text);

    jclass parsePositionClass = env->FindClass(parsePositionClassName);
    jclass longClass =  env->FindClass(longClassName);
    jclass doubleClass =  env->FindClass(doubleClassName);
    jclass bigDecimalClass = env->FindClass(bigDecimalClassName);
    jclass bigIntegerClass = env->FindClass(bigIntegerClassName);

    jmethodID getIndexMethodID = env->GetMethodID(parsePositionClass,
            "getIndex", "()I");
    jmethodID setIndexMethodID = env->GetMethodID(parsePositionClass,
            "setIndex", "(I)V");
    jmethodID setErrorIndexMethodID = env->GetMethodID(parsePositionClass,
            "setErrorIndex", "(I)V");

    jmethodID longInitMethodID = env->GetMethodID(longClass, "<init>", "(J)V");
    jmethodID dblInitMethodID = env->GetMethodID(doubleClass, "<init>", "(D)V");
    jmethodID bigDecimalInitMethodID = env->GetMethodID(bigDecimalClass, "<init>", "(Ljava/math/BigInteger;I)V");
    jmethodID bigIntegerInitMethodID = env->GetMethodID(bigIntegerClass, "<init>", "(Ljava/lang/String;)V");
    jmethodID doubleValueMethodID = env->GetMethodID(bigDecimalClass, "doubleValue", "()D");

    bool resultAssigned;
    int parsePos = env->CallIntMethod(position, getIndexMethodID, NULL);

    // make sure the ParsePosition is valid. Actually icu4c would parse a number
    // correctly even if the parsePosition is set to -1, but since the RI fails
    // for that case we have to fail too
    if(parsePos < 0 || parsePos > strlength) {
        return NULL;
    }

    Formattable res;

    const UnicodeString src((UChar*)str, strlength, strlength);
    ParsePosition pp;

    pp.setIndex(parsePos);

    DigitList digits;

    ((const DecimalFormat*)fmt)->parse(src, resultAssigned, res, pp, FALSE, digits);

    env->ReleaseStringChars(text, str);

    if(pp.getErrorIndex() == -1) {
        parsePos = pp.getIndex();
    } else {
        env->CallVoidMethod(position, setErrorIndexMethodID,
                (jint) pp.getErrorIndex());
        return NULL;
    }

    Formattable::Type numType;
    numType = res.getType();
    UErrorCode fmtStatus;

    double resultDouble;
    long resultLong;
    int64_t resultInt64;
    UnicodeString resultString;
    jstring resultStr;
    int resLength;
    const char * resultUTF;
    jobject resultObject1, resultObject2;
    jdouble doubleTest;
    jchar * result;

    if (resultAssigned)
    {
        switch(numType) {
        case Formattable::kDouble:
            resultDouble = res.getDouble();
            env->CallVoidMethod(position, setIndexMethodID, (jint) parsePos);
            return env->NewObject(doubleClass, dblInitMethodID,
                    (jdouble) resultDouble);
        case Formattable::kLong:
            resultLong = res.getLong();
            env->CallVoidMethod(position, setIndexMethodID, (jint) parsePos);
            return env->NewObject(longClass, longInitMethodID,
                    (jlong) resultLong);
        case Formattable::kInt64:
            resultInt64 = res.getInt64();
            env->CallVoidMethod(position, setIndexMethodID, (jint) parsePos);
            return env->NewObject(longClass, longInitMethodID,
                    (jlong) resultInt64);
        default:
            return NULL;
        }
    }
    else
    {
        int scale = digits.fCount - digits.fDecimalAt;
        // ATTENTION: Abuse of Implementation Knowlegde!
        digits.fDigits[digits.fCount] = 0;
        if (digits.fIsPositive) {
            resultStr = env->NewStringUTF(digits.fDigits);
        } else {
            if (digits.fCount == 0) {
                env->CallVoidMethod(position, setIndexMethodID, (jint) parsePos);
                return env->NewObject(doubleClass, dblInitMethodID, (jdouble)-0);
            } else {
                // ATTENTION: Abuse of Implementation Knowlegde!
                *(digits.fDigits - 1) = '-';
                resultStr = env->NewStringUTF(digits.fDigits - 1);
            }
        }

        env->CallVoidMethod(position, setIndexMethodID, (jint) parsePos);

        resultObject1 = env->NewObject(bigIntegerClass, bigIntegerInitMethodID, resultStr);
        resultObject2 = env->NewObject(bigDecimalClass, bigDecimalInitMethodID, resultObject1, scale);
        return resultObject2;
    }
}

static jint cloneImpl(JNIEnv *env, jclass clazz, jint addr) {

    UErrorCode status = U_ZERO_ERROR;

    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    UNumberFormat *result = unum_clone(fmt, &status);

    if(icuError(env, status) != FALSE) {
        return 0;
    }

    return (long) result;

}

static JNINativeMethod gMethods[] = {
    /* name, signature, funcPtr */
    {"openDecimalFormatImpl", "(Ljava/lang/String;Ljava/lang/String;)I",
            (void*) openDecimalFormatImpl},
    {"closeDecimalFormatImpl", "(I)V", (void*) closeDecimalFormatImpl},
    {"setSymbol", "(IILjava/lang/String;)V", (void*) setSymbol},
    {"getSymbol", "(II)Ljava/lang/String;", (void*) getSymbol},
    {"setAttribute", "(III)V", (void*) setAttribute},
    {"getAttribute", "(II)I", (void*) getAttribute},
    {"setTextAttribute", "(IILjava/lang/String;)V", (void*) setTextAttribute},
    {"getTextAttribute", "(II)Ljava/lang/String;", (void*) getTextAttribute},
    {"applyPatternImpl", "(IZLjava/lang/String;)V", (void*) applyPatternImpl},
    {"toPatternImpl", "(IZ)Ljava/lang/String;", (void*) toPatternImpl},
    {"format",
            "(IJLjava/text/FieldPosition;Ljava/lang/String;Ljava/lang/StringBuffer;)Ljava/lang/String;",
            (void*) formatLong},
    {"format",
            "(IDLjava/text/FieldPosition;Ljava/lang/String;Ljava/lang/StringBuffer;)Ljava/lang/String;",
            (void*) formatDouble},
    {"format",
            "(ILjava/lang/String;Ljava/text/FieldPosition;Ljava/lang/String;Ljava/lang/StringBuffer;I)Ljava/lang/String;",
            (void*) formatDigitList},
    {"parse",
            "(ILjava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Number;",
            (void*) parse},
    {"cloneImpl", "(I)I", (void*) cloneImpl}
};
int register_com_ibm_icu4jni_text_NativeDecimalFormat(JNIEnv* env) {
    return jniRegisterNativeMethods(env,
            "com/ibm/icu4jni/text/NativeDecimalFormat", gMethods,
            NELEM(gMethods));
}
