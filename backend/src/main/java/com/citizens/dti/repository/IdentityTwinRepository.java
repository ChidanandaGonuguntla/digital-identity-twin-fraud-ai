package com.citizens.dti.repository;

import com.citizens.dti.model.IdentityTwin;
import com.citizens.dti.persistence.CustomerTwinJpaRepository;
import com.citizens.dti.persistence.TwinMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Durable, PostgreSQL-backed store for identity twins.
 *
 * <p>Replaces the original in-memory map: the twin baseline now survives restarts and is
 * shared across instances. The running aggregates are typed columns; the document-shaped
 * behavioral state (devices, category counts, hour histogram, last location) is a single
 * JSONB column — the twin is still read and written as one coherent unit.
 *
 * <p>{@code findOrCreate} returns an unsaved twin for a new customer; it is persisted by
 * the first {@link #save} once the synchronization step runs.
 */
@Repository
public class IdentityTwinRepository {

    private final CustomerTwinJpaRepository jpa;
    private final TwinMapper mapper;

    public IdentityTwinRepository(CustomerTwinJpaRepository jpa, TwinMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public IdentityTwin findOrCreate(String customerId) {
        return jpa.findById(customerId)
                .map(mapper::toDomain)
                .orElseGet(() -> new IdentityTwin(customerId));
    }

    @Transactional(readOnly = true)
    public Optional<IdentityTwin> find(String customerId) {
        return jpa.findById(customerId).map(mapper::toDomain);
    }

    @Transactional
    public void save(IdentityTwin twin) {
        jpa.save(mapper.toEntity(twin));
    }
}
