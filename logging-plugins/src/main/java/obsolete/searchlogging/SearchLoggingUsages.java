package obsolete.searchlogging;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import loggingcomponents.LoggingComponents;
import org.jetbrains.annotations.NotNull;

// logger for development environment, configured in this plugin project's resources/logback.xml file
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
// logger for integrated plugin environment, configured in intellij's bin/log.xml file
import com.intellij.openapi.diagnostic.Logger;

import java.util.List;

public class SearchLoggingUsages extends AnAction {
    // slf4j logger for development environment, configured in this plugin project's resources/logback.xml file
    //private static final Logger logger = LoggerFactory.getLogger(SearchLoggingUsages.class);

    // intellij logger for integrated plugin environment, configured in intellij's bin/log.xml file
    private static final Logger logger = Logger.getInstance(SearchLoggingUsages.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to search logging statements in project " + projectName +".");

        // find all logging statements in the project
        List<PsiMethodCallExpression> loggingStatements = LoggingSearchUtils.findLoggingStatementsInProject(project);

        /*
        StringBuilder loggers = new StringBuilder();
        for (PsiElement element : elements) {
            loggers.append("getText(): ").append(element.getText()).append("\n");
            loggers.append("toString(): ").append(element.toString()).append("\n");
        }
        */

        StringBuilder loggingStatementsStr = new StringBuilder();
        loggingStatementsStr.append(LoggingComponents.getLogComponentsHeader()).append("\n");
        for (PsiMethodCallExpression log : loggingStatements) {
            PsiFile psiFile = log.getContainingFile();
            int lineNumber = StringUtil.offsetToLineNumber(psiFile.getText(), log.getTextOffset()) + 1;

            // get the location of the logging statement
            //loggingStatementsStr.append(psiFile.getVirtualFile().getPath()).append(":").append(lineNumber).append(":");

            // get the components of the logging statement
            LoggingComponents metrics = new LoggingComponents(log);
            loggingStatementsStr.append(metrics.getLogComponents()).append("\n");
        }
        //logger.debug("\"Logger\" occurrences: \n{}", loggers);
        logger.info("Logging metrics for project " + projectName + ":\n" + loggingStatementsStr);

        // list the logging statements in the find tool window view
        LoggingSearchUtils.listPsiMethodCallExpressionsInFindToolWindow(project, loggingStatements);
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        boolean visibility = event.getProject() != null;
        event.getPresentation().setEnabled(visibility);
        event.getPresentation().setVisible(visibility);
    }

}
