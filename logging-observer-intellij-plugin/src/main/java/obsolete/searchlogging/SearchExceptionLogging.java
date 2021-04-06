package obsolete.searchlogging;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import exceptionloggingmetrics.ExceptionLoggingMetrics;

import java.util.ArrayList;
import java.util.List;

// logger for development environment, configured in this plugin project's resources/logback.xml file
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
// logger for integrated plugin environment, configured in intellij's bin/log.xml file
import com.intellij.openapi.diagnostic.Logger;

public class SearchExceptionLogging extends AnAction {
    // slf4j logger for development environment, configured in this plugin project's resources/logback.xml file
    //private static final Logger logger = LoggerFactory.getLogger(SampleLoggingUsages.class);

    // intellij logger for integrated plugin environment, configured in intellij's bin/log.xml file
    private static final Logger logger = Logger.getInstance(SearchLoggingUsages.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to search logging statements in project " + projectName +".");

        // find all logging statements in the project
        List<PsiMethodCallExpression> loggingStatements = LoggingSearchUtils.findLoggingStatementsInProject(project);

        // select exception logging
        List<PsiMethodCallExpression> exceptionLogs = new ArrayList<>();
        for (PsiMethodCallExpression log : loggingStatements) {
            PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(log, PsiCatchSection.class);
            if (catchSection != null) {
                exceptionLogs.add(log);
            }
        }

        //logger.debug("Metrics header: " + ExceptionLoggingMetrics.getLoggingMetricsHeader());

        StringBuilder loggingStatementsStr = new StringBuilder();
        loggingStatementsStr.append(ExceptionLoggingMetrics.getLoggingMetricsHeader()).append(("\n"));
        for (PsiMethodCallExpression log : exceptionLogs) {
            ExceptionLoggingMetrics metrics = new ExceptionLoggingMetrics(log);
            loggingStatementsStr.append(metrics.getLoggingMetrics()).append("\n");
        }
        logger.info("Exception logging metrics for project " + projectName + ": \n" + loggingStatementsStr);

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
