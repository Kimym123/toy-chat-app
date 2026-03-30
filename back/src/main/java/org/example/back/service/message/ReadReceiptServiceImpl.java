package org.example.back.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.message.ChatMessage;
import org.example.back.domain.message.TypingStatus;
import org.example.back.domain.room.ChatParticipant;
import org.example.back.dto.websocket.request.ReadMessageRequest;
import org.example.back.dto.websocket.response.ReadReceiptResponse;
import org.example.back.exception.chatroom.ChatRoomErrorCode;
import org.example.back.exception.chatroom.ChatRoomException;
import org.example.back.exception.message.ChatMessageErrorCode;
import org.example.back.exception.message.ChatMessageException;
import org.example.back.repository.message.ChatMessageRepository;
import org.example.back.repository.participant.ChatParticipantQueryRepository;
import org.example.back.repository.participant.ChatParticipantRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadReceiptServiceImpl implements ReadReceiptService {
    
    private final ChatParticipantQueryRepository chatParticipantQueryRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    @Override
    @Transactional
    public void updateLastReadMessageId(ReadMessageRequest request) {
        log.debug("[ReadReceipt] 시작 - 채팅방 ID={}, 사용자 ID={}, 메시지 ID={}",
                request.getChatRoomId(), request.getMemberId(), request.getMessageId());
        
        ChatMessage message = chatMessageRepository.findById(request.getMessageId())
                .orElseThrow(() -> {
                    log.warn("읽음 처리 실패 - 존재하지 않는 메시지 ID -MessageId: {}", request.getMessageId());
                    return new ChatMessageException(ChatMessageErrorCode.MESSAGE_NOT_FOUND);
                });
        
        if (!message.getChatRoom().getId().equals(request.getChatRoomId())) {
            log.warn("읽음 처리 실패 - 메시지의 채팅방 ID 불일치. 요청={}, 실제={}",
                    request.getChatRoomId(), message.getChatRoom().getId());
            throw new ChatMessageException(ChatMessageErrorCode.MESSAGE_NOT_IN_ROOM);
        }
        
        boolean isParticipant = chatParticipantRepository.findByChatRoomIdAndMemberId(
                        request.getChatRoomId(), request.getMemberId())
                .isPresent();
        
        if (!isParticipant) {
            log.warn("읽음 처리 실패 - 사용자가 해당 채팅방의 참여자가 아님 - chatRoomId={}, MemberId={}",
                    request.getChatRoomId(), request.getMemberId());
            throw new ChatRoomException(ChatRoomErrorCode.NOT_PARTICIPANT);
        }
        
        long updated = chatParticipantQueryRepository.updateLastReadMessageId(request);
        
        if (updated > 0) {
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
    
    @Override
    public List<ReadReceiptResponse> getReadStatuses(Long chatRoomId, Long memberId) {
        log.debug("[ReadReceiptService] 채팅방 ID={} 읽음 상태 조회 시작 - 요청자 ID={}", chatRoomId, memberId);
        
        // 참여자 검증
        boolean isParticipant = chatParticipantRepository.findByChatRoomIdAndMemberId(
                        chatRoomId, memberId)
                .isPresent();
        
        if (!isParticipant) {
            log.warn("읽음 상태 조회 실패 - 채팅방 참여자가 아님 - chatRoomId={}, memberId={}", chatRoomId, memberId);
            throw new ChatRoomException(ChatRoomErrorCode.NOT_PARTICIPANT);
        }
        
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
    
    @Override
    public int getUnreadMessageCount(Long chatRoomId, Long memberId) {
        log.debug("[ReadReceipt] 안읽은 메시지 수 조회 시작 - chatRoomId={}, memberId={}", chatRoomId,
                memberId);
        
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndMemberId(
                        chatRoomId, memberId)
                .orElseThrow(() -> {
                    log.warn("채팅방 참여자 조회 실패 - chatRoomId={}, memberId={}", chatRoomId, memberId);
                    return new ChatRoomException(ChatRoomErrorCode.NOT_PARTICIPANT);
                });
        
        Long lastReadMessageId = participant.getLastReadMessageId();
        
        if (lastReadMessageId == null) {
            lastReadMessageId = 0L;
        }
        
        int count = chatMessageRepository.countByChatRoomIdAndIdGreaterThan(chatRoomId,
                lastReadMessageId);
        log.debug("[ReadReceipt] 안읽은 메시지 수: {}", count);
        return count;
    }
    
    @Override
    public void broadcastTypingStatus(Long chatRoomId, Long memberId, TypingStatus status) {
        log.debug("[TypingStatus] 브로드캐스트 시작 - chatRoomId={}, memberId={}, status={}",
                chatRoomId, memberId, status.getValue());
        
        String nickname = chatParticipantRepository.findByChatRoomIdAndMemberId(
                        chatRoomId, memberId)
                .map(p -> p.getMember().getNickname())
                .orElseThrow(() -> {
                    log.warn("타이핑 상태 전송 실패 - 채팅방 참여자 없음. chatRoomId={}, memberId={}",
                            chatRoomId, memberId);
                    return new ChatRoomException(ChatRoomErrorCode.NOT_PARTICIPANT);
                });
        
        Map<String, Object> payload = Map.of(
                "chatRoomId", chatRoomId,
                "memberId", memberId,
                "nickname", nickname,
                "status", status.getValue()
        );
        
        final String destination = String.format("/sub/chat/room/%d/typing", chatRoomId);
        simpMessagingTemplate.convertAndSend(destination, payload);
    }
}
