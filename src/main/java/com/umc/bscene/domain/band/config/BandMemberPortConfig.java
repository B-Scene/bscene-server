package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.adapter.StreamAdapter;
import com.umc.bscene.domain.band.adapter.UserAdapter;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.PerformancePort;
import com.umc.bscene.domain.band.port.SessionPort;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.user.port.BandPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BandMemberPortConfig {

    @Bean
    public BandPort BandUserAdapter(
            BandMemberProfileRepository bandMemberProfileRepository,
            BandMemberRepository bandMemberRepository,
            FollowPort followPort,
            SessionPort sessionPort,
            PerformancePort performancePort
    ) {
        return new UserAdapter(
                bandMemberProfileRepository,
                bandMemberRepository,
                followPort,
                sessionPort,
                performancePort
        );
    }

    @Bean
    public StreamAdapter streamAdapter(BandMemberRepository bandMemberRepository, BandRepository bandRepository) {
        return new StreamAdapter(bandMemberRepository, bandRepository);
    }
}
