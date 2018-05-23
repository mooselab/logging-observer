package loggingmetrics;

import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
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
    String logLevel;
    private boolean isStackTraceLogged;
    private List<PsiType> exceptionTypes;
    private List<PsiMethod> exceptionMethods;
    private Project project;

    public ExceptionLoggingMetrics(PsiMethodCallExpression logStmt) {
        this.logStmt = logStmt;
        this.project = logStmt.getProject();
        this.exceptionTypes = deriveExceptionTypes(logStmt);
        this.exceptionMethods = resolveExceptionMethods(logStmt);
        this.logLevel = extractLogLevel(logStmt);
        this.isStackTraceLogged = deriveIsStackTraceLogged(logStmt);

        // debugging
        /*
        for (PsiType t : exceptionTypes) {
            //logger.debug("Type name: " + t.getCanonicalText() + ", type class: " + getExceptionClass(t).getQualifiedName());
            //getExceptionSource(t);
            logger.debug("Exception: " + t.getCanonicalText() +
                    ", Exception category: " + getExceptionCategory(t) +
                    ", Exception source: " + getExceptionSource(t));
        }
        logger.debug("Presentable exception types: " + getPresentableExceptionType());
        logger.debug("Presentable exception category: " + getPresentableExceptionCategory());
        logger.debug("Presentable exception source: " + getPresentableExceptionSource());
        */
        /*
        logger.debug("Exception methods: " + getPresentableExceptionMethod());
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
        metricsHeader.add("containingPackage");
        metricsHeader.add("exceptionType"); // exceptions caught by the containing catch block
        metricsHeader.add("parentExType");
        metricsHeader.add("grandParentExType");
        metricsHeader.add("exceptionPackage");
        metricsHeader.add("exceptionCategory"); // Normal exception, RuntimeException, or Error
        metricsHeader.add("exceptionSource"); // project, library, or JDK
        metricsHeader.add("exceptionNum"); // number of exceptions caught by the containing catch block
        metricsHeader.add("exceptionMethodCall"); // method call that throws the caught exceptions
        metricsHeader.add("exceptionMethodPackage");
        metricsHeader.add("exceptionMethodSource"); // project, library, or JDK
        metricsHeader.add("exceptionMethodNum"); // number of methods that throw the caught exceptions
        metricsHeader.add("catchInLoop"); // if the containing catch block is in a loop
        metricsHeader.add("isLogInInnerLoop"); // if the logging statement is in a loop within the containing catch block
        metricsHeader.add("isLogInInnerBranch"); // if the logging statement is in a branch statement (excluding logging guard) within the containing catch block
        metricsHeader.add("isLogInInnerTryBlock"); // if the logging statement is in a try block within the containing catch block (i.e., an inner try - finally block without catch block)
        metricsHeader.add("throwInCatchBlock"); // does the catch block contain throw statements
        metricsHeader.add("returnInCatchBlock"); // does the catch block contain return statements
        metricsHeader.add("throwInTryBlock"); // does the try block contain throw statements
        metricsHeader.add("returnInTryBlock"); // does the try block contain return statements
        metricsHeader.add("numMethodCallsBeforeLogging"); // number of method calls in the containing catch block before the logging statement
        metricsHeader.add("numMethodCallsAfterLogging"); // number of method calls in the containing catch block after the logging statement
        metricsHeader.add("LOCBeforeLogging"); // lines of code in the containing catch block before the logging statement
        metricsHeader.add("LOCAfterLogging"); // lines of code the containing block after the logging statement
        metricsHeader.add("numMethodCallsInTryBlock"); // number of method calls in the containing try block
        metricsHeader.add("LOCInTryBlock"); // LOC in the try block
        metricsHeader.add("LOCInFile"); // LOC in the file
        metricsHeader.add("LOCInMethod"); // LOC in the containing method
        metricsHeader.add("numMethodCallsInMethod"); // number of method calls n the containing method


        return String.join(",", metricsHeader);
    }

    public String getLoggingMetrics() {
        List<String> metrics = new ArrayList<>();

        // log identification/index
        String fileLocation = getLocationInFile(this.logStmt);
        metrics.add(fileLocation);

        // response variables
        metrics.add(getLogLevel());
        metrics.add(String.valueOf(getIsStackTraceLogged()));

        //explanatory variables
        metrics.add(getContainingPackageName(this.logStmt));
        metrics.add(getPresentableExceptionType());
        metrics.add(getParentExceptionType());
        metrics.add(getGrandParentType());
        metrics.add(getExceptionPackageName());
        metrics.add(getPresentableExceptionCategory());
        metrics.add(getPresentableExceptionSource());
        metrics.add(String.valueOf(getExceptionTypes().size()));
        metrics.add(getPresentableExceptionMethod());
        metrics.add(getExceptionMethodPackageName());
        metrics.add(getPresentableExceptionMethodSource());
        metrics.add(String.valueOf(getExceptionMethods().size()));
        metrics.add(String.valueOf(isCatchBlockWithInLoop()));
        metrics.add(String.valueOf(isLoggingStatementWithinInnerLoop()));
        metrics.add(String.valueOf(isLoggingStatementWithinInnerBranch()));
        metrics.add(String.valueOf(isLoggingStatementWithinInnderTryBlock()));
        metrics.add(String.valueOf(getNumThrowInCatchBlock() > 0));
        metrics.add(String.valueOf(getNumReturnInCatchBlock() > 0));
        metrics.add(String.valueOf(getNumThrowInTryBlock() > 0));
        metrics.add(String.valueOf(getNumReturnInTryBlock() > 0));
        int[] numMethodCalls = getNumMethodCallsBeforeAndAfterLogging();
        metrics.add(String.valueOf(numMethodCalls[0]));
        metrics.add(String.valueOf(numMethodCalls[1]));
        int[] LOCs = getLOCBeforeAndAfterLogging();
        metrics.add(String.valueOf(LOCs[0]));
        metrics.add(String.valueOf(LOCs[1]));
        metrics.add(String.valueOf(getNumMethodCallsInTryBlock()));
        metrics.add(String.valueOf(getTryBlockLOC()));
        metrics.add(String.valueOf(getFileLOC()));
        metrics.add(String.valueOf(getMethodLOC()));
        metrics.add(String.valueOf(getNumMethodCallsInMethod()));

        return String.join(",",metrics);
    }

    @NotNull
    public String getLocationInFile(PsiElement element) {
        PsiFile psiFile = element.getContainingFile();
        int lineNumber = StringUtil.offsetToLineNumber(psiFile.getText(), element.getTextOffset()) + 1;
        return psiFile.getVirtualFile().getName() + ":" + lineNumber;
    }

    public List<PsiType> getExceptionTypes() {return this.exceptionTypes;}
    public List<PsiMethod> getExceptionMethods() {return this.exceptionMethods;}

    public String getLogLevel() { return this.logLevel; }

    public boolean getIsStackTraceLogged() { return this.isStackTraceLogged; }

    public String getContainingPackageName(PsiElement element) {
        //PsiJavaFile javaFile = (PsiJavaFile) element.getContainingFile();
        PsiFile file = element.getContainingFile();
        if (file instanceof PsiClassOwner) {
            return ((PsiClassOwner)file).getPackageName();
        } else {
            return "UnknownPackageName";
        }
    }

    public String getPresentableExceptionType() {
        if (this.exceptionTypes.size() == 0) {
            return "UnknownException";
        } else {// if (this.exceptionTypes.size() == 1) {
            // only consider the first exception
            return this.exceptionTypes.get(0).getPresentableText();
        } /*else {
            return "MultiExceptions";
        }*/
    }

    public String getParentExceptionType() {
        if (this.exceptionTypes.size() == 0) {
            return "UnknownException";
        } else {
            PsiType[] parentTypes = this.exceptionTypes.get(0).getSuperTypes();
            if (parentTypes.length == 0) {
                return "NoParentException";
            } else {
                return parentTypes[0].getPresentableText();
            }
        }
    }

    public String getGrandParentType() {
        if (this.exceptionTypes.size() == 0) {
            return "UnknownException";
        } else {
            PsiType[] parentTypes = this.exceptionTypes.get(0).getSuperTypes();
            if (parentTypes.length == 0) {
                return "NoParentException";
            } else {
                PsiType[] grandParentTypes = parentTypes[0].getSuperTypes();
                if (grandParentTypes.length == 0) {
                    return "NoGrandParentException";
                } else {
                    return grandParentTypes[0].getPresentableText();
                }
            }
        }
    }

    public String getPresentableExceptionMethod() {
        if (this.exceptionMethods.size() == 0) {
            return "UnknownMethod";
        } else {//if (this.exceptionMethods.size() == 1) {
            // only consider the first method
            PsiMethod method = this.exceptionMethods.get(0);
            PsiClass containingClass = method.getContainingClass();
            return containingClass.getName() + "." + method.getName();
        } /*else {
            return "MultiMethods";
        }*/
    }

    private String getExceptionPackageName() {
        if (this.exceptionTypes.size() == 0) {
            return "UnknownException";
        } else {
            PsiClass exClass = ((PsiClassType)this.exceptionTypes.get(0)).resolve();
            if (exClass == null) {
                return "UnknownExceptionClass";
            } else {
                return getContainingPackageName(exClass);
            }
        }
    }

    public String getExceptionMethodPackageName() {
        if (this.exceptionMethods.size() == 0) {
            return "UnknownMethod";
        } else {//if (this.exceptionMethods.size() == 1) {
            // only consider the first method
            PsiMethod method = this.exceptionMethods.get(0);
            return getContainingPackageName(method);
        } /*else {
            return "MultiMethods";
        }*/
    }

    public String getPresentableExceptionSource() {
        if (this.exceptionTypes.size() == 0) {
            return "UnknownException";
        } else { //if (this.exceptionTypes.size() == 1) {s
            // only consider the first exception
            return getExceptionSource(this.exceptionTypes.get(0)).name();
        }
        /*
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
        */
    }

    public String getPresentableExceptionCategory() {
        if (this.exceptionTypes.size() == 0) {
            return "UnknownException";
        } else { //if (this.exceptionTypes.size() == 1) {
            // only consider the first exception
            return getExceptionCategory(this.exceptionTypes.get(0)).name();
        }
        /*
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
        */
    }

    public String getPresentableExceptionMethodSource() {
        if (this.exceptionMethods.size() == 0) {
            return "UnknownMethod";
        } else { //if (this.exceptionMethods.size() == 1) {
            // only consider the first method
            return getMethodSource(this.exceptionMethods.get(0)).name();
        }

        /*
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
        */
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
    public boolean isLoggingStatementWithinInnerLoop() {
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
    public boolean isLoggingStatementWithinInnerBranch() {
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
                if (method != null) {
                    try {
                        if (method.getContainingClass().getName().equals("Logger")) {
                            ifStatement = PsiTreeUtil.getParentOfType(ifStatement, PsiIfStatement.class);
                        }
                    } catch (NullPointerException e) {
                        //logger.warn("Cannot get containing class of method " + method.getName());
                    }
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

    public boolean isLoggingStatementWithinInnderTryBlock() {
        // catch clause
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);

        // containing try statement of the logging statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(this.logStmt, PsiTryStatement.class);

        if (tryStatement == null) {
            return false;
        }

        // if the containing try statement is inside the containing catch block
        if (PsiTreeUtil.isAncestor(catchSection, tryStatement, true)) {
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

    public int getTryBlockLOC() {
        // containing catch block
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return 0;
        }

        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(catchSection, PsiTryStatement.class);
        if (tryStatement == null) {
            return 0;
        }

        // try block
        PsiCodeBlock tryBlock = PsiTreeUtil.getChildOfType(tryStatement, PsiCodeBlock.class);

        // position of the try block
        int try_start_offset = tryBlock.getTextOffset();
        int try_end_offset = try_start_offset + tryBlock.getTextLength() - 1;
        PsiFile file = tryBlock.getContainingFile();
        int try_start_line = StringUtil.offsetToLineNumber(file.getText(), try_start_offset) + 1;
        int try_end_line = StringUtil.offsetToLineNumber(file.getText(), try_end_offset) + 1;

        return try_end_line - try_start_line - 1;
    }

    public int getMethodLOC() {
        // containing method
        PsiMethod method = PsiTreeUtil.getParentOfType(this.logStmt, PsiMethod.class);

        if (method != null) {

            // position of the method
            int method_start_offset = method.getTextOffset();
            int method_end_offset = method_start_offset + method.getTextLength() - 1;
            PsiFile file = method.getContainingFile();
            //if (method_end_offset > file.getText().length()) method_end_offset = file.getText().length();
            int method_start_line = StringUtil.offsetToLineNumber(file.getText(), method_start_offset) + 1;
            int method_end_line = StringUtil.offsetToLineNumber(file.getText(), method_end_offset) + 1;

            if (method_end_line - method_start_line -1 < 0) {
                method_end_line = getFileLOC();
                logger.warn("LOC is negative: " + getLocationInFile(this.logStmt) +
                        ", method_start_offset: " + method_start_offset +
                ", method_end_offset: " + method_end_offset +
                ", method_start_line: " + method_start_line +
                ", method_end_line: " + method_end_line +
                ", text length: " + file.getText().length());
            }

            return method_end_line - method_start_line - 1;
        } else {

            PsiClassInitializer classInitializer = PsiTreeUtil.getParentOfType(this.logStmt,
                    PsiClassInitializer.class);

            if (classInitializer == null) {
                logger.warn("Could not find containing method or class initializer of logging statement at: " +
                        getLocationInFile(this.logStmt));
                return 0;
            }

            // position of the class initializer
            int initializer_start_offset = classInitializer.getTextOffset();
            int initializer_end_offset = initializer_start_offset + classInitializer.getTextLength() - 1;
            PsiFile file = classInitializer.getContainingFile();
            //if (initializer_end_offset > file.getText().length()) initializer_end_offset = file.getText().length();
            int initializer_start_line = StringUtil.offsetToLineNumber(file.getText(), initializer_start_offset) + 1;
            int initializer_end_line = StringUtil.offsetToLineNumber(file.getText(), initializer_end_offset) + 1;

            if (initializer_end_line - initializer_start_line - 1 < 0) {
                initializer_end_line = getFileLOC();
            }

            return initializer_end_line - initializer_start_line - 1;
        }
    }

    public int getFileLOC() {
        PsiFile file = this.logStmt.getContainingFile();
        return StringUtil.getLineBreakCount(file.getText());
    }

    public int getNumMethodCallsInTryBlock() {
        // containing catch block
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return 0;
        }

        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(catchSection, PsiTryStatement.class);
        if (tryStatement == null) {
            return 0;
        }

        // try block
        PsiCodeBlock tryBlock = PsiTreeUtil.getChildOfType(tryStatement, PsiCodeBlock.class);

        // number of the method calls in the try block
        Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(tryBlock, PsiMethodCallExpression.class);

        return methodCalls.size();
    }

    public int getNumMethodCallsInMethod() {
        // containing method
        PsiMethod method = PsiTreeUtil.getParentOfType(this.logStmt, PsiMethod.class);
        Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(method, PsiMethodCallExpression.class);

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
        // containing catch block
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return 0;
        }

        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(catchSection, PsiTryStatement.class);
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
        // containing catch block
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(this.logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return 0;
        }

        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(catchSection, PsiTryStatement.class);
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
        if (exClass == null) {
            return ReferenceSource.UNKNOWN;
        }
        VirtualFile vf = exClass.getContainingFile().getVirtualFile();
        //logger.debug("Virtual file: " + vf.getCanonicalPath());
        ReferenceSource source = getReferenceSourceOfVirtualFile(vf);

        //logger.debug("Exception: " + ex.getCanonicalText() +
        //        ", source: " + source);

        return source;
    }

    private ExceptionCategory getExceptionCategory(PsiType ex) {
        if (isRuntimeExceptionType(ex)) {
            return ExceptionCategory.RUNTIME;
        } else if (isErrorType(ex)) {
            return ExceptionCategory.ERROR;
        } else if (isCheckedException(ex)) {
            return ExceptionCategory.CHECKED;
        } else {
            return ExceptionCategory.GENERAL;
        }
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
        List<PsiType> exceptionTypes = extractExceptionTypesForCatchSection(catchSection);

        return exceptionTypes;
    }

    private List<PsiType> extractExceptionTypesForCatchSection(PsiCatchSection catchSection) {
        // exception parameter declaration
        PsiParameter para = PsiTreeUtil.findChildOfType(catchSection, PsiParameter.class);

        //logger.debug("Para: " + para.getText());

        // exception types
        Collection<PsiTypeElement> typeElements = PsiTreeUtil.findChildrenOfType(para, PsiTypeElement.class);

        /*
        StringBuilder typeStr = new StringBuilder();
        for (PsiTypeElement t : typeElements) {
            typeStr.append(t.getType().getCanonicalText()).append(" ").
                    append(PsiTreeUtil.findChildrenOfType(t, PsiTypeElement.class).size() > 0).append(",");
        }
        */
        //logger.debug("Exception Types: " + typeStr);

        // for compound exception types (e.g., catch (InvocationTargetException | IllegalAccessException e)),
        // only keep the basic exception types which are the children of the compound exception types
        typeElements.removeIf(e -> (PsiTreeUtil.findChildrenOfType(e, PsiTypeElement.class).size() > 0));

        return typeElements.stream().map(e -> e.getType()).collect(Collectors.toList());
    }

    /**
     * Get the methods in the try block that "throws" the logged exception types (or their sub types)
     * @return
     */
    private List<PsiMethod> resolveExceptionMethods(PsiMethodCallExpression logStmt) {
        List<PsiMethod> exMethods = new ArrayList<>();

        // containing catch block
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return null;
        }

        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(catchSection, PsiTryStatement.class);
        if (tryStatement == null) {
            return null;
        }


        // exceptions caught by prior sibling catches
        PsiCatchSection[] siblingCatches = PsiTreeUtil.getChildrenOfType(tryStatement, PsiCatchSection.class);
        List<PsiType> priorSiblingCaughtExceptions = new ArrayList<>();
        for (PsiCatchSection c : siblingCatches) {
            if (c.getTextOffset() < catchSection.getTextOffset()) { // prior sibling catch clause
                priorSiblingCaughtExceptions.addAll(extractExceptionTypesForCatchSection(c));
            }
        }

        // exceptions caught by all prior catches (including sibling catches and internal catches)
        Collection<PsiCatchSection> allPriorCatches = PsiTreeUtil.findChildrenOfType(tryStatement, PsiCatchSection.class);
        allPriorCatches.removeIf(c -> {
            return c.getTextOffset() >= catchSection.getTextOffset();
        });

        /*
        List<PsiType> priorCaughtExceptions = new ArrayList<>();
        for (PsiCatchSection c : allCatches) {
            if (c.getTextOffset() < catchSection.getTextOffset()) { // prior catch clause
                priorCaughtExceptions.addAll(extractExceptionTypesForCatchSection(c));
            }
        }
        */


        // try-block
        PsiCodeBlock tryBlock = PsiTreeUtil.getChildOfType(tryStatement, PsiCodeBlock.class);

        // checked if the caught exceptions are thrown in the containing try block

        Collection<PsiThrowStatement> throwStatements = PsiTreeUtil.findChildrenOfType(tryBlock, PsiThrowStatement.class);
        for (PsiThrowStatement throwS : throwStatements) {
            PsiNewExpression newExpr = PsiTreeUtil.findChildOfType(throwS, PsiNewExpression.class);
            if (newExpr == null) {
                //logger.debug("Cannot get new expression of throw statement: " + throwS.getText());
                continue;
            }
            PsiClass exClass = (PsiClass) newExpr.getClassReference().resolve();
            if (exClass == null) {
                //logger.debug("Fail to resolve exception class for new expression: " + newExpr.getText());
                continue;
            }
            //logger.info("Resolved exception class for new expression: " + newExpr.getText() +
            //        ", to exception class: " + exClass.getQualifiedName());

            //logger.debug("New expression: " + newExpr.getText() + ", Exception Class name: " + exClass.getQualifiedName());
            PsiType thrownExType = getPsiTypeByQualifiedName(exClass.getQualifiedName());
            if (thrownExType == null) continue;

            // CHeck if the thrown exception is caught by prior catches
            boolean caughtByPriorCatches = false;
            for (PsiCatchSection priorCatch : allPriorCatches) {
                // Check if the prior catch is able to catch the thrown exception
                // (exception is thrown within the corresponding try block
                PsiTryStatement matchingTryStmt = PsiTreeUtil.getParentOfType(priorCatch, PsiTryStatement.class);
                PsiCodeBlock matchingTryBlock = PsiTreeUtil.getChildOfType(matchingTryStmt, PsiCodeBlock.class);
                if (!PsiTreeUtil.isAncestor(matchingTryBlock, throwS, true)) {
                    continue;
                }

                // check if the prior caught exceptions match the thrown exception
                List<PsiType> caughtTypes = extractExceptionTypesForCatchSection(priorCatch);
                for (PsiType caughtType : caughtTypes) {
                    if (isSubType(thrownExType, caughtType, false)) {
                        caughtByPriorCatches = true;
                        break;
                    }
                }
                if (caughtByPriorCatches) break;
            }

            if (caughtByPriorCatches) continue;

            // Check if the thrown exception is caught by current catch
            boolean caughtByCurrentCatch = false;
            for (PsiType caughtType : this.exceptionTypes) {
                if (isSubType(thrownExType, caughtType, false)) {
                    PsiMethod method = PsiTreeUtil.getParentOfType(throwS, PsiMethod.class);
                    exMethods.add(method);
                    caughtByCurrentCatch = true;
                    break;
                }
            }
            if (caughtByCurrentCatch) break;
        }

        // method calls in the try block
        Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(tryBlock, PsiMethodCallExpression.class);

        // new expressions (e.g., new LdapName(dn)) can also throw exceptions
        Collection<PsiNewExpression> newExpressions = PsiTreeUtil.findChildrenOfType(tryBlock, PsiNewExpression.class);

        // combine method calls and new expressions
        List<PsiCallExpression> callExpressions = new ArrayList<>();
        callExpressions.addAll(methodCalls);
        callExpressions.addAll(newExpressions);

        // if there is no method call, return
        if (callExpressions.size() == 0) return exMethods;

        // if there is only one method call in the try block, then it is the exception throwing method
        if (exMethods.size() == 0  && callExpressions.size() == 1) {
            PsiMethod method = callExpressions.get(0).resolveMethod();
            if (method != null) {
                exMethods.add(method);
                return exMethods;
            }
        }

        // Check if the caught exceptions are specified in methods' throws list

        // Match exceptions with methods inside the try block
        // Caught exception is equal to or is parent of method-specified exception
        // Excluding exceptions that are caught by prior catches
        for (PsiCallExpression callExpr : callExpressions) {
            PsiMethod method = callExpr.resolveMethod();
            if (method == null) continue;

            PsiType[] throwsTypes = method.getThrowsList().getReferencedTypes();

            for (PsiType throwsType: throwsTypes) {
                // Check if the specified exception is caught by prior catches
                boolean caughtByPriorCatches = false;
                for (PsiCatchSection priorCatch : allPriorCatches) {
                    // Check if the prior catch is able to catch the thrown exception
                    // (exception is thrown within the corresponding try block
                    PsiTryStatement matchingTryStmt = PsiTreeUtil.getParentOfType(priorCatch, PsiTryStatement.class);
                    PsiCodeBlock matchingTryBlock = PsiTreeUtil.getChildOfType(matchingTryStmt, PsiCodeBlock.class);
                    if (!PsiTreeUtil.isAncestor(matchingTryBlock, callExpr, true)) {
                        continue;
                    }

                    // check if the prior caught exceptions match the thrown exception
                    List<PsiType> caughtTypes = extractExceptionTypesForCatchSection(priorCatch);
                    for (PsiType caughtType : caughtTypes) {
                        if (isSubType(throwsType, caughtType, false)) {
                            caughtByPriorCatches = true;
                            break;
                        }
                    }
                    if (caughtByPriorCatches) break;
                }

                if (caughtByPriorCatches) continue;

                // Check if the specified exception is caught by the current catch
                boolean caughtByCurrentCatch = false;
                for (PsiType caughtType : this.exceptionTypes) {
                    if (isSubType(throwsType, caughtType, false)) {
                        exMethods.add(method);
                        caughtByCurrentCatch = true;
                        break;
                    }
                }
                if (caughtByCurrentCatch) break;
            }
            //if (match) break; // only choose the first matching method.

        }

        // if caught exceptions are resolved to either throw statements or method calls, return the results
        if (exMethods.size() > 0) {
            return exMethods;
        }

        // If caught exceptions are not resolved, try something else (release the throw-throws-catch matching criterion)

        // Child match (catch exception can be the child of a method-specified exception)
        // In cases when the exception thrown at runtime can be the caught exception, even though the specified exception is the parent
        for (PsiCallExpression callExpr : callExpressions) {
            PsiMethod method = callExpr.resolveMethod();
            if (method == null) continue;

            PsiType[] throwsTypes = method.getThrowsList().getReferencedTypes();

            for (PsiType throwsType: throwsTypes) {
                // Check if the specified exception is caught by prior catches
                boolean caughtByPriorCatches = false;
                for (PsiCatchSection priorCatch : allPriorCatches) {
                    // Check if the prior catch is able to catch the thrown exception
                    // (exception is thrown within the corresponding try block
                    PsiTryStatement matchingTryStmt = PsiTreeUtil.getParentOfType(priorCatch, PsiTryStatement.class);
                    PsiCodeBlock matchingTryBlock = PsiTreeUtil.getChildOfType(matchingTryStmt, PsiCodeBlock.class);
                    if (!PsiTreeUtil.isAncestor(matchingTryBlock, callExpr, true)) {
                        continue;
                    }

                    // check if the prior caught exceptions match the thrown exception
                    List<PsiType> caughtTypes = extractExceptionTypesForCatchSection(priorCatch);
                    for (PsiType caughtType : caughtTypes) {
                        if (isSubType(throwsType, caughtType, false)) {
                            caughtByPriorCatches = true;
                            break;
                        }
                    }
                    if (caughtByPriorCatches) break;
                }

                if (caughtByPriorCatches) continue;

                // Check if the specified exception is caught by the current catch
                boolean caughtByCurrentCatch = false;
                for (PsiType caughtType : this.exceptionTypes) {
                    // release the throws-catch matching criterion
                    if (isSubType(throwsType, caughtType, false) ||
                            isSubType(caughtType, throwsType, true)) {
                        exMethods.add(method);
                        caughtByCurrentCatch = true;
                        break;
                    }
                }
                if (caughtByCurrentCatch) break;
            }
            //if (match) break; // only choose the first matching method.
        }

        if (exMethods.size() > 0) {
            return exMethods;
        }

        // if caught exceptions are still not resolved, return the first method call
        for (PsiCallExpression callExpr : callExpressions) {
            PsiMethod method = callExpr.resolveMethod();
            if (method != null) {
                exMethods.add(method);
                break;
            }
        }

        /*

        // resolve method calls to method declarations
        List<PsiMethod> methods = new ArrayList<>();
        List<PsiMethod> innerTryMethods = new ArrayList<>();
        for (PsiMethodCallExpression methodCall : methodCalls) {
            PsiMethod method = (PsiMethod) (methodCall.getMethodExpression().resolve());
            if (method == null) {
                continue;
            }

            // save method calls in internal try blocks separately
            PsiTryStatement innerTry = PsiTreeUtil.getParentOfType(methodCall, PsiTryStatement.class);
            if (tryStatement.equals(innerTry)) {
                methods.add(method);
            } else {// if (PsiTreeUtil.isAncestor(tryStatement, innerTry, true)) {
                innerTryMethods.add(method);
            }

        }
        // resolve new expressions to method declarations (i.e., constructor declarations)
        for (PsiNewExpression newExpr : newExpressions) {
            PsiMethod method = newExpr.resolveMethod();
            if (method == null) { // the resolved result can be null when a new expression is a anonymous class
                continue;
            }

            // save method calls in internal try blocks separately
            PsiTryStatement innerTry = PsiTreeUtil.getParentOfType(newExpr, PsiTryStatement.class);
            if (tryStatement.equals(innerTry)) {
                methods.add(method);
            } else {// if (PsiTreeUtil.isAncestor(tryStatement, innerTry, true)) {
                innerTryMethods.add(method);
            }
        }

        */

        /*

        // select methods that declare exceptions same as the given exception type

        // First round: match exceptions with methods inside the try block (but outside inner try blocks)
        // Caught exception is equal to or is parent of method-specified exception
        // Excluding exceptions that are caught by prior sibling catches
        for (PsiMethod method : methods) {
            PsiType[] throwsTypes = method.getThrowsList().getReferencedTypes();

            boolean match = false;
            for (PsiType throwsType: throwsTypes) {
                boolean caughtByPriorSiblingCatches = false;
                // check if the throws type is caught by prior sibling catches
                for (PsiType caughtType : priorSiblingCaughtExceptions) {
                    if (isSubType(throwsType, caughtType, false)) {
                        caughtByPriorSiblingCatches = true;
                        break;
                    }
                }
                if (caughtByPriorSiblingCatches) continue;

                for (PsiType caughtType : this.exceptionTypes) {
                    if (isSubType(throwsType, caughtType, false)) {
                        exMethods.add(method);
                        match = true;
                        break;
                    }
                }
                if (match) break;
            }
            if (match) break; // only choose the first matching method.
        }
        if (exMethods.size() > 0) {
            return exMethods;
        }

        */

        /*

        // Second round: match exceptions with methods inside inner try blocks
        // Caught exception is equal to or is parent of method-specified exception
        // Excluding exceptions that are caught by all prior catches (sibling or inner catches)
        for (PsiMethod method : innerTryMethods) {
            PsiType[] throwsTypes = method.getThrowsList().getReferencedTypes();

            boolean match = false;
            for (PsiType throwsType: throwsTypes) {
                boolean caughtByPriorCatches = false;
                // check if the throws type is caught by prior sibling catches
                for (PsiType caughtType : priorCaughtExceptions) {
                    if (isSubType(throwsType, caughtType, false)) {
                        caughtByPriorCatches = true;
                        break;
                    }
                }
                if (caughtByPriorCatches) continue;

                for (PsiType caughtType : this.exceptionTypes) {
                    if (isSubType(throwsType, caughtType, false)) {
                        exMethods.add(method);
                        match = true;
                        break;
                    }
                }
                if (match) break;
            }
            if (match) break; // only choose the first matching method.
        }
        if (exMethods.size() > 0) {
            return exMethods;
        }

        */

        /*

        // Third round: child match (catch exception is child of method-specified exception)
        // In cases when the exception thrown at runtime can be the caught exception,
        // even though the declared exception is the parent
        methods.addAll(innerTryMethods);
        for (PsiMethod method : methods) {
            PsiType[] throwsTypes = method.getThrowsList().getReferencedTypes();

            boolean match = false;
            for (PsiType throwsType: throwsTypes) {
                boolean caughtByPriorCatches = false;
                // check if the throws type is caught by prior catches
                for (PsiType caughtType : priorCaughtExceptions) {
                    if (isSubType(throwsType, caughtType, false)) {
                        caughtByPriorCatches = true;
                        break;
                    }
                }
                if (caughtByPriorCatches) continue;

                for (PsiType caughtType : this.exceptionTypes) {
                    if (isSubType(caughtType, throwsType, false)) {
                        exMethods.add(method);
                        match = true;
                        break;
                    }
                }
                if (match) break;
            }
            if (match) break; // only choose the first matching method.
        }

        */

        /*
        // if no method matches the caught exceptions, use the first method in the try block
        if (exMethods.size() == 0) {
            exMethods.add(methods.get(0));
        }
        */
        return exMethods;
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

    private boolean isSameType(PsiType t1, PsiType t2) {
        return t1.getCanonicalText().equals(t2.getCanonicalText());
    }

    private boolean isThrowableType(PsiType t) {
        /*
        String throwableText = "java.lang.Throwable";
        if (t.getCanonicalText().equals(throwableText)) {
            return true;
        }

        PsiType[] superTypes = t.getSuperTypes();
        logger.debug("Throwable: " + throwableText);
        for (PsiType superT : superTypes) {
            logger.debug("Super type: " + superT.getCanonicalText());
            if (superT.getCanonicalText().equals(throwableText)) {
                return true;
            }
        }
        return false;
        */
        PsiType throwable = PsiType.getTypeByName("java.lang.Throwable", this.project,
                GlobalSearchScope.allScope(this.project));
        return isSubType(t, throwable, false);
    }

    private boolean isRuntimeExceptionType(PsiType t) {
        PsiType runtimeException = PsiType.getTypeByName("java.lang.RuntimeException", this.project,
                GlobalSearchScope.allScope(this.project));
        return isSubType(t, runtimeException, false);
    }

    private boolean isErrorType(PsiType t) {
        PsiType error = PsiType.getTypeByName("java.lang.Error", this.project,
                GlobalSearchScope.allScope(this.project));
        return isSubType(t, error, false);
    }

    private boolean isCheckedException(PsiType t) {
        PsiType exception = PsiType.getTypeByName("java.lang.Exception", this.project,
                GlobalSearchScope.allScope(this.project));
        return isSubType(t, exception, true) && !isRuntimeExceptionType(t);
    }

    private PsiType getPsiTypeByQualifiedName(String typeQName) {
        return PsiType.getTypeByName(typeQName, this.project,
                GlobalSearchScope.allScope(this.project));
    }

    private String extractLogLevel(PsiMethodCallExpression logStmt) {
        PsiReferenceExpression methodCall = logStmt.getMethodExpression();
        String logMethodName = PsiTreeUtil.getChildOfType(methodCall, PsiIdentifier.class).getText();
        return logMethodName;
    }

    private boolean deriveIsStackTraceLogged(PsiMethodCallExpression logStmt) {
        // exception variable of the containing catch block
        // catch session
        /*
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(logStmt, PsiCatchSection.class);
        if (catchSection == null) {
            return false;
        }
        PsiParameter exPara = PsiTreeUtil.getChildOfType(catchSection, PsiParameter.class);
        if (exPara == null) {
            return false;
        }
        String exName = PsiTreeUtil.getChildOfType(exPara, PsiIdentifier.class).getText();
        */

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

}
