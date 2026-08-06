package com.kb.youngly.exception;

public class FssMarketPdfDownloadException extends RuntimeException {

    public FssMarketPdfDownloadException(String message) {
        super(message);
    }

    public FssMarketPdfDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
