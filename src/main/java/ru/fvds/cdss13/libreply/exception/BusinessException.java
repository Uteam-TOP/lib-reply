package ru.fvds.cdss13.libreply.exception;



public class BusinessException extends AbstractOpenApiException {


    public BusinessException(String httpStatus, String message) {
        super(httpStatus, message);
    }

    public BusinessException(String httpStatus, String message, String userMessage) {
        super(httpStatus, message, userMessage);
    }

    public BusinessException(String httpStatus, String message, Throwable cause) {
        super(httpStatus, message, cause);
    }

    public BusinessException(String httpStatus, String message, String userMessage, Throwable cause) {
        super(httpStatus, message, userMessage, cause);
    }
}
