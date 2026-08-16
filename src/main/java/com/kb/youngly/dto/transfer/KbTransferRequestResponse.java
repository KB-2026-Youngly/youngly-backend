package com.kb.youngly.dto.transfer;

import com.kb.youngly.enums.KbTransferStatus;
import com.kb.youngly.enums.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 로그인 사용자가 조회할 수 있는 라운드 정산 송금 요청 한 건의 응답 DTO.
 *
 * <p>{@code kb_transfer_requests}의 멱등성 키와 출발·도착 KB 계좌 식별자는
 * 송금 재처리를 위한 서버 내부 값이므로 API 응답에서 제외한다. 대신 화면에서 정산 진행
 * 상태와 실패 원인을 표시하는 데 필요한 금액, 수령자, 상태 및 처리 시각을 제공한다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbTransferRequestResponse {

    /** Youngly 내부에서 송금 요청 한 건을 식별하는 값. */
    private Long transferRequestId;

    /** 요청한 정산 송금 금액. */
    private BigDecimal amount;

    /** 이 요청의 업무 구분. 이 API에서는 항상 SETTLEMENT이다. */
    private TransactionCategory transactionCategory;

    /** PENDING, SUCCESS, FAILED, UNKNOWN 중 현재 송금 처리 상태. */
    private KbTransferStatus transferStatus;

    /** KB가 발급한 거래 식별자. 아직 접수되지 않았거나 실패한 경우 null일 수 있다. */
    private String kbTransactionId;

    /** 실패 코드. 성공 또는 처리 중인 요청이면 null이다. */
    private String failureCode;

    /** 사용자에게 정산 실패 원인을 안내할 때 사용할 메시지. */
    private String failureMessage;

    /** 실제 정산금을 받는 라운드 참여자의 user_id. */
    private String settlementReceiverId;

    /** 조회 대상 라운드 식별자. */
    private Long roundId;

    /** 송금 요청이 최초 생성된 시각. */
    private LocalDateTime requestedAt;

    /** 송금 결과가 확정된 시각. 미확정 상태이면 null이다. */
    private LocalDateTime completedAt;

    /** 요청 상태나 실패 정보가 마지막으로 변경된 시각. */
    private LocalDateTime updatedAt;
}
