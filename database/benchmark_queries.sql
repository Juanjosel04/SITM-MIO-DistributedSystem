-- Average speed per route
SELECT
    route_id,
    AVG(speed)
FROM datagrams
GROUP BY route_id;

-- Events per route
SELECT
    route_id,
    COUNT(*)
FROM datagrams
GROUP BY route_id;

-- Top buses generating events
SELECT
    bus_code,
    COUNT(*)
FROM datagrams
GROUP BY bus_code
ORDER BY COUNT(*) DESC;

-- Average processing statistics
SELECT
    dataset_type,
    AVG(processing_time)
FROM throughput_metrics
GROUP BY dataset_type;