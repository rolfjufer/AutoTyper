package ch.autotyper.service;

import ch.autotyper.parser.StepParser;
import ch.autotyper.parser.StepParser.StepBlock;
import ch.autotyper.parser.StepParser.TypingPlan;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Project-level service that simulates typing text into the active editor,
 * supporting structured step-by-step typing via @step markers.
 */
@Service(Service.Level.PROJECT)
public final class TypingSimulatorService {

    private static final Logger LOG = Logger.getInstance(TypingSimulatorService.class);
    private static final Random RANDOM = new Random();

    private final Project project;

    // Typing state
    private javax.swing.Timer typingTimer;
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isWaitingForNextStep = new AtomicBoolean(false);
    private final AtomicInteger charIndex = new AtomicInteger(0);

    // Step state
    private TypingPlan currentPlan;
    private int currentBlockIndex = 0;
    private List<StepBlock> insertedBlocks = new ArrayList<>();
    private boolean autoPauseBetweenSteps = true;

    // Speed settings
    private int baseDelayMs = 50;
    private int variationMs = 30;
    private int newlineDelayMs = 200;

    // Listener
    private TypingStatusListener statusListener;

    public interface TypingStatusListener {
        void onStatusChanged(String status);
        void onProgressChanged(int currentStep, int totalSteps, int charsCurrent, int charsTotal);
        void onStepPaused(int completedStep, int nextStep, int totalSteps);
        void onTypingFinished();
    }

    public TypingSimulatorService(Project project) {
        this.project = project;
    }

    public void setStatusListener(TypingStatusListener listener) {
        this.statusListener = listener;
    }

    public void setAutoPauseBetweenSteps(boolean autoPause) {
        this.autoPauseBetweenSteps = autoPause;
    }

    public boolean isAutoPauseBetweenSteps() {
        return autoPauseBetweenSteps;
    }

    /**
     * Starts structured typing of the given source code.
     */
    public void startTyping(String sourceCode) {
        if (isRunning.get()) {
            stop();
        }

        Editor editor = getActiveEditor();
        if (editor == null) {
            notifyStatus("⚠️ No active editor found. Please open a file first.");
            return;
        }

        // Parse the source into a typing plan
        currentPlan = StepParser.parse(sourceCode);
        currentBlockIndex = 0;
        insertedBlocks.clear();
        charIndex.set(0);

        isRunning.set(true);
        isPaused.set(false);
        isWaitingForNextStep.set(false);

        if (currentPlan.hasStepMarkers()) {
            notifyStatus("▶️ Typing step 1/" + currentPlan.getTotalSteps() + "...");
        } else {
            notifyStatus("▶️ Typing (no step markers found, sequential mode)...");
        }

        startTypingCurrentBlock();
    }

    /**
     * Advances to the next step (called by button or keyboard shortcut Alt+N).
     */
    public void nextStep() {
        if (!isWaitingForNextStep.get()) return;

        isWaitingForNextStep.set(false);
        currentBlockIndex++;
        charIndex.set(0);

        if (currentBlockIndex >= currentPlan.getBlocks().size()) {
            finish();
            return;
        }

        int stepNum = currentPlan.getBlocks().get(currentBlockIndex).getStepNumber();
        notifyStatus("▶️ Typing step " + stepNum + "/" + currentPlan.getTotalSteps() + "...");
        startTypingCurrentBlock();
    }

    public void pause() {
        if (isRunning.get() && !isPaused.get() && !isWaitingForNextStep.get()) {
            isPaused.set(true);
            notifyStatus("⏸️ Paused");
        }
    }

    public void resume() {
        if (isRunning.get() && isPaused.get()) {
            isPaused.set(false);
            int stepNum = currentPlan.getBlocks().get(currentBlockIndex).getStepNumber();
            notifyStatus("▶️ Typing step " + stepNum + "/" + currentPlan.getTotalSteps() + "...");
        }
    }

    public void togglePause() {
        if (isPaused.get()) resume();
        else pause();
    }

    public void stop() {
        isRunning.set(false);
        isPaused.set(false);
        isWaitingForNextStep.set(false);
        if (typingTimer != null) {
            typingTimer.stop();
            typingTimer = null;
        }
        notifyStatus("⏹️ Stopped");
    }

