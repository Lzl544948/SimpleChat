package org.example.SimpleChat.ws;

import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.example.SimpleChat.config.WebSocketConfigurator;
import org.example.SimpleChat.service.ChatService;
import org.example.SimpleChat.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@ServerEndpoint(value = "/chat", configurator = WebSocketConfigurator.class)
@Component
@Slf4j
public class ChatEndpoint {

    private String username;
    private String sessionId;
    private ChatService chatService;

    @Autowired
    public void setChatService(ChatService chatService) {
        this.chatService = chatService;
    }

    //用于检查是否创建了新实例
    @PostConstruct
    public void init() {
        log.info("ChatEndpoint 实例被创建：{}" , this);
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) throws IOException {
        //get token from url
        String token = (String) config.getUserProperties().get("token");

        // 从 token 参数中获取用户名
        Map<String, Object> params = JwtUtil.parseJwt(token);
        this.username = (String) params.get("username");
        this.sessionId = (String) config.getUserProperties().get("sessionId");

        if (username == null || username.isEmpty() || sessionId == null || sessionId.isEmpty()) {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Username is required"));
            return;
        }
        log.info("[OPEN] 新连接 sessionId={}, userId={}", sessionId, username);
        chatService.addSession(sessionId, username, session);

    }

    @OnMessage
    public void onMessage(String message) {
        chatService.sendMessage(sessionId, username, message);

    }

    @OnClose
    public void onClose() {
        log.info("[CLOSE] 连接关闭 sessionId={}, userId={}", sessionId, username);
        chatService.removeSession(sessionId, username);

    }

}
