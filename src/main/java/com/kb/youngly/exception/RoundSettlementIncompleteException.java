package com.kb.youngly.exception;

/**
 * 한 라운드에서 일부 참여자의 KB 송금이 실패했음을 나타내는 예외.
 *
 * <p>이 예외는 모든 참여자에 대한 송금 시도를 마친 뒤 발생한다. 정산 서비스는 이 예외를
 * 롤백 대상에서 제외하여 성공한 참여자의 정산 결과와 전체 참여자의 실패 대응값 초기화를
 * 커밋하고, 개발용 컨트롤러는 예외 메시지를 그룹별 실패 결과에 포함한다.</p>
 */
public class RoundSettlementIncompleteException extends RuntimeException {

    public RoundSettlementIncompleteException(String message) {
        super(message);
    }
}
