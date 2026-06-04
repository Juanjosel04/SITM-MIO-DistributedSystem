package processing.forkjoin;

import domain.Datagram;

import java.util.Collections;
import java.util.List;

public final class BusDatagramGroup {
    private final String busId;
    private final List<Datagram> datagrams;

    public BusDatagramGroup(String busId, List<Datagram> datagrams) {
        this.busId = busId == null ? "" : busId.trim();
        this.datagrams = datagrams == null ? Collections.<Datagram>emptyList() : Collections.unmodifiableList(datagrams);
    }

    public String getBusId() {
        return busId;
    }

    public List<Datagram> getDatagrams() {
        return datagrams;
    }
}
