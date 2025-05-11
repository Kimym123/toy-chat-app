
package org.example.back.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.back.exception.member.MemberErrorCode.USER_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.example.back.domain.member.Member;
import org.example.back.domain.room.ChatParticipant;
import org.example.back.domain.room.ChatRoom;
import org.example.back.dto.room.request.CreateGroupChatRoomRequest;
import org.example.back.dto.room.request.CreatePrivateChatRoomRequest;
import org.example.back.dto.room.request.InviteChatRoomRequest;
import org.example.back.dto.room.request.LeaveChatRoomRequest;
import org.example.back.dto.room.response.ChatRoomResponse;
import org.example.back.exception.member.MemberException;
import org.example.back.repository.ChatParticipantRepository;
import org.example.back.repository.room.ChatRoomQueryRepository;
import org.example.back.repository.room.ChatRoomRepository;
import org.example.back.repository.MemberRepository;
import org.example.back.service.room.ChatRoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomServiceImpl 단위 테스트")
public class ChatRoomServiceTest {
    
    private static final Logger log = LoggerFactory.getLogger(ChatRoomServiceTest.class);
    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private ChatParticipantRepository chatParticipantRepository;
    
    @Mock
    private ChatRoomQueryRepository chatRoomQueryRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Nested
    @DisplayName("1:1 채팅방 생성 테스트")
    class CreatePrivateChatRoomTest {
        
        Member requester;
        Member target;
        
        @BeforeEach
        void 준비() {
            requester = Member.builder()
                    .id(1L)
                    .username("requester")
                    .password("pw")
                    .nickname("req")
                    .email("req@test.com")
                    .phone("010-1111-1111")
                    .build();
            
            target = Member.builder()
                    .id(2L)
                    .username("target")
                    .password("pw")
                    .nickname("tar")
                    .email("tar@test.com")
                    .phone("010-2222-2222")
                    .build();
        }
        
        @Test
        @DisplayName("기존 1:1 채팅방이 있으면 재사용")
        void 기존_채팅방_재사용() {
            
            // given
            ChatRoom existingRoom = ChatRoom.createPrivateRoom();
            setPrivateField(existingRoom, "id", 100L);
            
            CreatePrivateChatRoomRequest request = new CreatePrivateChatRoomRequest(
                    requester.getId(),
                    target.getId());
            
            when(memberRepository.findById(request.getMemberId())).thenReturn(
                    Optional.of(requester));
            when(memberRepository.findById(request.getTargetMemberId())).thenReturn(
                    Optional.of(target));
            when(chatRoomQueryRepository.findPrivateChatRoom(request.getMemberId(),
                    request.getTargetMemberId())).thenReturn(Optional.of(existingRoom));
            when(chatParticipantRepository.findByChatRoomId(existingRoom.getId())).thenReturn(
                    Arrays.asList(
                            ChatParticipant.create(existingRoom, requester),
                            ChatParticipant.create(existingRoom, target)
                    ));
            
            // when
            ChatRoomResponse response = chatRoomService.createPrivateChatRoom(request);
            
            // then
            assertThat(response.getRoomId()).isEqualTo(100L);
            assertThat(response.getParticipantIds()).containsExactlyInAnyOrder(requester.getId(),
                    target.getId());
        }
        
        @Test
        @DisplayName("기존 방이 없으면 새로 생성")
        void 기존_채팅방_없으므로_새로_생성() {
            
            // given
            CreatePrivateChatRoomRequest request = new CreatePrivateChatRoomRequest(
                    requester.getId(), target.getId());
            
            when(memberRepository.findById(request.getMemberId())).thenReturn(
                    Optional.of(requester));
            when(memberRepository.findById(request.getTargetMemberId())).thenReturn(
                    Optional.of(target));
            
            when(chatRoomQueryRepository.findPrivateChatRoom(request.getMemberId(),
                    request.getTargetMemberId())).thenReturn(Optional.empty());
            
            when(chatRoomRepository.save(any())).thenAnswer(invocation -> {
                ChatRoom savedRoom = invocation.getArgument(0);
                setPrivateField(savedRoom, "id", 200L);
                return savedRoom;
            });
            
            // when
            ChatRoomResponse response = chatRoomService.createPrivateChatRoom(request);
            
            // then
            assertThat(response).isNotNull();
            assertThat(response.getRoomId()).isEqualTo(200L);
            assertThat(response.getParticipantIds()).containsExactlyInAnyOrder(
                    requester.getId(), target.getId());
        }
        
