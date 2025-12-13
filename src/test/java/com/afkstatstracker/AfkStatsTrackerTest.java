package com.afkstatstracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AfkStatsTrackerTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AfkStatsTrackerPlugin.class);
		RuneLite.main(args);
	}
}