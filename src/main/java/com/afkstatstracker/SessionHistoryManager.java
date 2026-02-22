package com.afkstatstracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SessionHistoryManager
{
    private static final int MAX_SESSIONS = 20;

    private final ConfigStorage storage;
    private final Gson gson;
    private List<Session> sessions;

    public interface ConfigStorage
    {
        String load();
        void save(String json);
    }

    public SessionHistoryManager(ConfigStorage storage, Gson gson)
    {
        this.storage = storage;
        this.gson = gson;
        this.sessions = loadSessions();
    }

    private List<Session> loadSessions()
    {
        String json = storage.load();
        if (json == null || json.isEmpty())
        {
            return new ArrayList<>();
        }

        try
        {
            Type listType = new TypeToken<List<Session>>(){}.getType();
            List<Session> loaded = gson.fromJson(json, listType);
            return loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        }
        catch (Exception e)
        {
            log.warn("Failed to parse session history, starting fresh", e);
            return new ArrayList<>();
        }
    }

    private void saveSessions()
    {
        String json = gson.toJson(sessions);
        storage.save(json);
    }

    public List<Session> getSessions()
    {
        return new ArrayList<>(sessions);
    }

    public void addSession(Session session)
    {
        sessions.add(session);

        while (sessions.size() > MAX_SESSIONS)
        {
            sessions.remove(0);
        }

        saveSessions();
    }

    public void renameSession(String id, String newName)
    {
        for (Session session : sessions)
        {
            if (session.getId().equals(id))
            {
                session.setName(newName);
                saveSessions();
                return;
            }
        }
    }

    public void deleteSession(String id)
    {
        sessions.removeIf(s -> s.getId().equals(id));
        saveSessions();
    }
}
