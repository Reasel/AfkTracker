# Session History Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add persistent session history with rename, copy, and delete capabilities to the AFK Stats Tracker plugin.

**Architecture:** New Session data class holds session data, SessionHistoryManager handles persistence via RuneLite ConfigManager with JSON serialization. Panel gets a collapsible history section with inline editing.

**Tech Stack:** Java 11, RuneLite Client API, Gson (for JSON), Swing UI

---

## Task 1: Add Gson Dependency

**Files:**
- Modify: `build.gradle:18-27`

**Step 1: Add Gson to dependencies**

In `build.gradle`, add Gson to the dependencies block:

```gradle
dependencies {
    compileOnly group: 'net.runelite', name:'client', version: runeLiteVersion

    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'

    implementation 'com.google.code.gson:gson:2.10.1'

    testImplementation 'junit:junit:4.12'
    testImplementation group: 'net.runelite', name:'client', version: runeLiteVersion
    testImplementation group: 'net.runelite', name:'jshell', version: runeLiteVersion
}
```

**Step 2: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add build.gradle
git commit -m "feat: add Gson dependency for JSON serialization"
```

---

## Task 2: Create Session Data Class

**Files:**
- Create: `src/main/java/com/afkstatstracker/Session.java`
- Create: `src/test/java/com/afkstatstracker/SessionTest.java`

**Step 1: Write the test**

Create `src/test/java/com/afkstatstracker/SessionTest.java`:

```java
package com.afkstatstracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionTest
{
    @Test
    public void testSessionCreation()
    {
        Session session = new Session(
            "test-id",
            "Test Session",
            1000L,
            2000L,
            42,
            85L,
            45000.0
        );

        assertEquals("test-id", session.getId());
        assertEquals("Test Session", session.getName());
        assertEquals(1000L, session.getStartTime());
        assertEquals(2000L, session.getEndTime());
        assertEquals(42, session.getClickCount());
        assertEquals(85L, session.getConsistencyScore());
        assertEquals(45000.0, session.getAvgInterval(), 0.001);
    }

    @Test
    public void testSessionSetName()
    {
        Session session = new Session(
            "test-id",
            "Original Name",
            1000L,
            2000L,
            10,
            50L,
            30000.0
        );

        session.setName("New Name");
        assertEquals("New Name", session.getName());
    }

    @Test
    public void testToClipboardText()
    {
        Session session = new Session(
            "test-id",
            "Fishing Session",
            1708523400000L,  // Feb 21 2024 14:30:00 UTC
            1708527000000L,
            42,
            85L,
            45000.0
        );

        String clipboardText = session.toClipboardText();

        assertTrue(clipboardText.contains("Fishing Session"));
        assertTrue(clipboardText.contains("Consistency: 85"));
        assertTrue(clipboardText.contains("Avg: 45000ms"));
        assertTrue(clipboardText.contains("Clicks: 42"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests SessionTest`
Expected: FAIL - Session class does not exist

**Step 3: Write the Session class**

Create `src/main/java/com/afkstatstracker/Session.java`:

```java
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
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests SessionTest`
Expected: BUILD SUCCESSFUL, all tests pass

**Step 5: Commit**

```bash
git add src/main/java/com/afkstatstracker/Session.java src/test/java/com/afkstatstracker/SessionTest.java
git commit -m "feat: add Session data class with clipboard formatting"
```

---

## Task 3: Create SessionHistoryManager

**Files:**
- Create: `src/main/java/com/afkstatstracker/SessionHistoryManager.java`
- Create: `src/test/java/com/afkstatstracker/SessionHistoryManagerTest.java`

**Step 1: Write the tests**

Create `src/test/java/com/afkstatstracker/SessionHistoryManagerTest.java`:

```java
package com.afkstatstracker;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class SessionHistoryManagerTest
{
    private SessionHistoryManager manager;
    private TestConfigStorage storage;

    @Before
    public void setUp()
    {
        storage = new TestConfigStorage();
        manager = new SessionHistoryManager(storage, new Gson());
    }

    @Test
    public void testAddSession()
    {
        Session session = createTestSession("id1", "Session 1");
        manager.addSession(session);

        List<Session> sessions = manager.getSessions();
        assertEquals(1, sessions.size());
        assertEquals("Session 1", sessions.get(0).getName());
    }

    @Test
    public void testRenameSession()
    {
        Session session = createTestSession("id1", "Original");
        manager.addSession(session);

        manager.renameSession("id1", "Renamed");

        assertEquals("Renamed", manager.getSessions().get(0).getName());
    }

    @Test
    public void testDeleteSession()
    {
        manager.addSession(createTestSession("id1", "Session 1"));
        manager.addSession(createTestSession("id2", "Session 2"));

        manager.deleteSession("id1");

        List<Session> sessions = manager.getSessions();
        assertEquals(1, sessions.size());
        assertEquals("id2", sessions.get(0).getId());
    }

    @Test
    public void testMaxSessionsLimit()
    {
        for (int i = 0; i < 25; i++)
        {
            manager.addSession(createTestSession("id" + i, "Session " + i));
        }

        List<Session> sessions = manager.getSessions();
        assertEquals(20, sessions.size());
        // Oldest sessions (0-4) should be dropped, newest (5-24) kept
        assertEquals("id5", sessions.get(0).getId());
        assertEquals("id24", sessions.get(19).getId());
    }

    @Test
    public void testPersistence()
    {
        manager.addSession(createTestSession("id1", "Persisted"));

        // Create new manager with same storage
        SessionHistoryManager newManager = new SessionHistoryManager(storage, new Gson());

        List<Session> sessions = newManager.getSessions();
        assertEquals(1, sessions.size());
        assertEquals("Persisted", sessions.get(0).getName());
    }

    @Test
    public void testCorruptJsonReturnsEmptyList()
    {
        storage.setData("not valid json [[[");
        SessionHistoryManager newManager = new SessionHistoryManager(storage, new Gson());

        List<Session> sessions = newManager.getSessions();
        assertTrue(sessions.isEmpty());
    }

    private Session createTestSession(String id, String name)
    {
        return new Session(id, name, 1000L, 2000L, 10, 50L, 30000.0);
    }

    // Simple test storage implementation
    static class TestConfigStorage implements SessionHistoryManager.ConfigStorage
    {
        private String data = null;

        @Override
        public String load()
        {
            return data;
        }

        @Override
        public void save(String json)
        {
            this.data = json;
        }

        public void setData(String data)
        {
            this.data = data;
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests SessionHistoryManagerTest`
Expected: FAIL - SessionHistoryManager class does not exist

**Step 3: Write the SessionHistoryManager class**

Create `src/main/java/com/afkstatstracker/SessionHistoryManager.java`:

```java
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
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests SessionHistoryManagerTest`
Expected: BUILD SUCCESSFUL, all tests pass

**Step 5: Commit**

```bash
git add src/main/java/com/afkstatstracker/SessionHistoryManager.java src/test/java/com/afkstatstracker/SessionHistoryManagerTest.java
git commit -m "feat: add SessionHistoryManager with persistence and 20-session limit"
```

---

## Task 4: Integrate SessionHistoryManager into Plugin

**Files:**
- Modify: `src/main/java/com/afkstatstracker/AfkStatsTrackerPlugin.java`

**Step 1: Add ConfigStorage implementation and wire up manager**

Modify `AfkStatsTrackerPlugin.java` to add these imports at the top:

```java
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
```

**Step 2: Add fields after existing fields (around line 43)**

Add after `private boolean isTracking = false;`:

```java
	@Inject
	private ConfigManager configManager;

	private SessionHistoryManager sessionHistoryManager;
```

**Step 3: Create ConfigStorage implementation and initialize manager in startUp()**

Add at the beginning of `startUp()` method, before the panel creation:

```java
		Gson gson = new Gson();
		SessionHistoryManager.ConfigStorage storage = new SessionHistoryManager.ConfigStorage()
		{
			private static final String CONFIG_GROUP = "afkStatsTracker";
			private static final String CONFIG_KEY = "sessionHistory";

			@Override
			public String load()
			{
				return configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
			}

			@Override
			public void save(String json)
			{
				configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, json);
			}
		};
		sessionHistoryManager = new SessionHistoryManager(storage, gson);
```

**Step 4: Update panel constructor call**

Change `panel = new AfkStatsTrackerPanel(this);` to:

```java
		panel = new AfkStatsTrackerPanel(this, sessionHistoryManager);
```

**Step 5: Add getter for click count**

Add this method after `getAverageClickInterval()`:

```java
	public int getClickCount()
	{
		return mouseListener.getClickCounter().size();
	}
```

**Step 6: Modify stopSession() to save the session**

Replace the `stopSession()` method:

```java
	public void stopSession()
	{
		if (!isTracking)
		{
			return;
		}

		long endTime = System.currentTimeMillis();
		String id = UUID.randomUUID().toString();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String name = "Session " + dateFormat.format(new Date(startTime));

		Session session = new Session(
			id,
			name,
			startTime,
			endTime,
			getClickCount(),
			getConsistency(),
			getAverageClickInterval()
		);

		sessionHistoryManager.addSession(session);
		isTracking = false;
	}
```

**Step 7: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (panel constructor change will cause error, that's expected - we fix in next task)

**Step 8: Commit (after Task 5 panel update)**

This commit will happen together with Task 5.

---

## Task 5: Add History Section to Panel

**Files:**
- Modify: `src/main/java/com/afkstatstracker/AfkStatsTrackerPanel.java`

**Step 1: Update imports**

Replace the imports section with:

```java
package com.afkstatstracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
```

**Step 2: Update fields**

Replace the fields section with:

```java
	private final AfkStatsTrackerPlugin plugin;
	private final SessionHistoryManager sessionHistoryManager;

	private Timer timer;
	private JButton startButton;
	private JButton stopButton;
	private JLabel consistencyLabel;
	private JLabel averageClickIntervalLabel;

	private JPanel historyContainer;
	private boolean historyExpanded = true;
```

**Step 3: Update constructor signature**

Change constructor to:

```java
	public AfkStatsTrackerPanel(AfkStatsTrackerPlugin plugin, SessionHistoryManager sessionHistoryManager)
	{
		this.plugin = plugin;
		this.sessionHistoryManager = sessionHistoryManager;
```

**Step 4: Replace the rest of the constructor body**

Replace from `setLayout(new BorderLayout());` through end of constructor:

```java
		setLayout(new BorderLayout());

		timer = new Timer(1000, e -> updateStats());
		timer.setRepeats(true);

		// Main content panel
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

		// Button panel
		JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
		startButton = new JButton("Start Session");
		stopButton = new JButton("Stop Session");
		stopButton.setEnabled(false);

		startButton.addActionListener(e -> {
			plugin.startSession();
			updateStats();
			timer.start();
			startButton.setEnabled(false);
			stopButton.setEnabled(true);
		});

		stopButton.addActionListener(e -> {
			timer.stop();
			plugin.stopSession();
			startButton.setEnabled(true);
			stopButton.setEnabled(false);
			updateStats();
			refreshHistoryPanel();
		});

		buttonPanel.add(startButton);
		buttonPanel.add(stopButton);
		buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonPanel.getPreferredSize().height));

		// Stats panel
		JPanel statsPanel = new JPanel(new GridLayout(2, 1));
		consistencyLabel = new JLabel("Consistency: 0");
		averageClickIntervalLabel = new JLabel("Average Click Interval: 0");

		consistencyLabel.setToolTipText("Score (0-100) indicating how consistent click intervals are; higher means more regular timing.");
		averageClickIntervalLabel.setToolTipText("Average time between clicks in ms");

		statsPanel.add(consistencyLabel);
		statsPanel.add(averageClickIntervalLabel);
		statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, statsPanel.getPreferredSize().height));

		// History section
		JPanel historySection = createHistorySection();

		contentPanel.add(buttonPanel);
		contentPanel.add(statsPanel);
		contentPanel.add(historySection);

		add(contentPanel, BorderLayout.NORTH);
	}
```

**Step 5: Add history section methods after constructor**

Add these methods after the constructor:

```java
	private JPanel createHistorySection()
	{
		JPanel section = new JPanel(new BorderLayout());
		section.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		// Header with toggle
		JPanel header = new JPanel(new BorderLayout());
		JLabel headerLabel = new JLabel("▼ Session History");
		headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		headerLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				historyExpanded = !historyExpanded;
				headerLabel.setText((historyExpanded ? "▼" : "▶") + " Session History");
				historyContainer.setVisible(historyExpanded);
				revalidate();
			}
		});
		header.add(headerLabel, BorderLayout.WEST);

		// History container
		historyContainer = new JPanel();
		historyContainer.setLayout(new BoxLayout(historyContainer, BoxLayout.Y_AXIS));

		JScrollPane scrollPane = new JScrollPane(historyContainer);
		scrollPane.setPreferredSize(new Dimension(0, 300));
		scrollPane.setBorder(null);

		section.add(header, BorderLayout.NORTH);
		section.add(scrollPane, BorderLayout.CENTER);

		refreshHistoryPanel();
		return section;
	}

	public void refreshHistoryPanel()
	{
		historyContainer.removeAll();

		for (Session session : sessionHistoryManager.getSessions())
		{
			historyContainer.add(createSessionRow(session));
		}

		if (sessionHistoryManager.getSessions().isEmpty())
		{
			JLabel emptyLabel = new JLabel("No sessions recorded");
			emptyLabel.setForeground(Color.GRAY);
			historyContainer.add(emptyLabel);
		}

		historyContainer.revalidate();
		historyContainer.repaint();
	}

	private JPanel createSessionRow(Session session)
	{
		JPanel row = new JPanel(new BorderLayout(5, 2));
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(5, 5, 5, 5)
		));

		// Name (editable on click)
		String displayName = session.getName();
		if (displayName.length() > 30)
		{
			displayName = displayName.substring(0, 27) + "...";
		}

		JLabel nameLabel = new JLabel(displayName);
		nameLabel.setToolTipText(session.getName());
		nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		nameLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				makeEditable(row, session, nameLabel);
			}
		});

		// Stats line
		JLabel statsLabel = new JLabel(String.format("%d | %.0fms | %d clicks",
			session.getConsistencyScore(),
			session.getAvgInterval(),
			session.getClickCount()));
		statsLabel.setForeground(Color.GRAY);

		// Buttons
		JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		JButton copyButton = new JButton("Copy");
		JButton deleteButton = new JButton("Delete");

		copyButton.addActionListener(e -> {
			StringSelection selection = new StringSelection(session.toClipboardText());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
		});

		deleteButton.addActionListener(e -> {
			sessionHistoryManager.deleteSession(session.getId());
			refreshHistoryPanel();
		});

		buttonRow.add(copyButton);
		buttonRow.add(deleteButton);

		JPanel textPanel = new JPanel(new GridLayout(2, 1));
		textPanel.add(nameLabel);
		textPanel.add(statsLabel);

		row.add(textPanel, BorderLayout.CENTER);
		row.add(buttonRow, BorderLayout.SOUTH);

		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	private void makeEditable(JPanel row, Session session, JLabel nameLabel)
	{
		JTextField textField = new JTextField(session.getName());
		textField.selectAll();

		Runnable saveAndRestore = () -> {
			String newName = textField.getText().trim();
			if (!newName.isEmpty() && !newName.equals(session.getName()))
			{
				sessionHistoryManager.renameSession(session.getId(), newName);
			}
			refreshHistoryPanel();
		};

		textField.addActionListener(e -> saveAndRestore.run());
		textField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				saveAndRestore.run();
			}
		});
		textField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					refreshHistoryPanel();
				}
			}
		});

		JPanel textPanel = (JPanel) row.getComponent(0);
		textPanel.remove(nameLabel);
		textPanel.add(textField, 0);
		textPanel.revalidate();
		textField.requestFocusInWindow();
	}
