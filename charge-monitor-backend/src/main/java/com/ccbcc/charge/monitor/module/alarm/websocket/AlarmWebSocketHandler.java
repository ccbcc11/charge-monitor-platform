package com.ccbcc.charge.monitor.module.alarm.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警 WebSocket 处理器
 *
 * 第一版采用广播模式：
 * 所有连接 /ws/alarm 的客户端都能收到告警消息。
 */
@Slf4j
@Component
public class AlarmWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SESSION_MAP.put(session.getId(), session);

        log.info(
                "告警 WebSocket 客户端连接成功，sessionId={}，当前在线连接数={}",
                session.getId(),
                SESSION_MAP.size()
        );
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSION_MAP.remove(session.getId());

        log.info(
                "告警 WebSocket 客户端断开连接，sessionId={}，status={}，当前在线连接数={}",
                session.getId(),
                status,
                SESSION_MAP.size()
        );
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        SESSION_MAP.remove(session.getId());

        log.warn(
                "告警 WebSocket 连接异常，sessionId={}，当前在线连接数={}",
                session.getId(),
                SESSION_MAP.size(),
                exception
        );
    }

    /**
     * 广播文本消息
     */
    public void broadcast(String messageText) {
        Collection<WebSocketSession> sessions = SESSION_MAP.values();

        if (sessions.isEmpty()) {
            log.debug("当前没有在线 WebSocket 客户端，跳过告警推送");
            return;
        }

        for (WebSocketSession session : sessions) {
            sendMessage(session, messageText);
        }
    }

    /**
     * 发送消息到单个 session
     *
     * 注意：
     * WebSocketSession 不建议多个线程同时 sendMessage。
     * 所以这里对 session 加 synchronized，避免并发发送异常。
     */
    private void sendMessage(WebSocketSession session, String messageText) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(messageText));
            }
        } catch (Exception e) {
            SESSION_MAP.remove(session.getId());

            log.warn(
                    "告警 WebSocket 消息发送失败，sessionId={}，已移除该连接",
                    session.getId(),
                    e
            );
        }
    }

    public int getOnlineCount() {
        return SESSION_MAP.size();
    }
}
