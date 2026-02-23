AFK Stats Tracker

This RuneLite plugin tracks mouse clicks during AFK sessions in Old School RuneScape.

Tracked Stats:
- Consistency: A score from 0 to 100 showing how regular your click timing is. Higher scores mean more consistent intervals.
- Average Click Interval: The average time in milliseconds between clicks.

Purpose: To track afk metrics to compare between activies and methods. Similar to tracking DPS and Kills/hr.

## Contributing Data to the Wiki

Session data can be submitted to the [AFK Activity Tracker wiki page](https://oldschool.runescape.wiki/w/AFK_Activity_Tracker) so players can compare AFK methods.

1. Start a session, perform your AFK activity, and stop the session.
2. Rename the session to describe the activity (e.g., "Fishing Karambwan").
3. Click **Copy** in the plugin panel. This copies a pre-formatted Lua table row to your clipboard:
   ```lua
   { name = "Fishing Karambwan", group = "", consistency = 88, interval = 42000, clicks = 21, duration = 15 },
   ```
4. Edit the [data module](https://oldschool.runescape.wiki/w/Module:Chart_data/AFK_Activity_Tracker/data) on the wiki.
5. Paste your row at the bottom of the table, before the closing `}`.
6. Fill in the `group` field with an activity category (e.g., `"Bankstanding"`, `"Fishing"`, `"Salvaging"`). This groups your submission with similar activities on the chart.