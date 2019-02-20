package findelements;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class FindElementsUtils {

    private static final Logger logger = Logger.getInstance(FindElementsUtils.class);

    public static List<PsiFile> findJavaSourceFilesInProject(Project project) {
        //List<PsiCatchSection> catchSections = new ArrayList<>();
        List<PsiFile> psiFiles = new ArrayList<>();

        //StringBuilder files = new StringBuilder();
        //files.append("Files in Project " + project.getName() + ":\n");

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

                    psiFiles.add(psiFile);

                    //files.append(fileName).append("\n");

                }
                return true;
            }
        });

        //logger.info(files.toString());

        return psiFiles;
    }

    public static List<PsiCatchSection> findCatchSectionsInJavaProject(Project project) {
        List<PsiCatchSection> catchSections = new ArrayList<>();

        //StringBuilder files = new StringBuilder();
        //files.append("Files in Project " + project.getName() + ":\n");

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

                    //files.append(fileName).append("\n");

                }
                return true;
            }
        });

        //logger.info(files.toString());

        return catchSections;
    }

    public static List<PsiMethodCallExpression> findLoggingStatementsInFiles(Project project) {
        List<PsiMethodCallExpression> loggingStatements = new ArrayList<>();

        // Pattern for matching logging statements
        Pattern pLog = Pattern.compile(".*log.*\\.(trace|debug|info|warn|error|fatal)", Pattern.CASE_INSENSITIVE);

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

                    PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression.class).forEach(m -> {
                        if (pLog.matcher(m.getMethodExpression().getText()).matches()) {
                            loggingStatements.add(m);
                        }
                    });

                }
                return true;
            }
        });

        //logger.info(files.toString());

        return loggingStatements;
    }

    public static List<PsiMethodCallExpression> findLoggingStatementsInCatchSections(Project project) {
        //List<PsiCatchSection> catchSections = new ArrayList<>();
        List<PsiMethodCallExpression> loggingStatements = new ArrayList<>();

        // Pattern for matching logging statements
        Pattern pLog = Pattern.compile(".*log.*\\.(trace|debug|info|warn|error|fatal)", Pattern.CASE_INSENSITIVE);

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

                    //catchSections.addAll(catchSectionsInFile);

                    for (PsiCatchSection catchSection : catchSectionsInFile) {
                        //Collection<PsiMethodCallExpression> methodCallsInCatch =
                        //        PsiTreeUtil.findChildrenOfType(catchSection, PsiMethodCallExpression.class);
                        PsiTreeUtil.findChildrenOfType(catchSection, PsiMethodCallExpression.class).forEach(m -> {
                            if (pLog.matcher(m.getMethodExpression().getText()).matches()) {
                                loggingStatements.add(m);
                            }
                        });
                    }

                    //files.append(fileName).append("\n");

                }
                return true;
            }
        });

        //logger.info(files.toString());

        return loggingStatements;
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

        UsageViewManager.getInstance(project).showUsages(
                UsageTarget.EMPTY_ARRAY, usages.toArray(new Usage[usages.size()]), new UsageViewPresentation());
    }
}
