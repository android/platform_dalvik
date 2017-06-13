/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstCallSite;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;

/**
 * Representation of a call site reference in a DEX file.
 */
public final class CallSiteIdItem extends IndexedItem implements Comparable {

    /** The item size when placed in a DEX file. */
    private static final int ITEM_SIZE = 4;

    /** {@code non-null;} The call site represented by this identifier. */
    final CstCallSite callSite;

    /** {@code null-ok;} The call site as a DEX file item. */
    CallSiteItem data;

    /**
     * Constructs an instance.
     *
     * @param callSite {@code non-null;} The call site to represent in the DEX file.
     */
    public CallSiteIdItem(CstCallSite callSite) {
        this.callSite = callSite;
        this.data = null;
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_CALL_SITE_ID_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public int writeSize() {
        return ITEM_SIZE;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        if (data == null) {
            MixedItemSection byteData = file.getByteData();
            data = new CallSiteItem(callSite);
            byteData.add(data);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(DexFile file, AnnotatedOutput out) {
        int offset = data.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(0, indexString() + ' ' + callSite.toString());
            out.annotate(4, "call_site_off: " + Hex.u4(offset));
        }
        out.writeInt(offset);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Object o) {
        CallSiteIdItem other = (CallSiteIdItem) o;
        return callSite.compareTo(other.callSite);
    }
}
