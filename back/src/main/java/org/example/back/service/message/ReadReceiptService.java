package org.example.back.service.message;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.message.ChatMessage;
import org.example.back.domain.room.ChatParticipant;
import org.example.back.dto.websocket.request.ReadMessageRequest;
import org.example.back.dto.websocket.response.ReadReceiptResponse;
import org.example.back.repository.message.ChatMessageRepository;
import org.example.back.repository.participant.ChatParticipantQueryRepository;
import org.example.back.repository.participant.ChatParticipantRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadReceiptService {
    
    private final ChatParticipantQueryRepository chatParticipantQueryRepository;
    private final SimpMessagingTemplate simpMessagingTemplate; // WebSocket 전송용
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    /**
     * 메시지 읽음 처리 수행.
     * - 메시지 유효성 확인
     * - 채팅방 일치 여부 확인
     * - 참여자 여부 확인
     * - 읽음 상태 업데이트 및 읽음 알림 브로드캐스트
     */
    @Transactional
    public void updateLastReadMessageId(ReadMessageRequest request) {
        log.debug("[ReadReceipt] 시작 - 채팅방 ID={}, 사용자 ID={}, 메시지 ID={}",
                request.getChatRoomId(), request.getMemberId(), request.getMessageId());
        
        // 메시지 존재 및 채팅방 매핑 확인
        ChatMessage message = chatMessageRepository.findById(request.getMessageId())
                .orElseThrow(() -> {
                    log.warn("읽음 처리 실패 - 존재하지 않는 메시지 ID -MessageId: {}", request.getMessageId());
                    return new IllegalArgumentException("존재하지 않는 메시지 ID 입니다.");
                });
        
        if (!message.getChatRoom().getId().equals(request.getChatRoomId())) {
            log.warn("읽음 처리 실패 - 메시지의 채팅방 ID 불일치. 요청={}, 실제={}",
                    request.getChatRoomId(), message.getChatRoom().getId());
            throw new IllegalArgumentException("해당 메시지가 요청한 채팅방에 속하지 않습니다.");
        }
        
        // 채팅방 참여자 여부 확인
        boolean isParticipant = chatParticipantRepository.findByChatRoomIdAndMemberId(
                request.getChatRoomId(), request.getMemberId())
                .isPresent();
        
        if (!isParticipant) {
            log.warn("읽음 처리 실패 - 사용자가 해당 채팅방의 참여자가 아님 - chatRoomId={}, MemberId={}",
                    request.getChatRoomId(), request.getMemberId());
            throw new IllegalArgumentException("사용자가 해당 채팅방 참여자가 아닙니다.");
        }
        
        long updated = chatParticipantQueryRepository.updateLastReadMessageId(request);
        
        if (updated > 0) {
            // 다른 참여자에게 읽음 알림 전송
            ReadReceiptResponse response = ReadReceiptResponse.builder()
                    .chatRoomId(request.getChatRoomId())
                    .memberId(request.getMemberId())
                    .messageId(request.getMessageId())
                    .build();
            
            simpMessagingTemplate.convertAndSend(
                    "/sub/chat/room/" + request.getChatRoomId() + "/read", response);
            log.debug("[ReadReceipt] 읽음 처리 성공 - 브로드캐스트 완료");
        } else {
            log.debug("[ReadReceipt] 읽음 처리 실패 - 업데이트 수 없음");
        }
    }
    
    /**
     * 채팅방 참여자들의 읽음 상태 목록 조회.
     */
    public List<ReadReceiptResponse> getReadStatuses(Long chatRoomId) {
        log.debug("[ReadReceiptService] 채팅방 ID={} 읽음 상태 조회 시작", chatRoomId);
        
        List<ReadReceiptResponse> result = chatParticipantRepository.findByChatRoomId(chatRoomId)
                .stream()
                .map(p -> ReadReceiptResponse.builder()
                        .chatRoomId(chatRoomId)
                        .memberId(p.getMember().getId())
                        .messageId(p.getLastReadMessageId())
                        .build())
                .toList();
        
        log.debug("[ReadReceiptService] 조회 완료 - 응답 수: {}", result.size());
        return result;
    }
    
    /**
     * 사용자의 채팅방 내 안읽은 메시지 수를 반환.
     */
    public int getUnreadMessageCount(Long chatRoomId, Long memberId) {
        log.debug("[ReadReceipt] 안읽은 메시지 수 조회 시작 - chatRoomId={}, memberId={}", chatRoomId, memberId);
        
        // 사용자의 마지막 읽은 메시지 ID 조회
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> {
                    log.warn("채팅방 참여자 조회 실패 - chatRoomId={}, memberId={}", chatRoomId, memberId);
                    return new IllegalArgumentException("해당 채팅방에 참여 중인 사용자가 아닙니다.");
                });
        
        Long lastReadMessageId = participant.getLastReadMessageId();
        
        if (lastReadMessageId == null) {
            lastReadMessageId = 0L;
        }
        
        // 해당 채팅방에서 더 높은 메시지 수 조회
        int count = chatMessageRepository.countByChatRoomIdAndIdGreaterThan(chatRoomId, lastReadMessageId);
        log.debug("[ReadReceipt] 안읽은 메시지 수: {}", count);
        return count;
    }
}
