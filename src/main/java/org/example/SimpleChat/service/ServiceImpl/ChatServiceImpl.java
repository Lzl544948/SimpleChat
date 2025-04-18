package org.example.SimpleChat.service.ServiceImpl;

import com.alibaba.fastjson.JSON;
import jakarta.websocket.Session;
import org.example.SimpleChat.pojo.ChatMsg;
import org.example.SimpleChat.service.ChatService;
import org.example.SimpleChat.utils.MessageUtils;
import org.example.SimpleChat.ws.pojo.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatServiceImpl implements ChatService {
    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();//thread safe map
    // RedisTemplate
    private static StringRedisTemplate redisTemplate;

    // 注入 RedisTemplate（Spring Boot 启动时设置）
    @Autowired
    public void setRedisTemplate(StringRedisTemplate template) {
        redisTemplate = template;
    }

    @Override
    @Transactional
    public void addSession(String sessionId, String username, Session session) {
        redisTemplate.opsForSet().add("chat:onlineUsers:"+sessionId, username);
        redisTemplate.opsForSet().add("chat:sessionList:"+username, sessionId);
        onlineUsers.put(username+sessionId, session);
        String msg = MessageUtils.getMessage(true, null, onlineUsers.keySet());
        broadcast(msg);
    }

    @Override
    @Transactional
    public void removeSession(String sessionId, String username) {
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
        broadcast(msg);
    }

    @Override
    @Transactional
    public void sendMessage(String sessionId, String senderName, String message) {
        Message msg = JSON.parseObject(message, Message.class);
        String toName = msg.getToName();
        Session session = onlineUsers.get(toName+sessionId);

        //封装消息记录
        ChatMsg chatMsg = new ChatMsg(msg.getMessage(),senderName,toName, LocalDateTime.now());
        //缓存到redis，管理同一会话中的消息列表和最后一条消息
        redisTemplate.opsForList().rightPush("chat:msgList:"+sessionId, JSON.toJSONString(chatMsg));
        redisTemplate.opsForValue().set("chat:msg:last:"+sessionId, JSON.toJSONString(chatMsg));

        String msgStr = MessageUtils.getMessage(false, senderName, msg.getMessage());
        try {
            session.getBasicRemote().sendText(msgStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void broadcast(String sessionId, String message) {
        try {
            for (Session session : onlineUsers.values()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void broadcast(String message) {
        try {
            for (Session session : onlineUsers.values()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