        @Test
        @DisplayName("상대방 ID가 존재하지 않을 경우 MemberException 발생")
        void 상대방_ID_미존재_예외() {
            
            // given
            Long invalidTargetId = 999L;
            CreatePrivateChatRoomRequest request = new CreatePrivateChatRoomRequest(
                    requester.getId(), invalidTargetId);
            
            when(memberRepository.findById(invalidTargetId)).thenReturn(
                    Optional.empty());
            
            // expect
            assertThatThrownBy(() -> chatRoomService.createPrivateChatRoom(request)).isInstanceOf(
                    MemberException.class);
        }
    }
    
    @Nested
    @DisplayName("그룹 채팅방 생성 테스트")
    class CreateGroupChatRoomTest {
        
        Member requester;
        Member memberA;
        Member memberB;
        String roomName = "TestRoom";
        
        @BeforeEach
        void 준비() {
            requester = Member.builder()
                    .id(1L)
                    .username("requester")
                    .password("pw")
                    .nickname("req")
                    .email("req@test.com")
                    .phone("010-1111-1111")
                    .build();
            
            memberA = Member.builder()
                    .id(2L)
                    .username("memberA")
                    .password("pw")
                    .nickname("memA")
                    .email("a@test.com")
                    .phone("010-0000-0000")
                    .build();
            
            memberB = Member.builder()
                    .id(3L)
                    .username("memberB")
                    .password("pw")
                    .nickname("memB")
                    .email("b@test.com")
                    .phone("010-9999-9999")
                    .build();
        }
        
        @Test
        @DisplayName("요청자가 포함된 정상적인 그룹 채팅방 생성")
        void 그룹_채팅방_생성_성공() {
            
            // given
            List<Long> memberIds = Arrays.asList(1L, 2L, 3L);
            
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(roomName,
                    memberIds);
            
            when(memberRepository.findAllById(memberIds))
                    .thenReturn(List.of(requester, memberA, memberB));
            when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
                ChatRoom room = invocation.getArgument(0);
                setPrivateField(room, "id", 10L);
                return room;
            });
            
            // when
            ChatRoomResponse response = chatRoomService.createGroupChatRoom(requester.getId(),
                    request);
            
            // then
            assertThat(response.getRoomId()).isEqualTo(10L);
            assertThat(response.getName()).isEqualTo(roomName);
            assertThat(response.getParticipantIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }
        
