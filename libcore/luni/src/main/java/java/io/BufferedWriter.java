/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.io;

import java.security.AccessController;

import org.apache.harmony.luni.util.Msg;
import org.apache.harmony.luni.util.PriviAction;

// BEGIN android-added
import java.util.logging.Logger;
// END android-added

/**
 * Wraps an existing {@link Writer} and <em>buffers</em> the output. Expensive
 * interaction with the underlying reader is minimized, since most (smaller)
 * requests can be satisfied by accessing the buffer alone. The drawback is that
 * some extra space is required to hold the buffer and that copying takes place
 * when filling that buffer, but this is usually outweighed by the performance
 * benefits.
 * 
 * <p/>A typical application pattern for the class looks like this:<p/>
 * 
 * <pre>
 * BufferedWriter buf = new BufferedWriter(new FileWriter(&quot;file.java&quot;));
 * </pre>
 * 
 * @see BufferedReader
 * 
 * @since Android 1.0
 */
public class BufferedWriter extends Writer {

    private Writer out;

    private char buf[];

    private int pos;

    private final String lineSeparator = AccessController
            .doPrivileged(new PriviAction<String>("line.separator")); //$NON-NLS-1$

    /**
     * Constructs a new {@code BufferedWriter} with {@code out} as the writer
     * for which to buffer write operations. The buffer size is set to the
     * default value of 8 KB.
     * 
     * @param out
     *            the writer for which character writing is buffered.
     * @since Android 1.0
     */
    public BufferedWriter(Writer out) {
        super(out);
        this.out = out;
        buf = new char[8192];

        // BEGIN android-added
        /*
         * For Android, we want to discourage the use of this constructor (with
         * its arguably too-large default), so we note its use in the log. We
         * don't disable it, nor do we alter the default, however, because we
         * still aim to behave compatibly, and the default value, though not
         * documented, is established by convention.
         */
        Logger.global.info(
                "Default buffer size used in BufferedWriter " +
                "constructor. It would be " +
                "better to be explicit if an 8k-char buffer is required.");
        // END android-added
    }

