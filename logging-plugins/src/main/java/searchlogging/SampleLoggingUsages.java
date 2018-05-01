package searchlogging;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
// logger for integrated plugin environment, configured in intellij's bin/log.xml file
import com.intellij.openapi.diagnostic.Logger;

import java.util.*;

public class SampleLoggingUsages extends AnAction {
    // slf4j logger for development environment, configured in this plugin project's resources/logback.xml file
    //private static final Logger logger = LoggerFactory.getLogger(SampleLoggingUsages.class);

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

        // randomly sample k logging statements
        int k =100;
        List<PsiMethodCallExpression> sampledLogs = loggingStatements;
        if (loggingStatements.size() > k) {
            Collections.shuffle(sampledLogs, new Random(111));
            sampledLogs = new ArrayList<>(sampledLogs.subList(0, k));
        }

        StringBuilder loggingStatementsStr = new StringBuilder();
        for (PsiMethodCallExpression log : sampledLogs) {
            PsiFile psiFile = log.getContainingFile();
            int lineNumber = StringUtil.offsetToLineNumber(psiFile.getText(), log.getTextOffset()) + 1;
            loggingStatementsStr.append(psiFile.getVirtualFile().getPath()).append(":").append(lineNumber).append("\n");
            loggingStatementsStr.append(log.getText()).append(("\n"));
        }
        logger.info("Logging statements: \n" + loggingStatementsStr);

        // list the logging statements in the find tool window view
        LoggingSearchUtils.listPsiMethodCallExpressionsInFindToolWindow(project, sampledLogs);
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        boolean visibility = event.getProject() != null;
        event.getPresentation().setEnabled(visibility);
        event.getPresentation().setVisible(visibility);
    }
}
