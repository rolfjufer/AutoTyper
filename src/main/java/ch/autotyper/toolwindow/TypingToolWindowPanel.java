package ch.autotyper.toolwindow;

import ch.autotyper.repository.CodeSnippetRepository;
import ch.autotyper.service.FileCreationService;
import ch.autotyper.service.TypingSimulatorService;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;

/**
 * Main UI panel for the AutoTyper tool window.
 * Supports structured step-by-step typing, auto file creation, and spacebar navigation.
 */
public class TypingToolWindowPanel {

    private final Project project;
    private JPanel mainPanel;
    private DefaultListModel<String> listModel;
    private JBList<String> snippetList;
    private JSlider speedSlider;
    private JLabel statusLabel;
    private JLabel progressLabel;
    private JLabel stepLabel;
    private JProgressBar progressBar;
    private JButton startButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton nextStepButton;
    private JCheckBox autoPauseCheckbox;
    private JCheckBox autoCreateFileCheckbox;

    private TypingSimulatorService typingService;
    private FileCreationService fileCreationService;

    public TypingToolWindowPanel(Project project) {
        this.project = project;
        this.typingService = project.getService(TypingSimulatorService.class);
        this.fileCreationService = new FileCreationService(project);
        buildUI();
        setupStatusListener();
        
        refreshSnippetList();
    }

    public JPanel getContent() {
        return mainPanel;
    }

    // =========================================================================
    // UI Construction
    // =========================================================================

