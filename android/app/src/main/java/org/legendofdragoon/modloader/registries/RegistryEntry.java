package org.legendofdragoon.modloader.registries;

public class RegistryEntry {
    private final RegistryId registryId;

    public RegistryEntry(final RegistryId registryId) {
        this.registryId = registryId;
    }

    public RegistryId getRegistryId() {
        return registryId;
    }
}
