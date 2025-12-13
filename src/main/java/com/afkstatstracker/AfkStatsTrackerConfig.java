package com.afkstatstracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("afkStatsTracker")
public interface AfkStatsTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "showPanel",
		name = "Show Panel",
		description = "Show the AFK Stats Tracker panel"
	)
	default boolean showPanel()
	{
		return true;
	}
}