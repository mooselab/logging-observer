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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import common.Locators;

public class LoggingComponents {
    private static final Logger logger = LoggerFactory.getLogger(LoggingComponents.class);

    private PsiMethodCallExpression logStmt;
    String logLevel;
    String logStringWithoutVariables;
    String logStringWithVariableNames;
    String logStringWithVariableTypes;
    String logBody;
    boolean isStackTraceLogged;

    private Project project;

    public LoggingComponents(PsiMethodCallExpression logStmt) {
        this.logStmt = logStmt;
        this.project = logStmt.getProject();
        this.logLevel = extractLogLevel(logStmt);
        this.logStringWithoutVariables = extractLogStringWithoutVariables(logStmt);
        this.logStringWithVariableNames = extractLogStringWithVariableNames(logStmt);
        this.logStringWithVariableTypes = extractLogStringWithVariableTypes(logStmt);
        this.logBody = extractLogBody(logStmt);
        this.isStackTraceLogged = extractIsStackTraceLogged(logStmt);
    }

    public static String getLogComponentsHeader() {
        List<String> componentsHeader = new ArrayList<>();

        componentsHeader.add("logLocation");
        //componentsHeader.add("logBody");
        componentsHeader.add("logLevel");
        componentsHeader.add("logStringWithoutVariables");
        componentsHeader.add("logStringWithVariableNames");
        componentsHeader.add("logStringWithVariableTypes");

        return String.join(";;;", componentsHeader);
    }

    public String getLogComponents() {
        List<String> logComponents = new ArrayList<>();

        logComponents.add(Locators.getLocationInFile(this.logStmt));
        //logComponents.add(getLogBody());
        logComponents.add(getLogLevel());
        logComponents.add(getLogStringWithoutVariables().replaceAll("\\r\\n|\\r|\\n", " "));
        logComponents.add(getLogStringWithVariableNames().replaceAll("\\r\\n|\\r|\\n", " "));
        logComponents.add(getLogStringWithVariableTypes().replaceAll("\\r\\n|\\r|\\n", " "));

        return String.join(";;;", logComponents);
    }


    public String getLogLevel() { return this.logLevel; }
    public String getLogStringWithoutVariables() { return this.logStringWithoutVariables; }
    public String getLogStringWithVariableNames() { return this.logStringWithVariableNames; }
    public String getLogStringWithVariableTypes() { return this.logStringWithVariableTypes; }
    public String getLogBody() { return this.logBody; }
    public boolean getIsStackTraceLogged() {return this.isStackTraceLogged; }
    public String getContainingMethod() {return this.extractContainingMethod(this.logStmt);}

    private String extractLogLevel(PsiMethodCallExpression logStmt) {
        PsiReferenceExpression methodCall = logStmt.getMethodExpression();
        String logMethodName = PsiTreeUtil.getChildOfType(methodCall, PsiIdentifier.class).getText();
        return logMethodName;
    }

    /*
    private String extractLogStringWithoutVariables(PsiMethodCallExpression logStmt) {
        PsiExpressionList expressionList = logStmt.getArgumentList();
        Collection<PsiLiteralExpression> literalExpressions =
                PsiTreeUtil.findChildrenOfType(expressionList, PsiLiteralExpression.class);
        if (literalExpressions.size() >= 1) {
            return literalExpressions.stream()
                .map( l -> {
                    Object value = l.getValue();
                    //return (value == null) ? "" : value.toString();
                    return (value == null) ? l.getText() : value.toString();
                })
                .collect(Collectors.joining(" "));
        } else {
            return "";
        }
    }
    */

    private String extractLogStringWithoutVariables(PsiMethodCallExpression logStmt) {
        PsiExpressionList expressionList = logStmt.getArgumentList();
        int argCount = expressionList.getExpressionCount();

        StringBuilder logString = new StringBuilder();
        boolean isFirstArg = true;
        List<String> vars = new ArrayList<>();
        PsiExpression expression = expressionList.getExpressions()[0]; // get the first argument

        if (expression instanceof PsiLiteralExpression) {
            try {
                logString.append(((PsiLiteralExpression) expression).getValue().toString());
            } catch (NullPointerException e) {
                logString.append(((PsiLiteralExpression) expression).getText());
            }
        } else if (expression instanceof PsiPolyadicExpression) {
            logString.append(concatPolyadicExpressionToStringWithoutVariables((PsiPolyadicExpression)expression));
        } else {
            // should not come here
        }

        if (argCount == 1) {
            return logString.toString();
        }

        // remove the place holders ("{}") in the log string

        // first remove " {}"
        Pattern pPlaceHolder = Pattern.compile(" \\{\\}");
        Matcher mPlaceHolder = pPlaceHolder.matcher(logString);
        String resultStr = new String(mPlaceHolder.replaceAll(""));

        // then remove "{}"
        Pattern pPlaceHolder2 = Pattern.compile("\\{\\}");
        Matcher mPlaceHolder2 = pPlaceHolder2.matcher(resultStr);
        String resultStr2 = new String(mPlaceHolder2.replaceAll(""));

        return resultStr2;
    }

