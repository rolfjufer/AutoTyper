package ch.autotyper.action;

import ch.autotyper.service.TypingSimulatorService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Global action that triggers "Next Step" in the AutoTyper.
 * Default shortcut: Alt+N (configurable via Settings → Keymap → "AutoTyper Next Step")
 */
public class NextStepAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        TypingSimulatorService service = project.getService(TypingSimulatorService.class);
        if (service != null && service.isWaitingForNextStep()) {
            service.nextStep();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        TypingSimulatorService service = project.getService(TypingSimulatorService.class);
        e.getPresentation().setEnabled(service != null && service.isWaitingForNextStep());
    }
}
