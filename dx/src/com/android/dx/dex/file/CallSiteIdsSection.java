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

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstCallSite;
import java.util.Collection;
import java.util.TreeMap;

/**
 * A section in the DEX file for call site identifiers.
 */
public final class CallSiteIdsSection extends UniformItemSection {

    /** A map from call site constant to their DEX file identifier. */
    private final TreeMap<CstCallSite, CallSiteIdItem> callSites = new TreeMap<>();

    /**
     * Constructs an instance.
     *
     * @param dexFile {@code non-null;} file that this instance is part of
     */
    public CallSiteIdsSection(DexFile dexFile) {
        super("call_site_ids", dexFile, 4);
    }

    /** {@inheritDoc} */
    @Override
    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();

        IndexedItem result = callSites.get((CstCallSite) cst);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void orderItems() {
        int index = 0;
        for (CallSiteIdItem callSiteId : callSites.values()) {
            callSiteId.setIndex(index++);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        return callSites.values();
    }

    /**
     * Interns a call site into this instance.
     *
     * @param callSite {@code non-null;} the reference to intern
     */
    public void intern(CstCallSite callSite) {
        if (callSite == null) {
            throw new NullPointerException("callSite == null");
        }

        throwIfPrepared();

        CallSiteIdItem result = callSites.get(callSite);
        if (result == null) {
            result = new CallSiteIdItem(callSite);
            callSites.put(callSite, result);
        }
    }
}
