package com.afkstatstracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class AfkStatsTrackerPanel extends PluginPanel
{
	private final AfkStatsTrackerPlugin plugin;
	private final SessionHistoryManager sessionHistoryManager;

	private Timer timer;
	private JButton startButton;
	private JButton stopButton;
	private JLabel consistencyValueLabel;
	private JLabel avgIntervalValueLabel;

	private JPanel historyContainer;
	private boolean historyExpanded = true;

	public AfkStatsTrackerPanel(AfkStatsTrackerPlugin plugin, SessionHistoryManager sessionHistoryManager)
	{
		this.plugin = plugin;
		this.sessionHistoryManager = sessionHistoryManager;

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
		JPanel statsPanel = new JPanel();
		statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
		statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

		JPanel consistencyPanel = createStatCard("Consistency",
			"Score (0-100) indicating how consistent click intervals are; higher means more regular timing.");
		consistencyValueLabel = (JLabel) ((BorderLayout) consistencyPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);

		JPanel avgIntervalPanel = createStatCard("Avg Click Interval",
			"Average time between clicks in ms");
		avgIntervalValueLabel = (JLabel) ((BorderLayout) avgIntervalPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);

		statsPanel.add(consistencyPanel);
		statsPanel.add(avgIntervalPanel);
		statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, statsPanel.getPreferredSize().height));

		// History section
		JPanel historySection = createHistorySection();

		contentPanel.add(buttonPanel);
		contentPanel.add(statsPanel);
		contentPanel.add(historySection);

		add(contentPanel, BorderLayout.NORTH);
	}

	private JPanel createStatCard(String title, String tooltip)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(8, 5, 8, 5)
		));
		card.setToolTipText(tooltip);

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(Color.GRAY);
		card.add(titleLabel, BorderLayout.NORTH);

		JLabel valueLabel = new JLabel("0");
		valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 20f));
		valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
		card.add(valueLabel, BorderLayout.CENTER);

		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
		return card;
	}

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
		JButton deleteButton = new JButton("Delete");

		deleteButton.addActionListener(e -> {
			sessionHistoryManager.deleteSession(session.getId());
			refreshHistoryPanel();
		});

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
		textField.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		Dimension pref = textField.getPreferredSize();
		textField.setPreferredSize(new Dimension(pref.width, pref.height + 5));
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

	public void updateStats()
	{
		consistencyValueLabel.setText(String.valueOf(plugin.getConsistency()));
		avgIntervalValueLabel.setText(String.format("%.0f ms", plugin.getAverageClickInterval()));
	}

	public void stopTimer()
	{
		if (timer != null)
		{
			timer.stop();
		}
	}
}
