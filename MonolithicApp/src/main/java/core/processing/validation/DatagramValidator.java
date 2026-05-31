package core.processing.validation;

import core.model.Datagram;
import shared.exceptions.InvalidDatagramException;

public class DatagramValidator {
    public void validate(Datagram datagram) throws InvalidDatagramException {
        if (datagram == null) {
            throw new InvalidDatagramException("Datagram is null");
        }
        if (isBlank(datagram.getBusCode())) {
            throw new InvalidDatagramException("Bus code is empty");
        }
        if (datagram.getLatitude() < -90.0 || datagram.getLatitude() > 90.0) {
            throw new InvalidDatagramException("Latitude is invalid");
        }
        if (datagram.getLongitude() < -180.0 || datagram.getLongitude() > 180.0) {
            throw new InvalidDatagramException("Longitude is invalid");
        }
        if (datagram.getTimestamp() == null) {
            throw new InvalidDatagramException("Timestamp is invalid");
        }
        if (datagram.getSpeed() != null && datagram.getSpeed().doubleValue() < 0.0) {
            throw new InvalidDatagramException("Speed is negative");
        }
        if (isBlank(datagram.getRawPayload())) {
            throw new InvalidDatagramException("Raw payload is empty");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
