package org.example.back.service.message;

import static org.example.back.exception.chatroom.ChatRoomErrorCode.*;
import static org.example.back.exception.message.ChatMessageErrorCode.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.member.Member;
import org.example.back.domain.message.ChatMessage;
import org.example.back.domain.message.MessageType;
import org.example.back.domain.room.ChatRoom;
import org.example.back.dto.message.event.ChatMessageEvent;
import org.example.back.dto.message.request.ChatMessageEditRequest;
import org.example.back.dto.message.request.ChatMessageRequest;
import org.example.back.dto.message.response.ChatMessageResponse;
import org.example.back.exception.chatroom.ChatRoomException;
import org.example.back.exception.member.MemberErrorCode;
import org.example.back.exception.member.MemberException;
import org.example.back.exception.message.ChatMessageException;
import org.example.back.repository.MemberRepository;
import org.example.back.repository.message.ChatMessageQueryRepository;
import org.example.back.repository.message.ChatMessageRepository;
import org.example.back.repository.participant.ChatParticipantRepository;
import org.example.back.repository.room.ChatRoomQueryRepository;
import org.example.back.repository.room.ChatRoomRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    @Transactional
    public ChatMessage saveMessage(ChatMessageRequest request) {

        log.debug("메시지 저장 요청: {}", request);

        if (request.getClientMessageId() == null || request.getClientMessageId().isBlank()) {
            log.warn("clientMessageId 누락됨 - 메시지 무시됨");
            throw new ChatMessageException(CLIENT_MESSAGE_ID_REQUIRED);
        }

        Optional<ChatMessage> existing = chatMessageRepository.findByClientMessageId(
                request.getClientMessageId());
        if (existing.isPresent()) {
            log.warn("중복 메시지 요청 - clientMessageId: {}", request.getClientMessageId());
            return existing.get();
        }

        ChatRoom room = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 채팅방 요청 - chatRoomId: {}", request.getChatRoomId());
                    return new ChatRoomException(ROOM_NOT_FOUND);
                });

        if (room.getIsDeleted()) {
            log.warn("삭제된 채팅방으로 메시지 전송 시도 - chatRoomId: {}", request.getChatRoomId());
            throw new ChatRoomException(DELETED_ROOM);
        }

        Member sender = memberRepository.findById(request.getSenderId())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자 요청 - senderId: {}", request.getSenderId());
                    return new MemberException(MemberErrorCode.USER_NOT_FOUND);
                });

        if (chatParticipantRepository.findByChatRoomIdAndMemberId(
                room.getId(), sender.getId()).isEmpty()) {
            log.warn("메시지 전송 요청자는 참여자가 아님 - senderId: {}, chatRoomId: {}",
                    sender.getId(), room.getId());
            throw new ChatRoomException(NOT_PARTICIPANT);
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getType())
                .clientMessageId(request.getClientMessageId())
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
            throw new ChatMessageException(CONTENT_REQUIRED);
        }

        return saveMessage(request);
    }

    @Override
    @Transactional
    public ChatMessage saveFileMessage(ChatMessageRequest request) {
        log.debug("파일 메시지 저장 요청: {}", request);

        if (request.getFileUrl() == null || request.getFileUrl().isBlank()) {
            log.warn("fileUrl 누락 - FILE/IMAGE 메시지");
            throw new ChatMessageException(FILE_URL_REQUIRED);
        }

        request.setContent(request.getFileUrl());

        return saveMessage(request);
    }

    @Override
    public List<ChatMessageResponse> getMessages(Long chatRoomId, Long memberId, Pageable pageable) {
        log.debug("메시지 리스트 조회 요청 - roomId: {}, memberId: {}, page: {}", chatRoomId, memberId, pageable.getPageNumber());

        if (chatParticipantRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId).isEmpty()) {
            log.warn("메시지 조회 실패 - 채팅방 참여자가 아님 - chatRoomId: {}, memberId: {}", chatRoomId, memberId);
            throw new ChatRoomException(NOT_PARTICIPANT);
        }

        List<ChatMessage> messages = chatMessageQueryRepository.findMessagesByChatRoomId(chatRoomId, pageable);

        List<ChatMessageResponse> responseList = new ArrayList<>();

        for (ChatMessage message : messages) {
            responseList.add(ChatMessageResponse.from(message));
        }

        return responseList;
    }

    @Override
    public List<ChatMessageResponse> getRecentMessages(Long chatRoomId, int limit) {
        log.debug("최근 메시지 {} 개 조회 요청 - roomId: {}", limit, chatRoomId);
        List<ChatMessage> messages = chatMessageQueryRepository.findRecentMessagesByChatRoomId(chatRoomId, limit);

        List<ChatMessageResponse> responseList = new ArrayList<>();

        for (ChatMessage message : messages) {
            responseList.add(ChatMessageResponse.from(message));
        }

        return responseList;
    }

    @Override
    @Transactional
    public void sendSystemMessageAndBroadcast(Long chatRoomId, String content) {
        log.debug("시스템 메시지 전송 요청 - chatRoomId: {}, content: {}", chatRoomId, content);

        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 채팅방 요청 - chatRoomId: {}", chatRoomId);
                    return new ChatRoomException(ROOM_NOT_FOUND);
                });

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(null)
                .content(content)
                .messageType(MessageType.SYSTEM)
                .clientMessageId("SYSTEM-" + UUID.randomUUID())
                .build();

        chatMessageRepository.save(message);

        ChatMessageResponse response = ChatMessageResponse.from(message);
        simpMessagingTemplate.convertAndSend("/sub/chat/room/" + chatRoomId, response);
    }

    @Override
    @Transactional
    public ChatMessageResponse editMessage(Long memberId, Long messageId, ChatMessageEditRequest request) {

        ChatMessage message = findMessageById(messageId);

        if (message.isDeleted()) {
            throw new ChatMessageException(ALREADY_DELETED_MESSAGE);
        }

        if (!message.getSender().getId().equals(memberId)) {
            throw new ChatMessageException(NOT_MESSAGE_OWNER);
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        if (message.getCreatedAt().isBefore(fiveMinutesAgo)) {
            throw new ChatMessageException(EDIT_TIME_EXPIRED);
        }

        log.info("메시지 수정 요청 - memberId: {}, messageId: {}", memberId, message.getId());

        message.updateContent(request.getNewContent());

        ChatMessageEvent editEvent = ChatMessageEvent.edited(
                messageId,
                message.getChatRoom().getId(),
                memberId,
                message.getSender().getNickname(),
                request.getNewContent()
        );

        simpMessagingTemplate.convertAndSend(
                "/sub/chat/room/" + message.getChatRoom().getId() + "/edit",
                editEvent
        );

        log.info("메시지 수정 완료 및 실시간 알림 전송 - messageId: {}, chatRoomId: {}",
                messageId, message.getChatRoom().getId());

        return ChatMessageResponse.from(message);
    }

    @Override
    @Transactional
    public void deleteMessage(Long memberId, Long messageId) {
        ChatMessage message = findMessageById(messageId);

        if (message.getSender() == null) {
            throw new ChatMessageException(SYSTEM_MESSAGE_NOT_DELETABLE);
        }

        if (!message.getSender().getId().equals(memberId)) {
            throw new ChatMessageException(NOT_MESSAGE_OWNER);
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        if (message.getCreatedAt().isBefore(fiveMinutesAgo)) {
            throw new ChatMessageException(DELETE_TIME_EXPIRED);
        }

        log.info("메시지 삭제 요청 - memberId: {}, messageId: {}", memberId, messageId);

        message.softDelete();

        ChatMessageEvent deleteEvent = ChatMessageEvent.deleted(
                messageId,
                message.getChatRoom().getId(),
                memberId,
                message.getSender().getNickname()
        );

        simpMessagingTemplate.convertAndSend(
                "/sub/chat/room/" + message.getChatRoom().getId() + "/delete",
                deleteEvent
        );

        log.info("메시지 삭제 완료 및 실시간 알림 전송 - messageId: {}, chatRoomId: {}",
                messageId, message.getChatRoom().getId());
    }

    @Override
    @Transactional
    public void restoreMessage(Long memberId, Long messageId) {
        ChatMessage message = findMessageById(messageId);

        if (message.getSender() == null) {
            throw new ChatMessageException(SYSTEM_MESSAGE_NOT_RESTORABLE);
        }

        if (!message.getSender().getId().equals(memberId)) {
            throw new ChatMessageException(NOT_MESSAGE_OWNER);
        }

        if (!message.isDeleted()) {
            throw new ChatMessageException(NOT_DELETED_MESSAGE);
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        if (message.getDeletedAt().isBefore(fiveMinutesAgo)) {
            throw new ChatMessageException(RESTORE_TIME_EXPIRED);
        }

        log.info("메시지 복구 요청 - memberId: {}, messageId: {}", memberId, messageId);

        message.restore();

        ChatMessageEvent restoreEvent = ChatMessageEvent.restored(
                messageId,
                message.getChatRoom().getId(),
                memberId,
                message.getSender().getNickname()
        );

        simpMessagingTemplate.convertAndSend(
                "/sub/chat/room/" + message.getChatRoom().getId() + "/restore",
                restoreEvent
        );

        log.info("메시지 복구 완료 및 실시간 알림 전송 - messageId: {}, chatRoomId: {}",
                messageId, message.getChatRoom().getId());
    }

    private ChatMessage findMessageById(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ChatMessageException(MESSAGE_NOT_FOUND));
    }
}
