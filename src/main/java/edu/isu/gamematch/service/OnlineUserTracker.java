package edu.isu.gamematch.service;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserTracker {
    private final Set<Integer> onlineUserIds = ConcurrentHashMap.newKeySet();

    public void userLoggedIn(int userId) { onlineUserIds.add(userId); }
    public void userLoggedOut(int userId) { onlineUserIds.remove(userId); }
    public boolean isOnline(int userId) { return onlineUserIds.contains(userId); }
}