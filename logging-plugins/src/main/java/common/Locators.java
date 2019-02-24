package common;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class Locators {
    @NotNull
    public static String getLocationInFile(PsiElement element) {
        PsiFile psiFile = element.getContainingFile();
        int lineNumber = StringUtil.offsetToLineNumber(psiFile.getText(), element.getTextOffset()) + 1;
        return psiFile.getVirtualFile().getName() + ":" + lineNumber;
    }
}
