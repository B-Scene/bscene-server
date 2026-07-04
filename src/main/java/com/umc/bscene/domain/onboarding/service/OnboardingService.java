package com.umc.bscene.domain.onboarding.service;

import com.umc.bscene.domain.onboarding.dto.response.GenreResponse;
import com.umc.bscene.domain.onboarding.enums.Genre;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    // 장르 목록 조회
    public List<GenreResponse> getGenres() {
        return Arrays.stream(Genre.values())
                .map(genre -> new GenreResponse(
                        genre.name(),
                        genre.getName()
                ))
                .toList();
    }
}