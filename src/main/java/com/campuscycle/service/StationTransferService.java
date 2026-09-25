package com.campuscycle.service;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.StationDao;
import com.campuscycle.model.Cycle;
import com.campuscycle.model.Station;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service managing dock rebalancing and fleet logistics across campus.
 */
public class StationTransferService {
    private static final Logger LOGGER = Logger.getLogger(StationTransferService.class.getName());
    private final CycleDao cycleDao;
    private final StationDao stationDao;

    public StationTransferService() {
        this.cycleDao = new CycleDao();
        this.stationDao = new StationDao();
    }

    /**
     * Relocates a cycle from one docking station to another.
     */
    public boolean rebalanceCycle(int cycleId, int targetStationId) {
        Optional<Cycle> cycleOpt = cycleDao.findById(cycleId);
        Optional<Station> stationOpt = stationDao.findById(targetStationId);

        if (cycleOpt.isEmpty() || stationOpt.isEmpty()) {
            return false;
        }

        Cycle cycle = cycleOpt.get();
        Station targetStation = stationOpt.get();

        boolean ok = cycleDao.updateStatus(cycleId, cycle.getStatus(), targetStationId);
        if (ok) {
            LOGGER.info(String.format("Rebalanced Cycle #%d (%s) -> Moved to %s [Station #%d]",
                cycleId, cycle.getModel(), targetStation.getName(), targetStationId));
            return true;
        }
        return false;
    }
}
