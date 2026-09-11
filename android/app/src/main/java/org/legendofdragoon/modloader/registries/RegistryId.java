package org.legendofdragoon.modloader.registries;

import java.util.Objects;

public final class RegistryId {
    private final String value;

    public RegistryId(final String value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof RegistryId && value.equals(((RegistryId) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
