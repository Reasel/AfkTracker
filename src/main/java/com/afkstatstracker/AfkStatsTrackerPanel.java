package com.afkstatstracker;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.runelite.client.ui.PluginPanel;

public class AfkStatsTrackerPanel extends PluginPanel
{
	private final AfkStatsTrackerPlugin plugin;

	private Timer timer;
	private JButton startButton;
	private JButton stopButton;
	private JLabel consistencyLabel;
    private JLabel averageClickIntervalLabel;

	public AfkStatsTrackerPanel(AfkStatsTrackerPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());

		timer = new Timer(1000, e -> updateStats());
		timer.setRepeats(true);

		JPanel buttonPanel = new JPanel(new GridLayout(3, 3));
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
		});

		buttonPanel.add(startButton);
		buttonPanel.add(stopButton);

		JPanel statsPanel = new JPanel(new GridLayout(5, 1));
		consistencyLabel = new JLabel("Consistency : 0-100");
        averageClickIntervalLabel = new JLabel("Average Click Interval : 0");

        consistencyLabel.setToolTipText("Score (0-100) indicating how consistent click intervals are; higher means more regular timing.");
        averageClickIntervalLabel.setToolTipText("Average time between clicks in ms");

		statsPanel.add(consistencyLabel);
        statsPanel.add(averageClickIntervalLabel);

		add(buttonPanel, BorderLayout.NORTH);
		add(statsPanel, BorderLayout.CENTER);
	}

	public void updateStats()
    {
        consistencyLabel.setText("Consistency: " + plugin.getConsistency());
        averageClickIntervalLabel.setText("Average Click Interval: " + String.format("%.0f", plugin.getAverageClickInterval()));
	}

	public void stopTimer()
	{
		if (timer != null)
		{
			timer.stop();
		}
	}
}