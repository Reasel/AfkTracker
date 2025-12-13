package com.afkstatstracker;

import com.google.inject.Provides;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "AFK Stats Tracker"
)
public class AfkStatsTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private AfkStatsTrackerConfig config;

	@Inject
	private ClientToolbar clientToolbar;

    @Inject
    private MouseManager mouseManager;

    private MouseClickCounterListener mouseListener;

	private AfkStatsTrackerPanel panel;
	private NavigationButton navButton;

	private long startTime;
	private boolean isTracking = false;

	@Override
	protected void startUp() throws Exception
	{
		log.info("AFK Stats Tracker plugin started!");

		panel = new AfkStatsTrackerPanel(this);

		// Add to toolbar
		navButton = NavigationButton.builder()
			.tooltip("AFK Stats Tracker")
			.icon(ImageUtil.loadImageResource(getClass(), "kofi.png")) // Need to add icon
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

        mouseListener = new MouseClickCounterListener(client);
        mouseManager.registerMouseListener(mouseListener);

	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("AFK Stats Tracker plugin stopped!");

		clientToolbar.removeNavigation(navButton);

	       mouseManager.unregisterMouseListener(mouseListener);
	       mouseListener = null;

	       panel.stopTimer();
	}

	public void startSession()
	{
		mouseListener.resetMouseClickCounterListener();
		startTime = System.currentTimeMillis();
		isTracking = true;
	}

	public void stopSession()
	{
		isTracking = false;
	}

	public long getConsistency()
	{
		return computeConsistency(mouseListener.getClickCounter());
	}

    public long computeConsistency(List<Long> timestamps)
    {
        int n = timestamps.size();
        if (n < 2) return 0;

        List<Long> sorted = new ArrayList<>(timestamps);
        Collections.sort(sorted);

        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < n; i++)
        {
            intervals.add((double) (sorted.get(i) - sorted.get(i - 1)));
        }

        double totalSpan = sorted.get(n - 1) - sorted.get(0);
        int numIntervals = intervals.size();
        double meanInterval = totalSpan / numIntervals;

        double sumSqDiff = 0;
        for (double interval : intervals)
        {
            sumSqDiff += Math.pow(interval - meanInterval, 2);
        }
        double variance = sumSqDiff / (numIntervals - 1);
        double stdDev = Math.sqrt(variance);

		double cv = stdDev / meanInterval;
        return (long)(1 / (1 + cv) * 100);
    }

    public double getAverageClickInterval()
    {
        List<Long> timestamps = mouseListener.getClickCounter();
        int n = timestamps.size();
        if (n < 2) return 0.0;

        List<Long> sorted = new ArrayList<>(timestamps);
        Collections.sort(sorted);

        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < n; i++)
        {
            intervals.add((double) (sorted.get(i) - sorted.get(i - 1)));
        }

        double sum = 0.0;
        for (double interval : intervals)
        {
            sum += interval;
        }
        return sum / intervals.size();
    }

	@Provides
	AfkStatsTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AfkStatsTrackerConfig.class);
	}
}