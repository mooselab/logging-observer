package searchlogging;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import loggingmetrics.ExceptionLoggingMetrics;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// logger for development environment, configured in this plugin project's resources/logback.xml file
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// logger for integrated plugin environment, configured in intellij's bin/log.xml file
//import com.intellij.openapi.diagnostic.Logger;

public class SearchExceptionLogging extends AnAction {
    // slf4j logger for development environment, configured in this plugin project's resources/logback.xml file
    private static final Logger logger = LoggerFactory.getLogger(SampleLoggingUsages.class);

    // intellij logger for integrated plugin environment, configured in intellij's bin/log.xml file
    //private static final Logger logger = Logger.getInstance(SearchLoggingUsages.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to search logging statements in project " + projectName +".");

        // find all logging statements in the project
        List<PsiMethodCallExpression> loggingStatements = LoggingSearchUtils.findLoggingStatementsInProject(project);

        /*
        // randomly sample k logging statements
        int k =100;
        List<PsiMethodCallExpression> exceptionLogs = loggingStatements;
        if (loggingStatements.size() > k) {
            Collections.shuffle(exceptionLogs, new Random(111));
            exceptionLogs = new ArrayList<>(exceptionLogs.subList(0, k));
        }
        */

        // select exception logging
        List<PsiMethodCallExpression> exceptionLogs = new ArrayList<>();
        for (PsiMethodCallExpression log : loggingStatements) {
            PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(log, PsiCatchSection.class);
            if (catchSection != null) {
                exceptionLogs.add(log);
            }
        }

        StringBuilder loggingStatementsStr = new StringBuilder();
        for (PsiMethodCallExpression log : exceptionLogs) {
            PsiFile psiFile = log.getContainingFile();
            int lineNumber = StringUtil.offsetToLineNumber(psiFile.getText(), log.getTextOffset()) + 1;

            ExceptionLoggingMetrics metrics = new ExceptionLoggingMetrics(log);
            List<PsiType> psiTypes = metrics.getExceptionTypes();
            loggingStatementsStr.append(psiFile.getVirtualFile().getPath()).append(":").append(lineNumber).append("\n");
            /*
            if (psiTypes.size() > 0) {
                loggingStatementsStr.append("Exception type: ").append(psiType.getCanonicalText()).append("\n");
            } else {
                loggingStatementsStr.append("Exception type: ").append("null").append("\n");
            }
            */
            String typesStr = psiTypes.stream().map(t -> t.getCanonicalText()).reduce("", (a, b) -> a + " " + b);
            loggingStatementsStr.append("Exception type: ").append(typesStr).append("\n");

            List<PsiMethod> methods = metrics.getExceptionMethods();
            String methodsStr = methods.stream().map(m -> m.getName()).reduce("", (a,b) -> a + " " +b );
            loggingStatementsStr.append("Exception methods: ").append(methodsStr).append("\n");

            loggingStatementsStr.append(log.getText()).append(("\n"));
        }
        logger.info("Logging statements: \n" + loggingStatementsStr);

        // list the logging statements in the find tool window view
        LoggingSearchUtils.listPsiMethodCallExpressionsInFindToolWindow(project, exceptionLogs);
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        boolean visibility = event.getProject() != null;
        event.getPresentation().setEnabled(visibility);
        event.getPresentation().setVisible(visibility);
    }
}