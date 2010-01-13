/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.harmony.nio.internal;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.harmony.luni.platform.FileDescriptorHandler;
import org.apache.harmony.luni.platform.Platform;

/*
 * Default implementation of java.nio.channels.Selector
 * 
 */
final class SelectorImpl extends AbstractSelector {

    private static final int MOCK_WRITEBUF_SIZE = 1;

    private static final int MOCK_READBUF_SIZE = 8;

    private static final int NA = 0;

    private static final int READABLE = 1;

    private static final int WRITEABLE = 2;

    private static final int SELECT_BLOCK = -1;

    private static final int SELECT_NOW = 0;

    // keysLock is used to brief synchronization when get selectionKeys snapshot
    // before selection
    final Object keysLock = new Object();

    private final Set<SelectionKey> keys = new HashSet<SelectionKey>();

    private Set<SelectionKey> unmodifiableKeys = Collections
            .unmodifiableSet(keys);

    private final Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();

    private Set<SelectionKey> unaddableSelectedKeys = new UnaddableSet<SelectionKey>(
            selectedKeys);

    // sink and source are used by wakeup()
    private Pipe.SinkChannel sink;

    private Pipe.SourceChannel source;
    
    private FileDescriptor sourcefd;
    
    private SelectionKey[] readableChannels;

    private SelectionKey[] writableChannels;

    private List<FileDescriptor> readableFDs = new ArrayList<FileDescriptor>();

    private List<FileDescriptor> writableFDs = new ArrayList<FileDescriptor>();

    private FileDescriptor[] readable;

    private FileDescriptor[] writable;

    public SelectorImpl(SelectorProvider selectorProvider) {
        super(selectorProvider);
        try {
            Pipe mockSelector = selectorProvider.openPipe();
            sink = mockSelector.sink();
            source = mockSelector.source();
            sourcefd = ((FileDescriptorHandler)source).getFD();
            source.configureBlocking(false);
        } catch (IOException e) {
            // do nothing
        }
    }

    /*
     * @see java.nio.channels.spi.AbstractSelector#implCloseSelector()
     */
    protected void implCloseSelector() throws IOException {
        doCancel();
        for (SelectionKey sk : keys) {
            deregister((AbstractSelectionKey) sk);
        }
        wakeup();
    }

    /*
     * @see java.nio.channels.spi.AbstractSelector#register(java.nio.channels.spi.AbstractSelectableChannel,
     *      int, java.lang.Object)
     */
    protected SelectionKey register(AbstractSelectableChannel channel,
            int operations, Object attachment) {
        if (!provider().equals(channel.provider())) {
            throw new IllegalSelectorException();
        }
        synchronized (this) {
            synchronized (keys) {
                SelectionKey sk = new SelectionKeyImpl(channel, operations,
                        attachment, this);
                keys.add(sk);
                return sk;
            }
        }
    }

    /*
     * @see java.nio.channels.Selector#keys()
     */
    public synchronized Set<SelectionKey> keys() {
        closeCheck();
        return unmodifiableKeys;
    }

    private void closeCheck() {
        if (!isOpen()) {
            throw new ClosedSelectorException();
        }
    }

    /*
     * @see java.nio.channels.Selector#select()
     */
    public int select() throws IOException {
        return selectInternal(SELECT_BLOCK);
    }

