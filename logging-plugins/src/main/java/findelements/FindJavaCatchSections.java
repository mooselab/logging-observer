package findelements;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import searchlogging.LoggingSearchUtils;

import java.util.List;
import java.util.stream.Collectors;

public class FindJavaCatchSections extends AnAction {
    private static final Logger logger = Logger.getInstance(FindJavaSourceFiles.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to find Java catch sections in project " + projectName +".");

        // find all logging statements in the project
        List<PsiCatchSection> psiCatchSections = FindElementsUtils.findCatchSectionsInJavaProject(project);

        StringBuilder catchSectionsStr = new StringBuilder();
        catchSectionsStr.append("Catch sections in Project " + project.getName() + ":\n");

        for (PsiCatchSection catchSection : psiCatchSections) {
            catchSectionsStr.append(catchSection.getCatchType().getCanonicalText()).append("\n");
        }
        logger.info(catchSectionsStr.toString());

        // list the logging statements in the find tool window view
        LoggingSearchUtils.listPsiElementsInFindToolWindow(project,
                psiCatchSections.stream().map(e -> (PsiElement)e).collect(Collectors.toList()));
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        boolean visibility = event.getProject() != null;
        event.getPresentation().setEnabled(visibility);
        event.getPresentation().setVisible(visibility);
    }

}