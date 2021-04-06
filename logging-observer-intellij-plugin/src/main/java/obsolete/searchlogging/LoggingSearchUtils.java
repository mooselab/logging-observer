package obsolete.searchlogging;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LoggingSearchUtils {
    private static final Logger logger = LoggerFactory.getLogger(SearchLoggingUsages.class);
    /**
     * Find logging statements in a Java project, excluding those in test files
     * @param project
     * @return
     */
    public static List<PsiMethodCallExpression> findLoggingStatementsInProject(Project project) {
        // find all occurences of "Logger"
        //List<PsiElement> elements = new ArrayList<>();

        List<PsiMethodCallExpression> loggingStatements = new ArrayList<>();
        PsiSearchHelper.SERVICE.getInstance(project).processElementsWithWord(
                (psiElement, offsetInElement) -> {
                    if (psiElement instanceof PsiTypeElement) {
                        // exclude test files
                        // TODO: compile regex pattern first, to improve performance
                        PsiFile psiFile = psiElement.getContainingFile();
                        String fileName = psiFile.getName();
                        boolean isJavaFile = false, isTestFile = false;
                        if (fileName.matches(".*\\.java")) {
                            isJavaFile = true;
                            if (fileName.matches(".*Test\\.java")) {
                                isTestFile = true;
                            }
                        }
                        if (!isJavaFile || isTestFile) return true;
//                        logger.debug("File name: " + psiFile.getName() + ", type: " + psiFile.getFileType().getName() +
//                                ", isJavaFile: " + isJavaFile + ", isTestFile: " + isTestFile);
                        //elements.add(psiElement);
                        PsiElement parent = psiElement.getParent();
                        //elements.add(parent);
                        if (parent instanceof PsiField) {
                            PsiField field = (PsiField)parent;
                            String fieldName = field.getName();
                            //logger.debug("Field name: " + fieldName);
                            Query<PsiReference> references = ReferencesSearch.search(field);
                            for (PsiReference ref : references) {
                                PsiElement usage = ref.getElement();
                                //elements.add(usage);
                                //elements.add(usage.getParent());
                                //elements.add(usage.getParent().getParent());

                                PsiElement grandparent = usage.getParent().getParent();
                                if (grandparent instanceof PsiMethodCallExpression) {
                                    PsiMethodCallExpression methodCall = (PsiMethodCallExpression)grandparent;
                                    String methodStr = methodCall.getMethodExpression().getText();
                                    if (methodStr.matches(fieldName +
                                            "\\.(trace|debug|info|warn|error|fatal)")) {
                                        loggingStatements.add(methodCall);
                                    }
                                }
                            }

                        }

                    }
                    return true;
                }, GlobalSearchScope.projectScope(project), "Logger",
                UsageSearchContext.IN_CODE, true
        );
        return loggingStatements;
    }



    /**
     * Show a list of method calls in the find tool window
     * Refer to: https://intellij-support.jetbrains.com/hc/en-us/community/posts/
     * 206756375-Showing-custom-usage-results-ReferenceSearch-in-Find-toolwindow
     */
    public static void listPsiMethodCallExpressionsInFindToolWindow(Project project, List<PsiMethodCallExpression> loggingStatements) {

        final List<Usage> usages = new ArrayList<>();
        for (PsiElement log : loggingStatements) {
            final UsageInfo usageInfo = new UsageInfo(log);
            Usage usage = new UsageInfo2UsageAdapter(usageInfo);
            usages.add(usage);
        }

        UsageViewPresentation presentation = new UsageViewPresentation();
        presentation.setTabName("Search result");
        presentation.setTabText("Found " + usages.size() + " instances");
        presentation.setToolwindowTitle("Search result");
        UsageViewManager.getInstance(project).showUsages(
                UsageTarget.EMPTY_ARRAY, usages.toArray(new Usage[usages.size()]), presentation);
    }

    /**
     * Show a list of Psi elements in the find tool window
     * Refer to: https://intellij-support.jetbrains.com/hc/en-us/community/posts/
     * 206756375-Showing-custom-usage-results-ReferenceSearch-in-Find-toolwindow
     */
    public static void listPsiElementsInFindToolWindow(Project project, List<PsiElement> psiElements) {

        final List<Usage> usages = new ArrayList<>();
        for (PsiElement psiElement : psiElements) {
            final UsageInfo usageInfo = new UsageInfo(psiElement);
            Usage usage = new UsageInfo2UsageAdapter(usageInfo);
            usages.add(usage);
        }

        UsageViewPresentation presentation = new UsageViewPresentation();
        presentation.setTabName("Search result");
        presentation.setTabText("Found " + usages.size() + " instances");
        presentation.setToolwindowTitle("Search result");
        UsageViewManager.getInstance(project).showUsages(
                UsageTarget.EMPTY_ARRAY, usages.toArray(new Usage[usages.size()]), presentation);
    }

}
