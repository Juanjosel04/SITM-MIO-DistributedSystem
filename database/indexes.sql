CREATE INDEX idx_datagrams_route
ON datagrams(route_id);

CREATE INDEX idx_datagrams_timestamp
ON datagrams(timestamp);

CREATE INDEX idx_datagrams_bus
ON datagrams(bus_code);

CREATE INDEX idx_events_bus
ON operational_events(bus_id);

CREATE INDEX idx_events_priority
ON operational_events(priority);

CREATE INDEX idx_statistics_route
ON route_statistics(route_id);

CREATE INDEX idx_bus_positions_bus
ON bus_positions(bus_id);