package org.example.SimpleChat.ws;

import com.alibaba.fastjson.JSON;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.example.SimpleChat.config.WebSocketConfigurator;
import org.example.SimpleChat.pojo.ChatMsg;
import org.example.SimpleChat.utils.JwtUtil;
import org.example.SimpleChat.utils.MessageUtils;
import org.example.SimpleChat.ws.pojo.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat", configurator = WebSocketConfigurator.class)
@Component
public class ChatEndpoint {
    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();//thread safe map

    private String username;
    private String sessionId;

    // RedisTemplate
    private static StringRedisTemplate redisTemplate;

    // 注入 RedisTemplate（Spring Boot 启动时设置）
    @Autowired
    public void setRedisTemplate(StringRedisTemplate template) {
        redisTemplate = template;
    }
    @OnOpen
  //  @Transactional
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

        redisTemplate.opsForSet().add("chat:onlineUsers:"+sessionId, username);
        redisTemplate.opsForSet().add("chat:sessionList:"+username, sessionId);
        onlineUsers.put(username+sessionId, session);
        String msg = MessageUtils.getMessage(true, null, onlineUsers.keySet());
        boardcast(msg);
    }

    @OnMessage
   // @Transactional
    public void onMessage(String message) {

        Message msg = JSON.parseObject(message, Message.class);
        String toName = msg.getToName();
        Session session = onlineUsers.get(toName+sessionId);

        //封装消息记录
        ChatMsg chatMsg = new ChatMsg(msg.getMessage(),username,toName, LocalDateTime.now());
        //缓存到redis，管理同一会话中的消息列表和最后一条消息
        redisTemplate.opsForList().rightPush("chat:msgList:"+sessionId, JSON.toJSONString(chatMsg));
        redisTemplate.opsForValue().set("chat:msg:last:"+sessionId, JSON.toJSONString(chatMsg));

        String msgStr = MessageUtils.getMessage(false, username, msg.getMessage());
        try {
            session.getBasicRemote().sendText(msgStr);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @OnClose
  //  @Transactional
    public void onClose() {
        onlineUsers.remove(username+sessionId);
        redisTemplate.opsForSet().remove("chat:onlineUsers:"+sessionId, username);
        redisTemplate.opsForSet().remove("chat:sessionList:"+username, sessionId);
        //若该用户已没有活跃会话，把该用户的记录删除
        if( 0==redisTemplate.opsForSet().size("chat:sessionList:"+username)){
            redisTemplate.delete("chat:sessionList:"+username);
        }
        //若该会话已没有活跃用户，把该会话的记录删除，同时将聊天消息持久化存储于MySQL中
        if( 0==redisTemplate.opsForSet().size("chat:onlineUsers:"+sessionId) ){
           // SaveMsgService.saveMsg(sessionId);
            redisTemplate.delete("chat:onlineUsers:"+sessionId);
        }
        String msg = MessageUtils.getMessage(true, null, onlineUsers.keySet());
        boardcast(msg);
    }

    private void boardcast(String message) {
        try {
            for (Session session : onlineUsers.values()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
                map.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }
}