    private String concatPolyadicExpressionToStringWithoutVariables(PsiPolyadicExpression polyadicExpression) {
        PsiExpression[] expressions = polyadicExpression.getOperands();
        StringBuilder logString = new StringBuilder();

        for (PsiExpression expression : expressions) {
            if (expression instanceof PsiLiteralExpression) {
                try {
                    logString.append(((PsiLiteralExpression) expression).getValue().toString());
                } catch (NullPointerException e) {
                    logString.append(((PsiLiteralExpression) expression).getText());
                }
            } else if (expression instanceof PsiPolyadicExpression) {
                logString.append(concatPolyadicExpressionToStringWithoutVariables((PsiPolyadicExpression)expression));
            }
        }

        return logString.toString();
    }

    private String extractLogStringWithVariableNames(PsiMethodCallExpression logStmt) {
        PsiExpressionList expressionList = logStmt.getArgumentList();
        int argCount = expressionList.getExpressionCount();

        StringBuilder logString = new StringBuilder();
        boolean isFirstArg = true;
        List<String> vars = new ArrayList<>();
        for (PsiExpression expression : expressionList.getExpressions()) {
            // get the log string (the first argument)
            if (isFirstArg) {
                if (expression instanceof PsiLiteralExpression) {
                    try {
                        logString.append(((PsiLiteralExpression) expression).getValue().toString());
                    } catch (NullPointerException e) {
                        logString.append(((PsiLiteralExpression) expression).getText());
                    }
                } else if (expression instanceof PsiPolyadicExpression) {
                    logString.append(concatPolyadicExpressionToStringWithVarNames((PsiPolyadicExpression)expression));
                } else if (expression instanceof PsiReferenceExpression) {
                    logString.append(PsiTreeUtil.getChildOfType(expression, PsiIdentifier.class).getText());
                } else if(expression instanceof PsiMethodCallExpression) {
                    PsiReferenceExpression methodCallRef = PsiTreeUtil.getChildOfType(expression,
                            PsiReferenceExpression.class);
                    logString.append(PsiTreeUtil.getChildOfType(methodCallRef, PsiIdentifier.class).getText());
                }
                else { // should not come here
                    logString.append(expression.getText());
                }
                isFirstArg = false;
                continue;
            }

            // get the variables
            if (expression instanceof PsiReferenceExpression) {
                vars.add(PsiTreeUtil.getChildOfType(expression, PsiIdentifier.class).getText());
            } else if(expression instanceof PsiMethodCallExpression) {
                PsiReferenceExpression methodCallRef = PsiTreeUtil.getChildOfType(expression,
                        PsiReferenceExpression.class);
                vars.add(PsiTreeUtil.getChildOfType(methodCallRef, PsiIdentifier.class).getText());
            } else if (expression instanceof PsiLiteralExpression) {
                try {
                    vars.add(((PsiLiteralExpression) expression).getValue().toString());
                } catch (NullPointerException e) {
                    vars.add(((PsiLiteralExpression) expression).getText());
                }
            } else if (expression instanceof PsiPolyadicExpression) {
                vars.add(concatPolyadicExpressionToStringWithVarNames((PsiPolyadicExpression)expression));
            } else { // should not come here
                vars.add(expression.getText());
            }
        }

        if (vars.size() == 0) {
            return logString.toString();
        }

        // replace the place holders ("{}") in the log string with the variables
        Pattern pPlaceHolder = Pattern.compile("\\{\\}");
        Matcher mPlaceHolder = pPlaceHolder.matcher(logString);
        int varIndex = 0;
        StringBuffer combinedStr = new StringBuffer();
        while (mPlaceHolder.find()) {
            //int start = mPlaceHolder.start();
            //int end = mPlaceHolder.end();
            mPlaceHolder.appendReplacement(combinedStr, vars.get(varIndex));
            varIndex += 1;
            if (varIndex >= vars.size()) {
                break;
            }
        }
        mPlaceHolder.appendTail(combinedStr);

        // add the exception, if any, and other remaining vars to the end of the string
        if (varIndex < vars.size()) {
            for (String var : vars.subList(varIndex, vars.size())) {
                combinedStr.append(" ").append(var);
            }
        }

        return combinedStr.toString();
    }

