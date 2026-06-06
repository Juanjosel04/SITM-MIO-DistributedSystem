package master.map;

import master.ingestion.CompactDatagramRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapPlaybackSampler {
    public static final int DEFAULT_MAX_VISUAL_POINTS = 5000;
    public static final int MAX_POINTS_PER_TICK = 80;
    public static final int PLAYBACK_INTERVAL_MS = 180;
    private static final double COORDINATE_SCALE = 10000000.0;
    private static final int INITIAL_SAMPLE_STEP = 10;

    private final int maxVisualPoints;
    private final List<MapPlaybackPoint> points = new ArrayList<MapPlaybackPoint>();
    private final Map<String, Long> candidateCountByVisualBusKey = new HashMap<String, Long>();
    private long totalSeen;
    private int sampleStep = INITIAL_SAMPLE_STEP;

    public MapPlaybackSampler() {
        this(readMaxVisualPoints());
    }

    public MapPlaybackSampler(int maxVisualPoints) {
        this.maxVisualPoints = maxVisualPoints <= 0 ? DEFAULT_MAX_VISUAL_POINTS : maxVisualPoints;
    }

    public void offer(CompactDatagramRecord record, long sequence) {
        totalSeen++;
        if (record == null) {
            return;
        }
        String visualBusKey = record.getVisualBusKey() == null ? "" : record.getVisualBusKey().trim();
        if (visualBusKey.isEmpty()) {
            return;
        }
        long busCandidateCount = nextBusCandidateCount(visualBusKey);
        if (busCandidateCount % sampleStep != 0L) {
            return;
        }
        MapPlaybackPoint point = toPoint(record, sequence);
        if (point == null) {
            return;
        }
        points.add(point);
        if (points.size() > maxVisualPoints) {
            sampleStep = sampleStep * 2;
            compact();
        }
    }

    public List<MapPlaybackPoint> snapshot() {
        return Collections.unmodifiableList(new ArrayList<MapPlaybackPoint>(points));
    }

    public long getTotalSeen() {
        return totalSeen;
    }

    public int getTotalKept() {
        return points.size();
    }

    public int getSampleStep() {
        return sampleStep;
    }

    public int getMaxVisualPoints() {
        return maxVisualPoints;
    }

    private MapPlaybackPoint toPoint(CompactDatagramRecord record, long sequence) {
        try {
            if (record.getBusId() == null || record.getBusId().trim().isEmpty()) {
                return null;
            }
            if (record.getVisualBusKey() == null || record.getVisualBusKey().trim().isEmpty()) {
                return null;
            }
            if (record.getRouteId() == null || record.getRouteId().trim().isEmpty()
                    || "-1".equals(record.getRouteId().trim())) {
                return null;
            }
            Double latitude = normalizeLatitude(record.getLatitude());
            Double longitude = normalizeLongitude(record.getLongitude());
            if (latitude == null || longitude == null) {
                return null;
            }
            return new MapPlaybackPoint(record.getBusId(), record.getVisualBusKey(), record.getRouteId(), record.getTimestamp(),
                    latitude.doubleValue(), longitude.doubleValue(), sequence);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Double normalizeLatitude(String value) {
        Double coordinate = parseCoordinate(value);
        if (coordinate == null || coordinate.doubleValue() < -90.0 || coordinate.doubleValue() > 90.0) {
            return null;
        }
        return coordinate;
    }

    private Double normalizeLongitude(String value) {
        Double coordinate = parseCoordinate(value);
        if (coordinate == null || coordinate.doubleValue() < -180.0 || coordinate.doubleValue() > 180.0) {
            return null;
        }
        return coordinate;
    }

    private Double parseCoordinate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        double coordinate = Double.parseDouble(value.trim());
        if (coordinate == -1.0 || Double.isNaN(coordinate) || Double.isInfinite(coordinate)) {
            return null;
        }
        if (Math.abs(coordinate) > 180.0) {
            coordinate = coordinate / COORDINATE_SCALE;
        }
        if (Double.isNaN(coordinate) || Double.isInfinite(coordinate)) {
            return null;
        }
        return Double.valueOf(coordinate);
    }

    private void compact() {
        if (points.isEmpty()) {
            return;
        }
        List<MapPlaybackPoint> compacted = new ArrayList<MapPlaybackPoint>();
        for (int index = 0; index < points.size(); index += 2) {
            compacted.add(points.get(index));
        }
        points.clear();
        points.addAll(compacted);
    }

    private long nextBusCandidateCount(String visualBusKey) {
        Long current = candidateCountByVisualBusKey.get(visualBusKey);
        long next = current == null ? 1L : current.longValue() + 1L;
        candidateCountByVisualBusKey.put(visualBusKey, Long.valueOf(next));
        return next;
    }

    private static int readMaxVisualPoints() {
        String value = System.getProperty("sitm.map.max.points", String.valueOf(DEFAULT_MAX_VISUAL_POINTS));
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed <= 0 ? DEFAULT_MAX_VISUAL_POINTS : parsed;
        } catch (RuntimeException exception) {
            return DEFAULT_MAX_VISUAL_POINTS;
        }
    }
}
