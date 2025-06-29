package brad.grier.alhena;

import java.util.concurrent.atomic.AtomicLong;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public class CountingWriteStream implements WriteStream<Buffer> {

    private final WriteStream<Buffer> delegate;
    private final AtomicLong bytesWritten = new AtomicLong();
    private Handler<Long> progressHandler;

    public CountingWriteStream(WriteStream<Buffer> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<Void> end() {
        return delegate.end();
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        delegate.end(handler);
    }

    @Override
    public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
        delegate.setWriteQueueMaxSize(maxSize);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return delegate.writeQueueFull();
    }

    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
        delegate.drainHandler(handler);
        return this;
    }

    @Override
    public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    public CountingWriteStream progressHandler(Handler<Long> handler) {
        this.progressHandler = handler;
        return this;
    }

    public long getBytesWritten() {
        return bytesWritten.get();
    }

    @Override
    public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
        long count = bytesWritten.addAndGet(data.length());
        if (progressHandler != null) {
            progressHandler.handle(count);
        }
        delegate.write(data, handler);
    }

    @Override
    public Future<Void> write(Buffer data) {
        long count = bytesWritten.addAndGet(data.length());
        if (progressHandler != null) {
            progressHandler.handle(count);
        }
        return delegate.write(data);
    }
}
