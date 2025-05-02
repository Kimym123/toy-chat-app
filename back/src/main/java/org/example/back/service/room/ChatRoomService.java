package org.example.back.service.room;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.back.dto.room.request.CreateGroupChatRoomRequest;
import org.example.back.dto.room.request.CreatePrivateChatRoomRequest;
import org.example.back.dto.room.request.InviteChatRoomRequest;
import org.example.back.dto.room.request.LeaveChatRoomRequest;
import org.example.back.dto.room.response.ChatRoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 채팅방 관련 서비스 인터페이스 주요 기능:
 * 1. 1:1 채팅방 생성
 * 2. 그룹 채팅방 생성
 * 3. 채팅방 목록 조회 (페이징)
 * 4. 채팅방 초대
 * 5. 채팅방 나가기
 * 6. 채팅방 소프트 삭제
 */
@Tag(name = "채팅방 서비스", description = "ChatRoom Service API")
public interface ChatRoomService {
    
    @Operation(summary = "1:1 채팅방 생성", description = "요청자 ID와 상대방 ID를 기반으로 1:1 채팅방을 생성합니다. 이미 존재하면 기존 방을 반환합니다.")
    ChatRoomResponse createPrivateChatRoom(CreatePrivateChatRoomRequest request);
    
    @Operation(summary = "그룹 채팅방 생성", description = "요청자 ID와 방 이름 및 참여할 회원 ID 목록을 기반으로 그룹 채팅방을 생성합니다.")
    ChatRoomResponse createGroupChatRoom(Long requestedId, CreateGroupChatRoomRequest request);
    
    @Operation(summary = "내 채팅방 목록 조회", description = "요청자의 참여 중인 채팅방 목록을 페이징하여 조회합니다.")
    Page<ChatRoomResponse> getMyChatRooms(Long memberId, Pageable pageable);
    
    @Operation(summary = "채팅방에 멤버 초대", description = "기존 그룹 채팅방에 새로운 멤버들을 초대합니다. (PRIVATE 방 초대 불가)")
    void inviteMembers(Long chatRoomId, InviteChatRoomRequest request);
    
    @Operation(summary = "채팅방 나가기", description = "요청자가 특정 채팅방에서 나갑니다. (Soft Delete 처리)")
    void leaveChatRoom(Long chatRoomId, LeaveChatRoomRequest request);
    
    @Operation(summary = "채팅방 소프트 삭제", description = "방장이 채팅방을 소프트 삭제 처리합니다. (isDeleted 플래그 변경)")
    void softDeleteChatRoom(Long chatRoomId, Long memberId);
}
