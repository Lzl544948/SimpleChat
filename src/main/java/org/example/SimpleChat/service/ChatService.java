package org.example.SimpleChat.service;

import jakarta.websocket.Session;

public interface ChatService {
    void addSession(String sessionId, String username, Session session);
    void removeSession(String sessionId, String username);
    void sendMessage(String sessionId,String senderName, String message);
    void broadcast(String sessionId, String message);
    void broadcast(String message);
}
