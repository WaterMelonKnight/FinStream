package io.finstream.persistence;

import io.finstream.domain.FinancialEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FinancialEventRepository
        extends JpaRepository<FinancialEvent, UUID>, JpaSpecificationExecutor<FinancialEvent> {}
