package com.afkstatstracker;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Session
{
    private final String id;
    private String name;
    private final long startTime;
    private final long endTime;
    private final int clickCount;
    private final long consistencyScore;
    private final double avgInterval;

    public Session(String id, String name, long startTime, long endTime,
                   int clickCount, long consistencyScore, double avgInterval)
    {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.clickCount = clickCount;
        this.consistencyScore = consistencyScore;
        this.avgInterval = avgInterval;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public int getClickCount() { return clickCount; }
    public long getConsistencyScore() { return consistencyScore; }
    public double getAvgInterval() { return avgInterval; }

    public String toClipboardText()
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        String dateStr = dateFormat.format(new Date(startTime));
        return String.format("Session: %s | %s | Consistency: %d | Avg: %.0fms | Clicks: %d",
            name, dateStr, consistencyScore, avgInterval, clickCount);
    }
}
