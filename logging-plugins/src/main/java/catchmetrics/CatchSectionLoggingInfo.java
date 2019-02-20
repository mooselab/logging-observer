package catchmetrics;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import exceptionloggingmetrics.ExceptionLoggingMetrics;
import loggingcomponents.LoggingComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CatchSectionLoggingInfo {
    private PsiCatchSection catchSection;
    private Project project;
    private List<PsiType> exceptionTypes;
    private boolean isLogged = false;
    private int logNum = 0;
    private boolean isStackTraceLogged = false;
    private int stackTraceNum = 0;
    public CatchSectionLoggingInfo(PsiCatchSection catchSection) {
        this.catchSection = catchSection;
        this.project = catchSection.getProject();
        this.fetchLoggingInfo();
        this.exceptionTypes =
                ExceptionLoggingMetrics.extractExceptionTypesForCatchSection(catchSection);
    }

    private void fetchLoggingInfo() {
        // Pattern for matching logging statements
        Pattern pLog = Pattern.compile(".*log.*\\.(trace|debug|info|warn|error|fatal)", Pattern.CASE_INSENSITIVE);

        PsiTreeUtil.findChildrenOfType(catchSection, PsiMethodCallExpression.class).forEach(m -> {
            if (pLog.matcher(m.getMethodExpression().getText()).matches()) {
                this.isLogged = true;
                this.logNum += 1;
                //loggingStatements.add(m);
                LoggingComponents logComponents = new LoggingComponents(m);
                if (logComponents.getIsStackTraceLogged()) {
                    this.isStackTraceLogged = true;
                    this.stackTraceNum += 1;
                }
            }
        });
    }


    private String getExceptionTypeStr() {
        if (this.exceptionTypes.size() == 0) {
            return "UnknownException";
        } else {// if (this.exceptionTypes.size() == 1) {
            // only consider the first exception
            return this.exceptionTypes.get(0).getPresentableText();
        } /*else {
            return "MultiExceptions";
        }*/
    }

    public static String getCatchSectionLoggingInfoHeader() {
        List<String> infoHeader = new ArrayList<>();

        infoHeader.add("catchLocation"); // the location of the catch section - fileName:lineNumber
        infoHeader.add("exceptionType");
        infoHeader.add("isLogged");
        infoHeader.add("isStackTraceLogged");
        infoHeader.add("logNum");
        infoHeader.add("stackTraceNum");

        return String.join(",", infoHeader);
    }

    public String getCatchSectionLoggingInfo() {
        List<String> logInfo = new ArrayList<>();

        logInfo.add(ExceptionLoggingMetrics.getLocationInFile(this.catchSection));
        logInfo.add(this.getExceptionTypeStr());
        logInfo.add(String.valueOf(this.isLogged));
        logInfo.add(String.valueOf(this.isStackTraceLogged));
        logInfo.add(String.valueOf(this.logNum));
        logInfo.add(String.valueOf(this.stackTraceNum));

        return String.join(",", logInfo);
    }
}
