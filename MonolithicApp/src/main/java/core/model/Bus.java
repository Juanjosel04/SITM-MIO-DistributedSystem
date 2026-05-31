package core.model;

public class Bus {
    private long id;
    private String code;
    private String plate;
    private int routeId;
    private String status;

    public Bus() {
    }

    public Bus(long id, String code, String plate, int routeId, String status) {
        this.id = id;
        this.code = code;
        this.plate = plate;
        this.routeId = routeId;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
