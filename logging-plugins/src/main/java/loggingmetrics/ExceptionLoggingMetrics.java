package loggingmetrics;

import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ExceptionLoggingMetrics {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionLoggingMetrics.class);

    private PsiMethodCallExpression logStmt;
    List<PsiType> exceptionTypes;
    List<PsiMethod> exceptionMethods;
    Project project;

    public ExceptionLoggingMetrics(PsiMethodCallExpression _logstmt) {
        logStmt = _logstmt;
        exceptionTypes = deriveExceptionTypes();
        exceptionMethods = deriveExceptionMethods();
        project = logStmt.getProject();

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
        for (PsiMethod m : exceptionMethods) {
            getMethodSource(m);
        } */
        /*
        logger.debug("Exception methods: " + getPresentableExceptionMethods());
        logger.debug("Method source: " + getPresentableMethodSource());
        */
    }

    public static String getLoggingMetricsHeader() {
        List<String> metricsHeader = new ArrayList<>();
        metricsHeader.add("exceptionType"); // exceptions caught by the containing catch block
        metricsHeader.add("exceptionCategory"); // Normal exception, RuntimeException, or Error
        metricsHeader.add("exceptionSource"); // project, library, or JDK
        metricsHeader.add("exceptionNum"); // number of exceptions caught by the containing catch block
        metricsHeader.add("methodCall"); // method call that throws the caught exceptions
        metricsHeader.add("methodSource"); // project, library, or JDK
        metricsHeader.add("methodNum"); // number of methods that throw the caught exceptions
        metricsHeader.add("catchInLoop"); // if the containing catch block is in a loop
        metricsHeader.add("isLogInLoop"); // if the logging statement is in a loop within the containing catch block

        return String.join(",", metricsHeader);
    }

    public String getLoggingMetrics() {
        List<String> metrics = new ArrayList<>();
        metrics.add(getPresentableExceptionTypes());
        metrics.add(getPresentableExceptionCategory());
        metrics.add(getPresentableExceptionSource());
        metrics.add(String.valueOf(getExceptionTypes().size()));
        metrics.add(getPresentableExceptionMethods());
        metrics.add(getPresentableMethodSource());
        metrics.add(String.valueOf(getExceptionMethods().size()));
        metrics.add(String.valueOf(isCatchBlockWithInLoop()));

        return String.join(",",metrics);
    }

    public List<PsiType> getExceptionTypes() {return exceptionTypes;}
    public List<PsiMethod> getExceptionMethods() {return exceptionMethods;}

    public String getPresentableExceptionTypes() {
        if (exceptionTypes.size() == 0) {
            return "UnknownException";
        } else if (exceptionTypes.size() == 1) {
            return exceptionTypes.get(0).getPresentableText();
        } else {
            return "MultiExceptions";
/*            return exceptionTypes.
                    stream().
                    map(t -> t.getCanonicalText()).
                    reduce("", (a, b) -> a + " " + b);*/
        }
    }

    public String getPresentableExceptionMethods() {
        if (exceptionMethods.size() == 0) {
            return "UnKnownMethod";
        } else if (exceptionMethods.size() == 1) {
            PsiMethod method = exceptionMethods.get(0);
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
        if (exceptionTypes.size() == 1) {
            return getExceptionSource(exceptionTypes.get(0)).name();
        }
        // if all sources are the same, return the source; otherwise return "MIXED"
        String source = null;
        for (PsiType e: exceptionTypes) {
            if (source == null) {
                source = new String(getExceptionSource(e).name());
            } else if (!source.equals(getExceptionSource(e).name())) {
                return "MIXED";
            }
        }
        return source;
    }

    public String getPresentableExceptionCategory() {
        if (exceptionTypes.size() == 1) {
            return getExceptionCategory(exceptionTypes.get(0)).name();
        }
        // if all categories are the same, return the category; otherwise return "MIXED"
        String category = null;
        for (PsiType e : exceptionTypes) {
            if (category == null) {
                category = new String(getExceptionCategory(e).name());
            } else if (!category.equals(getExceptionCategory(e).name())) {
                return "MIXED";
            }
        }
        return category;
    }

    public String getPresentableMethodSource() {
        if (exceptionMethods.size() == 1) {
            return getMethodSource(exceptionMethods.get(0)).name();
        }
        // if all sources are the same, return the source; otherwise return "MIXED"
        String source = null;
        for (PsiMethod m : exceptionMethods) {
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
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(logStmt, PsiCatchSection.class);
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
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(logStmt, PsiCatchSection.class);

        // containing loop statement of the logging statement
        PsiLoopStatement loop = PsiTreeUtil.getParentOfType(logStmt, PsiLoopStatement.class);

        if (loop == null) {
            return false;
        }

        // if the containing loop is inside the containing catch block
        if (PsiTreeUtil.isAncestor(catchSection, loop, true)) {
            return true;
        }
        return false;
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
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
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
    private List<PsiType> deriveExceptionTypes() {
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
    private List<PsiMethod> deriveExceptionMethods() {
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
                for (PsiType caughtType : exceptionTypes) {
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
