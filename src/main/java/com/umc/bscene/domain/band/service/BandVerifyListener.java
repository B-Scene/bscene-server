package com.umc.bscene.domain.band.service;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.umc.bscene.domain.band.dto.BandVerifyAcceptResult;
import com.umc.bscene.domain.band.exception.BandException;

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
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

/**
 * Discord 밴드 생성 검수 메시지의 [수락하기]/[거절하기] 버튼 인터랙션 처리.
 * custom_id의 requestId는 BandCreationRequest.id이며, 처리 후에는
 * 비활성 [수락됨]/[거절됨] 버튼으로 교체되어 중복 클릭이 방지된다.
 *
 * DB 작업이 있는 경로는 Discord의 3초 응답 제한을 넘길 수 있으므로(비관적 락 대기 등)
 * 반드시 deferEdit()로 선응답한 뒤 hook으로 결과를 반영한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BandVerifyListener extends ListenerAdapter {

    private static final String APPROVE_PREFIX = "band_approve:";
    private static final String REJECT_PREFIX = "band_reject:";
    // 동명 ACCEPTED 밴드가 있을 때 교체 여부를 재확인하는 버튼
    private static final String REPLACE_YES_PREFIX = "band_replace_yes:";
    private static final String REPLACE_NO_PREFIX = "band_replace_no:";
    private static final String REJECT_REASON_PREFIX = "band_reject_reason:";
    private static final String REJECT_MODAL_PREFIX = "band_reject_modal:";
    private static final String CUSTOM_REASON = "custom";
    private static final String CANCEL_REJECT = "cancel";

    // 임베드 세로줄 색: 대기(금색)는 전송 시 지정되고, 처리 결과에 따라 아래 색으로 교체
    private static final int ACCEPTED_COLOR = 0x3A834C; // RGB(58, 131, 76)
    private static final int REJECTED_COLOR = 0xED4245;

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BandVerifyService bandVerifyService;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        try {
            if (componentId.startsWith(APPROVE_PREFIX)) {
                long requestId = Long.parseLong(componentId.substring(APPROVE_PREFIX.length()));
                event.deferEdit().queue();
                handleAccept(event, requestId, false);
                return;
            }

            if (componentId.startsWith(REPLACE_YES_PREFIX)) {
                long requestId = Long.parseLong(componentId.substring(REPLACE_YES_PREFIX.length()));
                event.deferEdit().queue();
                handleAccept(event, requestId, true);
                return;
            }

            if (componentId.startsWith(REPLACE_NO_PREFIX)) {
                String requestId = componentId.substring(REPLACE_NO_PREFIX.length());
                // 교체 확인을 취소하고 원래의 수락/거절 버튼으로 복원 (DB 작업이 없어 바로 편집)
                event.editMessage("")
                        .setComponents(ActionRow.of(approveRejectButtons(requestId)))
                        .queue();
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
        } catch (Exception e) {
            log.error("밴드 검수 버튼 처리 중 오류. componentId = {}", componentId, e);
            acknowledgeFailure(event);
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith(REJECT_REASON_PREFIX)) {
            return;
        }

        try {
            String requestId = componentId.substring(REJECT_REASON_PREFIX.length());
            String selected = event.getValues().get(0);

            if (selected.equals(CANCEL_REJECT)) {
                // 드롭다운을 원래의 수락/거절 버튼으로 복원 (DB 작업이 없어 바로 편집)
                event.editComponents(ActionRow.of(approveRejectButtons(requestId))).queue();
                return;
            }

            if (selected.equals(CUSTOM_REASON)) {
                // 모달 표시 자체가 이 인터랙션의 응답이므로 defer하지 않는다
                TextInput reasonInput = TextInput.create("reason", "사유", TextInputStyle.PARAGRAPH)
                        .setMaxLength(200)
                        .build();
                event.replyModal(Modal.create(REJECT_MODAL_PREFIX + requestId, "거절 사유 입력")
                                .addComponents(ActionRow.of(reasonInput))
                                .build())
                        .queue();
                return;
            }

            event.deferEdit().queue();
            handleReject(event, event.getMessage(), Long.parseLong(requestId), selected);
        } catch (Exception e) {
            log.error("밴드 검수 거절 사유 처리 중 오류. componentId = {}", componentId, e);
            acknowledgeFailure(event);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith(REJECT_MODAL_PREFIX)) {
            return;
        }

        try {
            long requestId = Long.parseLong(event.getModalId().substring(REJECT_MODAL_PREFIX.length()));
            ModalMapping reasonValue = event.getValue("reason");
            String reason = reasonValue == null ? "사유 미입력" : reasonValue.getAsString();

            event.deferEdit().queue();
            handleReject(event, event.getMessage(), requestId, reason);
        } catch (Exception e) {
            log.error("밴드 검수 거절 모달 처리 중 오류. modalId = {}", event.getModalId(), e);
            acknowledgeFailure(event);
        }
    }

    // 수락 처리. 동명 ACCEPTED 밴드 교체 여부 판정은 accept() 트랜잭션(비관적 락) 안에서 이루어지고,
    // NEEDS_REPLACE_CONFIRM이 반환되면 대상 밴드 정보를 담은 확인 단계로 전환한다
    private void handleAccept(ButtonInteractionEvent event, long requestId, boolean replaceConfirmed) {
        BandVerifyAcceptResult result;
        try {
            result = bandVerifyService.accept(requestId, event.getUser().getName(), replaceConfirmed);
        } catch (BandException e) {
            // 라이브 이력 등 도메인 안전장치에 걸린 경우 — 운영진이 원인을 알 수 있게 코드 메시지를 그대로 노출
            log.warn("밴드 검수 수락 차단됨. requestId = {}, code = {}", requestId, e.getBaseResponseCode().getCode());
            replyEphemeral(event, "수락할 수 없습니다 — " + e.getBaseResponseCode().getMessage());
            return;
        } catch (Exception e) {
            log.error("밴드 검수 수락 처리 실패. requestId = {}", requestId, e);
            replyEphemeral(event, "처리 중 오류가 발생했습니다. 로그를 확인해주세요.");
            return;
        }

        switch (result.outcome()) {
            case ALREADY_PROCESSED -> replyEphemeral(event, "이미 처리된 요청입니다.");

            case NEEDS_REPLACE_CONFIRM -> {
                BandVerifyAcceptResult.ReplaceTarget target = result.replaceTarget();
                // 대상이 활동 없는 더미인지, 실제 운영 중인 밴드인지 운영진이 판단할 근거를 함께 보여준다
                String confirmText = String.format(
                        "이미 등록된 동명 밴드가 있습니다. 밴드 ID %d / 멤버 %d명 / 팔로워 %d명 / 생성일 %s. "
                                + "수락하면 이 밴드가 삭제되고 새 밴드로 교체됩니다. 정말 교체하시겠습니까?",
                        target.bandId(),
                        target.memberCount(),
                        target.followerCount(),
                        target.createdAt() == null ? "-" : target.createdAt().format(CREATED_AT_FORMAT)
                );
                event.getHook().editOriginal(confirmText)
                        .setComponents(ActionRow.of(
                                Button.danger(REPLACE_YES_PREFIX + requestId, "예"),
                                Button.secondary(REPLACE_NO_PREFIX + requestId, "아니오")
                        ))
                        .queue(null, failure ->
                                log.error("교체 확인 카드 표시 실패. requestId = {}", requestId, failure));
            }

            case ACCEPTED -> {
                log.info("수락되어 밴드 생성됨. bandId = {}", result.bandId());
                // 교체 확인 단계에서 넘어온 경우 확인 문구가 남아 있으므로 content를 비운다
                var edit = event.getHook().editOriginal("")
                        .setComponents(ActionRow.of(acceptedButton()));
                recolorEmbed(event.getMessage(), ACCEPTED_COLOR)
                        .ifPresent(edit::setEmbeds);
                edit.queue(null, failure ->
                        log.error("검수 카드 갱신 실패 (수락은 이미 반영됨). requestId = {}", requestId, failure));
            }
        }
    }

    // 사유 드롭다운 선택과 기타(모달) 입력이 공유하는 거절 처리.
    // 두 이벤트 모두 deferEdit로 선응답된 상태이므로 hook으로 원본 메시지를 수정한다
    private void handleReject(IMessageEditCallback event, Message message, long requestId, String reason) {
        boolean processed;
        try {
            processed = bandVerifyService.reject(requestId, reason, interactionUserName(event));
        } catch (BandException e) {
            // 라이브 이력 등 도메인 안전장치에 걸린 경우 — 운영진이 원인을 알 수 있게 코드 메시지를 그대로 노출
            log.warn("밴드 검수 거절 차단됨. requestId = {}, code = {}", requestId, e.getBaseResponseCode().getCode());
            event.getHook().editOriginal("거절할 수 없습니다 — " + e.getBaseResponseCode().getMessage())
                    .setComponents(ActionRow.of(approveRejectButtons(String.valueOf(requestId))))
                    .queue(null, failure ->
                            log.error("거절 차단 카드 갱신 실패. requestId = {}", requestId, failure));
            return;
        } catch (Exception e) {
            log.error("밴드 검수 거절 처리 실패. requestId = {}", requestId, e);
            event.getHook().editOriginal("처리 중 오류가 발생했습니다. 로그를 확인해주세요.")
                    .queue(null, failure ->
                            log.error("거절 오류 카드 갱신 실패. requestId = {}", requestId, failure));
            return;
        }

        if (!processed) {
            event.getHook().editOriginal("이미 처리된 요청입니다.")
                    .queue(null, failure ->
                            log.error("거절 중복 카드 갱신 실패. requestId = {}", requestId, failure));
            return;
        }

        log.info("거절됨. 사유 : {}", reason);
        var edit = event.getHook().editOriginal("거절됨 — 사유 : " + reason)
                .setComponents(ActionRow.of(rejectedButton()));
        recolorEmbed(message, REJECTED_COLOR)
                .ifPresent(edit::setEmbeds);
        edit.queue(null, failure ->
                log.error("검수 카드 갱신 실패 (거절은 이미 반영됨). requestId = {}", requestId, failure));
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

    // 선응답(deferEdit) 이후에는 hook의 ephemeral 후속 메시지로 응답한다
    private void replyEphemeral(ButtonInteractionEvent event, String content) {
        event.getHook().sendMessage(content).setEphemeral(true)
                .queue(null, failure -> log.error("검수 처리 결과 응답 실패", failure));
    }

    // 예외로 정상 응답을 만들지 못한 경우에도 인터랙션이 "실패" 상태로 방치되지 않도록 최소 응답을 보낸다
    private void acknowledgeFailure(IMessageEditCallback event) {
        try {
            if (event.isAcknowledged()) {
                event.getHook().sendMessage("처리 중 오류가 발생했습니다. 로그를 확인해주세요.")
                        .setEphemeral(true)
                        .queue(null, failure -> log.error("검수 오류 응답 실패", failure));
                return;
            }
            event.deferEdit().queue(hook ->
                            event.getHook().sendMessage("처리 중 오류가 발생했습니다. 로그를 확인해주세요.")
                                    .setEphemeral(true)
                                    .queue(null, failure -> log.error("검수 오류 응답 실패", failure)),
                    failure -> log.error("검수 오류 선응답 실패", failure));
        } catch (Exception e) {
            log.error("검수 오류 응답 처리 중 추가 오류", e);
        }
    }

    private static Button[] approveRejectButtons(String requestId) {
        return new Button[]{
                Button.success(APPROVE_PREFIX + requestId, "수락하기"),
                Button.danger(REJECT_PREFIX + requestId, "거절하기")
        };
    }

    // 처리 완료 후 중복 클릭 방지용 비활성 버튼 (비활성 버튼은 Discord가 자동으로 흐리게 렌더링)
    private static Button acceptedButton() {
        return Button.success("band_processed", "수락됨").asDisabled();
    }

    private static Button rejectedButton() {
        return Button.danger("band_processed", "거절됨").asDisabled();
    }
}
