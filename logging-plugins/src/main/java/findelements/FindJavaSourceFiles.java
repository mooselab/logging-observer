package findelements;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import searchlogging.LoggingSearchUtils;

import java.util.List;
import java.util.stream.Collectors;

public class FindJavaSourceFiles extends AnAction {
    private static final Logger logger = Logger.getInstance(FindJavaSourceFiles.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to find Java source files in project " + projectName +".");

        // find all logging statements in the project
        List<PsiFile> psiFiles = FindElementsUtils.findJavaSourceFilesInProject(project);

        StringBuilder files = new StringBuilder();
        files.append("Files in Project " + project.getName() + ":\n");

        for (PsiFile file : psiFiles) {
            files.append(file.getName()).append("\n");
        }
        logger.info(files.toString());

        // list the logging statements in the find tool window view
        LoggingSearchUtils.listPsiElementsInFindToolWindow(project,
                psiFiles.stream().map(e -> (PsiElement)e).collect(Collectors.toList()));
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        boolean visibility = event.getProject() != null;
        event.getPresentation().setEnabled(visibility);
        event.getPresentation().setVisible(visibility);
    }

}
