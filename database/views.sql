CREATE VIEW route_average_speed AS
SELECT
    route_id,
    AVG(speed) AS average_speed
FROM datagrams
GROUP BY route_id;

CREATE VIEW route_event_summary AS
SELECT
    route_id,
    COUNT(event_code) AS total_events
FROM datagrams
GROUP BY route_id;

CREATE VIEW active_alerts AS
SELECT
    id,
    priority,
    message,
    created_at
FROM alerts
WHERE acknowledged = FALSE;