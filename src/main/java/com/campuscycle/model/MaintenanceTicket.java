package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;

/**
 * Model representing a maintenance work order or reported equipment damage.
 */
public class MaintenanceTicket implements Identifiable {
    public enum IssueCategory {
        FLAT_TIRE("Flat Tire / Puncture"),
        BRAKE_ISSUE("Brakes Malfunction"),
        CHAIN_GEAR("Chain / Gear Derailleur"),
        BATTERY_ELECTRICAL("E-Bike Battery / Display Fault"),
        STRUCTURAL("Handlebar / Seat / Frame"),
        ROUTINE_CHECKUP("Periodic Safety Inspection");

        private final String label;
        IssueCategory(String label) {
            this.label = label;
        }
        public String getLabel() {
            return label;
        }
        @Override
        public String toString() {
            return label;
        }
    }

    public enum TicketStatus {
        OPEN("Open / Reported"),
        IN_PROGRESS("In Repair"),
        RESOLVED("Resolved / Re-commissioned");

        private final String label;
        TicketStatus(String label) {
            this.label = label;
        }
        public String getLabel() {
            return label;
        }
        @Override
        public String toString() {
            return label;
        }
    }

    private int id;
    private int cycleId;
    private String cycleName;
    private int reportedByUserId;
    private String reportedByUserName;
    private IssueCategory issueCategory;
    private String description;
    private TicketStatus status;
    private String reportedAt;
    private String resolvedAt;
    private String technicianNotes;
    private double repairCost;

    public MaintenanceTicket(int id, int cycleId, String cycleName, int reportedByUserId,
                             String reportedByUserName, IssueCategory issueCategory,
                             String description, TicketStatus status, String reportedAt,
                             String resolvedAt, String technicianNotes, double repairCost) {
        this.id = id;
        this.cycleId = cycleId;
        this.cycleName = cycleName;
        this.reportedByUserId = reportedByUserId;
        this.reportedByUserName = reportedByUserName;
        this.issueCategory = issueCategory;
        this.description = description;
        this.status = status;
        this.reportedAt = reportedAt;
        this.resolvedAt = resolvedAt;
        this.technicianNotes = technicianNotes;
        this.repairCost = repairCost;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCycleId() {
        return cycleId;
    }

    public String getCycleName() {
        return cycleName;
    }

    public int getReportedByUserId() {
        return reportedByUserId;
    }

    public String getReportedByUserName() {
        return reportedByUserName;
    }

    public IssueCategory getIssueCategory() {
        return issueCategory;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getReportedAt() {
        return reportedAt;
    }

    public String getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(String resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getTechnicianNotes() {
        return technicianNotes;
    }

    public void setTechnicianNotes(String technicianNotes) {
        this.technicianNotes = technicianNotes;
    }

    public double getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(double repairCost) {
        this.repairCost = repairCost;
    }
}
