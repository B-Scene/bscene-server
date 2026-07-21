package com.umc.bscene.domain.follow.entity;


import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "Follow",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"bandId", "userId"})
        }
)
public class Follow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

}