```

**Step 6: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 7: Commit Task 4 and Task 5 together**

```bash
git add src/main/java/com/afkstatstracker/AfkStatsTrackerPlugin.java src/main/java/com/afkstatstracker/AfkStatsTrackerPanel.java
git commit -m "feat: integrate session history into plugin and panel UI"
```

---

## Task 6: Manual Testing

**Step 1: Launch the plugin**

Run the plugin in IntelliJ by running `AfkStatsTrackerTest.main()`.

**Step 2: Test session creation**

1. Click "Start Session"
2. Click around in the game window a few times
3. Click "Stop Session"
4. Verify session appears in history with auto-generated name

**Step 3: Test rename**

1. Click on a session name
2. Type a new name
3. Press Enter
4. Verify name is updated

**Step 4: Test copy**

1. Click "Copy" on a session
2. Paste somewhere (e.g., notepad)
3. Verify format: `Session: {name} | {date} | Consistency: {score} | Avg: {interval}ms | Clicks: {count}`

**Step 5: Test delete**

1. Click "Delete" on a session
2. Verify session is removed from list

**Step 6: Test persistence**

1. Create a few sessions
2. Restart the plugin (stop and start via RuneLite plugin manager)
3. Verify sessions are still there

**Step 7: Commit if any fixes needed**

```bash
git add -A
git commit -m "fix: address issues found in manual testing"
```

---

## Task 7: Final Cleanup and Feature Branch Complete

**Step 1: Verify all tests pass**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

**Step 2: Verify build is clean**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 3: Review git log**

Run: `git log --oneline feature/session-history ^master`
Expected: See all feature commits

**Step 4: Ready for merge/PR**

Feature branch is complete and ready for review.