    /*
     * @see java.nio.channels.Selector#select(long)
     */
    public int select(long timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException();
        }
        return selectInternal((0 == timeout) ? SELECT_BLOCK : timeout);
    }

    /*
     * @see java.nio.channels.Selector#selectNow()
     */
    public int selectNow() throws IOException {
        return selectInternal(SELECT_NOW);
    }

    private int selectInternal(long timeout) throws IOException {
        closeCheck();
        synchronized (this) {
            synchronized (keys) {
                synchronized (selectedKeys) {
                    doCancel();
                    int[] readyChannels = null;
                    boolean isBlock = (SELECT_NOW != timeout);
                    // BEGIN android-removed
                    // copied from newer version of harmony
                    // if (keys.size() == 0) {
                    //     return 0;
                    // }
                    // END android-removed
                    prepareChannels();
                    try {
                        if (isBlock) {
                            begin();
                        }
                        readyChannels = Platform.getNetworkSystem().select(readable, writable, timeout);
                    } finally {
                        // clear results for next select
                        readableFDs.clear();
                        writableFDs.clear();                        
                        if (isBlock) {
                            end();
                        }
                    }
                    return processSelectResult(readyChannels);                    
                }
            }
        }
    }

    private boolean isConnected(SelectionKeyImpl key) {
        SelectableChannel channel = key.channel();
        if (channel instanceof SocketChannel) {
            return ((SocketChannel) channel).isConnected();
        }
        return true;
    }

    private boolean isConnectionPending(SelectionKeyImpl key) {
        SelectableChannel channel = key.channel();
        if (channel instanceof SocketChannel) {
            return ((SocketChannel) channel).isConnectionPending();
        }
        return true;
    }

    // Prepares and adds channels to list for selection
    private void prepareChannels() {
        readableFDs.add(sourcefd);        
        List<SelectionKey> readChannelList = new ArrayList<SelectionKey>();
        readChannelList.add(source.keyFor(this));
        List<SelectionKey> writeChannelList = new ArrayList<SelectionKey>();
        synchronized (keysLock) {
            for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
                SelectionKeyImpl key = (SelectionKeyImpl) i.next();
                key.oldInterestOps = key.interestOps();
                boolean isReadableChannel = ((SelectionKey.OP_ACCEPT | SelectionKey.OP_READ) & key.oldInterestOps) != 0;
                boolean isWritableChannel = (((isConnectionPending(key) ? SelectionKey.OP_CONNECT : 0) | SelectionKey.OP_WRITE) & key.oldInterestOps) != 0;
                SelectableChannel channel = key.channel();                  
                if (isReadableChannel) {
                    readChannelList.add(channel.keyFor(this));
                    readableFDs.add(((FileDescriptorHandler)channel).getFD());
                }
                if (isWritableChannel) {
                    writeChannelList.add(channel.keyFor(this));
                    writableFDs.add(((FileDescriptorHandler)channel).getFD());
                }
            }
        }
        readableChannels = readChannelList.toArray(new SelectionKey[0]);
        writableChannels = writeChannelList.toArray(new SelectionKey[0]);
        readable = readableFDs.toArray(new FileDescriptor[0]);
        writable = writableFDs.toArray(new FileDescriptor[0]);
    }

    // Analyses selected channels and adds keys of ready channels to
    // selectedKeys list
    private int processSelectResult(int[] readyChannels) throws IOException {
        if (0 == readyChannels.length) {
            return 0;
        }
        // if the mock channel is selected, read the content.
        if (READABLE == readyChannels[0]) {
            ByteBuffer readbuf = ByteBuffer.allocate(MOCK_READBUF_SIZE);
            while (source.read(readbuf) > 0) {
                readbuf.flip();
            }
        }
        int selected = 0;
        for (int i = 1; i < readyChannels.length; i++) {            
            SelectionKeyImpl key = (SelectionKeyImpl) (i >= readable.length ? writableChannels[i
                    - readable.length]
                    : readableChannels[i]);
            if (null == key) {
                continue;
            }
            boolean isOldSelectedKey = selectedKeys.contains(key);
            int selectedOp = 0;
            // set ready ops
            switch (readyChannels[i]) {
            case NA:
                selectedOp = 0;
                break;
            case READABLE:
                selectedOp = (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)
                        & key.oldInterestOps;
                break;
            case WRITEABLE:
                if (isConnected(key)) {
                    selectedOp = SelectionKey.OP_WRITE & key.oldInterestOps;
                } else {
                    selectedOp = SelectionKey.OP_CONNECT & key.oldInterestOps;
                }
                break;
            }

            if (0 != selectedOp) {
                if (isOldSelectedKey && key.readyOps() != selectedOp) {
                    key.setReadyOps(key.readyOps() | selectedOp);
                    selected++;
                } else if (!isOldSelectedKey) {
                    key.setReadyOps(selectedOp);
                    selectedKeys.add(key);
                    selected++;
                }
            }
        }
        readableChannels = null;
        writableChannels = null;
        return selected;
    }

    /*
     * @see java.nio.channels.Selector#selectedKeys()
     */
    public synchronized Set<SelectionKey> selectedKeys() {
        closeCheck();
        return unaddableSelectedKeys;
    }

    private void doCancel() {
        Set<SelectionKey> cancelledKeys = cancelledKeys();
        synchronized (cancelledKeys) {
            if (cancelledKeys.size() > 0) {
                for (SelectionKey currentkey : cancelledKeys) {
                    deregister((AbstractSelectionKey) currentkey);
                    keys.remove(currentkey);
                    selectedKeys.remove(currentkey);
                }
            }
            cancelledKeys.clear();
        }
    }

    /*
     * @see java.nio.channels.Selector#wakeup()
     */
    public Selector wakeup() {
        try {
            sink.write(ByteBuffer.allocate(MOCK_WRITEBUF_SIZE));
        } catch (IOException e) {
            // do nothing
        }
        return this;
    }

    private static class UnaddableSet<E> implements Set<E> {

        private Set<E> set;

        UnaddableSet(Set<E> set) {
            this.set = set;
        }

        public boolean equals(Object object) {
            return set.equals(object);
        }

        public int hashCode() {
            return set.hashCode();
        }

        public boolean add(E object) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            set.clear();
        }

        public boolean contains(Object object) {
            return set.contains(object);
        }

        public boolean containsAll(Collection<?> c) {
            return set.containsAll(c);
        }

        public boolean isEmpty() {
            return set.isEmpty();
        }

        public Iterator<E> iterator() {
            return set.iterator();
        }

        public boolean remove(Object object) {
            return set.remove(object);
        }

        public boolean removeAll(Collection<?> c) {
            return set.removeAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            return set.retainAll(c);
        }

        public int size() {
            return set.size();
        }

        public Object[] toArray() {
            return set.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return set.toArray(a);
        }
    }
}
