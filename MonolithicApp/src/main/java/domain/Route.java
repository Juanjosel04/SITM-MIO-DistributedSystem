package domain;

import java.util.Objects;

public final class Route {
    private final int id;
    private final String shortName;
    private final String description;

    public Route(int id, String shortName, String description) {
        this.id = id;
        this.shortName = normalize(shortName);
        this.description = normalize(description);
    }

    public int getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        if (!shortName.isEmpty()) {
            return shortName;
        }
        return "Route " + id;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Route)) {
            return false;
        }
        Route route = (Route) object;
        return id == route.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(id));
    }
}
