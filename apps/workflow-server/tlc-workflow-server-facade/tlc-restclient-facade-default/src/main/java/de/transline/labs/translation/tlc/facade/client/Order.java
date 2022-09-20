package de.transline.labs.translation.tlc.facade.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by aga on 26.01.2022.
 */
public class Order {

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("positions")
    private List<Position> positions;

    public Order() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }
}
