package master.map;

import master.ingestion.CompactDatagramRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MapPlaybackSampler {
    public static final int DEFAULT_MAX_VISUAL_POINTS = 5000;
    public static final int MAX_POINTS_PER_TICK = 80;
    public static final int PLAYBACK_INTERVAL_MS = 180;

    private final int maxVisualPoints;
    private final List<MapPlaybackPoint> points = new ArrayList<MapPlaybackPoint>();
    private long totalSeen;
    private int sampleStep = 1;

    public MapPlaybackSampler() {
        this(readMaxVisualPoints());
    }

    public MapPlaybackSampler(int maxVisualPoints) {
        this.maxVisualPoints = maxVisualPoints <= 0 ? DEFAULT_MAX_VISUAL_POINTS : maxVisualPoints;
    }

    public void offer(CompactDatagramRecord record, long sequence) {
        totalSeen++;
        if (record == null || totalSeen % sampleStep != 0L) {
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
            double latitude = Double.parseDouble(record.getLatitude());
            double longitude = Double.parseDouble(record.getLongitude());
            return new MapPlaybackPoint(record.getBusId(), record.getRouteId(), record.getTimestamp(),
                    latitude, longitude, sequence);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void compact() {
        if (points.isEmpty()) {
            return;
        }
        List<MapPlaybackPoint> compacted = new ArrayList<MapPlaybackPoint>();
        for (int index = 0; index < points.size(); index += 2) {
            compacted.add(points.get(index));
        }
        MapPlaybackPoint last = points.get(points.size() - 1);
        if (compacted.isEmpty() || compacted.get(compacted.size() - 1) != last) {
            compacted.add(last);
        }
        points.clear();
        points.addAll(compacted);
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
