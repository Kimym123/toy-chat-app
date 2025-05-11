package org.example.back.repository.message;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.back.domain.message.ChatMessage;
import org.example.back.domain.message.QChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/*
 * 채팅 메시지 관련 조회용 QueryDSL Repository
 * */
@Repository
@RequiredArgsConstructor
public class ChatMessageQueryRepository {
    
    private final JPAQueryFactory queryFactory;
    private final QChatMessage chatMessage = QChatMessage.chatMessage;
    
    // 채팅방 메시지 전체 조회 (오래된 순, 페이징)
    public List<ChatMessage> findMessagesByChatRoomId(Long chatRoomId, Pageable pageable) {
        return queryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.chatRoom.id.eq(chatRoomId))
                .orderBy(chatMessage.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
    
    // 최근 메시지 N 개 조회 (최신순)
    public List<ChatMessage> findRecentMessagesByChatRoomId(Long chatRoomId, int limit) {
        return queryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.chatRoom.id.eq(chatRoomId))
                .orderBy(chatMessage.createdAt.desc())
                .limit(limit)
                .fetch();
    }
}
