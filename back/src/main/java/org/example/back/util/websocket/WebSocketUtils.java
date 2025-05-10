package org.example.back.util.websocket;

import lombok.experimental.UtilityClass;
import org.springframework.web.socket.WebSocketSession;

@UtilityClass
public class WebSocketUtils {
    
    public static Long getMemberId(WebSocketSession session) {
        Object raw = session.getAttributes().get("memberId");
        return raw instanceof Long ? (Long) raw : null;
    }
    
    public static String getMemberRole(WebSocketSession session) {
        Object raw = session.getAttributes().get("role");
        return raw instanceof String ? (String) raw : null;
    }
}
