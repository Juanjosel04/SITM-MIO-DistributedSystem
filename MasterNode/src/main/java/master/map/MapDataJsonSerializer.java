package master.map;

import java.util.List;
import java.util.Locale;

public final class MapDataJsonSerializer {
    public String toJson(List<MapPlaybackPoint> points) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (points != null) {
            for (int index = 0; index < points.size(); index++) {
                if (index > 0) {
                    builder.append(",");
                }
                appendPoint(builder, points.get(index));
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private void appendPoint(StringBuilder builder, MapPlaybackPoint point) {
        String popup = "Bus: " + point.getBusId()
                + "<br>Ruta: " + point.getRouteId()
                + "<br>Fecha: " + point.getTimestamp()
                + "<br>Secuencia: " + point.getSequence();
        builder.append("{")
                .append("\"busId\":\"").append(escape(point.getBusId())).append("\",")
                .append("\"routeId\":\"").append(escape(point.getRouteId())).append("\",")
                .append("\"routeLabel\":\"").append(escape(point.getRouteId())).append("\",")
                .append("\"timestamp\":\"").append(escape(point.getTimestamp())).append("\",")
                .append("\"sequence\":").append(point.getSequence()).append(",")
                .append("\"latitude\":").append(String.format(Locale.US, "%.8f", Double.valueOf(point.getLatitude()))).append(",")
                .append("\"longitude\":").append(String.format(Locale.US, "%.8f", Double.valueOf(point.getLongitude()))).append(",")
                .append("\"popupText\":\"").append(escape(popup)).append("\"")
                .append("}");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
