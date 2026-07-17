package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.adapter.StreamAdapter;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BandMemberPortConfig {

    @Bean
    public StreamAdapter streamAdapter(BandMemberRepository bandMemberRepository, BandRepository bandRepository) {
        return new StreamAdapter(bandMemberRepository, bandRepository);
    }
}
