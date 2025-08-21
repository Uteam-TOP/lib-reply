package ru.fvds.cdss13.libreply.exception;



/**
 * Базовое исключение для всех исключений, которые отображаются в разделе response в open api.
 * <br>
 * Наследники ОБЯЗАНЫ иметь константы:
 * <ul>
 * <li>{@code public static final String CODE} - http статус в виде строки: "404", "500", и т.д.</li>
 * <li>{@code public static final String DESC} - описание ответа</li>
 * </ul>
 * <br>
 * Такой костыль нужен, чтобы не хордкодить код и описание в open api, а ссылаться на исключения.
 */

public abstract class AbstractOpenApiException extends RuntimeException {

    private Integer codeHttpStatus;

    /**
     * Описание ошибки, предназначенное для пользователя
     */
    private String userMessage;

    /**
     * @param httpStatus http статус ввиде строки: "404", "500", и т.д.
     */
    protected AbstractOpenApiException(Integer httpStatus, String message) {
        super(message);
        this.codeHttpStatus = httpStatus;
        this.userMessage = message;
    }

    /**
     * @param httpStatus http статус ввиде строки: "404", "500", и т.д.
     */
    protected AbstractOpenApiException(Integer httpStatus, String message, String userMessage) {
        super(message);
        this.codeHttpStatus = httpStatus;
        this.userMessage = userMessage;
    }

    /**
     * @param httpStatus http статус ввиде строки: "404", "500", и т.д.
     */
    protected AbstractOpenApiException(Integer httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.codeHttpStatus = httpStatus;
        this.userMessage = message;
    }

    /**
     * @param httpStatus http статус ввиде строки: "404", "500", и т.д.
     */
    protected AbstractOpenApiException(Integer httpStatus, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.codeHttpStatus = httpStatus;
        this.userMessage = userMessage;
    }

    public Integer getHttpStatus() {
        return codeHttpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.codeHttpStatus = httpStatus;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}
