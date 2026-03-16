package org.example.back.service.room;

import static org.example.back.exception.chatroom.ChatRoomErrorCode.*;
import static org.example.back.exception.member.MemberErrorCode.USER_NOT_FOUND;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.member.Member;
import org.example.back.domain.room.ChatParticipant;
import org.example.back.domain.room.ChatRoom;
import org.example.back.domain.room.ChatRoomType;
import org.example.back.dto.room.request.CreateGroupChatRoomRequest;
import org.example.back.dto.room.request.CreatePrivateChatRoomRequest;
import org.example.back.dto.room.request.InviteChatRoomRequest;
import org.example.back.dto.room.request.LeaveChatRoomRequest;
import org.example.back.dto.room.response.ChatRoomResponse;
import org.example.back.exception.chatroom.ChatRoomException;
import org.example.back.exception.member.MemberException;
import org.example.back.repository.participant.ChatParticipantRepository;
import org.example.back.repository.room.ChatRoomQueryRepository;
import org.example.back.repository.room.ChatRoomRepository;
import org.example.back.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatRoomQueryRepository chatRoomQueryRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public ChatRoomResponse createPrivateChatRoom(CreatePrivateChatRoomRequest request) {

        Member target = findMemberById(request.getTargetMemberId());
        Member requester = findMemberById(request.getMemberId());

        return chatRoomQueryRepository.findPrivateChatRoom(request.getMemberId(),
                        request.getTargetMemberId())
                .map(chatRoom -> {
                    List<Long> participantIds = getParticipantIds(chatRoom);
                    return ChatRoomResponse.from(chatRoom, participantIds);
                })
                .orElseGet(() -> {
                    ChatRoom chatRoom = ChatRoom.createPrivateRoom();
                    chatRoomRepository.save(chatRoom);

                    ChatParticipant requesterParticipant = ChatParticipant.create(chatRoom, requester);
                    ChatParticipant targetParticipant = ChatParticipant.create(chatRoom, target);
                    chatParticipantRepository.saveAll(List.of(requesterParticipant, targetParticipant));

                    List<Long> participantIds = Stream.of(requester.getId(), target.getId())
                            .sorted()
                            .toList();

                    return ChatRoomResponse.from(chatRoom, participantIds);
                });
    }

    @Override
    @Transactional
    public ChatRoomResponse createGroupChatRoom(Long requestedId, CreateGroupChatRoomRequest request) {

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ChatRoomException(ROOM_NAME_REQUIRED);
        }

        Set<Long> allMemberIds = new LinkedHashSet<>(request.getMemberIds());
        allMemberIds.add(requestedId);

        List<Member> members = memberRepository.findAllById(new ArrayList<>(allMemberIds));

        if (members.size() != allMemberIds.size()) {
            throw new ChatRoomException(INVALID_MEMBER_IDS);
        }

        ChatRoom chatRoom = ChatRoom.createGroupRoom(request.getName());
        chatRoomRepository.save(chatRoom);

        List<ChatParticipant> participants = members.stream()
                .map(member -> ChatParticipant.create(chatRoom, member))
                .toList();
        chatParticipantRepository.saveAll(participants);

        List<Long> participantIds = members.stream()
                .map(member -> member.getId())
                .sorted()
                .toList();

        return ChatRoomResponse.from(chatRoom, participantIds);
    }

    @Override
    public Page<ChatRoomResponse> getMyChatRooms(Long memberId, Pageable pageable) {

        pageable = (pageable != null) ? pageable : PageRequest.of(0, 20);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(USER_NOT_FOUND));

        Page<ChatRoom> chatRooms = chatRoomQueryRepository.findMyChatRooms(member.getId(), pageable);

        List<ChatRoomResponse> responses = chatRooms.getContent().stream()
                .map(chatRoom -> {
                    List<Long> participantIds = getParticipantIds(chatRoom);
                    return ChatRoomResponse.from(chatRoom, participantIds);
                }).toList();

        return new PageImpl<>(responses, pageable, chatRooms.getTotalElements());
    }

    @Override
    @Transactional
    public void inviteMembers(Long chatRoomId, Long memberId, InviteChatRoomRequest request) {

        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        chatParticipantRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new ChatRoomException(NOT_PARTICIPANT));

        if (chatRoom.getType().equals(ChatRoomType.PRIVATE)) {
            throw new ChatRoomException(PRIVATE_ROOM_INVITE_NOT_ALLOWED);
        }

        List<Long> existingMemberIds = getParticipantIds(chatRoom);

        List<Member> newMembers = memberRepository.findAllById(request.getMemberIds())
                .stream()
                .filter(member -> !existingMemberIds.contains(member.getId()))
                .toList();

        List<ChatParticipant> newParticipants = newMembers.stream()
                .map(member -> ChatParticipant.create(chatRoom, member))
                .toList();

        if (!newParticipants.isEmpty()) {
            chatParticipantRepository.saveAll(newParticipants);
        }
    }

    @Override
    @Transactional
    public void leaveChatRoom(Long chatRoomId, LeaveChatRoomRequest request) {

        chatParticipantRepository.deleteByMemberIdAndChatRoomId(request.getMemberId(), chatRoomId);

        List<ChatParticipant> remainingParticipants = chatParticipantRepository.findByChatRoomId(chatRoomId);

        if (remainingParticipants.isEmpty()) {
            ChatRoom chatRoom = findChatRoomById(chatRoomId);

            if (!chatRoom.getIsDeleted()) {
                chatRoom.markAsDeleted();
            }
        }
    }

    @Override
    @Transactional
    public void softDeleteChatRoom(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        if (chatRoom.getIsDeleted()) {
            throw new ChatRoomException(ALREADY_DELETED_ROOM);
        }

        boolean isParticipant = chatParticipantRepository.findByChatRoomId(chatRoomId).stream()
                .anyMatch(cp -> cp.getMember().getId().equals(memberId));

        if (!isParticipant) {
            throw new ChatRoomException(NOT_PARTICIPANT);
        }

        chatRoom.markAsDeleted();
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(USER_NOT_FOUND));
    }

    private ChatRoom findChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException(ROOM_NOT_FOUND));
    }

    private List<Long> getParticipantIds(ChatRoom chatRoom) {

        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomId(chatRoom.getId());

        if (participants.isEmpty()) {
            throw new ChatRoomException(NO_PARTICIPANTS);
        }

        return participants.stream()
                .map(cp -> cp.getMember().getId())
                .sorted()
                .toList();
    }
}
