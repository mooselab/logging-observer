package findelements;

import catchmetrics.CatchSectionLoggingInfo;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FindJavaCatchSections extends AnAction {
    private static final Logger logger = Logger.getInstance(FindJavaSourceFiles.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        String projectName = project.getName();
        logger.info("Start to find Java catch sections in project " + projectName +".");

        List<PsiCatchSection> psiCatchSections = findCatchSectionsInJavaProject(project);

        /*
        StringBuilder catchSectionsStr = new StringBuilder();
        catchSectionsStr.append("Catch sections in Project " + project.getName() + ":\n");

        for (PsiCatchSection catchSection : psiCatchSections) {
            catchSectionsStr.append(catchSection.getCatchType().getCanonicalText()).append("\n");
        }
        logger.info(catchSectionsStr.toString());
        */

        StringBuilder catchInfoStr = new StringBuilder();
        catchInfoStr.append(CatchSectionLoggingInfo.getCatchSectionLoggingInfoHeader()).append("\n");

        for(PsiCatchSection catchSection : psiCatchSections) {
            CatchSectionLoggingInfo catchLogInfo = new CatchSectionLoggingInfo(catchSection);
            catchInfoStr.append(catchLogInfo.getCatchSectionLoggingInfo()).append("\n");
        }

        logger.info("Catch section log info for project " + projectName + ": \n" + catchInfoStr);

        // list all the searched items in the find tool window view
        FindElementsUtils.listPsiElementsInFindToolWindow(project,
                psiCatchSections.stream().map(e -> (PsiElement)e).collect(Collectors.toList()));
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        boolean visibility = event.getProject() != null;
        event.getPresentation().setEnabled(visibility);
        event.getPresentation().setVisible(visibility);
    }

    public static List<PsiCatchSection> findCatchSectionsInJavaProject(Project project) {
        List<PsiCatchSection> catchSections = new ArrayList<>();

        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        projectFileIndex.iterateContent(new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile fileOrDir) {
                if (!fileOrDir.isDirectory() && projectFileIndex.isUnderSourceRootOfType(fileOrDir,
                        JavaModuleSourceRootTypes.SOURCES)) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(fileOrDir);
                    if (!(psiFile instanceof PsiJavaFile)) {
                        return true; // skip non-Java files
                    }
                    String fileName = psiFile.getName();
                    if (fileName.matches(".*Test\\.java")) {
                        return true; // skip test files
                    }

                    Collection<PsiCatchSection> catchSectionsInFile = PsiTreeUtil.findChildrenOfType(psiFile, PsiCatchSection.class);
                    if (catchSectionsInFile.size() == 0) {
                        return true;
                    }

                    catchSections.addAll(catchSectionsInFile);
                }
                return true;
            }
        });

        return catchSections;
    }

}