    private void buildUI() {
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // TOP: Folder selection
        JPanel folderPanel = createFolderPanel();

        // CENTER: Snippet list
        listModel = new DefaultListModel<>();
        snippetList = new JBList<>(listModel);
        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.setToolTipText("Select a Java class to type (use // @step N markers for structured typing)");

        JScrollPane listScroll = new JScrollPane(snippetList);
        listScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "📄 Java Classes",
                TitledBorder.LEFT, TitledBorder.TOP));

        // BOTTOM section
        JPanel bottomSection = new JPanel();
        bottomSection.setLayout(new BoxLayout(bottomSection, BoxLayout.Y_AXIS));
        bottomSection.add(createSpeedPanel());
        bottomSection.add(Box.createVerticalStrut(4));
        bottomSection.add(createOptionsPanel());
        bottomSection.add(Box.createVerticalStrut(4));
        bottomSection.add(createControlPanel());
        bottomSection.add(Box.createVerticalStrut(4));
        bottomSection.add(createStatusPanel());
        bottomSection.add(Box.createVerticalStrut(4));
        bottomSection.add(createInfoPanel());

        // LAYOUT
        JPanel topSection = new JPanel(new BorderLayout(4, 4));
        topSection.add(folderPanel, BorderLayout.NORTH);
        topSection.add(listScroll, BorderLayout.CENTER);

        mainPanel.add(topSection, BorderLayout.CENTER);
        mainPanel.add(bottomSection, BorderLayout.SOUTH);
    }

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
        refreshBtn.setToolTipText("Reload snippets from folder");
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

        JLabel wpmLabel = new JLabel("150 WPM");
        speedSlider.addChangeListener(e -> {
            wpmLabel.setText(speedSlider.getValue() + " WPM");
            typingService.setSpeed(speedSlider.getValue());
        });

        panel.add(speedSlider, BorderLayout.CENTER);
        panel.add(wpmLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 4, 2));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "⚙️ Options",
                TitledBorder.LEFT, TitledBorder.TOP));

        autoPauseCheckbox = new JCheckBox("Auto-pause between steps", true);
        autoPauseCheckbox.setToolTipText("Pause automatically after each @step block completes");
        autoPauseCheckbox.addActionListener(e ->
                typingService.setAutoPauseBetweenSteps(autoPauseCheckbox.isSelected()));

        autoCreateFileCheckbox = new JCheckBox("Auto-create file in project", true);
        autoCreateFileCheckbox.setToolTipText(
                "Automatically create the Java file in the correct package directory and open it in the editor");

        panel.add(autoPauseCheckbox);
        panel.add(autoCreateFileCheckbox);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));

        startButton = new JButton("▶ Start");
        pauseButton = new JButton("⏸ Pause");
        nextStepButton = new JButton("⏭ Next Step");
        stopButton = new JButton("⏹ Stop");

        startButton.setToolTipText("Start typing the selected snippet");
        pauseButton.setToolTipText("Pause/Resume typing");
        nextStepButton.setToolTipText("Continue to next step (or press Spacebar)");
        stopButton.setToolTipText("Stop typing completely");

        pauseButton.setEnabled(false);
        nextStepButton.setEnabled(false);
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startTyping());
        pauseButton.addActionListener(e -> togglePause());
        nextStepButton.addActionListener(e -> nextStep());
        stopButton.addActionListener(e -> stopTyping());

        panel.add(startButton);
        panel.add(pauseButton);
        panel.add(nextStepButton);
        panel.add(stopButton);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "📊 Status",
                TitledBorder.LEFT, TitledBorder.TOP));

        statusLabel = new JLabel("Ready — select a snippet and click Start");
        stepLabel = new JLabel("");
        progressLabel = new JLabel("");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        JPanel labelsPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        labelsPanel.add(statusLabel);
        labelsPanel.add(stepLabel);
        labelsPanel.add(progressLabel);

        panel.add(labelsPanel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JLabel infoLabel = new JLabel("<html><small>💡 Press <b>Alt+N</b> to advance to next step</small></html>");
        infoLabel.setForeground(Color.GRAY);
        panel.add(infoLabel);
        return panel;
    }

    // =========================================================================
    // Keyboard Shortcuts
    // =========================================================================


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

        // Auto-create file if option is enabled
        if (autoCreateFileCheckbox.isSelected()) {
            statusLabel.setText("📁 Creating file...");
            boolean created = fileCreationService.createAndOpenFile(selected, content);
            if (!created) {
                statusLabel.setText("⚠️ Failed to create file! Check project structure.");
                return;
            }

            // Small delay to let the editor open before typing starts
            Timer delayTimer = new Timer(500, e -> {
                ((Timer) e.getSource()).stop();
                beginTyping(content);
            });
            delayTimer.setRepeats(false);
            delayTimer.start();
        } else {
            beginTyping(content);
        }
    }

    private void beginTyping(String content) {
        // Apply settings
        typingService.setSpeed(speedSlider.getValue());
        typingService.setAutoPauseBetweenSteps(autoPauseCheckbox.isSelected());

        // UI state
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
        nextStepButton.setEnabled(false);
        snippetList.setEnabled(false);
        autoCreateFileCheckbox.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);

        // Start
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

    private void nextStep() {
        nextStepButton.setEnabled(false);
        pauseButton.setEnabled(true);
        typingService.nextStep();
    }

    private void stopTyping() {
        typingService.stop();
        resetControls();
    }

    private void resetControls() {
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        pauseButton.setText("⏸ Pause");
        nextStepButton.setEnabled(false);
        stopButton.setEnabled(false);
        snippetList.setEnabled(true);
        autoCreateFileCheckbox.setEnabled(true);
        progressBar.setVisible(false);
        stepLabel.setText("");
        progressLabel.setText("");
    }

    // =========================================================================
    // Status Listener
    // =========================================================================

    private void setupStatusListener() {
        typingService.setStatusListener(new TypingSimulatorService.TypingStatusListener() {
            @Override
            public void onStatusChanged(String status) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(status));
            }

            @Override
            public void onProgressChanged(int currentStep, int totalSteps, int charsCurrent, int charsTotal) {
                SwingUtilities.invokeLater(() -> {
                    stepLabel.setText("Step " + currentStep + " of " + totalSteps);
                    int percent = (int) ((charsCurrent / (double) charsTotal) * 100);
                    progressBar.setValue(percent);
                    progressLabel.setText(charsCurrent + " / " + charsTotal + " chars (" + percent + "%)");
                });
            }

            @Override
            public void onStepPaused(int completedStep, int nextStep, int totalSteps) {
                SwingUtilities.invokeLater(() -> {
                    nextStepButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    stepLabel.setText("Step " + completedStep + "/" + totalSteps +
                            " done → press Alt+N or click Next Step");
                });
            }

            @Override
            public void onTypingFinished() {
                SwingUtilities.invokeLater(() -> resetControls());
            }
        });
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

    private String shortenPath(String path) {
        if (path.length() > 35) {
            return "..." + path.substring(path.length() - 32);
        }
        return path;
    }
}
