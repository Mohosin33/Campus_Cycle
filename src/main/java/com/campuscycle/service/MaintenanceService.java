package com.campuscycle.service;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.MaintenanceTicketDao;
import com.campuscycle.model.Cycle;
import com.campuscycle.model.CycleStatus;
import com.campuscycle.model.MaintenanceTicket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service managing cycle maintenance tickets and rider damage reports.
 */
public class MaintenanceService {
    private static final Logger LOGGER = Logger.getLogger(MaintenanceService.class.getName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MaintenanceTicketDao ticketDao;
    private final CycleDao cycleDao;

    public MaintenanceService() {
        this.ticketDao = new MaintenanceTicketDao();
        this.cycleDao = new CycleDao();
    }

    /**
     * Reports an issue on a cycle, creates an open ticket, and marks the cycle for maintenance.
     */
    public MaintenanceTicket reportIssue(int cycleId, int reportedByUserId, MaintenanceTicket.IssueCategory category, String description) {
        String now = LocalDateTime.now().format(FORMATTER);

        Optional<Cycle> cycleOpt = cycleDao.findById(cycleId);
        String cycleName = cycleOpt.map(Cycle::getDisplayName).orElse("Cycle #" + cycleId);

        MaintenanceTicket ticket = new MaintenanceTicket(
            0, cycleId, cycleName, reportedByUserId, "", category,
            description, MaintenanceTicket.TicketStatus.OPEN, now, null, null, 0.0
        );

        MaintenanceTicket saved = ticketDao.save(ticket);
        if (saved != null) {
            // Automatically place cycle in MAINTENANCE status
            if (cycleOpt.isPresent()) {
                cycleDao.updateLocationAndStatus(cycleId, CycleStatus.MAINTENANCE, cycleOpt.get().getLocation());
            }
            LOGGER.info(String.format("Issue reported on cycle #%d [%s]. Marked into MAINTENANCE.", cycleId, category.getLabel()));
        }
        return saved;
    }

    /**
     * Resolves a work order with dockless return location.
     */
    public boolean resolveTicket(int ticketId, String technicianNotes, double repairCost, String returnLocation) {
        Optional<MaintenanceTicket> ticketOpt = ticketDao.findById(ticketId);
        if (ticketOpt.isEmpty()) return false;

        MaintenanceTicket ticket = ticketOpt.get();
        String now = LocalDateTime.now().format(FORMATTER);

        ticket.setStatus(MaintenanceTicket.TicketStatus.RESOLVED);
        ticket.setResolvedAt(now);
        ticket.setTechnicianNotes(technicianNotes);
        ticket.setRepairCost(repairCost);

        boolean updated = ticketDao.update(ticket);
        if (updated) {
            String loc = (returnLocation != null && !returnLocation.trim().isEmpty()) ? returnLocation.trim() : "Campus Core";
            cycleDao.updateLocationAndStatus(ticket.getCycleId(), CycleStatus.AVAILABLE, loc);
            LOGGER.info("Ticket #" + ticketId + " resolved. Cycle #" + ticket.getCycleId() + " restored to location: " + loc);
            return true;
        }
        return false;
    }

    public boolean resolveTicket(int ticketId, String technicianNotes, double repairCost, int ignoredStationId) {
        return resolveTicket(ticketId, technicianNotes, repairCost, "Campus Core");
    }

    public List<MaintenanceTicket> getOpenTickets() {
        return ticketDao.findOpenTickets();
    }

    public List<MaintenanceTicket> getAllTickets() {
        return ticketDao.findAll();
    }
}
