package ru.fvds.cdss13.libreply.exception;

public class NotFoundException extends AbstractOpenApiException {
    public static final Integer CODE = 404;
    public static final String DESC = "Не найдено";

    public NotFoundException(String message) {
        super(CODE, message);
    }
}
