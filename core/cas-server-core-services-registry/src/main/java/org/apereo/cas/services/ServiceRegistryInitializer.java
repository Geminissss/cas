package org.apereo.cas.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/**
 * Initializes a given service registry data store with available
 * JSON service definitions if necessary (based on configuration flag).
 *
 * @author Dmitriy Kopylenko
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@AllArgsConstructor
public class ServiceRegistryInitializer {
    private final ServiceRegistry jsonServiceRegistry;
    private final ServiceRegistry serviceRegistry;
    private final ServicesManager servicesManager;
    private final boolean initFromJson;

    /**
     * Init service registry if necessary.
     */
    public void initServiceRegistryIfNecessary() {
        final long size = this.serviceRegistry.size();
        LOGGER.debug("Service registry contains [{}] service definitions", size);

        if (!this.initFromJson) {
            LOGGER.info("The service registry database backed by [{}] will not be initialized from JSON services. "
                    + "If the service registry database ends up empty, CAS will refuse to authenticate services "
                    + "until service definitions are added to the registry. To auto-initialize the service registry, "
                    + "set 'cas.serviceRegistry.initFromJson=true' in your CAS settings.",
                    this.serviceRegistry.getName());
            return;
        }

        LOGGER.warn("Service registry [{}] will be auto-initialized from JSON service definitions. "
                + "This behavior is only useful for testing purposes and MAY NOT be appropriate for production. "
                + "Consider turning off this behavior via the setting [cas.serviceRegistry.initFromJson=false] "
                + "and explicitly register definitions in the services registry.", this.serviceRegistry.getName());

        final List<RegisteredService> servicesLoaded = this.jsonServiceRegistry.load();
        LOGGER.debug("Loading JSON services are [{}]", servicesLoaded);

        for (final RegisteredService r : servicesLoaded) {
            if (findExistingMatchForService(r)) {
                continue;
            }
            LOGGER.debug("Initializing service registry with the [{}] JSON service definition...", r);
            this.serviceRegistry.save(r);
        }
        this.servicesManager.load();
        LOGGER.info("Service registry [{}] contains [{}] service definitions", this.serviceRegistry.getName(), this.servicesManager.count());

    }

    private boolean findExistingMatchForService(final RegisteredService r) {
        RegisteredService match = this.serviceRegistry.findServiceById(r.getServiceId());
        if (match != null) {
            LOGGER.warn("Skipping [{}] JSON service definition as a matching service [{}] is found in the registry", r.getName(), match.getName());
            return true;
        }
        match = this.serviceRegistry.findServiceByExactServiceId(r.getServiceId());
        if (match != null) {
            LOGGER.warn("Skipping [{}] JSON service definition as a matching service [{}] is found in the registry", r.getName(), match.getName());
            return true;
        }
        match = this.serviceRegistry.findServiceById(r.getId());
        if (match != null) {
            LOGGER.warn("Skipping [{}] JSON service definition as a matching id [{}] is found in the registry", r.getName(), match.getId());
            return true;
        }
        return false;
    }
}
