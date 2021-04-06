package findelements;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import loggingcomponents.LoggingComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FindLoggingStatements extends AnAction {
    private static final Logger logger = Logger.getInstance(FindJavaSourceFiles.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to find logging statements in project " + projectName +".");

        // find all logging statements in the project
        List<PsiMethodCallExpression> loggingStatements = findLoggingStatementsInFiles(project);

        /*
        StringBuilder loggingStatementsStr = new StringBuilder();
        loggingStatementsStr.append("Logging statements in Project " + project.getName() + ":\n");

        for (PsiMethodCallExpression loggingStatement : loggingStatements) {
            loggingStatementsStr.append(loggingStatement.getText()).append("\n");
        }
        logger.info(loggingStatementsStr.toString());
        */

        // get the logging components of each logging statement
        StringBuilder loggingStatementsStr = new StringBuilder();
        loggingStatementsStr.append(LoggingComponents.getLogComponentsHeader()).append("\n");
        for (PsiMethodCallExpression log : loggingStatements) {
            // get the components of the logging statement
            LoggingComponents metrics = new LoggingComponents(log);
            //loggingStatementsStr.append("Method:").append("\n").append(metrics.getContainingMethod()).append("\n");
            //loggingStatementsStr.append("Logging:").append("\n").append(metrics.getLogBody()).append("\n\n");
            loggingStatementsStr.append(metrics.getLogComponents()).append("\n");
        }
        logger.info("Logging components for project " + projectName + ":\n" + loggingStatementsStr);

        // list the logging statements in the find tool window view
        FindElementsUtils.listPsiElementsInFindToolWindow(project,
                loggingStatements.stream().map(e -> (PsiElement)e).collect(Collectors.toList()));
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        boolean visibility = event.getProject() != null;
        event.getPresentation().setEnabled(visibility);
        event.getPresentation().setVisible(visibility);
    }

    public static List<PsiMethodCallExpression> findLoggingStatementsInFiles(Project project) {
        List<PsiMethodCallExpression> loggingStatements = new ArrayList<>();

        // Pattern for matching logging statements
        Pattern pLog = Pattern.compile(".*log.*\\.(trace|debug|info|warn|error|fatal)", Pattern.CASE_INSENSITIVE);

        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        projectFileIndex.iterateContent(new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile fileOrDir) {
                if (!fileOrDir.isDirectory() && projectFileIndex.isUnderSourceRootOfType(fileOrDir,
                        JavaModuleSourceRootTypes.SOURCES)) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(fileOrDir);
                    if (!(psiFile instanceof PsiJavaFile)) {
                        return true; // skip non-Java files
                    }
                    String fileName = psiFile.getName();
                    if (fileName.matches(".*Test\\.java")) {
                        return true; // skip test files
                    }

                    PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression.class).forEach(m -> {
                        if (pLog.matcher(m.getMethodExpression().getText()).matches()) {
                            loggingStatements.add(m);
                        }
                    });

                }
                return true;
            }
        });

        return loggingStatements;
    }
}
