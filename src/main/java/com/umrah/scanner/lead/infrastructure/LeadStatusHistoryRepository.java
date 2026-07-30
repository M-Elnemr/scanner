package com.umrah.scanner.lead.infrastructure;

import com.umrah.scanner.lead.domain.LeadStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadStatusHistoryRepository extends JpaRepository<LeadStatusHistory, UUID> {

    List<LeadStatusHistory> findAllByLeadIdOrderByChangedAtAsc(UUID leadId);
}
