package com.hrms.dto;

public class ApiResponse<T> {
    private ResponseStatus status;
    private T response;
    private String message;

    public ApiResponse() {}

    public ApiResponse(ResponseStatus status, T response, String message) {
        this.status = status;
        this.response = response;
        this.message = message;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
