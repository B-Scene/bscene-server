package com.umc.bscene.global.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Slf4j
@Configuration
@ConditionalOnExpression("!'${discord.bot.token:}'.isEmpty()")
public class DiscordBotConfig {

    @Bean(destroyMethod = "shutdown")
    public JDA jda(
            @Value("${discord.bot.token}") String token,
            List<ListenerAdapter> listeners
    ) throws InterruptedException {
        // createLight: 캐시/게이트웨이 인텐트 최소 구성 — 버튼 인터랙션 수신과 메시지 전송에는 충분
        JDA jda = JDABuilder.createLight(token)
                .addEventListeners(listeners.toArray())
                .build()
                .awaitReady();

        log.info("Discord 봇 연결 완료. bot = {}", jda.getSelfUser().getName());
        return jda;
    }
}
