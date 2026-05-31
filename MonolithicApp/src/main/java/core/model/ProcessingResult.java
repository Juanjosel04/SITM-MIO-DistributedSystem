package core.model;

import java.time.LocalDateTime;

public class ProcessingResult {
    private boolean success;
    private String message;
    private Datagram datagram;
    private LocalDateTime processedAt;

    public ProcessingResult(boolean success, String message, Datagram datagram, LocalDateTime processedAt) {
        this.success = success;
        this.message = message;
        this.datagram = datagram;
        this.processedAt = processedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Datagram getDatagram() {
        return datagram;
    }

    public void setDatagram(Datagram datagram) {
        this.datagram = datagram;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
