package com.github.iherasymenko.jlink;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

final class DigestBodyHandler<T> implements HttpResponse.BodyHandler<DigestBodyHandler.DigestResult<T>> {

    record DigestResult<T>(String checksum, T result) { }

    private class DigestBodySubscriber implements HttpResponse.BodySubscriber<DigestResult<T>> {

        private final HttpResponse.BodySubscriber<T> delegate;

        DigestBodySubscriber(HttpResponse.BodySubscriber<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletionStage<DigestResult<T>> getBody() {
            return delegate.getBody().thenApply(result -> new DigestResult<>(HexFormat.of().formatHex(digest.digest()), result));
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            for (ByteBuffer byteBuffer : item) {
                byteBuffer.mark();
                digest.update(byteBuffer);
                byteBuffer.reset();
            }
            delegate.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }

    }

    private final MessageDigest digest;

    private final HttpResponse.BodyHandler<T> delegate;

    DigestBodyHandler(HttpResponse.BodyHandler<T> delegate, String algorithm) throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance(Objects.requireNonNull(algorithm, "algorithm"));
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public HttpResponse.BodySubscriber<DigestResult<T>> apply(HttpResponse.ResponseInfo responseInfo) {
        return new DigestBodySubscriber(delegate.apply(responseInfo));
    }

}
