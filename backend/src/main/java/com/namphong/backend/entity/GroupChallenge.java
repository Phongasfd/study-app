package com.namphong.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_challenges")
@Getter
@Setter
@NoArgsConstructor
public class GroupChallenge {

    public enum Status { UPCOMING, ACTIVE, ENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Integer targetSeconds;

    /** inclusive */
    @Column(nullable = false)
    private LocalDate startDate;

    /** inclusive */
    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Status statusOn(LocalDate today) {
        if (today.isBefore(startDate)) {
            return Status.UPCOMING;
        }
        return today.isAfter(endDate) ? Status.ENDED : Status.ACTIVE;
    }
}
