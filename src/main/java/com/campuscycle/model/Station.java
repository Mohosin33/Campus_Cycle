package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;

/**
 * Model representing a physical docking station on campus.
 */
public class Station implements Identifiable {
    private int id;
    private String name;
    private String location;
    private int capacity;
    private int availableBikesCount;

    public Station(int id, String name, String location, int capacity) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.availableBikesCount = 0;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int capacity() {
        return capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getAvailableBikesCount() {
        return availableBikesCount;
    }

    public void setAvailableBikesCount(int availableBikesCount) {
        this.availableBikesCount = availableBikesCount;
    }

    @Override
    public String toString() {
        return name + " (" + location + ")";
    }
}
