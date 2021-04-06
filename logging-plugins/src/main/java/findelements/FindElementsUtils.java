package findelements;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;

import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;

public class FindElementsUtils {

    private static final Logger logger = Logger.getInstance(FindJavaSourceFiles.class);

    /**
     * Show a list of Psi elements in the find tool window
     * Refer to: https://intellij-support.jetbrains.com/hc/en-us/community/posts/
     * 206756375-Showing-custom-usage-results-ReferenceSearch-in-Find-toolwindow
     */
    public static void listPsiElementsInFindToolWindow(Project project, List<PsiElement> psiElements) {

        final List<Usage> usages = new ArrayList<>();

        // logger.info("Usage info of searched elements:");

        for (PsiElement psiElement : psiElements) {
            final UsageInfo usageInfo = new UsageInfo(psiElement);
            Usage usage = new UsageInfo2UsageAdapter(usageInfo);
            usages.add(usage);
            // logger.info(usage.toString());
        }

        // logger.info("usage size: " + usages.size());

        UsageViewPresentation presentation = new UsageViewPresentation();
        presentation.setTabName("Search result");
        presentation.setTabText("Found " + usages.size() + " instances");
        presentation.setToolwindowTitle("Search result");
        UsageViewManager.getInstance(project).showUsages(
                UsageTarget.EMPTY_ARRAY, usages.toArray(new Usage[usages.size()]), presentation);
    }
}
