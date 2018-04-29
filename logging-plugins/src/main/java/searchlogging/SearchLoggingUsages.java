package searchlogging;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Query;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SearchLoggingUsages extends AnAction {

    final static Logger logger = LoggerFactory.getLogger(SearchLoggingUsages.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to search logging statements in project {}.", projectName);

        // get all java files in a project
        Collection<VirtualFile> virtualFiles = FileBasedIndex.getInstance().getContainingFiles(
                FileTypeIndex.NAME,
                JavaFileType.INSTANCE,
                GlobalSearchScope.projectScope(project)
        );
        int nFiles = virtualFiles.size();
        StringBuilder filenames = new StringBuilder();
        for (VirtualFile virtualFile : virtualFiles) {
            filenames.append(virtualFile.getPath() + "\n");
        }
        logger.debug("The project {} has {} Java files, as listed below:\n{}",
                projectName, nFiles, filenames);

        // find all occurences of "Logger"
        List<PsiElement> elements = new ArrayList<>();
        List<PsiMethodCallExpression> loggingStatements = new ArrayList<>();
        PsiSearchHelper.SERVICE.getInstance(project).processElementsWithWord(
                (psiElement, offsetInElement) -> {
                    if (psiElement instanceof PsiTypeElement) {
                        elements.add(psiElement);
                        PsiElement parent = psiElement.getParent();
                        elements.add(parent);
                        if (parent instanceof PsiField) {
                            PsiField field = (PsiField)parent;
                            Query<PsiReference> references = ReferencesSearch.search(field);
                            for (PsiReference ref : references) {
                                PsiElement usage = ref.getElement();
                                elements.add(usage);
                                elements.add(usage.getParent());
                                elements.add(usage.getParent().getParent());

                                PsiElement grandparent = usage.getParent().getParent();
                                if (grandparent instanceof PsiMethodCallExpression) {
                                    loggingStatements.add((PsiMethodCallExpression)grandparent);
                                }
                            }

                        }

                    }
                    return true;
                }, GlobalSearchScope.projectScope(project), "Logger",
                UsageSearchContext.IN_CODE, true
        );

        StringBuilder loggers = new StringBuilder();
        for (PsiElement element : elements) {
            loggers.append("getText(): ").append(element.getText()).append("\n");
            loggers.append("toString(): ").append(element.toString()).append("\n");
        }

        StringBuilder loggingStatementsStr = new StringBuilder();
        for (PsiMethodCallExpression log : loggingStatements) {
            loggingStatementsStr.append(log.getText()).append(("\n"));
        }

        logger.debug("\"Logger\" occurrences: \n{}", loggers);
        logger.debug("Logging statements: \n{}", loggingStatementsStr);

        /**
         * Show logging statements in the find tool window
         * Refer to: https://intellij-support.jetbrains.com/hc/en-us/community/posts/
         * 206756375-Showing-custom-usage-results-ReferenceSearch-in-Find-toolwindow
         */
        final List<Usage> usages = new ArrayList<>();
        for (PsiElement log : loggingStatements) {
            final UsageInfo usageInfo = new UsageInfo(log);
            Usage usage = new UsageInfo2UsageAdapter(usageInfo);
            usages.add(usage);
        }

        UsageViewManager.getInstance(project).showUsages(
                UsageTarget.EMPTY_ARRAY, usages.toArray(new Usage[usages.size()]), new UsageViewPresentation());
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        boolean visibility = event.getProject() != null;
        event.getPresentation().setEnabled(visibility);
        event.getPresentation().setVisible(visibility);
    }

}
