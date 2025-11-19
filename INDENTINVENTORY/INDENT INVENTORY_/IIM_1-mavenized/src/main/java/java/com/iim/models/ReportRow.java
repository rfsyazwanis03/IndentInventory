package com.iim.models;

import java.util.Arrays;

public class ReportRow {
    private String departmentName;
    // index 0..11 -> Jan..Dec
    private final int[] months = new int[12];

    public ReportRow(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int[] getMonths() { return months; }

    public void addMonthQty(int month1to12, int qty) {
        if (month1to12 >= 1 && month1to12 <= 12) months[month1to12 - 1] += qty;
    }

    public int getTotal() { return Arrays.stream(months).sum(); }
}
