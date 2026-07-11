package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.adapter.StreamAdapter;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BandMemberPortConfig {

    @Bean
    public StreamAdapter streamAdapter(BandMemberRepository bandMemberRepository) {
        return new StreamAdapter(bandMemberRepository);
    }
}
