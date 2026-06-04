package processing;

import domain.Datagram;
import domain.Route;

import java.util.List;

/**
 * Contract for average-speed processors in the V2 pipeline.
 */
public interface AverageSpeedProcessor {
    AverageSpeedProcessingResult process(List<Route> routes, List<Datagram> datagrams);
}
