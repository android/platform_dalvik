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

package com.ibm.icu4jni.text;

import com.ibm.icu4jni.text.NativeDecimalFormat.UNumberFormatAttribute;
import com.ibm.icu4jni.text.NativeDecimalFormat.UNumberFormatTextAttribute;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Currency;
import java.util.Locale;

public class DecimalFormat extends NumberFormat {

    private int addr;

    private DecimalFormatSymbols symbols;

    // fix to be icu4j conform (harmony wants this field to exist)
    // for serialization of java.text.DecimalFormat
    @SuppressWarnings("unused")
    private boolean useExponentialNotation = false;
    @SuppressWarnings("unused")
    private byte minExponentDigits = 0;

    private boolean negPrefNull;
    private boolean negSuffNull;
    private boolean posPrefNull;
    private boolean posSuffNull;

    public DecimalFormat(String pattern, DecimalFormatSymbols icuSymbols) {
        this.addr = icuSymbols.getAddr();
        this.symbols = icuSymbols;
        applyPattern(pattern);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 37 + this.getPositivePrefix().hashCode();
    }

    @Override
    public Object clone() {
        String pat = this.toPattern();
        Locale loc = this.symbols.getLocale();
        DecimalFormatSymbols sym = (DecimalFormatSymbols) this.symbols.clone();
        DecimalFormat newdf = new DecimalFormat(pat, sym);
        newdf.setMaximumIntegerDigits(this.getMaximumIntegerDigits());
        newdf.setMaximumFractionDigits(this.getMaximumFractionDigits());
        newdf.setMinimumIntegerDigits(this.getMinimumIntegerDigits());
        newdf.setMinimumFractionDigits(this.getMinimumFractionDigits());
        newdf.setGroupingUsed(this.isGroupingUsed());
        newdf.setGroupingSize(this.getGroupingSize());
        return newdf;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof DecimalFormat)) {
            return false;
        }
        DecimalFormat obj = (DecimalFormat) object;

        if(obj.addr == this.addr) {
            return true;
        }

        boolean result = super.equals(object);


        result &= obj.toPattern().equals(this.toPattern());
        result &= obj.isDecimalSeparatorAlwaysShown() == this.isDecimalSeparatorAlwaysShown();
        result &= obj.getGroupingSize() == this.getGroupingSize();
        result &= obj.getMultiplier() == this.getMultiplier();
        result &= obj.getNegativePrefix().equals(this.getNegativePrefix());
        result &= obj.getNegativeSuffix().equals(this.getNegativeSuffix());
        result &= obj.getPositivePrefix().equals(this.getPositivePrefix());
        result &= obj.getPositiveSuffix().equals(this.getPositiveSuffix());
        result &= obj.getMaximumIntegerDigits() == this.getMaximumIntegerDigits();
        result &= obj.getMaximumFractionDigits() == this.getMaximumFractionDigits();
        result &= obj.getMinimumIntegerDigits() == this.getMinimumIntegerDigits();
        result &= obj.getMinimumFractionDigits() == this.getMinimumFractionDigits();
        result &= obj.isGroupingUsed() == this.isGroupingUsed();
        Currency objCurr = obj.getCurrency();
        Currency thisCurr = this.getCurrency();
        if(objCurr != null) {
            result &= objCurr.getCurrencyCode().equals(thisCurr.getCurrencyCode());
            result &= objCurr.getSymbol().equals(thisCurr.getSymbol());
            result &= objCurr.getDefaultFractionDigits() == thisCurr.getDefaultFractionDigits();
        } else {
            result &= thisCurr == null;
        }
        result &= obj.getDecimalFormatSymbols().equals(this.getDecimalFormatSymbols());

        return result;
    }

    @Override
    public StringBuffer format(Object value, StringBuffer buffer, FieldPosition field) {

        if(!(value instanceof Number)) {
            throw new IllegalArgumentException();
        }
        if(buffer == null || field == null) {
            throw new NullPointerException();
        }

        String fieldType = getFieldType(field.getFieldAttribute());

        Number number = (Number) value;

        if(number instanceof BigInteger) {
            BigInteger valBigInteger = (BigInteger) number;
            String result = NativeDecimalFormat.format(this.addr,
                    valBigInteger.toString(10), field, fieldType, null, 0);
            return buffer.append(result);
        } else if(number instanceof BigDecimal) {
            BigDecimal valBigDecimal = (BigDecimal) number;
            StringBuilder val = new StringBuilder();
            val.append(valBigDecimal.unscaledValue().toString(10));
            int scale = valBigDecimal.scale();
            scale = makeScalePositive(scale, val);
            String result = NativeDecimalFormat.format(this.addr,
                    val.toString(), field, fieldType, null, scale);
            return buffer.append(result);
        } else {
            double dv = number.doubleValue();
            long lv = number.longValue();
            if (dv == lv) {
                String result = NativeDecimalFormat.format(this.addr, lv, field,
                        fieldType, null);
                return buffer.append(result);
            }
            String result = NativeDecimalFormat.format(this.addr, dv, field,
                    fieldType, null);
            return buffer.append(result);
        }
    }

    @Override
    public StringBuffer format(long value, StringBuffer buffer, FieldPosition field) {

        if(buffer == null || field == null) {
            throw new NullPointerException();
        }

        String fieldType = getFieldType(field.getFieldAttribute());

        String result = NativeDecimalFormat.format(this.addr, value, field,
                fieldType, null);

        buffer.append(result.toCharArray(), 0, result.length());

        return buffer;
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {

        if(buffer == null || field == null) {
            throw new NullPointerException();
        }

        String fieldType = getFieldType(field.getFieldAttribute());

        String result = NativeDecimalFormat.format(this.addr, value, field,
                fieldType, null);

        buffer.append(result.toCharArray(), 0, result.length());

        return buffer;
    }

    public void applyLocalizedPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern was null");
        }
        try {
            NativeDecimalFormat.applyPatternImpl(this.addr, false, pattern);
        } catch(RuntimeException re) {
            throw new IllegalArgumentException(
                    "applying localized pattern failed for pattern: " + pattern, re);
        }
    }

    public void applyPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern was null");
        }
        try {
            NativeDecimalFormat.applyPatternImpl(this.addr, false, pattern);
        } catch(RuntimeException re) {
            throw new IllegalArgumentException(
                    "applying pattern failed for pattern: " + pattern, re);
        }
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object object) {
        if (!(object instanceof Number)) {
            throw new IllegalArgumentException();
        }
        Number number = (Number) object;
        String text = null;
        StringBuffer attributes = new StringBuffer();

        if(number instanceof BigInteger) {
            BigInteger valBigInteger = (BigInteger) number;
            text = NativeDecimalFormat.format(this.addr,
                    valBigInteger.toString(10), null, null, attributes, 0);
        } else if(number instanceof BigDecimal) {
            BigDecimal valBigDecimal = (BigDecimal) number;
            StringBuilder val = new StringBuilder();
            val.append(valBigDecimal.unscaledValue().toString(10));
            int scale = valBigDecimal.scale();
            scale = makeScalePositive(scale, val);
            text = NativeDecimalFormat.format(this.addr, val.toString(), null,
                    null, attributes, scale);
        } else {
            double dv = number.doubleValue();
            long lv = number.longValue();
            if (dv == lv) {
                text = NativeDecimalFormat.format(this.addr, lv, null,
                        null, attributes);
            } else {
                text = NativeDecimalFormat.format(this.addr, dv, null,
                        null, attributes);
            }
        }

        AttributedString as = new AttributedString(text.toString());

        String[] attrs = attributes.toString().split(";");
        // add NumberFormat field attributes to the AttributedString
        int size = attrs.length / 3;
        if(size * 3 != attrs.length) {
            return as.getIterator();
        }
        for (int i = 0; i < size; i++) {
            Format.Field attribute = getField(attrs[3*i]);
            as.addAttribute(attribute, attribute, Integer.parseInt(attrs[3*i+1]),
                    Integer.parseInt(attrs[3*i+2]));
        }

        // return the CharacterIterator from AttributedString
        return as.getIterator();
    }

    private int makeScalePositive(int scale, StringBuilder val) {
        if (scale < 0) {
            scale = -scale;
            for (int i = scale; i > 0; i--) {
                val.append('0');
            }
            scale = 0;
        }
        return scale;
    }

    public String toLocalizedPattern() {
        return NativeDecimalFormat.toPatternImpl(this.addr, true);
    }

    public String toPattern() {
        return NativeDecimalFormat.toPatternImpl(this.addr, false);
    }

    @Override
    public Number parse(String string, ParsePosition position) {
        return NativeDecimalFormat.parse(addr, string, position);
    }

    // start getter and setter

    @Override
    public int getMaximumFractionDigits() {
        return NativeDecimalFormat.getAttribute(this .addr,
                UNumberFormatAttribute.UNUM_MAX_FRACTION_DIGITS.ordinal());
    }

    @Override
    public int getMaximumIntegerDigits() {
        return NativeDecimalFormat.getAttribute(this .addr,
                UNumberFormatAttribute.UNUM_MAX_INTEGER_DIGITS.ordinal());
    }

    @Override
    public int getMinimumFractionDigits() {
        return NativeDecimalFormat.getAttribute(this .addr,
                UNumberFormatAttribute.UNUM_MIN_FRACTION_DIGITS.ordinal());
    }

    @Override
    public int getMinimumIntegerDigits() {
        return NativeDecimalFormat.getAttribute(this .addr,
                UNumberFormatAttribute.UNUM_MIN_INTEGER_DIGITS.ordinal());
    }

    @Override
    public Currency getCurrency() {
        return this.symbols.getCurrency();
    }

    public int getGroupingSize() {
        return NativeDecimalFormat.getAttribute(this.addr,
                UNumberFormatAttribute.UNUM_GROUPING_SIZE.ordinal());
    }

    public int getMultiplier() {
        return NativeDecimalFormat.getAttribute(this.addr,
                UNumberFormatAttribute.UNUM_MULTIPLIER.ordinal());
    }

    public String getNegativePrefix() {
        if (negPrefNull) {
            return null;
        }
        return NativeDecimalFormat.getTextAttribute(this.addr,
                UNumberFormatTextAttribute.UNUM_NEGATIVE_PREFIX.ordinal());
    }

    public String getNegativeSuffix() {
        if (negSuffNull) {
            return null;
        }
        return NativeDecimalFormat.getTextAttribute(this.addr,
                UNumberFormatTextAttribute.UNUM_NEGATIVE_SUFFIX.ordinal());
    }

    public String getPositivePrefix() {
        if (posPrefNull) {
            return null;
        }
        return NativeDecimalFormat.getTextAttribute(this.addr,
                UNumberFormatTextAttribute.UNUM_POSITIVE_PREFIX.ordinal());
    }

    public String getPositiveSuffix() {
        if (posSuffNull) {
            return null;
        }
        return NativeDecimalFormat.getTextAttribute(this.addr,
                UNumberFormatTextAttribute.UNUM_POSITIVE_SUFFIX.ordinal());
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return NativeDecimalFormat.getAttribute(this.addr,
                UNumberFormatAttribute.UNUM_DECIMAL_ALWAYS_SHOWN.ordinal()) != 0;
    }

    @Override
    public boolean isParseIntegerOnly() {
        return NativeDecimalFormat.getAttribute(this.addr,
                UNumberFormatAttribute.UNUM_PARSE_INT_ONLY.ordinal()) != 0;
    }

    @Override
    public boolean isGroupingUsed() {
        return NativeDecimalFormat.getAttribute(this.addr,
                UNumberFormatAttribute.UNUM_GROUPING_USED.ordinal()) != 0;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return this.symbols;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols icuSymbols) {
        this.symbols = icuSymbols;
    }

    public void setDecimalSeparatorAlwaysShown(boolean value) {
        int i = value ? -1 : 0;
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_DECIMAL_ALWAYS_SHOWN.ordinal(), i);
    }

    @Override
    public void setCurrency(Currency currency) {
        this.symbols.setCurrency(currency);
    }

    public void setGroupingSize(int value) {
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_GROUPING_SIZE.ordinal(), value);
    }

    @Override
    public void setGroupingUsed(boolean value) {
        int i = value ? -1 : 0;
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_GROUPING_USED.ordinal(), i);
    }

    @Override
    public void setMaximumFractionDigits(int value) {
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_MAX_FRACTION_DIGITS.ordinal(), value);
    }

    @Override
    public void setMaximumIntegerDigits(int value) {
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_MAX_INTEGER_DIGITS.ordinal(), value);
    }

    @Override
    public void setMinimumFractionDigits(int value) {
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_MIN_FRACTION_DIGITS.ordinal(), value);
    }

    @Override
    public void setMinimumIntegerDigits(int value) {
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_MIN_INTEGER_DIGITS.ordinal(), value);
    }

    public void setMultiplier(int value) {
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_MULTIPLIER.ordinal(), value);
    }

    public void setNegativePrefix(String value) {
        negPrefNull = value == null;
        if (!negPrefNull) {
            NativeDecimalFormat.setTextAttribute(this.addr,
                    UNumberFormatTextAttribute.UNUM_NEGATIVE_PREFIX.ordinal(),
                    value);
        }
    }

    public void setNegativeSuffix(String value) {
        negSuffNull = value == null;
        if (!negSuffNull) {
            NativeDecimalFormat.setTextAttribute(this.addr,
                    UNumberFormatTextAttribute.UNUM_NEGATIVE_SUFFIX.ordinal(),
                    value);
        }
    }

    public void setPositivePrefix(String value) {
        posPrefNull = value == null;
        if (!posPrefNull) {
            NativeDecimalFormat.setTextAttribute(this.addr,
                    UNumberFormatTextAttribute.UNUM_POSITIVE_PREFIX.ordinal(),
                    value);
        }
    }

    public void setPositiveSuffix(String value) {
        posSuffNull = value == null;
        if (!posSuffNull) {
            NativeDecimalFormat.setTextAttribute(this.addr,
                    UNumberFormatTextAttribute.UNUM_POSITIVE_SUFFIX.ordinal(),
                    value);
        }
    }

    @Override
    public void setParseIntegerOnly(boolean value) {
        int i = value ? -1 : 0;
        NativeDecimalFormat.setAttribute(this.addr,
                UNumberFormatAttribute.UNUM_PARSE_INT_ONLY.ordinal(), i);
    }

    static protected String getFieldType(Format.Field field) {
        if(field == null) {
            return null;
        }
        if(field.equals(NumberFormat.Field.SIGN)) {
            return "sign";
        }
        if(field.equals(NumberFormat.Field.INTEGER)) {
            return "integer";
        }
        if(field.equals(NumberFormat.Field.FRACTION)) {
            return "fraction";
        }
        if(field.equals(NumberFormat.Field.EXPONENT)) {
            return "exponent";
        }
        if(field.equals(NumberFormat.Field.EXPONENT_SIGN)) {
            return "exponent_sign";
        }
        if(field.equals(NumberFormat.Field.EXPONENT_SYMBOL)) {
            return "exponent_symbol";
        }
        if(field.equals(NumberFormat.Field.CURRENCY)) {
            return "currency";
        }
        if(field.equals(NumberFormat.Field.GROUPING_SEPARATOR)) {
            return "grouping_separator";
        }
        if(field.equals(NumberFormat.Field.DECIMAL_SEPARATOR)) {
            return "decimal_separator";
        }
        if(field.equals(NumberFormat.Field.PERCENT)) {
            return "percent";
        }
        if(field.equals(NumberFormat.Field.PERMILLE)) {
            return "permille";
        }
        return null;
    }

    protected Format.Field getField(String type) {
        if(type.equals("")) {
            return null;
        }
        if(type.equals("sign")) {
            return NumberFormat.Field.SIGN;
        }
        if(type.equals("integer")) {
            return NumberFormat.Field.INTEGER;
        }
        if(type.equals("fraction")) {
            return NumberFormat.Field.FRACTION;
        }
        if(type.equals("exponent")) {
            return NumberFormat.Field.EXPONENT;
        }
        if(type.equals("exponent_sign")) {
            return NumberFormat.Field.EXPONENT_SIGN;
        }
        if(type.equals("exponent_symbol")) {
            return NumberFormat.Field.EXPONENT_SYMBOL;
        }
        if(type.equals("currency")) {
            return NumberFormat.Field.CURRENCY;
        }
        if(type.equals("grouping_separator")) {
            return NumberFormat.Field.GROUPING_SEPARATOR;
        }
        if(type.equals("decimal_separator")) {
            return NumberFormat.Field.DECIMAL_SEPARATOR;
        }
        if(type.equals("percent")) {
            return NumberFormat.Field.PERCENT;
        }
        if(type.equals("permille")) {
            return NumberFormat.Field.PERMILLE;
        }
        return null;
    }
}
