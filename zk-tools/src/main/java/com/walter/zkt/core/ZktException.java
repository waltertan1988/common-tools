package com.walter.zkt.core;

/**
 * @author walter.tan
 * @date 2022/8/27
 *
 */
public class ZktException extends RuntimeException {

    public ZktException() {
    }

    public ZktException(final String message) {
        super(message);
    }

    public ZktException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ZktException(final Throwable cause) {
        super(cause);
    }

    public ZktException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
