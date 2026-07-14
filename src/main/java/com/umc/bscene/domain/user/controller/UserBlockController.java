package com.umc.bscene.domain.user.controller;

import com.umc.bscene.domain.user.dto.request.UserBlockRequest;
import com.umc.bscene.domain.user.response.code.UserSuccessCode;
import com.umc.bscene.domain.user.service.UserBlockService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/users")
public class UserBlockController {
    private final UserBlockService userBlockService;

    @PostMapping("/blocks")
    public SuccessResponse<Void> block(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody UserBlockRequest request
    ) {
        userBlockService.block(authMember.getUser().getId(), request.targetUserId());
        return new SuccessResponse<>(null, UserSuccessCode.USER_BLOCK_SUCCESS);
    }

    @DeleteMapping("/blocks")
    public SuccessResponse<Void> unblock(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody UserBlockRequest request
    ) {
        userBlockService.unblock(authMember.getUser().getId(), request.targetUserId());
        return new SuccessResponse<>(null, UserSuccessCode.USER_UNBLOCK_SUCCESS);
    }

}
