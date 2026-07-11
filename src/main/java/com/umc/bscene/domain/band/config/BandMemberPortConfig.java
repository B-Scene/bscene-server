package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.adapter.BandMemberPortAdapter;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BandMemberPortConfig {

    @Bean
    public BandMemberPortAdapter bandMemberPortAdapter(BandMemberRepository bandMemberRepository) {
        return new BandMemberPortAdapter(bandMemberRepository);
    }
}
