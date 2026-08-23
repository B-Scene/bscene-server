package com.umc.bscene.domain.band.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

/**
 * Discord 밴드 생성 검수 메시지의 [수락하기]/[거절하기] 버튼 인터랙션 처리.
 * custom_id의 requestId는 BandCreationRequest.id이며, 처리 후에는
 * 비활성 [수락됨]/[거절됨] 버튼으로 교체되어 중복 클릭이 방지된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BandVerifyListener extends ListenerAdapter {

    private static final String APPROVE_PREFIX = "band_approve:";
    private static final String REJECT_PREFIX = "band_reject:";
    private static final String REJECT_REASON_PREFIX = "band_reject_reason:";
    private static final String REJECT_MODAL_PREFIX = "band_reject_modal:";
    private static final String CUSTOM_REASON = "custom";
    private static final String CANCEL_REJECT = "cancel";

    // 임베드 세로줄 색: 대기(금색)는 전송 시 지정되고, 처리 결과에 따라 아래 색으로 교체
    private static final int ACCEPTED_COLOR = 0x3A834C; // RGB(58, 131, 76)
    private static final int REJECTED_COLOR = 0xED4245;

    private final BandVerifyService bandVerifyService;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(APPROVE_PREFIX)) {
            long requestId = Long.parseLong(componentId.substring(APPROVE_PREFIX.length()));
            handleApprove(event, requestId);
            return;
        }

        if (componentId.startsWith(REJECT_PREFIX)) {
            String requestId = componentId.substring(REJECT_PREFIX.length());
            StringSelectMenu reasonMenu = StringSelectMenu.create(REJECT_REASON_PREFIX + requestId)
                    .setPlaceholder("거절 사유를 선택하세요")
                    .addOption("활동 자료 불충분", "활동 자료 불충분")
                    .addOption("실존 확인 불가", "실존 확인 불가")
                    .addOption("중복 밴드", "중복 밴드")
                    .addOption("기타 (직접 입력)", CUSTOM_REASON)
                    .addOption("취소 (버튼으로 돌아가기)", CANCEL_REJECT)
                    .build();
            // 새 메시지 대신 원본 메시지의 버튼 영역을 드롭다운으로 교체
            event.editComponents(ActionRow.of(reasonMenu)).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith(REJECT_REASON_PREFIX)) {
            return;
        }

        String requestId = componentId.substring(REJECT_REASON_PREFIX.length());
        String selected = event.getValues().get(0);

        if (selected.equals(CANCEL_REJECT)) {
            // 드롭다운을 원래의 수락/거절 버튼으로 복원
            event.editComponents(ActionRow.of(
                    Button.success(APPROVE_PREFIX + requestId, "수락하기"),
                    Button.danger(REJECT_PREFIX + requestId, "거절하기")
            )).queue();
            return;
        }

        if (selected.equals(CUSTOM_REASON)) {
            TextInput reasonInput = TextInput.create("reason", "사유", TextInputStyle.PARAGRAPH)
                    .setMaxLength(200)
                    .build();
            event.replyModal(Modal.create(REJECT_MODAL_PREFIX + requestId, "거절 사유 입력")
                            .addComponents(ActionRow.of(reasonInput))
                            .build())
                    .queue();
            return;
        }

        handleReject(event, event.getMessage(), Long.parseLong(requestId), selected);
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith(REJECT_MODAL_PREFIX)) {
            return;
        }

        long requestId = Long.parseLong(event.getModalId().substring(REJECT_MODAL_PREFIX.length()));
        String reason = event.getValue("reason").getAsString();
        handleReject(event, event.getMessage(), requestId, reason);
    }

    private void handleApprove(ButtonInteractionEvent event, long requestId) {
        Optional<Long> acceptedBandId;
        try {
            acceptedBandId = bandVerifyService.accept(requestId, event.getUser().getName());
        } catch (Exception e) {
            log.error("밴드 검수 수락 처리 실패. requestId = {}", requestId, e);
            event.reply("처리 중 오류가 발생했습니다. 로그를 확인해주세요.").setEphemeral(true).queue();
            return;
        }

        if (acceptedBandId.isEmpty()) {
            event.reply("이미 처리된 요청입니다.").setEphemeral(true).queue();
            return;
        }

        log.info("수락되어 밴드 생성됨. bandId = {}", acceptedBandId.get());
        var edit = event.editComponents(ActionRow.of(acceptedButton()));
        recolorEmbed(event.getMessage(), ACCEPTED_COLOR)
                .ifPresent(edit::setEmbeds);
        edit.queue();
    }

    // 사유 드롭다운 선택과 기타(모달) 입력이 공유하는 거절 처리.
    // 두 이벤트 모두 원본 메시지에 대한 컴포넌트 인터랙션이라 editMessage로 원본을 수정할 수 있다.
    private void handleReject(IMessageEditCallback event, Message message, long requestId, String reason) {
        boolean processed;
        try {
            processed = bandVerifyService.reject(requestId, reason, interactionUserName(event));
        } catch (Exception e) {
            log.error("밴드 검수 거절 처리 실패. requestId = {}", requestId, e);
            event.editMessage("처리 중 오류가 발생했습니다. 로그를 확인해주세요.").queue();
            return;
        }

        if (!processed) {
            event.editMessage("이미 처리된 요청입니다.").queue();
            return;
        }

        log.info("거절됨. 사유 : {}", reason);
        var edit = event.editMessage("거절됨 — 사유 : " + reason)
                .setComponents(ActionRow.of(rejectedButton()));
        recolorEmbed(message, REJECTED_COLOR)
                .ifPresent(edit::setEmbeds);
        edit.queue();
    }

    // 원본 메시지의 임베드를 유지한 채 세로줄 색만 교체한 사본 반환
    private Optional<MessageEmbed> recolorEmbed(Message message, int color) {
        if (message == null || message.getEmbeds().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EmbedBuilder(message.getEmbeds().get(0)).setColor(color).build());
    }

    private String interactionUserName(IMessageEditCallback event) {
        return ((Interaction) event).getUser().getName();
    }

    // 처리 완료 후 중복 클릭 방지용 비활성 버튼 (비활성 버튼은 Discord가 자동으로 흐리게 렌더링)
    private static Button acceptedButton() {
        return Button.success("band_processed", "수락됨").asDisabled();
    }

    private static Button rejectedButton() {
        return Button.danger("band_processed", "거절됨").asDisabled();
    }
}
