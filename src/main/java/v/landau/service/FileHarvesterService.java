package v.landau.service;


import v.landau.config.HarvesterConfig;
import v.landau.exception.HarvesterException;
import v.landau.model.HarvestResult;

/**
 * Main service interface for file harvesting operations.
 */
public interface FileHarvesterService {
    /**
     * Harvest files from source to target directory according to configuration.
     *
     * @param config harvesting configuration
     * @return result of the harvesting operation
     * @throws HarvesterException if harvesting fails
     */
    HarvestResult harvest(HarvesterConfig config) throws HarvesterException;
}