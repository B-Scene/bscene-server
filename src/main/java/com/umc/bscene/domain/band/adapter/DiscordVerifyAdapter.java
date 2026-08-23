package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.dto.BandVerifyMessage;
import com.umc.bscene.domain.band.port.DiscordVerifyPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordVerifyAdapter implements DiscordVerifyPort {

    private static final int EMBED_COLOR = 0xFCBA43;

    // 봇 비활성 환경에서는 JDA 빈이 없으므로 ObjectProvider로 지연 조회
    private final ObjectProvider<JDA> jdaProvider;

    @Value("${discord.bot.verify-channel-id:}")
    private String verifyChannelId;

    @Override
    public String sendVerifyMessage(BandVerifyMessage message) {
        JDA jda = jdaProvider.getIfAvailable();
        if (jda == null || verifyChannelId.isBlank()) {
            log.warn("Discord 봇 미설정 — 밴드 검수 메시지 전송 생략. requestId = {}", message.requestId());
            return null;
        }

        try {
            TextChannel channel = jda.getTextChannelById(verifyChannelId);
            if (channel == null) {
                log.error("Discord 검수 채널을 찾을 수 없음. channelId = {}", verifyChannelId);
                return null;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor("밴드 생성 요청")
                    .setDescription("**" + message.bandName() + "**이(가) 밴드 생성을 요청했습니다.")
                    .setColor(EMBED_COLOR)
                    .addField("장르", message.genre(), true)
                    .addField("지역", message.region(), true)
                    .addField("소개", blankFallback(message.description()), false)
                    .setFooter("요청 ID: " + message.requestId());

            if (message.profileImageUrl() != null && !message.profileImageUrl().isBlank()) {
                embed.setThumbnail(message.profileImageUrl());
            }

            Message sent = channel.sendMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.success("band_approve:" + message.requestId(), "수락하기"),
                            Button.danger("band_reject:" + message.requestId(), "거절하기")
                    )
                    .complete();

            return sent.getId();
        } catch (Exception e) {
            log.error("밴드 검수 Discord 메시지 전송 실패. requestId = {}", message.requestId(), e);
            return null;
        }
    }

    private String blankFallback(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}
