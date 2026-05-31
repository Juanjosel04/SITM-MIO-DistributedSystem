package core.model;

public class Route {
    private int id;
    private String shortName;
    private String description;
    private String status;

    public Route() {
    }

    public Route(int id, String shortName, String description, String status) {
        this.id = id;
        this.shortName = shortName;
        this.description = description;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Route{" +
                "id=" + id +
                ", shortName='" + shortName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
