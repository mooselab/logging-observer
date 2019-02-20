package findelements;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;

import java.util.ArrayList;
import java.util.List;

public class FindElementsUtils {

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

        UsageViewManager.getInstance(project).showUsages(
                UsageTarget.EMPTY_ARRAY, usages.toArray(new Usage[usages.size()]), new UsageViewPresentation());
    }
}
