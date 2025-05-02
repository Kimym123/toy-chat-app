package org.example.back.service.room;

import static org.example.back.exception.member.MemberErrorCode.USER_NOT_FOUND;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
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
import org.example.back.exception.member.MemberException;
import org.example.back.repository.ChatParticipantRepository;
import org.example.back.repository.ChatRoomQueryRepository;
import org.example.back.repository.ChatRoomRepository;
import org.example.back.repository.MemberRepository;
import org.springframework.data.domain.Page;
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
    private final MemberRepository memberRepository; // 상대방 회원 조회용
    
    @Override
    @Transactional
    public ChatRoomResponse createPrivateChatRoom(CreatePrivateChatRoomRequest request) {
        
        // 상대방 Member 조회
        Member target = findMemberById(request.getTargetMemberId());
        
        // 본인 Member 조회
        Member requester = findMemberById(request.getMemberId());
        
        // 기존 1:1 채팅방 찾기 (QueryDSL)
        return chatRoomQueryRepository.findPrivateChatRoom(request.getMemberId(),
                        request.getTargetMemberId())
                .map(chatRoom -> {
                    // 기존 방 찾기 -> 응답
                    List<Long> participantIds = getParticipantIds(chatRoom.getId());
                    
                    return ChatRoomResponse.from(chatRoom, participantIds);
                })
                .orElseGet(() -> {
                    // 새 방 생성
                    ChatRoom chatRoom = ChatRoom.createPrivateRoom();
                    chatRoomRepository.save(chatRoom);
                    
                    // 참여자 등록
                    ChatParticipant requesterParticipant = ChatParticipant.create(chatRoom,
                            requester);
                    ChatParticipant targetParticipant = ChatParticipant.create(chatRoom, target);
                    chatParticipantRepository.saveAll(
                            List.of(requesterParticipant, targetParticipant));
                    
                    List<Long> participantIds = Stream.of(requester.getId(), target.getId())
                            .sorted()
                            .toList();
                    
                    return ChatRoomResponse.from(chatRoom, participantIds);
                });
    }
    
    @Override
    @Transactional
    public ChatRoomResponse createGroupChatRoom(Long requestedId,
            CreateGroupChatRoomRequest request) {
        
        Member requester = findMemberById(requestedId);
        
        // 요청자 ID + 기타 멤버 ID 리스트 구성
        List<Long> allMemberIds = new ArrayList<>(request.getMemberIds());
        if (!allMemberIds.contains(requestedId)) {
            allMemberIds.add(requestedId);
        }
        
        // 전체 멤버 조회
        List<Member> members = memberRepository.findAllById(allMemberIds);
        if (members.size() != allMemberIds.size()) {
            throw new IllegalArgumentException("일부 회원 ID 가 유효하지 않습니다.");
        }
        
        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.createGroupRoom(request.getName());
        chatRoomRepository.save(chatRoom);
        
        // 참여자 등록
        List<ChatParticipant> participants = members.stream()
                .map(member -> ChatParticipant.create(chatRoom, member))
                .toList();
        chatParticipantRepository.saveAll(participants);
        
        // 응답 반환
        List<Long> participantIds = members.stream()
                .map(member -> member.getId())
                .sorted()
                .toList();
        
        return ChatRoomResponse.from(chatRoom, participantIds);
    }
    
    @Override
    public Page<ChatRoomResponse> getMyChatRooms(Long memberId, Pageable pageable) {
        
        // 채팅방 목록 조회 (QueryDSL)
        Page<ChatRoom> chatRooms = chatRoomQueryRepository.findMyChatRooms(memberId, pageable);
        
        return chatRooms.map(chatRoom -> {
            List<Long> participantIds = getParticipantIds(chatRoom.getId());
            
            return ChatRoomResponse.from(chatRoom, participantIds);
        });
    }
    
    @Override
    public void inviteMembers(Long chatRoomId, InviteChatRoomRequest request) {
        
        // 채팅방 조회
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        
        if (chatRoom.getType().equals(ChatRoomType.PRIVATE)) {
            throw new IllegalArgumentException("1:1 채팅방에는 초대할 수 없습니다.");
        }
        
        // 기존 참여자 ID 목록
        List<Long> existingMemberIds = getParticipantIds(chatRoom.getId());
        
        // 요청된 멤버 중 기존 참여자 제외한 목록
        List<Member> newMembers = memberRepository.findAllById(request.getMemberIds())
                .stream()
                .filter(member -> !existingMemberIds.contains(member.getId()))
                .toList();
        
        // 새로운 참여자 목록
        List<ChatParticipant> newParticipants = newMembers.stream()
                .map(member -> ChatParticipant.create(chatRoom, member))
                .toList();
        
        chatParticipantRepository.saveAll(newParticipants);
    }
    
    @Override
    public void leaveChatRoom(Long chatRoomId, LeaveChatRoomRequest request) {
        
        // 요청자가 해당 방에서 나감
        chatParticipantRepository.deleteByMemberIdAndChatRoomId(request.getMemberId(), chatRoomId);
        
        // 남은 참여자 목록 조회
        List<ChatParticipant> remainingParticipants = chatParticipantRepository.findByChatRoomId(
                chatRoomId);
        
        // 참여자가 아무도 없으면 채팅방 삭제 (소프트 삭제)
        if (remainingParticipants.isEmpty()) {
            ChatRoom chatRoom = findChatRoomById(chatRoomId);
            
            // 이미 삭제된 방 확인
            if (!chatRoom.getIsDeleted()) {
                chatRoom.markAsDeleted();
            }
        }
    }
    
    @Override
    public void softDeleteChatRoom(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        
        boolean isParticipant = chatParticipantRepository.findByChatRoomId(chatRoomId).stream()
                .anyMatch(cp -> cp.getMember().getId().equals(memberId));
        
        if (!isParticipant) {
            throw new IllegalArgumentException("해당 사용자는 채팅방의 참여자가 아닙니다.");
        }
        
        chatRoom.markAsDeleted();
    }
    
    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(USER_NOT_FOUND));
    }
    
    private ChatRoom findChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(
                        () -> new EntityNotFoundException("ChatRoom not found: " + chatRoomId));
    }
    
    private List<Long> getParticipantIds(Long chatRoomId) {
        return chatParticipantRepository.findByChatRoomId(chatRoomId).stream()
                .map(cp -> cp.getMember().getId())
                .sorted()
                .toList();
    }
}
