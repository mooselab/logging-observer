package searchlogging;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Query;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;

// logger for development environment, configured in this plugin project's resources/logback.xml file
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
// logger for integrated plugin environment, configured in intellij's bin/log.xml file
import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

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


/*        // get all java files in a project
        Collection<VirtualFile> virtualFiles = FileBasedIndex.getInstance().getContainingFiles(
                FileTypeIndex.NAME,
                JavaFileType.INSTANCE,
                GlobalSearchScope.projectScope(project)
        );
        int nFiles = virtualFiles.size();
        *//*
        StringBuilder filenames = new StringBuilder();
        for (VirtualFile virtualFile : virtualFiles) {
            filenames.append(virtualFile.getPath() + "\n");
        }
        *//*
        logger.info("The project " + projectName + " has " + nFiles + " Java files.");*/

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
        for (PsiMethodCallExpression log : loggingStatements) {
            PsiFile psiFile = log.getContainingFile();
            int lineNumber = StringUtil.offsetToLineNumber(psiFile.getText(), log.getTextOffset()) + 1;
            loggingStatementsStr.append(psiFile.getVirtualFile().getPath()).append(":").append(lineNumber).append("\n");
            loggingStatementsStr.append(log.getText()).append(("\n"));

            /*
            loggingStatementsStr.append(log.getMethodExpression().getText()).append("\n");
            loggingStatementsStr.append(log.getMethodExpression().toString()).append("\n");
            String methodStr = log.getMethodExpression().getText();
            if (methodStr.matches(".*\\.(trace|debug|info|warn|error|fatal)")) {
                loggingStatementsStr.append(methodStr.substring(methodStr.lastIndexOf('.')+1)).append("\n");
            }
            */
        }
        //logger.debug("\"Logger\" occurrences: \n{}", loggers);
        logger.debug("Logging statements: \n" + loggingStatementsStr);

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
