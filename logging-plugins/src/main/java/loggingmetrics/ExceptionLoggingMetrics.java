package loggingmetrics;

import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ExceptionLoggingMetrics {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionLoggingMetrics.class);

    private PsiMethodCallExpression logstmt;

    public ExceptionLoggingMetrics(PsiMethodCallExpression _logstmt) {
        logstmt = _logstmt;
    }

    /**
     * Get the exception types of the catch block that contain the logging statement (i.e., logged exception types)
     * @return
     */
    public List<PsiType> getExceptionTypes() {
        // catch clause
        PsiCatchSection catchSection = PsiTreeUtil.getParentOfType(logstmt, PsiCatchSection.class);
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
    //public List<PsiMethod> getExceptionMethods(PsiType exType) {
    public List<PsiMethod> getExceptionMethods() {
        List<PsiMethod> exMethods = new ArrayList<>();

        // logged exception types
        List<PsiType> exTypes = this.getExceptionTypes();

        // try statement
        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(logstmt, PsiTryStatement.class);
        if (tryStatement == null) {
            return null;
        }

        // try-block
        PsiCodeBlock tryBlock = PsiTreeUtil.getChildOfType(tryStatement, PsiCodeBlock.class);

        // method calls in the try block
        Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(tryBlock, PsiMethodCallExpression.class);

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

        // select methods that declare exceptions same as the given exception type
        for (PsiMethod method : methods) {
            PsiType[] throwsTypes = method.getThrowsList().getReferencedTypes();

            boolean match = false;
            for (PsiType throwsType: throwsTypes) {
                for (PsiType caughtType : exTypes) {
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