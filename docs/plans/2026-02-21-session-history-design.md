# Session History Feature Design

## Overview

Add persistent session history to the AFK Stats Tracker plugin, allowing users to review past sessions, rename them, copy data to clipboard, and delete unwanted entries.

## Requirements

- Save last 20 sessions automatically when stopped
- Auto-name sessions with timestamp, allow renaming from history list
- Copy session data to clipboard as plain text
- Delete sessions from history
- Persist across plugin restarts using RuneLite ConfigManager

## Data Model

```java
Session {
    String id;              // UUID for unique identification
    String name;            // Auto: "Session 2026-02-21 14:30", user can rename
    long startTime;         // Epoch millis
    long endTime;           // Epoch millis
    int clickCount;         // Total clicks in session
    long consistencyScore;  // 0-100
    double avgInterval;     // milliseconds
}
```

**Storage**: Sessions serialized as JSON array, stored via ConfigManager as single string key `sessionHistory`.

## UI Layout

```
┌─────────────────────────────┐
│  [Start Session] [Stop]     │  ← Existing buttons
├─────────────────────────────┤
│  Consistency: 85            │  ← Existing live stats
│  Average Click Interval: 45000 │
├─────────────────────────────┤
│  ▼ Session History          │  ← Collapsible header
├─────────────────────────────┤
│  Session 2026-02-21 14:30   │  ← Row: name (click to edit)
│  85 | 45000ms | 42 clicks   │  ← Row: compact stats
│  [Copy] [Delete]            │  ← Row: action buttons
├─────────────────────────────┤
│  Session 2026-02-21 13:15   │  ← Next session...
└─────────────────────────────┘
```

**Interactions:**
- Click session name → inline edit, Enter to save
- Copy button → plain text to clipboard
- Delete button → instant removal (no confirmation)
- History section collapsible, remembers state

## Components

**New files:**
- `Session.java` - Data class for session info
- `SessionHistoryManager.java` - Save/load/delete operations with ConfigManager

**Modified files:**
- `AfkStatsTrackerPlugin.java` - Inject SessionHistoryManager, save on session stop
- `AfkStatsTrackerPanel.java` - Add history section UI

## Data Flow

1. **Startup**: SessionHistoryManager loads JSON from config → List<Session>
2. **Stop Session**: Plugin creates Session from current stats → save → update panel
3. **Rename**: Panel calls SessionHistoryManager.rename(id, newName) → saves
4. **Delete**: Panel calls SessionHistoryManager.delete(id) → saves
5. **Copy**: Panel formats Session as text → system clipboard

## Edge Cases

- **Empty session (0-1 clicks)**: Save with consistency 0
- **Corrupt JSON on load**: Log warning, start with empty history
- **History at limit**: Drop oldest when saving 21st session
- **Long session name**: Truncate display to ~30 chars, full name in tooltip

## Clipboard Format

```
Session: {name} | {date} | Consistency: {score} | Avg: {interval}ms | Clicks: {count}
```

Example:
```
Session: Fishing at Barbarian Village | Feb 21 2026 | Consistency: 85 | Avg: 45000ms | Clicks: 42
```
