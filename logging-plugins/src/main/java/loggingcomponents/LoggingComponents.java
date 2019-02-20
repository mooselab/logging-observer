package loggingcomponents;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LoggingComponents {
    private static final Logger logger = LoggerFactory.getLogger(LoggingComponents.class);

    private PsiMethodCallExpression logStmt;
    String logLevel;
    String logText;
    String logBody;
    private boolean isStackTraceLogged;

    private Project project;

    public LoggingComponents(PsiMethodCallExpression logStmt) {
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
