package com.umrah.scanner.company.infrastructure;

import com.umrah.scanner.company.domain.CompanyProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID>,
        JpaSpecificationExecutor<CompanyProfile> {

    Optional<CompanyProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /** Overrides the inherited findAllById to batch-fetch addresses+city in one query instead of
     * per-row lazy loads — used to warm the persistence context for an already-paginated list. */
    @EntityGraph(attributePaths = {"addresses", "addresses.city"})
    List<CompanyProfile> findAllById(Iterable<UUID> ids);
}
