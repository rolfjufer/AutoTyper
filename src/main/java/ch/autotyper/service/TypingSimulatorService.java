package ch.autotyper.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates typing text character by character into the active editor.
 * Supports adjustable speed, pause/resume, and stop functionality.
 */
public class TypingSimulatorService {

    private static final Logger LOG = Logger.getInstance(TypingSimulatorService.class);
    private static final Random RANDOM = new Random();

    private final Project project;

    // Typing state
    private Timer typingTimer;
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    // Speed settings (delay in ms between characters)
    private int baseDelayMs = 50;       // Base delay between characters
    private int variationMs = 30;       // Random variation (+/-)
    private int newlineDelayMs = 200;   // Extra delay after newline

    // Listener for status updates
    private TypingStatusListener statusListener;

    public interface TypingStatusListener {
        void onStatusChanged(String status);
        void onProgressChanged(int current, int total);
        void onTypingFinished();
    }

    public TypingSimulatorService(Project project) {
        this.project = project;
    }

    public void setStatusListener(TypingStatusListener listener) {
        this.statusListener = listener;
    }

    /**
     * Starts typing the given text into the active editor.
     */
    public void startTyping(String text) {
        if (isRunning.get()) {
            stop();
        }

        Editor editor = getActiveEditor();
        if (editor == null) {
            notifyStatus("⚠️ No active editor found. Please open a file first.");
            return;
        }

        isRunning.set(true);
        isPaused.set(false);
        currentIndex.set(0);

        notifyStatus("▶️ Typing...");

        typingTimer = new Timer(getNextDelay('\0'), null);
        typingTimer.setRepeats(false);

        typingTimer.addActionListener(e -> {
            if (!isRunning.get()) return;
            if (isPaused.get()) {
                scheduleNext('\0');
                return;
            }

            int idx = currentIndex.getAndIncrement();
            if (idx >= text.length()) {
                stop();
                notifyStatus("✅ Typing complete!");
                if (statusListener != null) {
                    statusListener.onTypingFinished();
                }
                return;
            }

            char c = text.charAt(idx);
            typeCharacter(editor, c);

            if (statusListener != null) {
                statusListener.onProgressChanged(idx + 1, text.length());
            }

            scheduleNext(c);
        });

        typingTimer.start();
    }

    /**
     * Pauses the typing simulation.
     */
    public void pause() {
        if (isRunning.get() && !isPaused.get()) {
            isPaused.set(true);
            notifyStatus("⏸️ Paused");
        }
    }

    /**
     * Resumes the typing simulation.
     */
    public void resume() {
        if (isRunning.get() && isPaused.get()) {
            isPaused.set(false);
            notifyStatus("▶️ Typing...");
        }
    }

    /**
     * Toggles between pause and resume.
     */
    public void togglePause() {
        if (isPaused.get()) {
            resume();
        } else {
            pause();
        }
    }

    /**
     * Stops the typing simulation completely.
     */
    public void stop() {
        isRunning.set(false);
        isPaused.set(false);
        if (typingTimer != null) {
            typingTimer.stop();
            typingTimer = null;
        }
        notifyStatus("⏹️ Stopped");
    }

    /**
     * Sets the typing speed.
     * @param wordsPerMinute approximate words per minute (30-600)
     */
    public void setSpeed(int wordsPerMinute) {
        // Average word = 5 characters, so chars/min = wpm * 5
        // delay = 60000 / (wpm * 5) ms per character
        int charsPerMinute = wordsPerMinute * 5;
        this.baseDelayMs = Math.max(5, 60000 / charsPerMinute);
        this.variationMs = baseDelayMs / 2;
        this.newlineDelayMs = baseDelayMs * 4;
    }

    /**
     * Returns the base delay in ms (for slider display purposes).
     */
    public int getBaseDelayMs() {
        return baseDelayMs;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void typeCharacter(Editor editor, char c) {
        String charStr = String.valueOf(c);
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                int offset = editor.getCaretModel().getOffset();
                editor.getDocument().insertString(offset, charStr);
                editor.getCaretModel().moveToOffset(offset + 1);

                // Auto-scroll to caret position
                editor.getScrollingModel().scrollToCaret(
                    com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE
                );
            });
        });
    }

    private void scheduleNext(char previousChar) {
        if (typingTimer == null || !isRunning.get()) return;

        int delay = getNextDelay(previousChar);
        typingTimer.setInitialDelay(delay);
        typingTimer.restart();
    }

    private int getNextDelay(char previousChar) {
        if (isPaused.get()) return 100; // Poll interval while paused

        int delay = baseDelayMs + RANDOM.nextInt(Math.max(1, variationMs * 2)) - variationMs;

        // Add extra delay after newlines (simulates thinking)
        if (previousChar == '\n') {
            delay += newlineDelayMs;
        }

        // Occasional longer pause (simulates natural typing rhythm)
        if (RANDOM.nextInt(20) == 0) {
            delay += baseDelayMs * 3;
        }

        return Math.max(5, delay);
    }

    private Editor getActiveEditor() {
        FileEditorManager fem = FileEditorManager.getInstance(project);
        return fem.getSelectedTextEditor();
    }

    private void notifyStatus(String status) {
        if (statusListener != null) {
            statusListener.onStatusChanged(status);
        }
    }
}
