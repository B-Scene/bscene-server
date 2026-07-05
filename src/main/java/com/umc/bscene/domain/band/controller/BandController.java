package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberInviteRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberSearchItem;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bands")
public class BandController {

    private final BandService bandService;

    // 밴드 개설 API
    @PostMapping
    public ResponseEntity<SuccessResponse<BandResponse>> createBand(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody BandCreateRequest request
    ) {
        BandResponse response = bandService.createBand(authMember.getUser().getId(), request);
        SuccessResponse<BandResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_CREATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 멤버 초대 API (오너만 가능)
    @PostMapping("/{bandId}/members")
    public ResponseEntity<SuccessResponse<BandMemberResponse>> inviteMember(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId,
            @Valid @RequestBody BandMemberInviteRequest request
    ) {
        BandMemberResponse response = bandService.inviteMember(authMember.getUser().getId(), bandId, request);
        SuccessResponse<BandMemberResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_MEMBER_INVITE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 초대 수락 API
    @PatchMapping("/{bandId}/members/accept")
    public ResponseEntity<SuccessResponse<Void>> acceptInvite(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        bandService.acceptInvite(authMember.getUser().getId(), bandId);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                BandSuccessCode.BAND_MEMBER_ACCEPT_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 초대 거절 API (row 삭제)
    @DeleteMapping("/{bandId}/members/reject")
    public ResponseEntity<SuccessResponse<Void>> rejectInvite(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        bandService.rejectInvite(authMember.getUser().getId(), bandId);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                BandSuccessCode.BAND_MEMBER_REJECT_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 멤버 제거 API (오너만 가능)
    @DeleteMapping("/{bandId}/members/{userId}")
    public ResponseEntity<SuccessResponse<Void>> removeMember(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId,
            @PathVariable Long userId
    ) {
        bandService.removeMember(authMember.getUser().getId(), bandId, userId);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                BandSuccessCode.BAND_MEMBER_REMOVE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 멤버 목록 조회 API
    @GetMapping("/{bandId}/members")
    public ResponseEntity<SuccessResponse<List<BandMemberResponse>>> getMembers(
            @PathVariable Long bandId
    ) {
        List<BandMemberResponse> response = bandService.getMembers(bandId);
        SuccessResponse<List<BandMemberResponse>> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_MEMBER_LIST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 초대 대상 닉네임 검색 API
    @GetMapping("/{bandId}/members/search")
    public ResponseEntity<SuccessResponse<List<BandMemberSearchItem>>> searchInviteTargets(
            @PathVariable Long bandId,
            @RequestParam String keyword
    ) {
        List<BandMemberSearchItem> response = bandService.searchInviteTargets(bandId, keyword);
        SuccessResponse<List<BandMemberSearchItem>> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_MEMBER_SEARCH_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
