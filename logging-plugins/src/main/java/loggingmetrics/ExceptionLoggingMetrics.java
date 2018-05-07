package loggingmetrics;

import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ExceptionLoggingMetrics {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionLoggingMetrics.class);

    private PsiMethodCallExpression logStmt;
    private LogLevel logLevel;
    private boolean isStackTraceLogged;
    private List<PsiType> exceptionTypes;
    private List<PsiMethod> exceptionMethods;
    private Project project;

    public ExceptionLoggingMetrics(PsiMethodCallExpression logStmt) {
        this.logStmt = logStmt;
        this.exceptionTypes = deriveExceptionTypes(logStmt);
        this.exceptionMethods = deriveExceptionMethods(logStmt);
        this.project = logStmt.getProject();

        // debugging
        /*
        for (PsiType t : exceptionTypes) {
            //logger.debug("Type name: " + t.getCanonicalText() + ", type class: " + getExceptionClass(t).getQualifiedName());
            //getExceptionSource(t);
            logger.debug("Exception: " + t.getCanonicalText() +
                    ", Exception category: " + getExceptionCategory(t) +
                    ", Exception source: " + getExceptionSource(t));
        }
        logger.debug("Presentable exception types: " + getPresentableExceptionTypes());
        logger.debug("Presentable exception category: " + getPresentableExceptionCategory());
        logger.debug("Presentable exception source: " + getPresentableExceptionSource());
        */
        /*
        logger.debug("Exception methods: " + getPresentableExceptionMethods());
        logger.debug("Method source: " + getPresentableExceptionMethodSource());
        */
    }

    public static String getLoggingMetricsHeader() {
        List<String> metricsHeader = new ArrayList<>();
        // log identification/index
        metricsHeader.add("logLocation"); // the location of the logging statement - fileName:lineNumber

        // response variables
        metricsHeader.add("logLevel"); // log level of the logging statement
        metricsHeader.add("logStackTrace"); // if the logging statement logged the stack trace

        //explanatory variables
        metricsHeader.add("exceptionType"); // exceptions caught by the containing catch block
        metricsHeader.add("exceptionCategory"); // Normal exception, RuntimeException, or Error
        metricsHeader.add("exceptionSource"); // project, library, or JDK
        metricsHeader.add("exceptionNum"); // number of exceptions caught by the containing catch block
        metricsHeader.add("exceptionMethodCall"); // method call that throws the caught exceptions
        metricsHeader.add("exceptionMethodSource"); // project, library, or JDK
        metricsHeader.add("exceptionMethodNum"); // number of methods that throw the caught exceptions
        metricsHeader.add("catchInLoop"); // if the containing catch block is in a loop
        metricsHeader.add("isLogInLoop"); // if the logging statement is in a loop within the containing catch block
        metricsHeader.add("numMethodCallsBeforeLogging"); // number of method calls in the containing catch block before the logging statement
        metricsHeader.add("numMethodCallsAfterLogging"); // number of method calls in the containing catch block after the logging statement
        metricsHeader.add("LOCBeforeLogging"); // lines of code in the containing catch block before the logging statement
        metricsHeader.add("LOCAfterLogging"); // lines of code the containing block after the logging statement
        metricsHeader.add("numMethodCallsInTryBlock"); // number of method calls in the containing try block
        metricsHeader.add("isLogInBranch"); // if the logging statement is in a branch statement (excluding logging guard) within the containing catch block
        metricsHeader.add("throwInCatchBlock"); // does the catch block contain throw statements
        metricsHeader.add("returnInCatchBlock"); // does the catch block contain return statements
        metricsHeader.add("throwInTryBlock"); // does the try block contain throw statements
        metricsHeader.add("returnInTryBlock"); // does the try block contain return statements

        return String.join(",", metricsHeader);
    }

    public String getLoggingMetrics() {
        List<String> metrics = new ArrayList<>();

        // log identification/index
        PsiFile psiFile = this.logStmt.getContainingFile();
        int lineNumber = StringUtil.offsetToLineNumber(psiFile.getText(), this.logStmt.getTextOffset()) + 1;
        metrics.add(psiFile.getVirtualFile().getName() + ":" + lineNumber);

        // response variables


        //explanatory variables
        metrics.add(getPresentableExceptionTypes());
        metrics.add(getPresentableExceptionCategory());
        metrics.add(getPresentableExceptionSource());
        metrics.add(String.valueOf(getExceptionTypes().size()));
        metrics.add(getPresentableExceptionMethods());
        metrics.add(getPresentableExceptionMethodSource());
        metrics.add(String.valueOf(getExceptionMethods().size()));
        metrics.add(String.valueOf(isCatchBlockWithInLoop()));
        metrics.add(String.valueOf(isLoggingStatementWithinLoop()));
        int[] numMethodCalls = getNumMethodCallsBeforeAndAfterLogging();
        metrics.add(String.valueOf(numMethodCalls[0]));
        metrics.add(String.valueOf(numMethodCalls[1]));
        int[] LOCs = getLOCBeforeAndAfterLogging();
        metrics.add(String.valueOf(LOCs[0]));
        metrics.add(String.valueOf(LOCs[1]));
        metrics.add(String.valueOf(getNumMethodCallsInTryBlock()));
        metrics.add(String.valueOf(isLoggingStatementWithinBranch()));
        metrics.add(String.valueOf(getNumThrowInCatchBlock() > 0));
        metrics.add(String.valueOf(getNumReturnInCatchBlock() > 0));
        metrics.add(String.valueOf(getNumThrowInTryBlock() > 0));
        metrics.add(String.valueOf(getNumReturnInTryBlock() > 0));


        return String.join(",",metrics);
    }

    public List<PsiType> getExceptionTypes() {return this.exceptionTypes;}
    public List<PsiMethod> getExceptionMethods() {return this.exceptionMethods;}

    public LogLevel getLogLevel() { return this.logLevel; }

    public boolean getIsStackTraceLogged() { return this.isStackTraceLogged; }

    public String getPresentableExceptionTypes() {
        if (this.exceptionTypes.size() == 0) {
            return "UnknownException";
        } else if (this.exceptionTypes.size() == 1) {
            return this.exceptionTypes.get(0).getPresentableText();
        } else {
            return "MultiExceptions";
/*            return exceptionTypes.
                    stream().
                    map(t -> t.getCanonicalText()).
                    reduce("", (a, b) -> a + " " + b);*/
        }
    }

    public String getPresentableExceptionMethods() {
        if (this.exceptionMethods.size() == 0) {
            return "UnknownMethod";
        } else if (this.exceptionMethods.size() == 1) {
            PsiMethod method = this.exceptionMethods.get(0);
            PsiClass containingClass = method.getContainingClass();
            return containingClass.getName() + "." + method.getName();
        } else {
            return "MultiMethods";
/*            return exceptionMethods.
                    stream().
                    map(m -> m.getName()).
                    reduce("", (a,b) -> a + " " +b );*/
        }
    }

    public String getPresentableExceptionSource() {
        if (this.exceptionTypes.size() == 1) {
            return getExceptionSource(this.exceptionTypes.get(0)).name();
        }
        // if all sources are the same, return the source; otherwise return "MIXED"
        String source = null;
        for (PsiType e: this.exceptionTypes) {
            if (source == null) {
                source = new String(getExceptionSource(e).name());
            } else if (!source.equals(getExceptionSource(e).name())) {
                return "MIXED";
            }
        }
        return source;
    }

    public String getPresentableExceptionCategory() {
        if (this.exceptionTypes.size() == 1) {
            return getExceptionCategory(this.exceptionTypes.get(0)).name();
        }
        // if all categories are the same, return the category; otherwise return "MIXED"
        String category = null;
        for (PsiType e : this.exceptionTypes) {
            if (category == null) {
                category = new String(getExceptionCategory(e).name());
            } else if (!category.equals(getExceptionCategory(e).name())) {
                return "MIXED";
            }
        }
        return category;
    }

    public String getPresentableExceptionMethodSource() {
        if (this.exceptionMethods.size() == 0) {
            return "UnknownMethod";
        }

        if (this.exceptionMethods.size() == 1) {
            return getMethodSource(this.exceptionMethods.get(0)).name();
        }
        // if all sources are the same, return the source; otherwise return "MIXED"
        String source = null;
        for (PsiMethod m : this.exceptionMethods) {
            if (source == null) {
                source = new String(getMethodSource(m).name());
            } else if (!source.equals(getMethodSource(m).name())) {
                return "MIXED";
            }
        }
        return source;
    }

    public boolean isCatchBlockWithInLoop() {
        // catch clause
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return false;
        }

        // containing loop statement of the catch block
        PsiLoopStatement loop = PsiTreeUtil.getParentOfType(catchSection, PsiLoopStatement.class);

        if (loop != null) {
            return true;
        }

        return false;
    }

    /**
     * If the logging statement is within a loop inside the containing catch block
     */
    public boolean isLoggingStatementWithinLoop() {
        // catch clause
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);

        // containing loop statement of the logging statement
        PsiLoopStatement loop = PsiTreeUtil.getParentOfType(this.logStmt, PsiLoopStatement.class);

        if (loop == null) {
            return false;
        }

        // if the containing loop is inside the containing catch block
        if (PsiTreeUtil.isAncestor(catchSection, loop, true)) {
            return true;
        }
        return false;
    }

    /**
     * If the logging statement is within a branch (excluding logging guard) inside the containing catch block
     */
    public boolean isLoggingStatementWithinBranch() {
        // catch clause
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);

        // containing branch statements
        PsiIfStatement ifStatement = PsiTreeUtil.getParentOfType(this.logStmt, PsiIfStatement.class);

        // exclude if statement as a logging guard
        if (ifStatement != null) {
            // getChildOfType only find the method calls in the if condition, i.e., the direct children of the if statement
            PsiMethodCallExpression firstMethodCall = PsiTreeUtil.getChildOfType(ifStatement, PsiMethodCallExpression.class);
            if (firstMethodCall != null) {
                PsiMethod method = (PsiMethod) (firstMethodCall.getMethodExpression().resolve());
                if (method.getContainingClass().getName().equals("Logger")) {
                    ifStatement = PsiTreeUtil.getParentOfType(ifStatement, PsiIfStatement.class);
                }
            }
        }

        PsiSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(this.logStmt, PsiSwitchStatement.class);

        if (ifStatement != null && PsiTreeUtil.isAncestor(catchSection, ifStatement, true)) {
            return true;
        }

        if (switchStatement != null && PsiTreeUtil.isAncestor(catchSection, switchStatement, true)) {
            return true;
        }

        return false;
    }

    /**
     *
     * @return numMethodCalls:
     * Index 0: number of method calls in the containing catch block that are before the logging statement;
     * Index 1: number of method calls in the containing catch block that are after the logging statement.
     */
    public int[] getNumMethodCallsBeforeAndAfterLogging() {
        // catch session
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return new int[2];
        }

        // catch-block
        PsiCodeBlock catchBlock = PsiTreeUtil.getChildOfType(catchSection, PsiCodeBlock.class);

        // method calls in the catch block
        Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(catchBlock, PsiMethodCallExpression.class);

        // position of the logging statement
        int log_start_offset =this.logStmt.getTextOffset();
        int log_end_offset = log_start_offset + this.logStmt.getTextLength() - 1;

        int[] numMethodCalls = new int[]{0, 0}; // index 0: number of method calls before the logging statement; index 1: after.
        for (PsiMethodCallExpression m : methodCalls) {
            int method_offset = m.getTextOffset();
            if (method_offset < log_start_offset) {
                numMethodCalls[0] += 1;
            } else if (method_offset > log_end_offset) {
                numMethodCalls[1] += 1;
            }
        }
        return numMethodCalls;
    }

    public int[] getLOCBeforeAndAfterLogging() {
        // catch session
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return new int[2];
        }

        // catch-block
        PsiCodeBlock catchBlock = PsiTreeUtil.getChildOfType(catchSection, PsiCodeBlock.class);

        // position of the catch block
        int catch_start_offset = catchBlock.getTextOffset();
        int catch_end_offset = catch_start_offset + catchBlock.getTextLength() - 1;
        PsiFile file = catchBlock.getContainingFile();
        int catch_start_line = StringUtil.offsetToLineNumber(file.getText(), catch_start_offset) + 1;
        int catch_end_line = StringUtil.offsetToLineNumber(file.getText(), catch_end_offset) + 1;

        // position of the logging statement
        int log_start_offset =this.logStmt.getTextOffset();
        int log_end_offset = log_start_offset + this.logStmt.getTextLength() - 1;
        int log_start_line = StringUtil.offsetToLineNumber(file.getText(), log_start_offset) + 1;
        int log_end_line = StringUtil.offsetToLineNumber(file.getText(), log_end_offset) + 1;

        int[] LOCs = new int[2];
        LOCs[0] = log_start_line - catch_start_line - 1;// loc before logging statement
        LOCs[1] = catch_end_line - log_end_line - 1; // loc after logging statement

        return LOCs;
    }

    public int getNumMethodCallsInTryBlock() {
        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(this.logStmt, PsiTryStatement.class);
        if (tryStatement == null) {
            return 0;
        }

        // try block
        PsiCodeBlock tryBlock = PsiTreeUtil.getChildOfType(tryStatement, PsiCodeBlock.class);

        // number of the method calls in the try block
        Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(tryBlock, PsiMethodCallExpression.class);

        return methodCalls.size();
    }

    public int getNumReturnInCatchBlock() {
        // catch session
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return 0;
        }

        Collection<PsiReturnStatement> returnStatements = PsiTreeUtil.findChildrenOfType(catchSection, PsiReturnStatement.class);
        return returnStatements.size();
    }

    public int getNumThrowInCatchBlock() {
        // catch session
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return 0;
        }

        Collection<PsiThrowStatement> throwStatements = PsiTreeUtil.findChildrenOfType(catchSection, PsiThrowStatement.class);
        return throwStatements.size();
    }

    public int getNumReturnInTryBlock() {
        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(this.logStmt, PsiTryStatement.class);
        if (tryStatement == null) {
            return 0;
        }

        // try block
        PsiCodeBlock tryBlock = PsiTreeUtil.getChildOfType(tryStatement, PsiCodeBlock.class);

        // number of the method calls in the try block
        Collection<PsiReturnStatement> returnStatements = PsiTreeUtil.findChildrenOfType(tryBlock, PsiReturnStatement.class);

        return returnStatements.size();

    }

    public int getNumThrowInTryBlock() {
        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(this.logStmt, PsiTryStatement.class);
        if (tryStatement == null) {
            return 0;
        }

        // try block
        PsiCodeBlock tryBlock = PsiTreeUtil.getChildOfType(tryStatement, PsiCodeBlock.class);

        // number of the method calls in the try block
        Collection<PsiThrowStatement> throwStatements = PsiTreeUtil.findChildrenOfType(tryBlock, PsiThrowStatement.class);

        return throwStatements.size();

    }

    private ReferenceSource getExceptionSource(PsiType ex) {
        PsiClass exClass = ((PsiClassType)ex).resolve();
        VirtualFile vf = exClass.getContainingFile().getVirtualFile();
        //logger.debug("Virtual file: " + vf.getCanonicalPath());
        ReferenceSource source = getReferenceSourceOfVirtualFile(vf);

        //logger.debug("Exception: " + ex.getCanonicalText() +
        //        ", source: " + source);

        return source;
    }

    private ExceptionCategory getExceptionCategory(PsiType ex) {
        String runtimeExStr = "java.lang.RuntimeException";
        String errorExStr = "java.lang.Error";

        if (ex.getCanonicalText().equals(runtimeExStr)) {
            return ExceptionCategory.RUNTIME;
        } else if (ex.getCanonicalText().equals(errorExStr)) {
            return ExceptionCategory.ERROR;
        }

        PsiType[] superTypes = ex.getSuperTypes();
        for (PsiType t : superTypes) {
            if (t.getCanonicalText().equals(runtimeExStr)) {
                return ExceptionCategory.RUNTIME;
            } else if (t.getCanonicalText().equals(errorExStr)) {
                return ExceptionCategory.ERROR;
            }
        }
        return ExceptionCategory.NORMAL;
    }

    private ReferenceSource getMethodSource(PsiMethod method) {
        VirtualFile vf = method.getContainingFile().getVirtualFile();
        ReferenceSource source = getReferenceSourceOfVirtualFile(vf);

        //logger.debug("Method: " + method.getContainingClass().getQualifiedName() + "." + method.getName() +
        //        ", source: " + source);
        return source;
    }

    @NotNull
    private ReferenceSource getReferenceSourceOfVirtualFile(VirtualFile vf) {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(this.project).getFileIndex();
        boolean isInLibraryOrJdk = projectFileIndex.isInLibrary(vf);
        boolean isInLibrary = false, isInJdk = false;
        //irtualFile ContentRoot = projectFileIndex.getContentRootForFile(vf);
        if (isInLibraryOrJdk) {
            List<OrderEntry> orderEntries = projectFileIndex.getOrderEntriesForFile(vf);
            for (OrderEntry entry : orderEntries) {
                if (entry instanceof LibraryOrderEntry) {
                    isInLibrary = true;
                }
                if (entry instanceof JdkOrderEntry) {
                    isInJdk = true;
                }
            }
        }

        if (isInJdk) {
            return ReferenceSource.FROMJDK;
        } else if (isInLibrary) {
            return ReferenceSource.FROMLIBRARY;
        } else {
            return ReferenceSource.FROMPROJECT;
        }
    }

    private PsiClass getExceptionClass(PsiType ex) {
        return ((PsiClassType)ex).resolve();
    }

    /**
     * Get the exception types of the catch block that contain the logging statement (i.e., logged exception types)
     * @return
     */
    private List<PsiType> deriveExceptionTypes(PsiMethodCallExpression logStmt) {
        // catch clause
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return null;
        }

        // exception parameter declaration
        PsiParameter para = PsiTreeUtil.findChildOfType(catchSection, PsiParameter.class);

        //logger.debug("Para: " + para.getText());

        // exception types
        Collection<PsiTypeElement> typeElements = PsiTreeUtil.findChildrenOfType(para, PsiTypeElement.class);

        StringBuilder typeStr = new StringBuilder();
        for (PsiTypeElement t : typeElements) {
            typeStr.append(t.getType().getCanonicalText()).append(" ").
                    append(PsiTreeUtil.findChildrenOfType(t, PsiTypeElement.class).size() > 0).append(",");
        }
        //logger.debug("Exception Types: " + typeStr);

        // for compound exception types (e.g., catch (InvocationTargetException | IllegalAccessException e)),
        // only keep the basic exception types which are the children of the compound exception types
        typeElements.removeIf(e -> (PsiTreeUtil.findChildrenOfType(e, PsiTypeElement.class).size() > 0));

        List<PsiType> exceptionTypes = typeElements.stream().map(e -> e.getType()).collect(Collectors.toList());

        return exceptionTypes;
    }

    /**
     * Get the methods in the try block that "throws" the logged exception types (or their sub types)
     * @return
     */
    private List<PsiMethod> deriveExceptionMethods(PsiMethodCallExpression logStmt) {
        List<PsiMethod> exMethods = new ArrayList<>();

        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(logStmt, PsiTryStatement.class);
        if (tryStatement == null) {
            return null;
        }

        // try-block
        PsiCodeBlock tryBlock = PsiTreeUtil.getChildOfType(tryStatement, PsiCodeBlock.class);

        // method calls in the try block
        Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(tryBlock, PsiMethodCallExpression.class);

        // new expressions (e.g., new LdapName(dn)) can also throw exceptions
        Collection<PsiNewExpression> newExpressions = PsiTreeUtil.findChildrenOfType(tryBlock, PsiNewExpression.class);

        // resolve method calls to method declarations
        List<PsiMethod> methods = new ArrayList<>();
        for (PsiMethodCallExpression methodCall : methodCalls) {
            //logger.debug("Method call: " + methodCall.getText());
            PsiMethod method = (PsiMethod) (methodCall.getMethodExpression().resolve());
            //PsiType[] throwsTypes = method.getThrowsList().getReferencedTypes();
            //logger.debug("Throws: " + Arrays.stream(throwsTypes).map(t -> t.getCanonicalText()).
            //        reduce("", (a, b) -> a + " " + b ));
            methods.add(method);
        }
        // resolve new expressions to method declarations (i.e., constructor declarations)
        for (PsiNewExpression newExpr : newExpressions) {
            PsiMethod method = newExpr.resolveMethod();
            //logger.debug("newExpression: " + newExpr.getText());
            //logger.debug("resolvedMethod: " + newExpr.resolveMethod().getName());
            //logger.debug("resolvedConsstructor: " + newExpr.resolveConstructor().getName());
            if (method != null) { // the resolved result can be null when a new expression is a anonymous class
                methods.add(method);
            }
        }

        // if there is only one method in the try block, then it is the exception throwing method
        if (methods.size() == 1) {
            return methods;
        }

        // select methods that declare exceptions same as the given exception type
        for (PsiMethod method : methods) {
            PsiType[] throwsTypes = method.getThrowsList().getReferencedTypes();

            boolean match = false;
            for (PsiType throwsType: throwsTypes) {
                for (PsiType caughtType : this.exceptionTypes) {
                    if (isSubTypeOrSameType(throwsType, caughtType)) {
                        exMethods.add(method);
                        match = true;
                        break;
                    }
                }
                if (match) break;
            }

        }

        return exMethods;
    }

    private boolean isSubTypeOrSameType(PsiType child, PsiType parent) {
        if (child.getCanonicalText().equals(parent.getCanonicalText())) {
            return true;
        }

        PsiType[] superTypes = child.getSuperTypes();
        for (PsiType superT : superTypes) {
            if (superT.getCanonicalText().equals(parent.getCanonicalText())) {
                return true;
            }
        }
        return false;
    }

}