    private String concatPolyadicExpressionToStringWithVarNames(PsiPolyadicExpression polyadicExpression) {
        PsiExpression[] expressions = polyadicExpression.getOperands();
        StringBuilder logString = new StringBuilder();

        for (PsiExpression expression : expressions) {
            if (expression instanceof PsiLiteralExpression) {
                try {
                    logString.append(((PsiLiteralExpression) expression).getValue().toString());
                } catch (NullPointerException e) {
                    logString.append(((PsiLiteralExpression) expression).getText());
                }

            } else if (expression instanceof PsiReferenceExpression) {
                logString.append(PsiTreeUtil.getChildOfType(expression, PsiIdentifier.class).getText());
            } else if(expression instanceof PsiMethodCallExpression) {
                PsiReferenceExpression methodCallRef = PsiTreeUtil.getChildOfType(expression,
                        PsiReferenceExpression.class);
                logString.append(PsiTreeUtil.getChildOfType(methodCallRef, PsiIdentifier.class).getText());
            } else if (expression instanceof PsiPolyadicExpression) {
                logString.append(concatPolyadicExpressionToStringWithVarNames((PsiPolyadicExpression)expression));
            }
        }

        return logString.toString();
    }

    private String extractLogStringWithVariableTypes(PsiMethodCallExpression logStmt) {
        PsiExpressionList expressionList = logStmt.getArgumentList();
        int argCount = expressionList.getExpressionCount();

        StringBuilder logString = new StringBuilder();
        boolean isFirstArg = true;
        List<String> vars = new ArrayList<>();
        for (PsiExpression expression : expressionList.getExpressions()) {
            // get the log string (the first argument)
            if (isFirstArg) {
                if (expression instanceof PsiLiteralExpression) {
                    try {
                        logString.append(((PsiLiteralExpression) expression).getValue().toString());
                    } catch (NullPointerException e) {
                        logString.append(((PsiLiteralExpression) expression).getText());
                    }
                } else if (expression instanceof PsiPolyadicExpression) {
                    logString.append(concatPolyadicExpressionToStringWithVarTypes((PsiPolyadicExpression)expression));
                } else if (expression instanceof PsiReferenceExpression) {
                    //logString.append(PsiTreeUtil.getChildOfType(expression, PsiIdentifier.class).getText());
                    PsiElement resolvedElement = expression.getReference().resolve();
                    if (resolvedElement == null) {
                        logString.append("UnresolvableVariable");
                        continue;
                    }
                    if (resolvedElement instanceof PsiVariable) {
                        try {
                            //PsiType variableType = PsiTreeUtil.findChildOfType((PsiVariable) resolvedElement,
                            //        PsiTypeElement.class).getType();
                            PsiType variableType = ((PsiVariable)resolvedElement).getType();
                            logString.append(variableType.getPresentableText());
                        } catch (NullPointerException e) {
                            logString.append("UnresolvableVariableNoType");
                        }
                    } else {
                        logString.append("NotAVariable");
                    }
                } else if(expression instanceof PsiMethodCallExpression) {
                    PsiMethod method = ((PsiMethodCallExpression) expression).resolveMethod();
                    if (method == null) {
                        logString.append("UnresolvableMethodCall");
                        continue;
                    }
                    String returnType = method.getReturnType().getPresentableText();
                    logString.append(returnType);
                }
                else { // should not come here
                    logString.append(expression.getText());
                }
                isFirstArg = false;
                continue;
            }

            // get the variables
            if (expression instanceof PsiReferenceExpression) {
                //vars.add(PsiTreeUtil.getChildOfType(expression, PsiIdentifier.class).getText());
                //PsiElement resolvedElement = expression.getReference().resolve();
                PsiElement resolvedElement = ((PsiReferenceExpression)expression).resolve();
                if (resolvedElement == null) {
                    vars.add("UnresolvableVariable");
                    continue;
                }
                if (resolvedElement instanceof PsiVariable) {
                    try {
                        //PsiType variableType = PsiTreeUtil.findChildOfType((PsiVariable) resolvedElement,
                        //        PsiTypeElement.class).getType();
                        PsiType variableType = ((PsiVariable)resolvedElement).getType();
                        vars.add(variableType.getPresentableText());
                    } catch (NullPointerException e) {
                        vars.add("UnresolvableVariableNoType");
                    }
                } else {
                    vars.add("NotAVariable");
                }
            } else if(expression instanceof PsiMethodCallExpression) {
                PsiMethod method = ((PsiMethodCallExpression) expression).resolveMethod();
                if (method == null) {
                    vars.add("UnresolvableMethodCall");
                    continue;
                }
                String returnType = method.getReturnType().getPresentableText();
                vars.add(returnType);
            } else if (expression instanceof PsiLiteralExpression) {
                try {
                    vars.add(((PsiLiteralExpression) expression).getValue().toString());
                } catch (NullPointerException e) {
                    logString.append(((PsiLiteralExpression) expression).getText());
                }
            } else if (expression instanceof PsiPolyadicExpression) {
                vars.add(concatPolyadicExpressionToStringWithVarTypes((PsiPolyadicExpression)expression));
            } else { // should not come here
                vars.add(expression.getText());
            }
        }

        if (vars.size() == 0) {
            return logString.toString();
        }

        // replace the place holders ("{}") in the log string with the variables
        Pattern pPlaceHolder = Pattern.compile("\\{\\}");
        Matcher mPlaceHolder = pPlaceHolder.matcher(logString);
        int varIndex = 0;
        StringBuffer combinedStr = new StringBuffer();
        while (mPlaceHolder.find()) {
            //int start = mPlaceHolder.start();
            //int end = mPlaceHolder.end();
            mPlaceHolder.appendReplacement(combinedStr, vars.get(varIndex));
            varIndex += 1;
            if (varIndex >= vars.size()) {
                break;
            }
        }
        mPlaceHolder.appendTail(combinedStr);

        // add the exception, if any, and other remaining vars to the end of the string
        if (varIndex < vars.size()) {
            for (String var : vars.subList(varIndex, vars.size())) {
                combinedStr.append(" ").append(var);
            }
        }

        return combinedStr.toString();
    }

