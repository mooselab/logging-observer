package catchmetrics;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.regex.Pattern;

public class CatchSectionLoggingMetrics {
    private PsiCatchSection catchSection;
    private Project project;
    private boolean isCatchLogged = false;
    private int logNum = 0;
    private boolean isStackTraceLogged = false;
    private int stackTraceNum = 0;
    public CatchSectionLoggingMetrics(PsiCatchSection catchSection) {
        this.catchSection = catchSection;
        this.project = catchSection.getProject();



    }

    private void calculateLoggingMetrics() {
        // Pattern for matching logging statements
        Pattern pLog = Pattern.compile(".*log.*\\.(trace|debug|info|warn|error|fatal)", Pattern.CASE_INSENSITIVE);

        PsiTreeUtil.findChildrenOfType(catchSection, PsiMethodCallExpression.class).forEach(m -> {
            if (pLog.matcher(m.getMethodExpression().getText()).matches()) {
                this.isCatchLogged = true;
                this.logNum += 1;
                //loggingStatements.add(m);
            }
        });
    }
}
