package io.provenly.commons.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

/**
 * Base entity class providing common fields for all domain entities.
 * Includes audit fields and standard entity operations.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {

    /**
     * Unique identifier for the entity.
     */
    @EqualsAndHashCode.Include
    protected UUID id;

    /**
     * When the entity was created.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    protected Instant createdAt;

    /**
     * When the entity was last modified.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    protected Instant updatedAt;

    /**
     * Version for optimistic locking.
     */
    protected Long version;

    /**
     * Check if this entity is new (not persisted yet).
     */
    public boolean isNew() {
        return id == null;
    }

    /**
     * Pre-persist callback to set creation timestamp.
     */
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /**
     * Pre-update callback to set update timestamp.
     */
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
