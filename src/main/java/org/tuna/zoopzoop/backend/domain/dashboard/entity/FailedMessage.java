package org.tuna.zoopzoop.backend.domain.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.MessageStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // 생성일시 자동 기록용
public class FailedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String queueName; // 실패한 원본 큐 이름

    @Lob // 메시지 내용이 길 수 있으므로 LOB 설정
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload; // JSON 형태의 메시지 본문

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // 에러 메시지 내용

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageStatus status = MessageStatus.PENDING; // 처리 상태 (대기/완료/무시)

    @CreatedDate
    private LocalDateTime createdAt; // 발생 시간

    // 처리 완료 상태로 변경하는 편의 메서드
    public void resolve() {
        this.status = MessageStatus.RESOLVED;
    }
}

