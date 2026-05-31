package core.processing.filtering;

import core.model.Datagram;

public class DatagramFilter {
    public boolean isProcessable(Datagram datagram) {
        return datagram != null && datagram.getRouteId() > 0;
    }
}
