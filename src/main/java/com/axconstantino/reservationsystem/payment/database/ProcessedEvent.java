package com.axconstantino.reservationsystem.payment.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_events",
        uniqueConstraints = @UniqueConstraint(columnNames = "id"))
@Getter
@NoArgsConstructor
public class ProcessedEvent {
    @Id
    private String id;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "event_type")
    private String eventType;

    public ProcessedEvent(String id) {
        this.id = id;
    }

}
