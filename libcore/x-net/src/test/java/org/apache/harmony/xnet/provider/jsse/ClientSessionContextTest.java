/*
 * Copyright (C) 2009 The Android Open Source Project
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

package org.apache.harmony.xnet.provider.jsse;

import junit.framework.TestCase;

import javax.net.ssl.SSLSession;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;

public class ClientSessionContextTest extends TestCase {

    public void testGetSessionById() {
        ClientSessionContext context = new ClientSessionContext(null, null);

        SSLSession a = new FakeSession("a");
        SSLSession b = new FakeSession("b");

        context.putSession(a);
        context.putSession(b);

        assertSame(a, context.getSession("a".getBytes()));
        assertSame(b, context.getSession("b".getBytes()));

        assertSame(a, context.getSession("a", 443));
        assertSame(b, context.getSession("b", 443));

        assertEquals(2, context.sessions.size());

        Set<SSLSession> sessions = new HashSet<SSLSession>();
        Enumeration ids = context.getIds();
        while (ids.hasMoreElements()) {
            sessions.add(context.getSession((byte[]) ids.nextElement()));
        }

        Set<SSLSession> expected = new HashSet<SSLSession>();
        expected.add(a);
        expected.add(b);

        assertEquals(expected, sessions);
    }

    public void testTrimToSize() {
        ClientSessionContext context = new ClientSessionContext(null, null);

        FakeSession a = new FakeSession("a");
        FakeSession b = new FakeSession("b");
        FakeSession c = new FakeSession("c");
        FakeSession d = new FakeSession("d");

        context.putSession(a);
        context.putSession(b);
        context.putSession(c);
        context.putSession(d);

        context.setSessionCacheSize(2);

        Set<SSLSession> sessions = new HashSet<SSLSession>();
        Enumeration ids = context.getIds();
        while (ids.hasMoreElements()) {
            sessions.add(context.getSession((byte[]) ids.nextElement()));
        }

        Set<SSLSession> expected = new HashSet<SSLSession>();
        expected.add(c);
        expected.add(d);

        assertEquals(expected, sessions);               
    }

}
