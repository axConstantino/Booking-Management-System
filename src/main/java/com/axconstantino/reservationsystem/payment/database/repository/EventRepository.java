package com.axconstantino.reservationsystem.payment.database.repository;

import com.axconstantino.reservationsystem.common.utils.BaseRepository;
import com.axconstantino.reservationsystem.payment.database.ProcessedEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends BaseRepository<ProcessedEvent, String> {
}
