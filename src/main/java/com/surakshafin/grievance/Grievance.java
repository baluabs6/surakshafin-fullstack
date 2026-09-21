package com.surakshafin.grievance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "grievances")
@Getter
@Setter
@NoArgsConstructor
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String issueType;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String routedTo;

    @Column(nullable = false)
    private String status = "DRAFTED";

    @Column(nullable = false, length = 4000)
    private String generatedComplaintText;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