    private String concatPolyadicExpressionToStringWithVarTypes(PsiPolyadicExpression polyadicExpression) {
        PsiExpression[] expressions = polyadicExpression.getOperands();
        StringBuilder logString = new StringBuilder();

        for (PsiExpression expression : expressions) {
            if (expression instanceof PsiLiteralExpression) {
                try {
                    logString.append(((PsiLiteralExpression) expression).getValue().toString());
                } catch (NullPointerException e) {
                    logString.append(((PsiLiteralExpression) expression).getText());
                }
            } else if (expression instanceof PsiReferenceExpression) {
                PsiElement resolvedElement = expression.getReference().resolve();
                if (resolvedElement == null) {
                    logString.append("UnresolvableVariable");
                    continue;
                }
                if (resolvedElement instanceof PsiVariable) {
                    try {
                        //PsiType variableType = PsiTreeUtil.findChildOfType((PsiVariable) resolvedElement,
                        //        PsiTypeElement.class).getType();
                        PsiType variableType = ((PsiVariable)resolvedElement).getType();
                        logString.append(variableType.getPresentableText());
                    } catch (NullPointerException e) {
                        logString.append("UnresolvableVariableNoType");
                    }
                } else {
                    logString.append("NotAVariable");
                }
            } else if(expression instanceof PsiMethodCallExpression) {
                PsiMethod method = ((PsiMethodCallExpression) expression).resolveMethod();
                if (method == null) {
                    logString.append("UnresolvableMethodCall");
                    continue;
                }
                String returnType = method.getReturnType().getPresentableText();
                logString.append(returnType);
            } else if (expression instanceof PsiPolyadicExpression) {
                logString.append(concatPolyadicExpressionToStringWithVarTypes((PsiPolyadicExpression)expression));
            }
        }

        return logString.toString();
    }

    private String extractLogBody(PsiMethodCallExpression logStmt) {
        return logStmt.getText();
    }

    private String extractContainingMethod(PsiMethodCallExpression logStmt) {
        PsiMethod method = PsiTreeUtil.getParentOfType(logStmt, PsiMethod.class);
        if (method!=null)
            return method.getText();
        else
            return "";
    }

    private boolean extractIsStackTraceLogged(PsiMethodCallExpression logStmt) {

        // parameter lists of the logging statement
        PsiExpressionList expressionList = PsiTreeUtil.getChildOfType(logStmt, PsiExpressionList.class);

        // PsiReferenceExpression parameters (parameters simply refer to other variables, no method invocation or calculation)
        // getChildrenOfType gets the PsiReferenceExpression (e.g., e) in a non-recursive way.
        // Thus expressions involving e (e.g., e.getMessage()) won't be counted.
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