    public void setSpeed(int wordsPerMinute) {
        int charsPerMinute = wordsPerMinute * 5;
        this.baseDelayMs = Math.max(5, 60000 / charsPerMinute);
        this.variationMs = baseDelayMs / 2;
        this.newlineDelayMs = baseDelayMs * 4;
    }

    public boolean isRunning() { return isRunning.get(); }
    public boolean isPaused() { return isPaused.get(); }
    public boolean isWaitingForNextStep() { return isWaitingForNextStep.get(); }

    // =========================================================================
    // Private: Typing Engine
    // =========================================================================

    private void startTypingCurrentBlock() {
        if (currentBlockIndex >= currentPlan.getBlocks().size()) {
            finish();
            return;
        }

        StepBlock block = currentPlan.getBlocks().get(currentBlockIndex);
        String content = block.getContent();

        Editor editor = getActiveEditor();
        if (editor == null) {
            notifyStatus("⚠️ Editor lost!");
            stop();
            return;
        }

        String editorContent = editor.getDocument().getText();
        int insertionOffset = StepParser.calculateInsertionOffset(editorContent, block, insertedBlocks);

        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                editor.getCaretModel().moveToOffset(insertionOffset);
                editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            });
        });

        typingTimer = new javax.swing.Timer(getNextDelay('\0'), null);
        typingTimer.setRepeats(false);
        typingTimer.addActionListener(e -> onTimerTick(block, content, insertionOffset));
        typingTimer.start();
    }

    private void onTimerTick(StepBlock block, String content, int baseOffset) {
        if (!isRunning.get()) return;

        if (isPaused.get()) {
            scheduleNext('\0');
            return;
        }

        int idx = charIndex.getAndIncrement();
        if (idx >= content.length()) {
            insertedBlocks.add(block);
            onBlockComplete(block);
            return;
        }

        char c = content.charAt(idx);
        typeCharacterAtOffset(baseOffset + idx, c);

        if (statusListener != null) {
            int stepNum = block.getStepNumber();
            int totalChars = content.length();
            statusListener.onProgressChanged(stepNum, currentPlan.getTotalSteps(), idx + 1, totalChars);
        }

        scheduleNext(c);
    }

    private void onBlockComplete(StepBlock completedBlock) {
        int nextBlockIndex = currentBlockIndex + 1;

        if (nextBlockIndex >= currentPlan.getBlocks().size()) {
            finish();
            return;
        }

        StepBlock nextBlock = currentPlan.getBlocks().get(nextBlockIndex);
        boolean stepChange = nextBlock.getStepNumber() != completedBlock.getStepNumber();

        if (stepChange && autoPauseBetweenSteps && currentPlan.hasStepMarkers()) {
            isWaitingForNextStep.set(true);
            notifyStatus("⏸️ Step " + completedBlock.getStepNumber() + " complete — press Alt+N or click 'Next Step'");
            if (statusListener != null) {
                statusListener.onStepPaused(
                        completedBlock.getStepNumber(),
                        nextBlock.getStepNumber(),
                        currentPlan.getTotalSteps()
                );
            }
        } else {
            currentBlockIndex++;
            charIndex.set(0);
            startTypingCurrentBlock();
        }
    }

    private void finish() {
        isRunning.set(false);
        notifyStatus("✅ All steps complete!");
        if (statusListener != null) {
            statusListener.onTypingFinished();
        }
    }

    private void typeCharacterAtOffset(int offset, char c) {
        String charStr = String.valueOf(c);
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                Editor editor = getActiveEditor();
                if (editor == null) return;
                int safeOffset = Math.min(offset, editor.getDocument().getTextLength());
                editor.getDocument().insertString(safeOffset, charStr);
                editor.getCaretModel().moveToOffset(safeOffset + 1);
                editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
            });
        });
    }

    private void scheduleNext(char previousChar) {
        if (typingTimer == null || !isRunning.get()) return;
        typingTimer.setInitialDelay(getNextDelay(previousChar));
        typingTimer.restart();
    }

    private int getNextDelay(char previousChar) {
        if (isPaused.get()) return 100;
        int delay = baseDelayMs + RANDOM.nextInt(Math.max(1, variationMs * 2)) - variationMs;
        if (previousChar == '\n') delay += newlineDelayMs;
        if (RANDOM.nextInt(20) == 0) delay += baseDelayMs * 3;
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
