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
import exceptionloggingmetrics.ExceptionLoggingMetrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FindLoggingStatementsInCatchSections extends AnAction {
    private static final Logger logger = Logger.getInstance(FindJavaSourceFiles.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to find logging statements in catch sections in project " + projectName +".");

        // find exception logging statements in the project
        List<PsiMethodCallExpression> loggingStatements = findLoggingStatementsInCatchSections(project);

        /*
        StringBuilder loggingStatementsStr = new StringBuilder();
        loggingStatementsStr.append("Logging statements in catch sections in Project " + project.getName() + ":\n");

        for (PsiMethodCallExpression loggingStatement : loggingStatements) {
            loggingStatementsStr.append(loggingStatement.getText()).append("\n");
        }
        logger.info(loggingStatementsStr.toString());
        */

        StringBuilder loggingStatementsStr = new StringBuilder();
        loggingStatementsStr.append(ExceptionLoggingMetrics.getLoggingMetricsHeader()).append(("\n"));
        for (PsiMethodCallExpression log : loggingStatements) {
            ExceptionLoggingMetrics metrics = new ExceptionLoggingMetrics(log);
            loggingStatementsStr.append(metrics.getLoggingMetrics()).append("\n");
        }
        logger.info("Exception logging metrics for project " + projectName + ": \n" + loggingStatementsStr);

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

    public static List<PsiMethodCallExpression> findLoggingStatementsInCatchSections(Project project) {
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

                    Collection<PsiCatchSection> catchSectionsInFile = PsiTreeUtil.findChildrenOfType(psiFile, PsiCatchSection.class);
                    if (catchSectionsInFile.size() == 0) {
                        return true;
                    }

                    for (PsiCatchSection catchSection : catchSectionsInFile) {
                        PsiTreeUtil.findChildrenOfType(catchSection, PsiMethodCallExpression.class).forEach(m -> {
                            if (pLog.matcher(m.getMethodExpression().getText()).matches()) {
                                loggingStatements.add(m);
                            }
                        });
                    }
                }
                return true;
            }
        });

        return loggingStatements;
    }
}
