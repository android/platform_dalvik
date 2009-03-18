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

import org.apache.harmony.luni.util.Msg;

// BEGIN android-added
import java.util.logging.Logger;
// END android-added

/**
 * Wraps an existing {@link OutputStream} and <em>buffers</em> the output.
 * Expensive interaction with the underlying input stream is minimized, since
 * most (smaller) requests can be satisfied by accessing the buffer alone. The
 * drawback is that some extra space is required to hold the buffer and that
 * copying takes place when flushing that buffer, but this is usually outweighed
 * by the performance benefits.
 * 
 * <p/>A typical application pattern for the class looks like this:<p/>
 * 
 * <pre>
 * BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(&quot;file.java&quot;));
 * </pre>
 * 
 * @see BufferedInputStream
 * 
 * @since Android 1.0
 */
public class BufferedOutputStream extends FilterOutputStream {
    /**
     * The buffer containing the bytes to be written to the target stream.
     * 
     * @since Android 1.0
     */
    protected byte[] buf;

    /**
     * The total number of bytes inside the byte array {@code buf}.
     * 
     * @since Android 1.0
     */
    protected int count;

    /**
     * Constructs a new {@code BufferedOutputStream} on the {@link OutputStream}
     * {@code out}. The buffer size is set to the default value of 8 KB.
     * 
     * @param out
     *            the {@code OutputStream} for which write operations are
     *            buffered.
     * @since Android 1.0
     */
    public BufferedOutputStream(OutputStream out) {
        super(out);
        buf = new byte[8192];

        // BEGIN android-added
        /*
         * For Android, we want to discourage the use of this constructor (with
         * its arguably too-large default), so we note its use in the log. We
         * don't disable it, nor do we alter the default, however, because we
         * still aim to behave compatibly, and the default value, though not
         * documented, is established by convention.
         */
        Logger.global.info(
                "Default buffer size used in BufferedOutputStream " +
                "constructor. It would be " +
                "better to be explicit if an 8k buffer is required.");
        // END android-added
    }

    /**
     * Constructs a new {@code BufferedOutputStream} on the {@link OutputStream}
     * {@code out}. The buffer size is set to {@code size}.
     * 
     * @param out
     *            the output stream for which write operations are buffered.
     * @param size
     *            the size of the buffer in bytes.
     * @throws IllegalArgumentException
     *             if {@code size <= 0}.
     * @since Android 1.0
     */
    public BufferedOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            // K0058=size must be > 0
            throw new IllegalArgumentException(Msg.getString("K0058")); //$NON-NLS-1$
        }
        buf = new byte[size];
    }

    /**
     * Flushes this stream to ensure all pending data is written out to the
     * target stream. In addition, the target stream is flushed.
     * 
     * @throws IOException
     *             if an error occurs attempting to flush this stream.
     * @since Android 1.0
     */
    @Override
    public synchronized void flush() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
        }
        count = 0;
        out.flush();
    }

    /**
     * Writes {@code count} bytes from the byte array {@code buffer} starting at
     * {@code offset} to this stream. If there is room in the buffer to hold the
     * bytes, they are copied in. If not, the buffered bytes plus the bytes in
     * {@code buffer} are written to the target stream, the target is flushed,
     * and the buffer is cleared.
     * 
     * @param buffer
     *            the buffer to be written.
     * @param offset
     *            the start position in {@code buffer} from where to get bytes.
     * @param length
     *            the number of bytes from {@code buffer} to write to this
     *            stream.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0} or {@code length < 0}, or if
     *             {@code offset + length} is greater than the size of
     *             {@code buffer}.
     * @throws IOException
     *             if an error occurs attempting to write to this stream.
     * @throws NullPointerException
     *             if {@code buffer} is {@code null}.
     * @since Android 1.0
     */
    @Override
    public synchronized void write(byte[] buffer, int offset, int length)
            throws IOException {
        if (buffer == null) {
            // K0047=buffer is null
            throw new NullPointerException(Msg.getString("K0047")); //$NON-NLS-1$
        }
        // BEGIN android-changed
        // Exception priorities (in case of multiple errors) differ from
        // RI, but are spec-compliant.
        // used (offset | length) < 0 instead of (offset < 0) || (length < 0)
        // to safe one operation
        if ((offset | length) < 0 || offset > buffer.length - length) {
            // K002f=Arguments out of bounds
            throw new ArrayIndexOutOfBoundsException(Msg.getString("K002f")); //$NON-NLS-1$
        }
        // END android-changed
        if (count == 0 && length >= buf.length) {
            out.write(buffer, offset, length);
            return;
        }
        int available = buf.length - count;
        if (length < available) {
            available = length;
        }
        if (available > 0) {
            System.arraycopy(buffer, offset, buf, count, available);
            count += available;
        }
        if (count == buf.length) {
            out.write(buf, 0, buf.length);
            count = 0;
            if (length > available) {
                offset += available;
                available = length - available;
                if (available >= buf.length) {
                    out.write(buffer, offset, available);
                } else {
                    System.arraycopy(buffer, offset, buf, count, available);
                    count += available;
                }
            }
        }
    }

    /**
     * Writes one byte to this stream. Only the low order byte of the integer
     * {@code oneByte} is written. If there is room in the buffer, the byte is
     * copied into the buffer and the count incremented. Otherwise, the buffer
     * plus {@code oneByte} are written to the target stream, the target is
     * flushed, and the buffer is reset.
     * 
     * @param oneByte
     *            the byte to be written.
     * @throws IOException
     *             if an error occurs attempting to write to this stream.
     * @since Android 1.0
     */
    @Override
    public synchronized void write(int oneByte) throws IOException {
        if (count == buf.length) {
            out.write(buf, 0, count);
            count = 0;
        }
        buf[count++] = (byte) oneByte;
    }
}
