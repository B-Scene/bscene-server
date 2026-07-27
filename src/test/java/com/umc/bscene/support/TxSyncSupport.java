package com.umc.bscene.support;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 프로덕션 코드가 TransactionSynchronizationManager.registerSynchronization으로
 * afterCommit 훅을 거는 구간을 단위 테스트에서 재현하기 위한 헬퍼.
 * 동기화를 열어두지 않으면 registerSynchronization이 IllegalStateException을 던진다.
 */
public final class TxSyncSupport {

    private TxSyncSupport() {
    }

    public static void begin() {
        if (!TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.initSynchronization();
    }

    public static void end() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    /** 등록된 동기화의 afterCommit을 실제 커밋처럼 실행한다. */
    public static void triggerAfterCommit() {
        List<TransactionSynchronization> synchronizations =
                List.copyOf(TransactionSynchronizationManager.getSynchronizations());

        synchronizations.forEach(TransactionSynchronization::afterCommit);
    }

    /** 커밋 훅이 몇 개 등록됐는지. 발송 스킵(쿨다운 등) 검증에 사용. */
    public static int registeredCount() {
        return TransactionSynchronizationManager.isSynchronizationActive()
                ? TransactionSynchronizationManager.getSynchronizations().size()
                : 0;
    }
}
