package com.github.luben.zstd;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

/**
 * An pool of buffers which uses a simple reference queue to recycle buffers.
 *
 * Do not use it as generic buffer pool - it is optimized and supports only
 * buffer sizes used by the Zstd classes.
 */
public class RecyclingBufferPool implements BufferPool {
    public static final BufferPool INSTANCE = new RecyclingBufferPool();

    private static final int buffSize = Math.max(Math.max(
            (int) ZstdOutputStreamBase.recommendedCOutSize(),
            (int) ZstdInputStreamBase.recommendedDInSize()),
            (int) ZstdInputStreamBase.recommendedDOutSize());

    private final ArrayDeque<SoftReference<ByteBuffer>> pool;

    private RecyclingBufferPool() {
        this.pool = new ArrayDeque<SoftReference<ByteBuffer>>();
    }

    @Override
    public synchronized ByteBuffer get(int capacity) {
        if (capacity > buffSize) {
            throw new RuntimeException(
                    "Unsupported buffer size: " + capacity +
                    ". Supported buffer sizes: " + buffSize + " or smaller."
                );
        }
        while(true) {
            SoftReference<ByteBuffer> sbuf = pool.pollFirst();
            if (sbuf == null) {
                return ByteBuffer.allocate(buffSize);
            }
            ByteBuffer buf = sbuf.get();
            if (buf != null) {
                return buf;
            }
        }
    }

    @Override
    public synchronized void release(ByteBuffer buffer) {
        if (buffer.capacity() >= buffSize) {
            buffer.clear();
            pool.addFirst(new SoftReference<ByteBuffer>(buffer));
        }
    }
}
