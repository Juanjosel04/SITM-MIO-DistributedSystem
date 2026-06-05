package visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapPlaybackSampler {
    public static final int MAX_PLAYBACK_POINTS = 20000;
    public static final int INITIAL_SAMPLE_STEP = 10;
    public static final int MAX_POINTS_PER_TICK = 80;
    public static final int PLAYBACK_INTERVAL_MS = 180;

    private final int maxPlaybackPoints;
    private final List<MapPlaybackPoint> playbackPoints = new ArrayList<MapPlaybackPoint>();
    private final Map<String, Long> candidateCountByBusId = new HashMap<String, Long>();
    private int sampleStep;

    public MapPlaybackSampler() {
        this(MAX_PLAYBACK_POINTS, INITIAL_SAMPLE_STEP);
    }

    public MapPlaybackSampler(int maxPlaybackPoints, int initialSampleStep) {
        this.maxPlaybackPoints = Math.max(5000, maxPlaybackPoints);
        this.sampleStep = Math.max(1, initialSampleStep);
    }

    public List<MapPlaybackPoint> sample(List<MapPlaybackPoint> candidates) {
        playbackPoints.clear();
        candidateCountByBusId.clear();
        sampleStep = Math.max(1, sampleStep);
        if (candidates == null) {
            return new ArrayList<MapPlaybackPoint>();
        }
        for (MapPlaybackPoint candidate : candidates) {
            offer(candidate);
        }
        return new ArrayList<MapPlaybackPoint>(playbackPoints);
    }

    public void offer(MapPlaybackPoint point) {
        if (point == null || point.getBusId().isEmpty()) {
            return;
        }
        Long currentCount = candidateCountByBusId.get(point.getBusId());
        long nextCount = currentCount == null ? 1L : currentCount.longValue() + 1L;
        candidateCountByBusId.put(point.getBusId(), Long.valueOf(nextCount));
        if (nextCount % sampleStep != 0L) {
            return;
        }

        playbackPoints.add(point);
        if (playbackPoints.size() > maxPlaybackPoints) {
            sampleStep = sampleStep * 2;
            compactSample();
        }
    }

    public int getSampleStep() {
        return sampleStep;
    }

    private void compactSample() {
        if (playbackPoints.isEmpty()) {
            return;
        }
        List<MapPlaybackPoint> compacted = new ArrayList<MapPlaybackPoint>();
        for (int i = 0; i < playbackPoints.size(); i += 2) {
            compacted.add(playbackPoints.get(i));
        }
        MapPlaybackPoint last = playbackPoints.get(playbackPoints.size() - 1);
        if (compacted.isEmpty() || compacted.get(compacted.size() - 1) != last) {
            compacted.add(last);
        }
        playbackPoints.clear();
        playbackPoints.addAll(compacted);
    }
}
