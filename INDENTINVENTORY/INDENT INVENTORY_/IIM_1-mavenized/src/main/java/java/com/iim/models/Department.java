package com.iim.models;

import java.sql.Timestamp;

public class Department {
    private int id;
    private String code;
    private String name;
    private String status;      // "active" | "inactive"  (lowercase to match DB)
    private Timestamp createdAt;

    public Department() {}

    public Department(int id, String code, String name, String status, Timestamp createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
