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

#include "JNIHelp.h"
#include "AndroidSystemNatives.h"
#include "unicode/numfmt.h"
#include "unicode/locid.h"
#include "unicode/ucal.h"
#include "unicode/gregocal.h"
#include "unicode/ucurr.h"
#include "unicode/calendar.h"
#include "unicode/datefmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/decimfmt.h"
#include "unicode/dcfmtsym.h"
#include "unicode/uclean.h"
#include "unicode/smpdtfmt.h"
#include "unicode/strenum.h"
#include "unicode/ustring.h"
#include "unicode/timezone.h"
#include "ErrorCode.h"
#include <cutils/log.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/time.h>

jclass string_class;

static UBool icuError(JNIEnv *env, UErrorCode errorcode)
{
  const char   *emsg      = u_errorName(errorcode);
        jclass  exception;

  if (U_FAILURE(errorcode)) {
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

static Locale getLocale(JNIEnv *env, jstring locale) {
    const char *name = env->GetStringUTFChars(locale, NULL);
    Locale result = Locale::createFromName(name);
    env->ReleaseStringUTFChars(locale, name);
    return result;
}

static jstring getJStringFromUnicodeString(JNIEnv *env, UnicodeString string) {
    
    UErrorCode status = U_ZERO_ERROR;

    int stringLength = string.length();
    jchar *res = (jchar *) malloc(sizeof(jchar) * (stringLength + 1));
    string.extract(res, stringLength+1, status);
    if(U_FAILURE(status)) {
        free(res);
        LOGI("Error getting string for getJStringFromUnicodeString");
        status = U_ZERO_ERROR;
        return NULL;
    }
    jstring result = env->NewString(res, stringLength);
    free(res);
    return result;
}

static void addObject(JNIEnv *env, jobjectArray result, const char *keyStr, jobject elem, int index) {
    jclass objArray_class = env->FindClass("java/lang/Object");
    jobjectArray element = env->NewObjectArray(2, objArray_class, NULL);
    jstring key = env->NewStringUTF(keyStr);
    env->SetObjectArrayElement(element, 0, key);
    env->SetObjectArrayElement(element, 1, elem);
    env->SetObjectArrayElement(result, index, element);
    env->DeleteLocalRef(key);
    env->DeleteLocalRef(element);
} 

static jint getFractionDigitsNative(JNIEnv* env, jclass clazz, 
        jstring currencyCode) {
    // LOGI("ENTER getFractionDigitsNative");
    
    UErrorCode status = U_ZERO_ERROR;
    
    NumberFormat *fmt = NumberFormat::createCurrencyInstance(status);
    if(U_FAILURE(status)) {
        return -1;
    }

    const jchar *cCode = env->GetStringChars(currencyCode, NULL);
    fmt->setCurrency(cCode, status);
    env->ReleaseStringChars(currencyCode, cCode);
    if(U_FAILURE(status)) {
        return -1;
    }
    
    // for CurrencyFormats the minimum and maximum fraction digits are the same.
    int result = fmt->getMinimumFractionDigits(); 
    delete(fmt);
    return result;
}

static jstring getCurrencyCodeNative(JNIEnv* env, jclass clazz, 
        jstring key) {
    // LOGI("ENTER getCurrencyCodeNative");

    UErrorCode status = U_ZERO_ERROR;

    UResourceBundle *supplData = ures_openDirect(NULL, "supplementalData", &status);
    if(U_FAILURE(status)) {
        return NULL;
    }

    UResourceBundle *currencyMap = ures_getByKey(supplData, "CurrencyMap", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(supplData);
        return NULL;
    }

    const char *keyChars = env->GetStringUTFChars(key, NULL);
    UResourceBundle *currency = ures_getByKey(currencyMap, keyChars, NULL, &status);
    env->ReleaseStringUTFChars(key, keyChars);
    if(U_FAILURE(status)) {
        ures_close(currencyMap);
        ures_close(supplData);
        return NULL;
    }

    UResourceBundle *currencyElem = ures_getByIndex(currency, 0, NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(currency);
        ures_close(currencyMap);
        ures_close(supplData);
        return env->NewStringUTF("None");
    }

    // check if there is a to date. If there is, the currency isn't used anymore.
    UResourceBundle *currencyTo = ures_getByKey(currencyElem, "to", NULL, &status);
    if(!U_FAILURE(status)) {
        // return and let the ResourceBundle throw an exception
        ures_close(currencyElem);
        ures_close(currency);
        ures_close(currencyMap);
        ures_close(supplData);
        return NULL;
    }
    status = U_ZERO_ERROR;
    ures_close(currencyTo);

    UResourceBundle *currencyId = ures_getByKey(currencyElem, "id", NULL, &status);
    if(U_FAILURE(status)) {
        // No id defined for this country
        ures_close(currencyElem);
        ures_close(currency);
        ures_close(currencyMap);
        ures_close(supplData);
        return env->NewStringUTF("None");
    }

    int length;
    const jchar *id = ures_getString(currencyId, &length, &status);
    if(U_FAILURE(status)) {
        ures_close(currencyId);
        ures_close(currencyElem);
        ures_close(currency);
        ures_close(currencyMap);
        ures_close(supplData);
        return env->NewStringUTF("None");
    }

    ures_close(currencyId);
    ures_close(currencyElem);
    ures_close(currency);
    ures_close(currencyMap);
    ures_close(supplData);

    if(length == 0) {
        return env->NewStringUTF("None");
    }
    return env->NewString(id, length);
}

static jstring getCurrencySymbolNative(JNIEnv* env, jclass clazz, 
        jstring locale, jstring currencyCode) {
    // LOGI("ENTER getCurrencySymbolNative");

    UErrorCode status = U_ZERO_ERROR;

    const char *locName = env->GetStringUTFChars(locale, NULL);
    UResourceBundle *root = ures_open(NULL, locName, &status);
    env->ReleaseStringUTFChars(locale, locName);
    if(U_FAILURE(status)) {
        return NULL;
    }

    UResourceBundle *rootElems = ures_getByKey(root, "Currencies", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(root);
        return NULL;
    }

    const char *currName = env->GetStringUTFChars(currencyCode, NULL);
    UResourceBundle *currencyElems = ures_getByKey(rootElems, currName, NULL, &status);
    env->ReleaseStringUTFChars(currencyCode, currName);
    if(U_FAILURE(status)) {
        ures_close(rootElems);
        ures_close(root);
        return NULL;
    }

    int currSymbL;
    const jchar *currSymbU = ures_getStringByIndex(currencyElems, 0, &currSymbL, &status);
    if(U_FAILURE(status)) {
        ures_close(currencyElems);
        ures_close(rootElems);
        ures_close(root);
        return NULL;
    }

    ures_close(currencyElems);
    ures_close(rootElems);
    ures_close(root);

    if(currSymbL == 0) {
        return NULL;
    }
    return env->NewString(currSymbU, currSymbL);
}

static jstring getDisplayCountryNative(JNIEnv* env, jclass clazz, 
        jstring targetLocale, jstring locale) {
    // LOGI("ENTER getDisplayCountryNative");
    
    UErrorCode status = U_ZERO_ERROR;

    Locale loc = getLocale(env, locale);
    Locale targetLoc = getLocale(env, targetLocale);
    
    UnicodeString string;
    targetLoc.getDisplayCountry(loc, string);

    jstring result = getJStringFromUnicodeString(env, string);

    return result;
}

static jstring getDisplayLanguageNative(JNIEnv* env, jclass clazz, 
        jstring targetLocale, jstring locale) {
    // LOGI("ENTER getDisplayLanguageNative");

    Locale loc = getLocale(env, locale);
    Locale targetLoc = getLocale(env, targetLocale);

    UnicodeString string;
    targetLoc.getDisplayLanguage(loc, string);
    
    jstring result = getJStringFromUnicodeString(env, string);
    
    return result;
}

static jstring getDisplayVariantNative(JNIEnv* env, jclass clazz, 
        jstring targetLocale, jstring locale) {
    // LOGI("ENTER getDisplayVariantNative");

    Locale loc = getLocale(env, locale);
    Locale targetLoc = getLocale(env, targetLocale);
    
    UnicodeString string;
    targetLoc.getDisplayVariant(loc, string);
    
    jstring result = getJStringFromUnicodeString(env, string);
    
    return result;
}

static jstring getISO3CountryNative(JNIEnv* env, jclass clazz, jstring locale) {
    // LOGI("ENTER getISO3CountryNative");

    Locale loc = getLocale(env, locale);
    
    const char *string = loc.getISO3Country();
    
    jstring result = env->NewStringUTF(string);
    
    return result;
}

static jstring getISO3LanguageNative(JNIEnv* env, jclass clazz, jstring locale) {
    // LOGI("ENTER getISO3LanguageNative");

    Locale loc = getLocale(env, locale);
    
    const char *string = loc.getISO3Language();
    
    jstring result = env->NewStringUTF(string);
    
    return result;
}

static jobjectArray getISOCountriesNative(JNIEnv* env, jclass clazz) {
    // LOGI("ENTER getISOCountriesNative");

    const char* const* strings = Locale::getISOCountries();
    
    int count = 0;
    while(strings[count] != NULL) {
        count++;
    }
  
    jobjectArray result = env->NewObjectArray(count, string_class, NULL);

    jstring res;
    for(int i = 0; i < count; i++) {
        res = env->NewStringUTF(strings[i]);
        env->SetObjectArrayElement(result, i, res);
        env->DeleteLocalRef(res);
    }
    return result;
}

static jobjectArray getISOLanguagesNative(JNIEnv* env, jclass clazz) {
    // LOGI("ENTER getISOLanguagesNative");
    
    const char* const* strings = Locale::getISOLanguages();
    
    const char *string = strings[0];
    
    int count = 0;
    while(strings[count] != NULL) {
        count++;
    }
    
    jobjectArray result = env->NewObjectArray(count, string_class, NULL);

    jstring res;
    for(int i = 0; i < count; i++) {
        res = env->NewStringUTF(strings[i]);
        env->SetObjectArrayElement(result, i, res);
        env->DeleteLocalRef(res);
    }
    return result;
}

static jobjectArray getAvailableLocalesNative(JNIEnv* env, jclass clazz) {
    // LOGI("ENTER getAvailableLocalesNative");
    
    int count = uloc_countAvailable();
    
    jobjectArray result = env->NewObjectArray(count, string_class, NULL);

    jstring res;
    const char * string;    
    for(int i = 0; i < count; i++) {
        string = uloc_getAvailable(i);
        res = env->NewStringUTF(string);
        env->SetObjectArrayElement(result, i, res);
        env->DeleteLocalRef(res);
    }

    return result;
}

static void getTimeZonesNative(JNIEnv* env, jclass clazz,
        jobjectArray outerArray, jstring locale) {
    // LOGI("ENTER getTimeZonesNative");
    
    UErrorCode status = U_ZERO_ERROR;

    jobjectArray zoneIdArray;
    jobjectArray longStdTimeArray;
    jobjectArray shortStdTimeArray;
    jobjectArray longDlTimeArray;
    jobjectArray shortDlTimeArray;

    jstring content;
    jstring strObj;
    const jchar *res;
    UnicodeString resU;
    jint length;
    const UnicodeString *zoneID;
    DateFormat *df;

    UnicodeString longPattern("zzzz","");
    UnicodeString shortPattern("z","");
      
    Locale loc = getLocale(env, locale);

    SimpleDateFormat longFormat(longPattern, loc, status);
    SimpleDateFormat shortFormat(shortPattern, loc, status);


    zoneIdArray = (jobjectArray) env->GetObjectArrayElement(outerArray, 0);
    longStdTimeArray = (jobjectArray) env->GetObjectArrayElement(outerArray, 1);
    shortStdTimeArray = (jobjectArray) env->GetObjectArrayElement(outerArray, 2);
    longDlTimeArray = (jobjectArray) env->GetObjectArrayElement(outerArray, 3);
    shortDlTimeArray = (jobjectArray) env->GetObjectArrayElement(outerArray, 4);

    int count = env->GetArrayLength(zoneIdArray);

    TimeZone* zones[count];
    
    // get all timezone objects
    for(int i = 0; i < count; i++) {
        strObj = (jstring) env->GetObjectArrayElement(zoneIdArray, i);
        length = env->GetStringLength(strObj);
        res = env->GetStringChars(strObj, NULL);
        const UnicodeString zoneID((UChar *)res, length);
        env->ReleaseStringChars(strObj, res);
        zones[i] = TimeZone::createTimeZone(zoneID);
        env->DeleteLocalRef(strObj);
    }

    // 15th January 2008
    UDate date1 = 1203105600000.0;
    // 15th July 2008
    UDate date2 = 1218826800000.0;

    for (int i = 0; i < count; i++) {
           TimeZone *tz = zones[i];
           longFormat.setTimeZone(*tz);
           shortFormat.setTimeZone(*tz);
           
           int32_t daylightOffset;
           int32_t rawOffset;
           UDate standardDate;
           UDate daylightSavingDate;
           tz->getOffset(date1, false, rawOffset, daylightOffset, status);
           if (daylightOffset != 0) {
               // The Timezone is reporting that we are in daylight time
               // for the winter date.  The dates are for the wrong hemisphere,
               // swap them.
               standardDate = date2;
               daylightSavingDate = date1;
           } else {
               standardDate = date1;
               daylightSavingDate = date2;
           }
                     
           UnicodeString shortDayLight;
           UnicodeString longDayLight;
           UnicodeString shortStandard;
           UnicodeString longStandard;
           
           shortFormat.format(daylightSavingDate, shortDayLight);
           content = getJStringFromUnicodeString(env, shortDayLight);
           env->SetObjectArrayElement(shortDlTimeArray, i, content);
           env->DeleteLocalRef(content);

           shortFormat.format(standardDate, shortStandard);
           content = getJStringFromUnicodeString(env, shortStandard);
           env->SetObjectArrayElement(shortStdTimeArray, i, content);
           env->DeleteLocalRef(content);

           longFormat.format (daylightSavingDate, longDayLight);
           content = getJStringFromUnicodeString(env, longDayLight);
           env->SetObjectArrayElement(longDlTimeArray, i, content);
           env->DeleteLocalRef(content);

           longFormat.format (standardDate, longStandard);
           content = getJStringFromUnicodeString(env, longStandard);
           env->SetObjectArrayElement(longStdTimeArray, i, content);
           env->DeleteLocalRef(content);
           delete(tz);
    }
}




static jstring getDisplayTimeZoneNative(JNIEnv* env, jclass clazz,
        jstring zoneID, jboolean isDST, jint style, jstring localeID) {

    // Build TimeZone object
    const jchar* idChars = env->GetStringChars(zoneID, NULL);
    jint idLength = env->GetStringLength(zoneID);
    UnicodeString idString((UChar*)idChars, idLength);
    TimeZone* zone = TimeZone::createTimeZone(idString);
    env->ReleaseStringChars(zoneID, idChars);

    // Build Locale object (can we rely on zero termination of JNI result?)
    const char* localeChars = env->GetStringUTFChars(localeID, NULL);
    jint localeLength = env->GetStringLength(localeID);
    Locale locale = Locale::createFromName(localeChars);

    // Try to get the display name of the TimeZone according to the Locale
    UnicodeString buffer;
    zone->getDisplayName((UBool)isDST, (style == 0 ? TimeZone::SHORT : TimeZone::LONG), locale, buffer);
    const UChar* tempChars = buffer.getBuffer();
    int tempLength = buffer.length();
    jstring result = env->NewString((jchar*)tempChars, tempLength);
    env->ReleaseStringUTFChars(localeID, localeChars);

    // Clean up everything
    delete(zone);
    
    return result;
}

static void getDayInitVector(JNIEnv *env, UResourceBundle *gregorian, int *values) {

    UErrorCode status = U_ZERO_ERROR;

    // get the First day of week and the minimal days in first week numbers
    UResourceBundle *gregorianElems = ures_getByKey(gregorian, "DateTimeElements", NULL, &status);
    if(U_FAILURE(status)) {
        return;
    }

    int intVectSize;
    const int *result;
    result = ures_getIntVector(gregorianElems, &intVectSize, &status);
    if(U_FAILURE(status)) {
        ures_close(gregorianElems);
        return;
    }

    if(intVectSize == 2) {
        values[0] = result[0];
        values[1] = result[1];
    }

    ures_close(gregorianElems);

}

static jobjectArray getAmPmMarkers(JNIEnv *env, UResourceBundle *gregorian) {
    
    jobjectArray amPmMarkers;
    jstring pmU, amU;

    UErrorCode status = U_ZERO_ERROR;

    UResourceBundle *gregorianElems;

    gregorianElems = ures_getByKey(gregorian, "AmPmMarkers", NULL, &status);
    if(U_FAILURE(status)) {
        return NULL;
    }

    int lengthAm, lengthPm;

    ures_resetIterator(gregorianElems);

    const jchar* am = ures_getStringByIndex(gregorianElems, 0, &lengthAm, &status);
    const jchar* pm = ures_getStringByIndex(gregorianElems, 1, &lengthPm, &status);

    if(U_FAILURE(status)) {
        ures_close(gregorianElems);
        return NULL;
    }
    
    amPmMarkers = env->NewObjectArray(2, string_class, NULL);
    amU = env->NewString(am, lengthAm);
    env->SetObjectArrayElement(amPmMarkers, 0, amU);
    env->DeleteLocalRef(amU);
    pmU = env->NewString(pm, lengthPm);
    env->SetObjectArrayElement(amPmMarkers, 1, pmU);
    env->DeleteLocalRef(pmU);
    ures_close(gregorianElems);

    return amPmMarkers;
}

static jobjectArray getEras(JNIEnv* env, UResourceBundle *gregorian) {
    
    jobjectArray eras;
    jstring eraU;
    const jchar* era;

    UErrorCode status = U_ZERO_ERROR;

    UResourceBundle *gregorianElems;
    UResourceBundle *eraElems;

    gregorianElems = ures_getByKey(gregorian, "eras", NULL, &status);
    if(U_FAILURE(status)) {
        return NULL;
    }

    eraElems = ures_getByKey(gregorianElems, "abbreviated", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(gregorianElems);
        return NULL;
    }

    int eraLength;

    int eraCount = ures_getSize(eraElems);
    eras = env->NewObjectArray(eraCount, string_class, NULL);

    ures_resetIterator(eraElems);
    for(int i = 0; i < eraCount; i++) {
        era = ures_getStringByIndex(eraElems, i, &eraLength, &status);
        if(U_FAILURE(status)) {
            ures_close(gregorianElems);
            ures_close(eraElems);
            return NULL;
        }
        eraU = env->NewString(era, eraLength);
        env->SetObjectArrayElement(eras, i, eraU);
        env->DeleteLocalRef(eraU);
    }
    ures_close(eraElems);
    ures_close(gregorianElems);

    return eras;
}

static jobjectArray getMonthNames(JNIEnv *env, UResourceBundle *gregorian) {
    
    UErrorCode status = U_ZERO_ERROR;

    const jchar* month;
    jstring monthU;

    UResourceBundle *gregorianElems = ures_getByKey(gregorian, "monthNames", NULL, &status);
    if(U_FAILURE(status)) {
        return NULL;
    }

    UResourceBundle *monthNameElems = ures_getByKey(gregorianElems, "format", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(gregorianElems);
        return NULL;
    }

    UResourceBundle *monthNameElemsFormat = ures_getByKey(monthNameElems, "wide", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(monthNameElems);
        ures_close(gregorianElems);
        return NULL;
    }

    int monthNameLength;
    ures_resetIterator(monthNameElemsFormat);
    int monthCount = ures_getSize(monthNameElemsFormat);
    jobjectArray months = env->NewObjectArray(monthCount + 1, string_class, NULL);
    for(int i = 0; i < monthCount; i++) {
        month = ures_getStringByIndex(monthNameElemsFormat, i, &monthNameLength, &status);
        if(U_FAILURE(status)) {
            ures_close(monthNameElemsFormat);
            ures_close(monthNameElems);
            ures_close(gregorianElems);
            return NULL;
        }
        monthU = env->NewString(month, monthNameLength);
        env->SetObjectArrayElement(months, i, monthU);
        env->DeleteLocalRef(monthU);
    }

    monthU = env->NewStringUTF("");
    env->SetObjectArrayElement(months, monthCount, monthU);
    env->DeleteLocalRef(monthU);

    ures_close(monthNameElemsFormat);
    ures_close(monthNameElems);
    ures_close(gregorianElems);
    return months;
}

static jobjectArray getShortMonthNames(JNIEnv *env, UResourceBundle *gregorian) {
    
    UErrorCode status = U_ZERO_ERROR;

    const jchar* shortMonth;
    jstring shortMonthU;

    UResourceBundle *gregorianElems = ures_getByKey(gregorian, "monthNames", NULL, &status);
    if(U_FAILURE(status)) {
        return NULL;
    }

    UResourceBundle *monthNameElems = ures_getByKey(gregorianElems, "format", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(gregorianElems);
        return NULL;
    }

    UResourceBundle *monthNameElemsFormat = ures_getByKey(monthNameElems, "abbreviated", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(monthNameElems);
        ures_close(gregorianElems);
        return NULL;
    }

    int shortMonthNameLength;
    ures_resetIterator(monthNameElemsFormat);
    int shortMonthCount = ures_getSize(monthNameElemsFormat);
    // the array length is +1 because the harmony locales had an empty string at the end of their month name array
    jobjectArray shortMonths = env->NewObjectArray(shortMonthCount + 1, string_class, NULL);
    for(int i = 0; i < shortMonthCount; i++) {
        shortMonth = ures_getStringByIndex(monthNameElemsFormat, i, &shortMonthNameLength, &status);
        if(U_FAILURE(status)) {
            ures_close(monthNameElemsFormat);
            ures_close(monthNameElems);
            ures_close(gregorianElems);
            return NULL;
        }
        shortMonthU = env->NewString(shortMonth, shortMonthNameLength);
        env->SetObjectArrayElement(shortMonths, i, shortMonthU);
        env->DeleteLocalRef(shortMonthU);
    }

    shortMonthU = env->NewStringUTF("");
    env->SetObjectArrayElement(shortMonths, shortMonthCount, shortMonthU);
    env->DeleteLocalRef(shortMonthU);

    ures_close(monthNameElemsFormat);
    ures_close(monthNameElems);
    ures_close(gregorianElems);
    return shortMonths;
}

static jobjectArray getWeekdayNames(JNIEnv *env, UResourceBundle *gregorian) {
    
    UErrorCode status = U_ZERO_ERROR;

    const jchar* day;
    jstring dayU;

    UResourceBundle *gregorianElems = ures_getByKey(gregorian, "dayNames", NULL, &status);
    if(U_FAILURE(status)) {
        return NULL;
    }

    UResourceBundle *dayNameElems = ures_getByKey(gregorianElems, "format", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(gregorianElems);
        return NULL;
    }

    UResourceBundle *dayNameElemsFormat = ures_getByKey(dayNameElems, "wide", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(dayNameElems);
        ures_close(gregorianElems);
        return NULL;
    }

    int dayNameLength;
    ures_resetIterator(dayNameElemsFormat);
    int dayCount = ures_getSize(dayNameElemsFormat);
    jobjectArray weekdays = env->NewObjectArray(dayCount + 1, string_class, NULL);
    // first entry in the weekdays array is an empty string
    env->SetObjectArrayElement(weekdays, 0, env->NewStringUTF(""));
    for(int i = 0; i < dayCount; i++) {
        day = ures_getStringByIndex(dayNameElemsFormat, i, &dayNameLength, &status);
        if(U_FAILURE(status)) {
            ures_close(dayNameElemsFormat);
            ures_close(dayNameElems);
            ures_close(gregorianElems);
            return NULL;
        }
        dayU = env->NewString(day, dayNameLength);
        env->SetObjectArrayElement(weekdays, i + 1, dayU);
        env->DeleteLocalRef(dayU);
    }
    
    ures_close(dayNameElemsFormat);
    ures_close(dayNameElems);
    ures_close(gregorianElems);
    return weekdays;

}

static jobjectArray getShortWeekdayNames(JNIEnv *env, UResourceBundle *gregorian) {
    
    UErrorCode status = U_ZERO_ERROR;

    const jchar* shortDay;
    jstring shortDayU;

    UResourceBundle *gregorianElems = ures_getByKey(gregorian, "dayNames", NULL, &status);
    if(U_FAILURE(status)) {
        return NULL;
    }

    UResourceBundle *dayNameElems = ures_getByKey(gregorianElems, "format", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(gregorianElems);
        return NULL;
    }

    UResourceBundle *dayNameElemsFormat = ures_getByKey(dayNameElems, "abbreviated", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(dayNameElems);
        ures_close(gregorianElems);
        return NULL;
    }

    int shortDayNameLength;
    ures_resetIterator(dayNameElemsFormat);
    int shortDayCount = ures_getSize(dayNameElemsFormat);
    jobjectArray shortWeekdays = env->NewObjectArray(shortDayCount + 1, string_class, NULL);
    env->SetObjectArrayElement(shortWeekdays, 0, env->NewStringUTF(""));
    for(int i = 0; i < shortDayCount; i++) {
        shortDay = ures_getStringByIndex(dayNameElemsFormat, i, &shortDayNameLength, &status);
        if(U_FAILURE(status)) {
            ures_close(dayNameElemsFormat);
            ures_close(dayNameElems);
            ures_close(gregorianElems);
            return NULL;
        }
        shortDayU = env->NewString(shortDay, shortDayNameLength);
        env->SetObjectArrayElement(shortWeekdays, i + 1, shortDayU);
        env->DeleteLocalRef(shortDayU);
    }

    ures_close(dayNameElemsFormat);
    ures_close(dayNameElems);
    ures_close(gregorianElems);
    return shortWeekdays;
}

static jstring getDecimalPatternChars(JNIEnv *env, UResourceBundle *rootElems) {
    
    UErrorCode status = U_ZERO_ERROR;

    int zeroL, digitL, decSepL, groupL, listL, percentL, permillL, expL, currSepL, minusL;
    int patternLength;

    jchar *patternChars;

    const jchar* zero = ures_getStringByIndex(rootElems, 4, &zeroL, &status);

    const jchar* digit = ures_getStringByIndex(rootElems, 5, &digitL, &status);

    const jchar* decSep = ures_getStringByIndex(rootElems, 0, &decSepL, &status);

    const jchar* group = ures_getStringByIndex(rootElems, 1, &groupL, &status);

    const jchar* list = ures_getStringByIndex(rootElems, 2, &listL, &status);

    const jchar* percent = ures_getStringByIndex(rootElems, 3, &percentL, &status);

    const jchar* permill = ures_getStringByIndex(rootElems, 8, &permillL, &status);

    const jchar* exp = ures_getStringByIndex(rootElems, 7, &expL, &status);

    const jchar* currSep = ures_getStringByIndex(rootElems, 0, &currSepL, &status);

    const jchar* minus = ures_getStringByIndex(rootElems, 6, &minusL, &status);

    if(U_FAILURE(status)) {
        return NULL;
    }


    patternChars = (jchar *) malloc(11 * sizeof(jchar));

    patternChars[0] = 0;

    u_strncat(patternChars, zero, 1);
    u_strncat(patternChars, digit, 1);
    u_strncat(patternChars, decSep, 1);
    u_strncat(patternChars, group, 1);
    u_strncat(patternChars, list, 1);
    u_strncat(patternChars, percent, 1);
    u_strncat(patternChars, permill, 1);
    u_strncat(patternChars, exp, 1);
    u_strncat(patternChars, currSep, 1);
    u_strncat(patternChars, minus, 1);

    jstring decimalPatternChars = env->NewString(patternChars, 10);

    free(patternChars);

    return decimalPatternChars;
}

static jstring getIntCurrencyCode(JNIEnv *env, jclass clazz, jstring locale) {

    const char *locStr = env->GetStringUTFChars(locale, NULL);
    char country[3] = {0,0,0};

    // getting the 2 character country name
    if(strlen(locStr) < 5) {
        env->ReleaseStringUTFChars(locale, locStr);
        return NULL;
    }
    if(locStr[3] < 'A' || locStr[3] > 'Z' || locStr[4] < 'A' || locStr[4] > 'Z') {
        env->ReleaseStringUTFChars(locale, locStr);
        return NULL;
    }
    country[0] = locStr[3];
    country[1] = locStr[4];

    env->ReleaseStringUTFChars(locale, locStr);

    return getCurrencyCodeNative(env, clazz, env->NewStringUTF(country));
}

static jstring getCurrencySymbol(JNIEnv *env, jclass clazz, jstring locale, jstring intCurrencySymbol) {

    jstring result = getCurrencySymbolNative(env, clazz, locale, intCurrencySymbol);
    if(result == intCurrencySymbol) {
        return NULL;
    }
    return result;

}

static jobjectArray getContentImpl(JNIEnv* env, jclass clazz, 
        jstring locale, jboolean needsTZ) {
    
    UErrorCode status = U_ZERO_ERROR;

    const char *loc = env->GetStringUTFChars(locale, NULL);
    UResourceBundle *root = ures_openU(NULL, loc, &status);

    env->ReleaseStringUTFChars(locale, loc);
    if(U_FAILURE(status)) {
        LOGI("Error getting resources");
        status = U_ZERO_ERROR;
        return NULL;
    }



    jclass obj_class = env->FindClass("java/lang/Object");
    jclass integer_class = env->FindClass("java/lang/Integer");
    jmethodID integerInit = env->GetMethodID(integer_class, "<init>", "(I)V");
    jobjectArray result;

    jobject firstDayOfWeek = NULL;
    jobject minimalDaysInFirstWeek = NULL;
    jobjectArray amPmMarkers = NULL;
    jobjectArray eras = NULL;
    jstring localPatternChars = NULL;
    jobjectArray weekdays = NULL;
    jobjectArray shortWeekdays = NULL;
    jobjectArray months = NULL;
    jobjectArray shortMonths = NULL;
    jstring time_SHORT = NULL;
    jstring time_MEDIUM = NULL;
    jstring time_LONG = NULL;
    jstring time_FULL = NULL;
    jstring date_SHORT = NULL;
    jstring date_MEDIUM = NULL;
    jstring date_LONG = NULL;
    jstring date_FULL = NULL;
    jstring decimalPatternChars = NULL;
    jstring naN = NULL;
    jstring infinity = NULL;
    jstring currencySymbol = NULL;
    jstring intCurrencySymbol = NULL;
    jstring numberPattern = NULL;
    jstring integerPattern = NULL;
    jstring currencyPattern = NULL;
    jstring percentPattern = NULL;
    jobjectArray zones = NULL;

    int counter = 0;

    int firstDayVals[2] = {-1, -1};

    const jchar* nan = (const jchar *)NULL;
    const jchar* inf = (const jchar *)NULL;
    int nanL, infL;


    UResourceBundle *gregorian;
    UResourceBundle *gregorianElems;
    UResourceBundle *rootElems;




    // get the resources needed
    rootElems = ures_getByKey(root, "calendar", NULL, &status);
    if(U_FAILURE(status)) {
        return NULL;
    }

    gregorian = ures_getByKey(rootElems, "gregorian", NULL, &status);
    if(U_FAILURE(status)) {
        ures_close(rootElems);
        return NULL;
    }



    // adding the first day of week and minimal days in first week values
    getDayInitVector(env, gregorian, firstDayVals);
    if((firstDayVals[0] != -1) && (firstDayVals[1] != -1)) {
        firstDayOfWeek = env->NewObject(integer_class, integerInit, firstDayVals[0]);
        minimalDaysInFirstWeek = env->NewObject(integer_class, integerInit, firstDayVals[1]);
        // adding First_Day and Minimal_Days integer to the result
        counter += 2;
    }


    // adding ampm string array to the result");
    amPmMarkers = getAmPmMarkers(env, gregorian);
    if(amPmMarkers != NULL) {
        counter++;
    }


    // adding eras string array to the result
    eras = getEras(env, gregorian);
    if(eras != NULL) {
        counter++;
    }


    // local pattern chars are initially always the same
    localPatternChars = env->NewStringUTF("GyMdkHmsSEDFwWahKzZ");
    // adding local pattern chars string to the result
    counter++;


    // adding month names string array to the result
    months = getMonthNames(env, gregorian);
    if(months != NULL) {
        counter++;
    }


    // adding short month names string array to the result
    shortMonths = getShortMonthNames(env, gregorian);
    if(shortMonths != NULL) {
        counter++;
    }


    // adding day names string array to the result
    weekdays = getWeekdayNames(env, gregorian);
    if(weekdays != NULL) {
        counter++;
    }


    // adding short day names string array to the result
    shortWeekdays = getShortWeekdayNames(env, gregorian);
    if(shortWeekdays != NULL) {
        counter++;
    }

    const UChar *pattern;
    jchar check[2] = {0, 0};
    u_uastrcpy(check, "v");
    jchar replacement[2] = {0, 0};
    u_uastrcpy(replacement, "z");
    jchar *pos;
    jchar *patternCopy;
    int patternLength;

    // adding date and time format patterns to the result
    gregorianElems = ures_getByKey(gregorian, "DateTimePatterns", NULL, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }

    pattern = ures_getStringByIndex(gregorianElems, 0, &patternLength, &status);
    // there are some patterns in icu that use the pattern character 'v'
    // java doesn't accept this, so it gets replaced by 'z' which has
    // about the same result as 'v', the timezone name. 
    // 'v' -> "PT", 'z' -> "PST", v is the generic timezone and z the standard tz
    // "vvvv" -> "Pacific Time", "zzzz" -> "Pacific Standard Time"
    patternCopy = (jchar *) malloc((patternLength + 1) * sizeof(jchar));
    u_strcpy(patternCopy, pattern);
    if(U_FAILURE(status)) {
        free(patternCopy);
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }
    while((pos = u_strchr(patternCopy, check[0])) != NULL) {
        u_memset(pos, replacement[0], 1);
    }
    time_FULL = env->NewString(patternCopy, patternLength);
    free(patternCopy);
    counter++;

    pattern = ures_getStringByIndex(gregorianElems, 1, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }
    time_LONG = env->NewString(pattern, patternLength);
    counter++;

    pattern = ures_getStringByIndex(gregorianElems, 2, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }
    time_MEDIUM = env->NewString(pattern, patternLength);
    counter++;

    pattern = ures_getStringByIndex(gregorianElems, 3, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }
    time_SHORT = env->NewString(pattern, patternLength);
    counter++;

    pattern = ures_getStringByIndex(gregorianElems, 4, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }
    date_FULL = env->NewString(pattern, patternLength);
    counter++;

    pattern = ures_getStringByIndex(gregorianElems, 5, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }
    date_LONG = env->NewString(pattern, patternLength);
    counter++;

    pattern = ures_getStringByIndex(gregorianElems, 6, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }
    date_MEDIUM = env->NewString(pattern, patternLength);
    counter++;

    pattern = ures_getStringByIndex(gregorianElems, 7, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto endOfCalendar;
    }
    date_SHORT = env->NewString(pattern, patternLength);
    counter++;


endOfCalendar:

    if(gregorianElems != NULL) {
        ures_close(gregorianElems);
    }
    ures_close(gregorian);
    ures_close(rootElems);


    rootElems = ures_getByKey(root, "NumberElements", NULL, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
    }

    if(ures_getSize(rootElems) >= 11) {

        // adding decimal pattern chars to the result
        decimalPatternChars = getDecimalPatternChars(env, rootElems);
        if(decimalPatternChars != NULL) {
            counter++;
        }

        // adding NaN pattern char to the result
        nan = ures_getStringByIndex(rootElems, 10, &nanL, &status);
        if(U_SUCCESS(status)) {
            naN = env->NewString(nan, nanL);
            counter++;
        }
        status = U_ZERO_ERROR;

        // adding infinity pattern char to the result
        inf = ures_getStringByIndex(rootElems, 9, &infL, &status);
        if(U_SUCCESS(status)) {
            infinity = env->NewString(inf, infL);
            counter++;
        }
        status = U_ZERO_ERROR;
    }

    ures_close(rootElems);


    // adding intl currency code to result
    intCurrencySymbol = getIntCurrencyCode(env, clazz, locale);
    if(intCurrencySymbol != NULL) {
        // adding currency symbol to result
        currencySymbol = getCurrencySymbol(env, clazz, locale, intCurrencySymbol);
    } else {
        intCurrencySymbol = env->NewStringUTF("XXX");
    }
    if(currencySymbol == NULL) {
        currencySymbol = env->NewStringUTF("\u00a4");
    }
    counter += 2;


    // adding number format patterns to the result
    int numOfEntries;
    int decSepOffset;
    NumberFormat *nf;
    jchar *tmpPattern;

    rootElems = ures_getByKey(root, "NumberPatterns", NULL, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        goto zones;
    }

    numOfEntries = ures_getSize(rootElems);
    if(numOfEntries < 3) {
        ures_close(rootElems);
        goto zones;
    }

    // number pattern
    pattern = ures_getStringByIndex(rootElems, 0, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        ures_close(rootElems);
        goto zones;
    }
    numberPattern = env->NewString(pattern, patternLength);
    counter++;

    // integer pattern derived from number pattern
    // We need to convert a C string literal to a UChar string for u_strcspn.
    static const char c_decSep[] = ".";
    UChar decSep[sizeof(c_decSep)];
    u_charsToUChars(c_decSep, decSep, sizeof(c_decSep));
    decSepOffset = u_strcspn(pattern, decSep);
    tmpPattern =  (jchar *) malloc((decSepOffset + 1) * sizeof(jchar));
    u_strncpy(tmpPattern, pattern, decSepOffset);
    integerPattern = env->NewString(tmpPattern, decSepOffset);
    free(tmpPattern);
    counter++;

    // currency pattern
    pattern = ures_getStringByIndex(rootElems, 1, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        ures_close(rootElems);
        goto zones;
    }
    currencyPattern = env->NewString(pattern, patternLength);
    counter++;

    // percent pattern
    pattern = ures_getStringByIndex(rootElems, 2, &patternLength, &status);
    if(U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        ures_close(rootElems);
        goto zones;
    }
    percentPattern = env->NewString(pattern, patternLength);
    counter++;

    ures_close(rootElems);

zones:

    ures_close(root);


    if(needsTZ == JNI_TRUE) {
        counter++; //add empty timezone
    }



    // collect all content and put it into an array
    result = env->NewObjectArray(counter, obj_class, NULL);

    int index = 0;
    
    if(needsTZ == JNI_TRUE) {
        addObject(env, result, "timezones", NULL, index++);
    }
    if(firstDayOfWeek != NULL && index < counter) {
        addObject(env, result, "First_Day", firstDayOfWeek, index++);
    }
    if(minimalDaysInFirstWeek != NULL && index < counter) {
        addObject(env, result, "Minimal_Days", minimalDaysInFirstWeek, index++);
    }
    if(amPmMarkers != NULL && index < counter) {
        addObject(env, result, "ampm", amPmMarkers, index++);
    }
    if(eras != NULL && index < counter) {
        addObject(env, result, "eras", eras, index++);
    }
    if(localPatternChars != NULL && index < counter) {
        addObject(env, result, "LocalPatternChars", localPatternChars, index++);
    }
    if(weekdays != NULL && index < counter) {
        addObject(env, result, "weekdays", weekdays, index++);
    }
    if(shortWeekdays != NULL && index < counter) {
        addObject(env, result, "shortWeekdays", shortWeekdays, index++);
    }
    if(months != NULL && index < counter) {
        addObject(env, result, "months", months, index++);
    }
    if(shortMonths != NULL && index < counter) {
        addObject(env, result, "shortMonths", shortMonths, index++);
    }
    if(time_SHORT != NULL && index < counter) {
        addObject(env, result, "Time_SHORT", time_SHORT, index++);
    }
    if(time_MEDIUM != NULL && index < counter) {
        addObject(env, result, "Time_MEDIUM", time_MEDIUM, index++);
    }
    if(time_LONG != NULL && index < counter) {
        addObject(env, result, "Time_LONG", time_LONG, index++);
    }
    if(time_FULL != NULL && index < counter) {
        addObject(env, result, "Time_FULL", time_FULL, index++);
    }
    if(date_SHORT != NULL && index < counter) {
        addObject(env, result, "Date_SHORT", date_SHORT, index++);
    }
    if(date_MEDIUM != NULL && index < counter) {
        addObject(env, result, "Date_MEDIUM", date_MEDIUM, index++);
    }
    if(date_LONG != NULL && index < counter) {
        addObject(env, result, "Date_LONG", date_LONG, index++);
    }
    if(date_FULL != NULL && index < counter) {
        addObject(env, result, "Date_FULL", date_FULL, index++);
    }
    if(decimalPatternChars != NULL && index < counter) {
        addObject(env, result, "DecimalPatternChars", decimalPatternChars, index++);
    }
    if(naN != NULL && index < counter) {
        addObject(env, result, "NaN", naN, index++);
    }
    if(infinity != NULL && index < counter) {
        addObject(env, result, "Infinity", infinity, index++);
    }
    if(currencySymbol != NULL && index < counter) {
        addObject(env, result, "CurrencySymbol", currencySymbol, index++);
    }
    if(intCurrencySymbol != NULL && index < counter) {
        addObject(env, result, "IntCurrencySymbol", intCurrencySymbol, index++);
    }
    if(numberPattern != NULL && index < counter) {
        addObject(env, result, "Number", numberPattern, index++);
    }
    if(integerPattern != NULL && index < counter) {
        addObject(env, result, "Integer", integerPattern, index++);
    }
    if(currencyPattern != NULL && index < counter) {
        addObject(env, result, "Currency", currencyPattern, index++);
    }
    if(percentPattern != NULL && index < counter) {
        addObject(env, result, "Percent", percentPattern, index++);
    }

    return result;

}

static JNINativeMethod gMethods[] = {
    /* name, signature, funcPtr */
    {"getFractionDigitsNative", "(Ljava/lang/String;)I",                   
            (void*) getFractionDigitsNative},
    {"getCurrencyCodeNative", "(Ljava/lang/String;)Ljava/lang/String;",
            (void*) getCurrencyCodeNative},
    {"getCurrencySymbolNative", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            (void*) getCurrencySymbolNative},
    {"getDisplayCountryNative", 
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            (void*) getDisplayCountryNative},
    {"getDisplayLanguageNative", 
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            (void*) getDisplayLanguageNative},
    {"getDisplayVariantNative",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            (void*) getDisplayVariantNative},
    {"getISO3CountryNative",
            "(Ljava/lang/String;)Ljava/lang/String;",
            (void*) getISO3CountryNative},
    {"getISO3LanguageNative",
            "(Ljava/lang/String;)Ljava/lang/String;",
            (void*) getISO3LanguageNative},
    {"getISOCountriesNative", "()[Ljava/lang/String;",
            (void*) getISOCountriesNative},
    {"getISOLanguagesNative", "()[Ljava/lang/String;",
            (void*) getISOLanguagesNative},
    {"getAvailableLocalesNative", "()[Ljava/lang/String;",
            (void*) getAvailableLocalesNative},
    {"getTimeZonesNative", 
            "([[Ljava/lang/String;Ljava/lang/String;)V",
            (void*) getTimeZonesNative},
    {"getDisplayTimeZoneNative", 
            "(Ljava/lang/String;ZILjava/lang/String;)Ljava/lang/String;",
            (void*) getDisplayTimeZoneNative},
    {"getContentImpl", 
            "(Ljava/lang/String;Z)[[Ljava/lang/Object;",
            (void*) getContentImpl},
};

int register_com_ibm_icu4jni_util_Resources(JNIEnv* env) {
    
    // initializing String

    jclass stringclass = env->FindClass("java/lang/String");

    if(stringclass == NULL) {
        LOGE("Can't find java/lang/String");
        jniThrowException(env, "java/lang/ClassNotFoundException", "java.lang.String");
        return -1;
    }
    
    string_class = (jclass) env->NewGlobalRef(stringclass);
    
    return jniRegisterNativeMethods(env, 
            "com/ibm/icu4jni/util/Resources", gMethods, 
            NELEM(gMethods));
}
