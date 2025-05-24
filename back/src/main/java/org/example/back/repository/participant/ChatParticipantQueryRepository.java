package org.example.back.repository.participant;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.back.domain.room.QChatParticipant;
import org.example.back.dto.websocket.request.ReadMessageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatParticipantQueryRepository {
    
    private final JPAQueryFactory queryFactory;
    private final QChatParticipant chatParticipant = QChatParticipant.chatParticipant;
    
    public long updateLastReadMessageId(ReadMessageRequest request) {
        return queryFactory
                .update(chatParticipant)
                .set(chatParticipant.lastReadMessageId, request.getMessageId())
                .where(
                        chatParticipant.chatRoom.id.eq(request.getChatRoomId()),
                        chatParticipant.member.id.eq(request.getMemberId()),
                        chatParticipant.lastReadMessageId.lt(request.getMessageId()) // 메시지 ID 증가 시에만 업데이트
                )
                .execute();
    }
}
