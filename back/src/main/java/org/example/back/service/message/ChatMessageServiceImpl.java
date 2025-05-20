package org.example.back.service.message;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.member.Member;
import org.example.back.domain.message.ChatMessage;
import org.example.back.domain.message.MessageType;
import org.example.back.domain.room.ChatRoom;
import org.example.back.dto.message.request.ChatMessageRequest;
import org.example.back.dto.message.response.ChatMessageResponse;
import org.example.back.repository.participant.ChatParticipantRepository;
import org.example.back.repository.MemberRepository;
import org.example.back.repository.message.ChatMessageQueryRepository;
import org.example.back.repository.message.ChatMessageRepository;
import org.example.back.repository.room.ChatRoomQueryRepository;
import org.example.back.repository.room.ChatRoomRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageServiceImpl implements ChatMessageService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomQueryRepository chatRoomQueryRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageQueryRepository chatMessageQueryRepository;
    
    @Override
    @Transactional
    public ChatMessage saveMessage(ChatMessageRequest request) {
        
        log.debug("메시지 저장 요청: {}", request);
        
        ChatRoom room = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 채팅방 요청 - chatRoomId: {}", request.getChatRoomId());
                    return new IllegalArgumentException("채팅방이 존재하지 않습니다.");
                });
        
        if (room.getIsDeleted()) {
            log.warn("삭제된 채팅방으로 메시지 전송 시도 - chatRoomId: {}", request.getChatRoomId());
            throw new IllegalArgumentException("삭제된 채팅방입니다.");
        }
        
        Member sender = memberRepository.findById(request.getSenderId())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자 요청 - senderId: {}", request.getSenderId());
                    return new IllegalArgumentException("전송자가 존재하지 않습니다.");
                });
        
        if (chatParticipantRepository.findByChatRoomIdAndMemberId(room.getId(), sender.getId())
                .isEmpty()) {
            log.warn("메시지 전송 요청자는 참여자가 아님 - senderId: {}, chatRoomId: {}", sender.getId(),
                    room.getId());
            throw new IllegalArgumentException("채팅방 참여자가 아닙니다.");
        }
        
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getType())
                .build();
        
        ChatMessage saved = chatMessageRepository.save(message);
        log.info("메시지 저장 완료 - id: {}, roomId: {}", saved.getId(), saved.getChatRoom().getId());
        return saved;
    }
    
    @Override
    @Transactional
    public ChatMessage saveTextMessage(ChatMessageRequest request) {
        log.debug("텍스트 메시지 저장 요청: {}", request);
        
        if (request.getContent() == null || request.getContent().isBlank()) {
            log.warn("TEXT 메시지 내용이 비어 있음");
            throw new IllegalArgumentException("텍스트 메시지에는 content가 필요합니다.");
        }
        
        return saveMessage(request);
    }
    
    @Override
    @Transactional
    public ChatMessage saveFileMessage(ChatMessageRequest request) {
        log.debug("파일 메시지 저장 요청: {}", request);
        
        if (request.getFileUrl() == null || request.getFileUrl().isBlank()) {
            log.warn("fileUrl 누락 - FILE/IMAGE 메시지");
            throw new IllegalArgumentException("파일 메시지에는 fileUrl이 필요합니다.");
        }
        
        // fileUrl을 content 필드에 매핑
        request.setContent(request.getFileUrl());
        
        return saveMessage(request); // 기존 saveMessage 재활용
    }
    
    @Override
    public List<ChatMessageResponse> getMessages(Long chatRoomId, Pageable pageable) {
        log.debug("메시지 리스트 조회 요청 - roomId: {}, page: {}", chatRoomId, pageable.getPageNumber());
        List<ChatMessage> messages = chatMessageQueryRepository.findMessagesByChatRoomId(chatRoomId,
                pageable);
        
        List<ChatMessageResponse> responseList = new ArrayList<>();
        
        for (ChatMessage message : messages) {
            responseList.add(ChatMessageResponse.from(message));
        }
        
        return responseList;
    }
    
    @Override
    public List<ChatMessageResponse> getRecentMessages(Long chatRoomId, int limit) {
        log.debug("최근 메시지 {} 개 조회 요청 - roomId: {}", limit, chatRoomId);
        List<ChatMessage> messages = chatMessageQueryRepository.findRecentMessagesByChatRoomId(
                chatRoomId, limit);
        
        List<ChatMessageResponse> responseList = new ArrayList<>();
        
        for (ChatMessage message : messages) {
            responseList.add(ChatMessageResponse.from(message));
        }
        
        return responseList;
    }
    
    @Override
    public ChatMessage sendSystemMessage(Long chatRoomId, String content) {
        log.debug("시스템 메시지 전송 요청 - roomId: {}, content: {}", chatRoomId, content);
        
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 채팅방 요청 - chatRoomId: {}", chatRoomId);
                    return new IllegalArgumentException("채팅방이 존재하지 않습니다.");
                });
        
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(null)
                .content(content)
                .messageType(MessageType.SYSTEM)
                .build();
        
        chatMessageRepository.save(message);
        
        return message;
    }
}
