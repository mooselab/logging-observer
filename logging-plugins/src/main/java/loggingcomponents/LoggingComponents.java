package loggingcomponents;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
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
    boolean isStackTraceLogged;

    private Project project;

    public LoggingComponents(PsiMethodCallExpression logStmt) {
        this.logStmt = logStmt;
        this.project = logStmt.getProject();
        this.logLevel = extractLogLevel(logStmt);
        this.logText = extractLogText(logStmt);
        this.logBody = extractLogBody(logStmt);
        this.isStackTraceLogged = extractIsStackTraceLogged(logStmt);
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
    public boolean getIsStackTraceLogged() {return this.isStackTraceLogged; }

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

    private boolean extractIsStackTraceLogged(PsiMethodCallExpression logStmt) {

        // parameter lists of the logging statement
        PsiExpressionList expressionList = PsiTreeUtil.getChildOfType(logStmt, PsiExpressionList.class);

        // PsiReferenceExpression parameters (parameters simply refer to other variables, no method invocation or calculation)
        PsiReferenceExpression[] referenceExpressions = PsiTreeUtil.getChildrenOfType(expressionList,
                PsiReferenceExpression.class);

        if (referenceExpressions == null || referenceExpressions.length == 0) {
            return false;
        }
        for (PsiReferenceExpression expr : referenceExpressions) {
            PsiElement resolvedElement = expr.getReference().resolve();
            //logger.debug("Resolved element: " + resolvedElement.getText());
            if (resolvedElement instanceof PsiVariable) {
                //logger.debug("Resolved element is a PsiVariable instance.");
                try {
                    PsiType variableType = PsiTreeUtil.findChildOfType((PsiVariable) resolvedElement,
                            PsiTypeElement.class).getType();
                    //logger.debug("Variable type: " + variableType.getCanonicalText());
                    if (isThrowableType(variableType)) {
                        //logger.debug("Variable type : " + variableType.getCanonicalText() + " is Throwable");
                        return true;
                    }
                } catch (NullPointerException e) {
                    return false;
                }
            }
            /*
            String expressionName = PsiTreeUtil.getChildOfType(expr, PsiIdentifier.class).getText();
            if (expressionName.equals(exName)) {
                return true;
            }
            */
        }
        return false;
    }

    private boolean isThrowableType(PsiType t) {
        PsiType throwable = PsiType.getTypeByName("java.lang.Throwable", this.project,
                GlobalSearchScope.allScope(this.project));
        return isSubType(t, throwable, false);
    }

    /**
     * Recursively check if a type is a sub-type of another type
     * @param child
     * @param parent
     * @param strict: true -> returns false for same type; false -> return true for same type.
     * @return
     */
    private boolean isSubType(PsiType child, PsiType parent, boolean strict) {

        if (child.getCanonicalText().equals(parent.getCanonicalText())) {
            if (strict) {
                return false;
            } else {
                return true;
            }
        }

        PsiType[] superTypes = child.getSuperTypes();
        for (PsiType t : superTypes) {
            if (t.getCanonicalText().equals(parent.getCanonicalText()) ||
                    isSubType(t, parent, true)) {
                return true;
            }
        }
        return false;
    }

}
