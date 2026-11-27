package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.band.dto.FollowerBlock;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkEntryResponse;
import com.umc.bscene.domain.band.dto.response.BandInviteLinkResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberAcceptResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberSearchItem;
import com.umc.bscene.domain.band.dto.response.BandNameCheckResponse;
import com.umc.bscene.domain.band.dto.response.BandProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.global.response.CursorPage;
import java.util.List;

public final class BandApiResponseSchemas {

    private BandApiResponseSchemas() {
    }

    public abstract static class ProfileEnvelope extends SwaggerResponseEnvelope<BandMemberProfileResponse> {
    }

    public abstract static class ProfileListEnvelope extends SwaggerResponseEnvelope<List<BandMemberProfileResponse>> {
    }

    public abstract static class BandCreationEnvelope extends SwaggerResponseEnvelope<BandResponse> {
    }

    public abstract static class NameCheckEnvelope extends SwaggerResponseEnvelope<BandNameCheckResponse> {
    }

    public abstract static class BandProfileEnvelope extends SwaggerResponseEnvelope<BandProfileResponse> {
    }

    public abstract static class MembershipEnvelope extends SwaggerResponseEnvelope<BandMemberResponse> {
    }

    public abstract static class MemberListEnvelope extends SwaggerResponseEnvelope<List<BandMemberResponse>> {
    }

    public abstract static class MemberAcceptEnvelope extends SwaggerResponseEnvelope<BandMemberAcceptResponse> {
    }

    public abstract static class InviteTargetsEnvelope extends SwaggerResponseEnvelope<List<BandMemberSearchItem>> {
    }

    public abstract static class InviteLinkEnvelope extends SwaggerResponseEnvelope<BandInviteLinkResponse> {
    }

    public abstract static class InviteLinkDetailsEnvelope extends SwaggerResponseEnvelope<BandInviteLinkDetailResponse> {
    }

    public abstract static class InviteLinkEntryEnvelope extends SwaggerResponseEnvelope<BandInviteLinkEntryResponse> {
    }

    public abstract static class FollowersEnvelope extends SwaggerResponseEnvelope<CursorPage<FollowerBlock>> {
    }
}
