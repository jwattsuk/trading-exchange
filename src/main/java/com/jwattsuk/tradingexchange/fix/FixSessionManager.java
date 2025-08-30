package com.jwattsuk.tradingexchange.fix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.SessionID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active FIX sessions.
 */
public class FixSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(FixSessionManager.class);
    
    private final Map<SessionID, SessionInfo> sessions;
    
    public FixSessionManager() {
        this.sessions = new ConcurrentHashMap<>();
    }
    
    public void addSession(SessionID sessionID) {
        SessionInfo sessionInfo = new SessionInfo(sessionID);
        sessions.put(sessionID, sessionInfo);
        logger.info("Added FIX session: {}", sessionID);
    }
    
    public void removeSession(SessionID sessionID) {
        sessions.remove(sessionID);
        logger.info("Removed FIX session: {}", sessionID);
    }
    
    public SessionInfo getSession(SessionID sessionID) {
        return sessions.get(sessionID);
    }
    
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Information about a FIX session.
     */
    public static class SessionInfo {
        private final SessionID sessionID;
        private final long connectTime;
        
        public SessionInfo(SessionID sessionID) {
            this.sessionID = sessionID;
            this.connectTime = System.currentTimeMillis();
        }
        
        public SessionID getSessionID() { return sessionID; }
        public long getConnectTime() { return connectTime; }
        
        @Override
        public String toString() {
            return String.format("SessionInfo{sessionID=%s, connectTime=%d}", sessionID, connectTime);
        }
    }
}
