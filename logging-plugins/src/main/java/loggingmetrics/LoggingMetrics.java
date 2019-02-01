package loggingmetrics;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LoggingMetrics {
    private static final Logger logger = LoggerFactory.getLogger(LoggingMetrics.class);

    private PsiMethodCallExpression logStmt;
    String logLevel;
    String logText;
    String logBody;
    private boolean isStackTraceLogged;

    private Project project;

    public LoggingMetrics(PsiMethodCallExpression logStmt) {
        this.logStmt = logStmt;
        this.project = logStmt.getProject();
        this.logLevel = extractLogLevel(logStmt);
        this.logText = extractLogText(logStmt);
        this.logBody = extractLogBody(logStmt);
    }

    public static String getLogComponentsHeader() {
        List<String> componentsHeader = new ArrayList<>();

        //componentsHeader.add("logBody");
        componentsHeader.add("logLevel");
        componentsHeader.add("logText");

        return String.join(";;;", componentsHeader);
    }

    public String getLogComponents() {
        List<String> logComponents = new ArrayList<>();

        //logComponents.add(getLogBody());
        logComponents.add(getLogLevel());
        logComponents.add(getLogText());

        return String.join(";;;", logComponents);
    }


    public String getLogLevel() { return this.logLevel; }
    public String getLogText() { return this.logText; }
    public String getLogBody() { return this.logBody; }

    private String extractLogLevel(PsiMethodCallExpression logStmt) {
        PsiReferenceExpression methodCall = logStmt.getMethodExpression();
        String logMethodName = PsiTreeUtil.getChildOfType(methodCall, PsiIdentifier.class).getText();
        return logMethodName;
    }

    private String extractLogText(PsiMethodCallExpression logStmt) {
        PsiExpressionList expressionList = logStmt.getArgumentList();
        Collection<PsiLiteralExpression> literalExpressions =
                PsiTreeUtil.findChildrenOfType(expressionList, PsiLiteralExpression.class);
        if (literalExpressions.size() >= 1) {
            return literalExpressions.stream()
                .map( l -> l.getText())
                .collect(Collectors.joining(" "));
        } else {
            return "";
        }
    }

    private String extractLogBody(PsiMethodCallExpression logStmt) {
        return logStmt.getText();
    }

}
