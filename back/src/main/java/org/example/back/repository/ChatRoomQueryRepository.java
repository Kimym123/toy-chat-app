package org.example.back.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.back.domain.room.ChatRoom;
import org.example.back.domain.room.ChatRoomType;
import org.example.back.domain.room.QChatParticipant;
import org.example.back.domain.room.QChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/*
 * 채팅방 관련 조회용 QueryDSL Repository
 * */
@Repository
@RequiredArgsConstructor
public class ChatRoomQueryRepository {
    
    private final JPAQueryFactory queryFactory;
    
    private final QChatRoom chatRoom = QChatRoom.chatRoom;
    private final QChatParticipant chatParticipant = QChatParticipant.chatParticipant;
    
    // 사용자가 참여 중인 채팅방 목록 조회 (페이징)
    public Page<ChatRoom> findMyChatRooms(Long memberId, Pageable pageable) {
        
        List<ChatRoom> results = queryFactory
                .select(chatRoom)
                .from(chatRoom)
                .join(chatParticipant)
                .on(chatParticipant.chatRoom.eq(chatRoom))
                .where(
                        chatParticipant.member.id.eq(memberId),
                        chatRoom.isDeleted.isFalse()
                )
                .orderBy(chatRoom.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        
        Long total = queryFactory
                .select(chatRoom.count())
                .from(chatRoom)
                .join(chatParticipant)
                .on(chatParticipant.chatRoom.eq(chatRoom))
                .where(
                        chatParticipant.member.id.eq(memberId),
                        chatRoom.isDeleted.isFalse()
                )
                .fetchOne();
        
        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }
    
    // 내가 참여 중인 채팅방 수 조회
    public Long countMyChatRooms(Long memberId) {
        return queryFactory
                .select(chatRoom.count())
                .from(chatRoom)
                .join(chatParticipant)
                .on(chatParticipant.chatRoom.eq(chatRoom))
                .where(
                        chatParticipant.member.id.eq(memberId),
                        chatRoom.isDeleted.isFalse()
                )
                .fetchOne();
    }
    
    // 두 유저간 1:1 채팅방 존재 여부 조회 (PRIVATE)
    public Optional<ChatRoom> findPrivateChatRoom(Long memberId1, Long memberId2) {
        List<ChatRoom> rooms = queryFactory
                .select(chatRoom)
                .from(chatRoom)
                .join(chatParticipant)
                .on(chatParticipant.chatRoom.eq(chatRoom))
                .where(
                        chatRoom.type.eq(ChatRoomType.PRIVATE),
                        chatRoom.isDeleted.isFalse(),
                        chatParticipant.member.id.in(memberId1, memberId2)
                )
                .groupBy(chatRoom.id)
                .having(chatParticipant.count().eq(2L))
                .fetch();
        
        return rooms.stream().findFirst();
    }
}
