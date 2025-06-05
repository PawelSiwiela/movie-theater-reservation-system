package models;

import java.io.Serializable;

public class Message implements Serializable {
    private MessageType type;
    private Object payload;
    private String statusCode;
    private String statusMessage;
    private String requestId;

    // Konstruktor dla żądań
    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.requestId = java.util.UUID.randomUUID().toString();
    }

    // Konstruktor dla odpowiedzi
    public Message(MessageType type, Object payload, String statusCode, String statusMessage) {
        this.type = type;
        this.payload = payload;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.requestId = java.util.UUID.randomUUID().toString();
    }

    // Konstruktor do tworzenia odpowiedzi na podstawie żądania
    public Message createResponse(Object responsePayload, String statusCode, String statusMessage) {
        Message response = new Message(MessageType.RESPONSE, responsePayload, statusCode, statusMessage);
        response.setRequestId(this.requestId);
        return response;
    }

    // Konstruktor do tworzenia odpowiedzi o błędzie
    public Message createErrorResponse(String errorMessage) {
        return createResponse(null, "ERROR", errorMessage);
    }

    // Konstruktor do tworzenia odpowiedzi o sukcesie
    public Message createSuccessResponse(Object responsePayload) {
        return createResponse(responsePayload, "SUCCESS", "Operation completed successfully");
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getRequestId() {
        return requestId;
    }

    // Setters
    public void setType(MessageType type) {
        this.type = type;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    // Helper methods
    public boolean isSuccess() {
        return "SUCCESS".equals(statusCode);
    }

    public boolean isError() {
        return "ERROR".equals(statusCode);
    }

    public boolean isRequest() {
        return type != MessageType.RESPONSE;
    }

    public boolean isResponse() {
        return type == MessageType.RESPONSE;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", statusCode='" + statusCode + '\'' +
                ", statusMessage='" + statusMessage + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