    /**
     * Constructs a new {@code BufferedWriter} with {@code out} as the writer
     * for which to buffer write operations. The buffer size is set to {@code
     * size}.
     * 
     * @param out
     *            the writer for which character writing is buffered.
     * @param size
     *            the size of the buffer in bytes.
     * @throws IllegalArgumentException
     *             if {@code size <= 0}.
     * @since Android 1.0
     */
    public BufferedWriter(Writer out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException(Msg.getString("K0058")); //$NON-NLS-1$
        }
        this.out = out;
        this.buf = new char[size];
    }

    /**
     * Closes this writer. The contents of the buffer are flushed, the target
     * writer is closed, and the buffer is released. Only the first invocation
     * of close has any effect.
     * 
     * @throws IOException
     *             if an error occurs while closing this writer.
     * @since Android 1.0
     */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (!isClosed()) {
                flush();
                out.close();
                buf = null;
                out = null;
            }
        }
    }

    /**
     * Flushes this writer. The contents of the buffer are committed to the
     * target writer and it is then flushed.
     * 
     * @throws IOException
     *             if an error occurs while flushing this writer.
     * @since Android 1.0
     */
    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException(Msg.getString("K005d")); //$NON-NLS-1$
            }
            if (pos > 0) {
                out.write(buf, 0, pos);
            }
            pos = 0;
            out.flush();
        }
    }

    /**
     * Indicates whether this writer is closed.
     * 
     * @return {@code true} if this writer is closed, {@code false} otherwise.
     */
    private boolean isClosed() {
        return out == null;
    }

    /**
     * Writes a newline to this writer. A newline is determined by the System
     * property "line.separator". The target writer may or may not be flushed
     * when a newline is written.
     * 
     * @throws IOException
     *             if an error occurs attempting to write to this writer.
     * @since Android 1.0
     */
    public void newLine() throws IOException {
        write(lineSeparator, 0, lineSeparator.length());
    }

    /**
     * Writes {@code count} characters starting at {@code offset} in
     * {@code cbuf} to this writer. If {@code count} is greater than this
     * writer's buffer, then the buffer is flushed and the characters are
     * written directly to the target writer.
     * 
     * @param cbuf
     *            the array containing characters to write.
     * @param offset
     *            the start position in {@code cbuf} for retrieving characters.
     * @param count
     *            the maximum number of characters to write.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0} or {@code count < 0}, or if
     *             {@code offset + count} is greater than the size of
     *             {@code cbuf}.
     * @throws IOException
     *             if this writer is closed or another I/O error occurs.
     * @since Android 1.0
     */
    @Override
    public void write(char[] cbuf, int offset, int count) throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException(Msg.getString("K005d")); //$NON-NLS-1$
            }
            // BEGIN android-changed
            // Exception priorities (in case of multiple errors) differ from
            // RI, but are spec-compliant.
            // made implicit null check explicit, used (offset | count) < 0
            // instead of (offset < 0) || (count < 0) to safe one operation
            if (cbuf == null) {
                throw new NullPointerException(Msg.getString("K0047")); //$NON-NLS-1$
            }
            if ((offset | count) < 0 || offset > cbuf.length - count) {
                throw new IndexOutOfBoundsException(Msg.getString("K002f")); //$NON-NLS-1$
            }
            // END android-changed
            if (pos == 0 && count >= this.buf.length) {
                out.write(cbuf, offset, count);
                return;
            }
            int available = this.buf.length - pos;
            if (count < available) {
                available = count;
            }
            if (available > 0) {
                System.arraycopy(cbuf, offset, this.buf, pos, available);
                pos += available;
            }
            if (pos == this.buf.length) {
                out.write(this.buf, 0, this.buf.length);
                pos = 0;
                if (count > available) {
                    offset += available;
                    available = count - available;
                    if (available >= this.buf.length) {
                        out.write(cbuf, offset, available);
                        return;
                    }

                    System.arraycopy(cbuf, offset, this.buf, pos, available);
                    pos += available;
                }
            }
        }
    }

    /**
     * Writes the character {@code oneChar} to this writer. If the buffer
     * gets full by writing this character, this writer is flushed. Only the
     * lower two bytes of the integer {@code oneChar} are written.
     * 
     * @param oneChar
     *            the character to write.
     * @throws IOException
     *             if this writer is closed or another I/O error occurs.
     * @since Android 1.0
     */
    @Override
    public void write(int oneChar) throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException(Msg.getString("K005d")); //$NON-NLS-1$
            }
            if (pos >= buf.length) {
                out.write(buf, 0, buf.length);
                pos = 0;
            }
            buf[pos++] = (char) oneChar;
        }
    }

    /**
     * Writes {@code count} characters starting at {@code offset} in {@code str}
     * to this writer. If {@code count} is greater than this writer's buffer,
     * then this writer is flushed and the remaining characters are written
     * directly to the target writer. If count is negative no characters are
     * written to the buffer. This differs from the behavior of the superclass.
     * 
     * @param str
     *            the non-null String containing characters to write.
     * @param offset
     *            the start position in {@code str} for retrieving characters.
     * @param count
     *            maximum number of characters to write.
     * @throws IOException
     *             if this writer has already been closed or another I/O error
     *             occurs.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0} or {@code offset + count} is greater
     *             than the length of {@code str}.
     * @since Android 1.0
     */
    @Override
    public void write(String str, int offset, int count) throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException(Msg.getString("K005d")); //$NON-NLS-1$
            }
            if (count <= 0) {
                return;
            }
            if (offset > str.length() - count || offset < 0) {
                throw new StringIndexOutOfBoundsException();
            }
            if (pos == 0 && count >= buf.length) {
                char[] chars = new char[count];
                str.getChars(offset, offset + count, chars, 0);
                out.write(chars, 0, count);
                return;
            }
            int available = buf.length - pos;
            if (count < available) {
                available = count;
            }
            if (available > 0) {
                str.getChars(offset, offset + available, buf, pos);
                pos += available;
            }
            if (pos == buf.length) {
                out.write(this.buf, 0, this.buf.length);
                pos = 0;
                if (count > available) {
                    offset += available;
                    available = count - available;
                    if (available >= buf.length) {
                        char[] chars = new char[count];
                        str.getChars(offset, offset + available, chars, 0);
                        out.write(chars, 0, available);
                        return;
                    }
                    str.getChars(offset, offset + available, buf, pos);
                    pos += available;
                }
            }
        }
    }
}
