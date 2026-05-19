package ch.autotyper.toolwindow;

import ch.autotyper.repository.CodeSnippetRepository;
import ch.autotyper.service.TypingSimulatorService;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;

/**
 * The main UI panel for the AutoTyper tool window.
 * Contains: snippet list, speed slider, control buttons, status display.
 */
public class TypingToolWindowPanel {

    private final Project project;
    private JPanel mainPanel;
    private DefaultListModel<String> listModel;
    private JList<String> snippetList;
    private JSlider speedSlider;
    private JLabel statusLabel;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    private JButton startButton;
    private JButton pauseButton;
    private JButton stopButton;

    private TypingSimulatorService typingService;

    public TypingToolWindowPanel(Project project) {
        this.project = project;
        this.typingService = new TypingSimulatorService(project);

        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // === TOP: Folder selection ===
        JPanel folderPanel = createFolderPanel();

        // === CENTER: Snippet list ===
        listModel = new DefaultListModel<>();
        snippetList = new JList<>(listModel);
        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.setToolTipText("Select a Java class to type");

        JScrollPane listScroll = new JScrollPane(snippetList);
        listScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "📄 Java Classes",
                TitledBorder.LEFT, TitledBorder.TOP));
        listScroll.setPreferredSize(new Dimension(250, 200));

        // === SPEED SLIDER ===
        JPanel speedPanel = createSpeedPanel();

        // === CONTROLS ===
        JPanel controlPanel = createControlPanel();

        // === STATUS ===
        JPanel statusPanel = new JPanel(new BorderLayout(4, 4));
        statusLabel = new JLabel("Ready");
        progressLabel = new JLabel("");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(progressLabel, BorderLayout.SOUTH);
        statusPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Status",
                TitledBorder.LEFT, TitledBorder.TOP));

        // === LAYOUT ===
        JPanel topSection = new JPanel(new BorderLayout(4, 4));
        topSection.add(folderPanel, BorderLayout.NORTH);
        topSection.add(listScroll, BorderLayout.CENTER);

        JPanel bottomSection = new JPanel(new BorderLayout(4, 4));
        bottomSection.add(speedPanel, BorderLayout.NORTH);
        bottomSection.add(controlPanel, BorderLayout.CENTER);
        bottomSection.add(statusPanel, BorderLayout.SOUTH);

        mainPanel.add(topSection, BorderLayout.CENTER);
        mainPanel.add(bottomSection, BorderLayout.SOUTH);

        // Setup typing status listener
        setupStatusListener();

        // Initial load
        refreshSnippetList();
    }

    public JPanel getContent() {
        return mainPanel;
    }

    // =========================================================================
    // UI Creation
    // =========================================================================

    private JPanel createFolderPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 0));

        CodeSnippetRepository repo = CodeSnippetRepository.getInstance();
        JLabel pathLabel = new JLabel(shortenPath(repo.getSnippetsDirectory().toString()));
        pathLabel.setToolTipText(repo.getSnippetsDirectory().toString());

        JButton browseBtn = new JButton("📁 Browse");
        browseBtn.setToolTipText("Select snippets folder");
        browseBtn.addActionListener(e -> {
            VirtualFile chosen = FileChooser.chooseFile(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    project, null);
            if (chosen != null) {
                Path newPath = Path.of(chosen.getPath());
                repo.setSnippetsDirectory(newPath);
                pathLabel.setText(shortenPath(newPath.toString()));
                pathLabel.setToolTipText(newPath.toString());
                refreshSnippetList();
            }
        });

        JButton refreshBtn = new JButton("🔄");
        refreshBtn.setToolTipText("Reload snippets");
        refreshBtn.addActionListener(e -> {
            repo.reload();
            refreshSnippetList();
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        btnPanel.add(refreshBtn);
        btnPanel.add(browseBtn);

        panel.add(new JLabel("Folder: "), BorderLayout.WEST);
        panel.add(pathLabel, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.EAST);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        return panel;
    }

    private JPanel createSpeedPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "⚡ Typing Speed",
                TitledBorder.LEFT, TitledBorder.TOP));

        // Slider: 30 WPM (slow) to 600 WPM (very fast)
        speedSlider = new JSlider(JSlider.HORIZONTAL, 30, 600, 150);
        speedSlider.setMajorTickSpacing(100);
        speedSlider.setMinorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(30, new JLabel("Slow"));
        labels.put(150, new JLabel("Normal"));
        labels.put(300, new JLabel("Fast"));
        labels.put(600, new JLabel("Turbo"));
        speedSlider.setLabelTable(labels);

        speedSlider.addChangeListener(e -> {
            typingService.setSpeed(speedSlider.getValue());
        });

        JLabel wpmLabel = new JLabel("150 WPM");
        speedSlider.addChangeListener(e -> {
            wpmLabel.setText(speedSlider.getValue() + " WPM");
        });

        panel.add(speedSlider, BorderLayout.CENTER);
        panel.add(wpmLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));

        startButton = new JButton("▶ Start");
        pauseButton = new JButton("⏸ Pause");
        stopButton = new JButton("⏹ Stop");

        startButton.setToolTipText("Start typing the selected snippet");
        pauseButton.setToolTipText("Pause/Resume typing");
        stopButton.setToolTipText("Stop typing");

        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startTyping());
        pauseButton.addActionListener(e -> togglePause());
        stopButton.addActionListener(e -> stopTyping());

        panel.add(startButton);
        panel.add(pauseButton);
        panel.add(stopButton);

        return panel;
    }

    // =========================================================================
    // Actions
    // =========================================================================

    private void startTyping() {
        String selected = snippetList.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("⚠️ Please select a snippet first!");
            return;
        }

        String content = CodeSnippetRepository.getInstance().getSnippetContent(selected);
        if (content.isEmpty()) {
            statusLabel.setText("⚠️ Selected snippet is empty!");
            return;
        }

        // Apply current speed setting
        typingService.setSpeed(speedSlider.getValue());

        // Update UI state
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
        snippetList.setEnabled(false);

        // Show progress bar
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setMaximum(content.length());

        // Start typing
        typingService.startTyping(content);
    }

    private void togglePause() {
        typingService.togglePause();
        if (typingService.isPaused()) {
            pauseButton.setText("▶ Resume");
        } else {
            pauseButton.setText("⏸ Pause");
        }
    }

    private void stopTyping() {
        typingService.stop();
        resetControls();
    }

    private void resetControls() {
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        pauseButton.setText("⏸ Pause");
        snippetList.setEnabled(true);
        progressBar.setVisible(false);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void refreshSnippetList() {
        listModel.clear();
        List<String> names = CodeSnippetRepository.getInstance().getSnippetNames();
        for (String name : names) {
            listModel.addElement(name);
        }
        if (!names.isEmpty()) {
            snippetList.setSelectedIndex(0);
        }
        statusLabel.setText(names.size() + " snippet(s) loaded");
    }

    private void setupStatusListener() {
        typingService.setStatusListener(new TypingSimulatorService.TypingStatusListener() {
            @Override
            public void onStatusChanged(String status) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(status));
            }

            @Override
            public void onProgressChanged(int current, int total) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(current);
                    int percent = (int) ((current / (double) total) * 100);
                    progressLabel.setText(current + " / " + total + " chars (" + percent + "%)");
                });
            }

            @Override
            public void onTypingFinished() {
                SwingUtilities.invokeLater(() -> resetControls());
            }
        });
    }

    private String shortenPath(String path) {
        if (path.length() > 35) {
            return "..." + path.substring(path.length() - 32);
        }
        return path;
    }
}