        @Test
        @DisplayName("요청자 ID 가 memberIds 에 포함되지 않은 경우 자동 추가")
        void 요청자_ID_미존재_자동_추가후_그룹_채팅방_생성() {
            
            // given
            List<Long> memberIds = Arrays.asList(2L, 3L); // 요청자 ID 1L 빠짐
            
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(roomName,
                    memberIds);
            
            when(memberRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(memberA, memberB, requester));
            when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
                ChatRoom room = invocation.getArgument(0);
                setPrivateField(room, "id", 20L);
                return room;
            });
            
            // when
            ChatRoomResponse response = chatRoomService.createGroupChatRoom(requester.getId(),
                    request);
            
            // then
            assertThat(response.getRoomId()).isEqualTo(20L);
            assertThat(response.getParticipantIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }
        
        @Test
        @DisplayName("중복된 멤버가 포함된 경우 중복 없이 생성")
        void 중복_멤버_존재시_중복_없이_올바른_생성() {
            
            // given
            List<Long> memberIds = Arrays.asList(1L, 2L, 2L, 3L); // memberA 중복
            
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(roomName,
                    memberIds);
            
            when(memberRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(requester, memberA, memberB));
            when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
                ChatRoom room = invocation.getArgument(0);
                setPrivateField(room, "id", 30L);
                return room;
            });
            
            // when
            ChatRoomResponse response = chatRoomService.createGroupChatRoom(requester.getId(),
                    request);
            
            // then
            assertThat(response.getRoomId()).isEqualTo(30L);
            assertThat(response.getParticipantIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }
        
        @Test
        @DisplayName("존재하지 않는 멤버가 포함된 경우 예외가 발생")
        void 미존재_멤버_예외() {
            
            // given
            List<Long> memberIds = Arrays.asList(1L, 2L, 999L); // 999L은 존재하지 않음
            
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(roomName,
                    memberIds);
            
            // 999L 누락
            when(memberRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(requester, memberA));
            
            // expect
            assertThatThrownBy(
                    () -> chatRoomService.createGroupChatRoom(requester.getId(), request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("일부 회원 ID 가 유효하지 않습니다.");
        }
        
        @Test
        @DisplayName("그룹 채팅방 이름이 비어 있으면 예외가 발생")
        void 그룹_채팅방_생성시_그룹_이름_없는_예외() {
            
            // given
            String roomName = ""; // 빈 문자열
            List<Long> memberIds = Arrays.asList(1L, 2L, 3L);
            
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(roomName,
                    memberIds);
            
            // expect
            assertThatThrownBy(
                    () -> chatRoomService.createGroupChatRoom(requester.getId(), request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("채팅방 이름은 비어 있을 수 없습니다");
        }
    }
    
    @Nested
    @DisplayName("채팅방 목록 조회 테스트")
    class GetMyChatRoomsTest {
        
        Member requester;
        Pageable pageable;
        
        @BeforeEach
        void 준비() {
            requester = Member.builder()
                    .id(1L)
                    .username("requester")
                    .password("pw")
                    .nickname("req")
                    .email("req@test.com")
                    .phone("010-1111-1111")
                    .build();
            
            pageable = PageRequest.of(0, 20);
        }
        
        @Test
        @DisplayName("정상적인 멤버 ID로 요청 시, 참여 중인 채팅방 목록 반환")
        void 올바른_채팅방_목록_반환() {
            
            // given
            Member memberA = Member.builder().id(2L).username("memberA").build();
            Member memberB = Member.builder().id(3L).username("memberB").build();
            
            ChatRoom privateRoom = ChatRoom.createPrivateRoom();
            ChatRoom groupRoom = ChatRoom.createGroupRoom("테스트");
            
            setPrivateField(privateRoom, "id", 100L);
            setPrivateField(groupRoom, "id", 101L);
            
            when(memberRepository.findById(requester.getId()))
                    .thenReturn(Optional.of(requester));
            when(chatRoomQueryRepository.findMyChatRooms(requester.getId(), pageable))
                    .thenReturn(new PageImpl<>(List.of(privateRoom, groupRoom), pageable, 2));
            
            // 각 채팅방에 대한 참여자 목록 모킹
            when(chatParticipantRepository.findByChatRoomId(100L))
                    .thenReturn(List.of(ChatParticipant.create(privateRoom, requester)));
            when(chatParticipantRepository.findByChatRoomId(101L))
                    .thenReturn(List.of(
                            ChatParticipant.create(groupRoom, requester),
                            ChatParticipant.create(groupRoom, memberA),
                            ChatParticipant.create(groupRoom, memberB)
                    ));
            
            // when
            Page<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(requester.getId(),
                    pageable);
            
            // then
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting("roomId")
                    .containsExactlyInAnyOrder(100L, 101L);
            verify(chatParticipantRepository).findByChatRoomId(100L);
            verify(chatParticipantRepository).findByChatRoomId(101L);
        }
        
        @Test
        @DisplayName("존재하지 않는 사용자 ID로 요청 시 예외가 발생")
        void 미존재_ID_요청_예외() {
            
            // given
            Long invalidId = 999L;
            when(memberRepository.findById(invalidId)).thenReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> chatRoomService.getMyChatRooms(invalidId, pageable))
                    .isInstanceOf(MemberException.class)
                    .hasMessageContaining(USER_NOT_FOUND.getMessage());
        }
        
        @Test
        @DisplayName("참여 중인 채팅방이 없는 경우 빈 목록을 반환")
        void 참여_채팅방_없는_상황_빈_목록_반환() {
            
            // given
            when(memberRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
            when(chatRoomQueryRepository.findMyChatRooms(requester.getId(), pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
            
            // when
            Page<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(requester.getId(),
                    pageable);
            
            // then
            assertThat(responses).isEmpty();
            assertThat(responses.getTotalElements()).isZero();
        }
        
        @Test
        @DisplayName("Pageable 이 null 인 경우 기본 Pageable 적용")
        void pageable_null_기본_pageable_반환() {
            
            // given
            Pageable defaultPageable = PageRequest.of(0, 20);
            
            when(memberRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
            when(chatRoomQueryRepository.findMyChatRooms(requester.getId(), defaultPageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), defaultPageable, 0));
            
            // when
            Page<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(requester.getId(),
                    null);
            
            // then
            assertThat(responses).isEmpty();
        }
        
        @Test
        @DisplayName("그룹 채팅과 1:1 채팅이 섞여 있는 경우 타입 구분됨")
        void 그룹_비밀_채팅방_타입_구분() {
            
            // given
            Member memberA = Member.builder().id(2L).username("memberA").build();
            Member memberB = Member.builder().id(3L).username("memberB").build();
            
            ChatRoom privateRoom = ChatRoom.createPrivateRoom();
            ChatRoom groupRoom = ChatRoom.createGroupRoom("테스트");
            
            setPrivateField(privateRoom, "id", 400L);
            setPrivateField(groupRoom, "id", 401L);
            
            when(memberRepository.findById(requester.getId()))
                    .thenReturn(Optional.of(requester));
            
            when(chatRoomQueryRepository.findMyChatRooms(requester.getId(), pageable))
                    .thenReturn(new PageImpl<>(List.of(privateRoom, groupRoom), pageable, 2));
            
            // 각 채팅방에 대한 참여자 목록 모킹
            when(chatParticipantRepository.findByChatRoomId(400L))
                    .thenReturn(List.of(ChatParticipant.create(privateRoom, requester)));
            when(chatParticipantRepository.findByChatRoomId(401L))
                    .thenReturn(List.of(
                            ChatParticipant.create(groupRoom, requester),
                            ChatParticipant.create(groupRoom, memberA),
                            ChatParticipant.create(groupRoom, memberB)
                    ));
            
            // when
            Page<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(requester.getId(),
                    pageable);
            
            // then
            assertThat(responses.getContent())
                    .extracting(ChatRoomResponse::getType)
                    .containsExactlyInAnyOrder("PRIVATE", "GROUP");
        }
        
        @Test
        @DisplayName("soft delete 된 채팅방 자체는 목록에 포함하지 않음")
        void 삭제된_채팅방_목록_제거() {
            
            // given
            ChatRoom deletedRoom = ChatRoom.createGroupRoom("삭제된 방");
            setPrivateField(deletedRoom, "id", 500L);
            deletedRoom.markAsDeleted(); // 삭제 처리
            
            when(memberRepository.findById(requester.getId()))
                    .thenReturn(Optional.of(requester));
            
            when(chatRoomQueryRepository.findMyChatRooms(requester.getId(), pageable))
                    .thenReturn(new PageImpl<>(List.of(deletedRoom), pageable, 1));
            
            // when
            Page<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(requester.getId(),
                    pageable);
            
            // then
            assertThat(responses).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("채팅방 초대 테스트")
    class InviteMembersTest {
        
        Member inviter;
        Member newMemberA;
        Member newMemberB;
        ChatRoom groupRoom;
        
        @BeforeEach
        void 준비() {
            inviter = Member.builder()
                    .id(1L)
                    .username("inviter")
                    .password("pw")
                    .nickname("inv")
                    .email("inv@test.com")
                    .phone("010-0000-0000")
                    .build();
            
            newMemberA = Member.builder()
                    .id(2L)
                    .username("newA")
                    .password("pw")
                    .nickname("a")
                    .email("a@test.com")
                    .phone("010-0000-0001")
                    .build();
            
            newMemberB = Member.builder()
                    .id(3L)
                    .username("newB")
                    .password("pw")
                    .nickname("b")
                    .email("b@test.com")
                    .phone("010-0000-0002")
                    .build();
            
            groupRoom = ChatRoom.createGroupRoom("테스트");
            setPrivateField(groupRoom, "id", 100L);
        }
        
        @Test
        @DisplayName("새로운 멤버 그룹 채팅방 초대")
        void 성공_멤버_초대() {
            
            // given
            List<Long> memberIdsToInvite = List.of(newMemberA.getId(), newMemberB.getId());
            InviteChatRoomRequest request = new InviteChatRoomRequest(memberIdsToInvite);
            
            when(chatRoomRepository.findById(groupRoom.getId())).thenReturn(Optional.of(groupRoom));
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId())).thenReturn(
                    List.of(ChatParticipant.create(groupRoom, inviter))
            );
            when(memberRepository.findAllById(memberIdsToInvite)).thenReturn(
                    List.of(newMemberA, newMemberB));
            
            // when
            chatRoomService.inviteMembers(groupRoom.getId(), request);
            
            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ChatParticipant>> captor = ArgumentCaptor.forClass(List.class);
            verify(chatParticipantRepository).saveAll(captor.capture());
            
            List<ChatParticipant> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(p -> p.getMember().getId())
                    .containsExactlyInAnyOrder(2L, 3L);
        }
        
        @Test
        @DisplayName("1:1 채팅방에 초대 요청 시 예외 발생")
        void 초대_실패_1대1_채팅방() {
            
            // given
            ChatRoom privateRoom = ChatRoom.createPrivateRoom();
            setPrivateField(privateRoom, "id", 200L);
            
            List<Long> memberIdsToInvite = List.of(newMemberA.getId());
            InviteChatRoomRequest request = new InviteChatRoomRequest(memberIdsToInvite);
            
            when(chatRoomRepository.findById(privateRoom.getId())).thenReturn(
                    Optional.of(privateRoom));
            
            // when & then
            assertThatThrownBy(() -> chatRoomService.inviteMembers(privateRoom.getId(), request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1:1 채팅방에는 초대할 수 없습니다.");
        }
        
        @Test
        @DisplayName("이미 참여 중인 멤버는 초대되지 않음 -> A, B 초대 중 A 만 이미 존재하여, B 는 초대")
        void 초대_실패_이미_참여중인_멤버() {
            
            // given
            List<Long> memberIdsToInvite = List.of(newMemberA.getId(), newMemberB.getId());
            InviteChatRoomRequest request = new InviteChatRoomRequest(memberIdsToInvite);
            
            when(chatRoomRepository.findById(groupRoom.getId())).thenReturn(Optional.of(groupRoom));
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId())).thenReturn(
                    List.of(ChatParticipant.create(groupRoom, inviter),
                            ChatParticipant.create(groupRoom, newMemberA) // 이미 참여
                    )
            );
            when(memberRepository.findAllById(memberIdsToInvite)).thenReturn(
                    List.of(newMemberA, newMemberB));
            
            // when
            chatRoomService.inviteMembers(groupRoom.getId(), request);
            
            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ChatParticipant>> captor = ArgumentCaptor.forClass(List.class);
            verify(chatParticipantRepository).saveAll(captor.capture());
            
            List<ChatParticipant> saved = captor.getValue();
            System.out.println(saved);
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getMember().getId()).isEqualTo(newMemberB.getId());
        }
        
        @Test
        @DisplayName("초대 요청에 존재하지 않는 멤버 ID 가 포함된 경우 유효한 멤버만 초대")
        void 존재하지_않는_멤버_ID_무시() {
            
            // given
            Long nonExistingId = 999L;
            List<Long> memberIdsToInvite = List.of(newMemberA.getId(), nonExistingId);
            InviteChatRoomRequest request = new InviteChatRoomRequest(memberIdsToInvite);
            
            when(chatRoomRepository.findById(groupRoom.getId())).thenReturn(Optional.of(groupRoom));
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId())).thenReturn(
                    List.of(ChatParticipant.create(groupRoom, inviter))
            );
            when(memberRepository.findAllById(memberIdsToInvite)).thenReturn(
                    List.of(newMemberA)); // 유효한 멤버 newMemberA 만 리턴
            
            // when
            chatRoomService.inviteMembers(groupRoom.getId(), request);
            
            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ChatParticipant>> captor = ArgumentCaptor.forClass(List.class);
            verify(chatParticipantRepository).saveAll(captor.capture());
            
            List<ChatParticipant> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getMember().getId()).isEqualTo(newMemberA.getId());
        }
        
        @Test
        @DisplayName("채팅방이 존재하지 않으면 예외 발생")
        void 초대_실패_채팅방_없음() {
            
            // given
            Long invalidChatRoomId = 999L;
            List<Long> memberIdsToInvite = List.of(newMemberA.getId());
            InviteChatRoomRequest request = new InviteChatRoomRequest(memberIdsToInvite);
            
            when(chatRoomRepository.findById(invalidChatRoomId)).thenReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> chatRoomService.inviteMembers(invalidChatRoomId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ChatRoom not found: " + invalidChatRoomId);
        }
        
        @Test
        @DisplayName("전체 멤버가 모두 기존 참여자일 경우 saveAll 이 호출되지 않음")
        void 초대_스킵_모두_기존_참여자() {
            
            // given
            List<Long> memberIdsToInvite = List.of(newMemberA.getId(), newMemberB.getId());
            InviteChatRoomRequest request = new InviteChatRoomRequest(memberIdsToInvite);
            
            when(chatRoomRepository.findById(groupRoom.getId())).thenReturn(Optional.of(groupRoom));
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId())).thenReturn(
                    List.of(ChatParticipant.create(groupRoom, inviter),
                            ChatParticipant.create(groupRoom, newMemberA),
                            ChatParticipant.create(groupRoom, newMemberB)
                    )
            );
            when(memberRepository.findAllById(memberIdsToInvite)).thenReturn(
                    List.of(newMemberA, newMemberB));
            
            // when
            chatRoomService.inviteMembers(groupRoom.getId(), request);
            
            // then
            verify(chatParticipantRepository, never()).saveAll(any());
        }
    }
    
    @Nested
    @DisplayName("채팅방 나가기 테스트")
    class LeaveChatRoomTest {
        
        Member member;
        ChatRoom groupRoom;
        
        @BeforeEach
        void 준비() {
            member = Member.builder()
                    .id(1L)
                    .username("user1")
                    .password("pw")
                    .nickname("닉네임")
                    .email("user1@test.com")
                    .phone("010-0000-0000")
                    .build();
            
            groupRoom = ChatRoom.createGroupRoom("테스트");
            setPrivateField(groupRoom, "id", 100L);
        }
        
        @Test
        @DisplayName("정상적으로 채팅방 나가기 요청하여 참여자 삭제")
        void 정상적으로_나가기() {
            
            // given
            LeaveChatRoomRequest request = new LeaveChatRoomRequest(member.getId());
            Member remaining = Member.builder().id(2L).username("남은사람").build();
            
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId()))
                    .thenReturn(List.of(ChatParticipant.create(groupRoom, remaining)));
            
            // when
            chatRoomService.leaveChatRoom(groupRoom.getId(), request);
            
            // then
            verify(chatParticipantRepository).deleteByMemberIdAndChatRoomId(member.getId(),
                    groupRoom.getId());
        }
        
        @Test
        @DisplayName("요청자가 마지막 참여자인 경우 채팅방은 소프트 삭제")
        void 마지막_참여자_소프트삭제() {
            
            // given
            LeaveChatRoomRequest request = new LeaveChatRoomRequest(member.getId());
            
            // 나가고 남은 참여자 없음
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId()))
                    .thenReturn(Collections.emptyList());
            when(chatRoomRepository.findById(groupRoom.getId()))
                    .thenReturn(Optional.of(groupRoom));
            
            // when
            chatRoomService.leaveChatRoom(groupRoom.getId(), request);
            
            // then
            verify(chatParticipantRepository).deleteByMemberIdAndChatRoomId(member.getId(),
                    groupRoom.getId());
            assertThat(groupRoom.getIsDeleted()).isTrue(); // 소프트 삭제 플래그 확인
        }
        
        @Test
        @DisplayName("이미 삭제된 채팅방인 경우 markAsDeleted() 호출안됨")
        void 이미_삭제된_채팅방은_재삭제되지_않음() {
            
            // given
            LeaveChatRoomRequest request = new LeaveChatRoomRequest(member.getId());
            
            groupRoom.markAsDeleted();
            
            // 나가기 후 참여자 없음
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId()))
                    .thenReturn(Collections.emptyList());
            when(chatRoomRepository.findById(groupRoom.getId()))
                    .thenReturn(Optional.of(groupRoom));
            
            // when
            chatRoomService.leaveChatRoom(groupRoom.getId(), request);
            
            // then
            verify(chatParticipantRepository).deleteByMemberIdAndChatRoomId(member.getId(),
                    groupRoom.getId());
            assertThat(groupRoom.getIsDeleted()).isTrue();
        }
        
        @Test
        @DisplayName("참여자가 남아있다면 채팅방은 삭제되지 않음")
        void 남은_참여자가_있다면_삭제되지_않음() {
            
            // given
            LeaveChatRoomRequest request = new LeaveChatRoomRequest(member.getId());
            
            ChatRoom activeRoom = spy(ChatRoom.createGroupRoom("삭제안되는 채팅방"));
            setPrivateField(activeRoom, "id", 123L);
            
            Member remainingMember = Member.builder()
                    .id(2L)
                    .username("남은멤버")
                    .build();
            
            ChatParticipant remainingParticipant = ChatParticipant.create(activeRoom,
                    remainingMember);
            
            // 남은 참여자 존재
            when(chatParticipantRepository.findByChatRoomId(activeRoom.getId()))
                    .thenReturn(List.of(remainingParticipant));
            
            // 참여자가 있으면 해당 코드는 실행되지 않
            lenient().when(chatRoomRepository.findById(activeRoom.getId()))
                    .thenReturn(Optional.of(activeRoom));
            
            // when
            chatRoomService.leaveChatRoom(activeRoom.getId(), request);
            
            // then
            verify(chatParticipantRepository).deleteByMemberIdAndChatRoomId(member.getId(),
                    activeRoom.getId());
            verify(activeRoom, never()).markAsDeleted(); // 삭제 호출 X
            assertThat(activeRoom.getIsDeleted()).isFalse();
        }
        
        @Test
        @DisplayName("존재하지 않는 채팅방 ID 로 나가기 예외")
        void 존재하지_않는_채팅방ID() {
            
            // given
            Long invalidRoomId = 999L;
            LeaveChatRoomRequest request = new LeaveChatRoomRequest(member.getId());
            
            // 참여자 삭제 시도
            when(chatParticipantRepository.findByChatRoomId(invalidRoomId))
                    .thenReturn(Collections.emptyList());
            
            // 존재하지 않는 방
            when(chatRoomRepository.findById(invalidRoomId))
                    .thenReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> chatRoomService.leaveChatRoom(invalidRoomId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ChatRoom not found: " + invalidRoomId);
            
            verify(chatParticipantRepository).deleteByMemberIdAndChatRoomId(member.getId(),
                    invalidRoomId);
            verify(chatRoomRepository).findById(invalidRoomId);
        }
        
        @Test
        @DisplayName("요청한 멤버가 실제 참여자가 아니더라도 deleteByMemberIdAndChatRoomId는 호출됨")
        void 참여자가_아닌_멤버가_나가기_요청() {
            
            // given
            Long invalidId = 999L;
            LeaveChatRoomRequest request = new LeaveChatRoomRequest(invalidId);
            
            Member remainingMember = Member.builder()
                    .id(2L)
                    .username("남은멤버")
                    .build();
            ChatParticipant remainingParticipant = ChatParticipant.create(groupRoom,
                    remainingMember);
            
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId()))
                    .thenReturn(List.of(remainingParticipant));
            
            lenient().when(chatRoomRepository.findById(groupRoom.getId()))
                    .thenReturn(Optional.of(groupRoom));
            
            // when
            chatRoomService.leaveChatRoom(groupRoom.getId(), request);
            
            // then
            verify(chatParticipantRepository).deleteByMemberIdAndChatRoomId(invalidId,
                    groupRoom.getId());
            verify(chatParticipantRepository).findByChatRoomId(groupRoom.getId());
        }
    }
    
    @Nested
    @DisplayName("채팅방 소프트 삭제 테스트")
    class SoftDeleteChatRoomTest {
        
        Member member;
        ChatRoom groupRoom;
        
        @BeforeEach
        void 준비() {
            member = Member.builder()
                    .id(1L)
                    .username("user1")
                    .password("pw")
                    .nickname("닉네임")
                    .email("user1@test.com")
                    .phone("010-0000-0000")
                    .build();
            
            groupRoom = ChatRoom.createGroupRoom("테스트");
            setPrivateField(groupRoom, "id", 100L);
        }
        
        @Test
        @DisplayName("채팅방 참여자가 정상적으로 소프트 삭제")
        void 성공_소프트_삭제() {
            
            // given
            ChatParticipant participant = ChatParticipant.create(groupRoom, member);
            
            when(chatRoomRepository.findById(groupRoom.getId())).thenReturn(Optional.of(groupRoom));
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId())).thenReturn(
                    List.of(participant));
            
            // when
            chatRoomService.softDeleteChatRoom(groupRoom.getId(), member.getId());
            
            // then
            assertThat(groupRoom.getIsDeleted()).isTrue(); // 상태 확인
            verify(chatParticipantRepository).findByChatRoomId(groupRoom.getId());
        }
        
        @Test
        @DisplayName("채팅방이 존재하지 않으면 예외 발생")
        void 실패_채팅방_없음() {
            
            // given
            Long invalidChatRoomId = 999L;
            
            when(chatRoomRepository.findById(invalidChatRoomId)).thenReturn(Optional.empty());
            
            // expect
            assertThatThrownBy(() ->
                    chatRoomService.softDeleteChatRoom(invalidChatRoomId, member.getId()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ChatRoom not found: " + invalidChatRoomId);
            
            verify(chatRoomRepository).findById(invalidChatRoomId);
            verify(chatParticipantRepository, never()).findByChatRoomId(any());
        }
        
        @Test
        @DisplayName("요청자가 채팅방 참여자가 아니면 예외 발생")
        void 실패_참여자_아님() {
            
            // given
            Member otherMember = Member.builder().id(999L).build();
            ChatParticipant otherParticipant = ChatParticipant.create(groupRoom, otherMember);
            
            when(chatRoomRepository.findById(groupRoom.getId())).thenReturn(Optional.of(groupRoom));
            when(chatParticipantRepository.findByChatRoomId(groupRoom.getId())).thenReturn(
                    List.of(otherParticipant));
            
            assertThatThrownBy(
                    () -> chatRoomService.softDeleteChatRoom(groupRoom.getId(), member.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 사용자는 채팅방의 참여자가 아닙니다");
            
            verify(chatParticipantRepository).findByChatRoomId(groupRoom.getId());
        }
        
        @Test
        @DisplayName("이미 삭제된 채팅방을 다시 삭제해도 무관")
        void 이미_삭제된_채팅방() {
            
            // given
            groupRoom.markAsDeleted(); // 삭제된 상태로 변경
            ChatParticipant participant = ChatParticipant.create(groupRoom, member);
            
            when(chatRoomRepository.findById(groupRoom.getId())).thenReturn(Optional.of(groupRoom));
            
            // then
            assertThatThrownBy(
                    () -> chatRoomService.softDeleteChatRoom(groupRoom.getId(), member.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 삭제된 채팅방입니다.");
            
            assertThat(groupRoom.getIsDeleted()).isTrue();
            verify(chatParticipantRepository, never()).findByChatRoomId(any());
        }
    }
}
