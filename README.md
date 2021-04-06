# Logging Observer #

This project develops an IntelliJ plugin (*Logging Observer*) for searching and analyzing logging code in Java projects. The plugin is based on the [IntelliJ IDEA platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html).

### What does *Logging Observer* provide? ###

* Searching logging-related elements in a Java project.
  - Searching all logging statements in the project.
  - Searching all exception logging statements (logging statements in catch blocks).
  - etc.

* Analyzing logging code
  - Analyzing logging components (e.g., log level, text, variables, stack traces)
  - Analyzing logging-related code metrics (e.g., exception type, containing code blocks)
  - etc.

### How to use *Logging Observer*? ###

The current version of *Logging Observer* is developed as an IDEA IntelliJ platform plugin. Pleaes check how to [Install Intellij plugins from repository or disk](https://www.jetbrains.com/help/idea/managing-plugins.html).

**Perform actions.** 
The features of the plugin are packaged into a menu item `Search & Analyze Logging` under the `Tools` menu. There are currnetly four `actions` in the menu item, as shown in the image below. You can click each `action` to perform the corresponding operation.

<img width="630" alt="plugin-menu-options" src="https://user-images.githubusercontent.com/82050406/113780028-96bfa880-96fc-11eb-8472-b3480758b607.png">

**View search results.** 
After performing an action (e.g, `Search & Analyze All Logs`), there will be a few seconds to a few minutes of waiting time depending on the size of the project and the performed action, then the search results will be displayed in the `Find` tool window usually in the lower left part of IntelliJ's UI, as shown below:

<img width="891" alt="plugin-search-result" src="https://user-images.githubusercontent.com/82050406/113780045-9cb58980-96fc-11eb-8496-2a18bb147302.png">

You can browse and click a searched item. When a searched item is clicked, the source file containing the searched item will be displayed with the searched item highlighted in the file, as shown below:

<img width="863" alt="plugin-locate-searched-item-in-code" src="https://user-images.githubusercontent.com/82050406/113780062-a2ab6a80-96fc-11eb-96ff-ee92203ad933.png">

**View analysis results.** 
The analysis results are stored in the form of ItelliJ's log files. You can access the log files through the `Show Log in Finder` action in the `Help` menu, as shown below:

<img width="537" alt="find-log-location" src="https://user-images.githubusercontent.com/82050406/113780075-a7701e80-96fc-11eb-940f-4200ead97892.png">

In the current version, when you perform action `Search & Analyze All Logs`, the analysis results will show a list of logging statements. For each logging statement, the results will include its source code location, the logging statement itself, and its components (log level and message string). The results start after the log message `Logging components for project ...`. Below is a short example:

```
2021-04-06 17:10:15,145 [6082719]   INFO - ndelements.FindJavaSourceFiles - Logging components for project qpid-java-build:
logLocation;;;logBody;;;logLevel;;;logStringWithVariableNames
Controller.java:77;;;LOGGER.info("Awaiting client registrations");;;info;;;Awaiting client registrations
EnvironmentUtils.java:190;;;LOGGER.debug("Setting BDB configuration parameter '{}' to value '{}'.", param, contextValue);;;debug;;;Setting BDB configuration parameter 'param' to value 'contextValue'.
```

When you perform action `Search & Analyze Exception Logs`, the analysis results will show a list of exceptino logging statements. For each exception logging statement, the results will include a list of context code metrics associated with the logging statement (e.g., exception type, containing package, etc.). The results start after the log message `Exception logging metrics for project...`. Below is a short example:

```
2021-04-06 15:31:48,062 [ 175636]   INFO - ndelements.FindJavaSourceFiles - Exception logging metrics for project qpid-java-build: 
catchLocation,logLocation,logLevel,logStackTrace,containingPackage,exceptionType,parentExType,grandParentExType,exceptionPackage,exceptionCategory,exceptionSource,exceptionNum,exceptionMethodCall,exceptionMethodPackage,exceptionMethodSource,exceptionMethodNum,catchInLoop,isLogInInnerLoop,isLogInInnerBranch,isLogInInnerTryBlock,throwInCatchBlock,returnInCatchBlock,throwInTryBlock,returnInTryBlock,numMethodCallsBeforeLogging,numMethodCallsAfterLogging,LOCBeforeLogging,LOCAfterLogging,numMethodCallsInTryBlock,LOCInTryBlock,LOCInFile,LOCInMethod,numMethodCallsInMethod,methodUsages
WLSTransactionManagerLocator.java:45,WLSTransactionManagerLocator.java:47,error,false,org.apache.qpid.ra.tm,Exception,Throwable,Object,java.lang,GENERAL,FROMJDK,1,InitialContext.lookup,javax.naming,FROMJDK,2,false,false,false,false,false,false,false,false,0,0,0,0,1,2,64,28,4,0
GlassfishTransactionManagerLocator.java:45,GlassfishTransactionManagerLocator.java:47,error,false,org.apache.qpid.ra.tm,Exception,Throwable,Object,java.lang,GENERAL,FROMJDK,1,InitialContext.lookup,javax.naming,FROMJDK,2,false,false,false,false,false,false,false,false,0,0,0,0,1,2,63,27,4,0
```


### How do I get set up? ###

You can install the plugin from IDEA's market place, download the package from this GitHub repo, or build the plugin locally.
* Install the *Logging Observer* plugin from IDEA market place
  - Plugin URL: https://plugins.jetbrains.com/plugin/16479-logging-observer
* Download the plugin from this repository
  - Get the latest version from the path logging-observer-intellij-plugin/build/distributions/
* Build the plugin locally
  - Configuration.
    This plugin uses Gradle for dependecy and build management. Usually you don't need to make any changes to the configuration. In some cases, you may need to config Gradle version (`distributionUrl`) in the file `gradle/wrapper/gradle-wrapper.properties`. You may also need to config the IntelliJ platform (`org.jetbrains.intellij`) version in the `build.gradle` file. 
  - Dependencies. 
    This plugin depends on IntelliJ, Intellij Platform SDK, Gradle, SLF4J, Logback, and JUnit.
  - Build the plugin.
    - Use Gradle Intellij Plugin: open `Gradle` panel, then run the `build` task.
    - Use stand-alone Gradle: go to folder `logging-observer-intellij-plugin`, then run `gradle build`.
    - The built plugin distribution will be in the `logging-observer-intellij-plugin/build/distributions/' folder.

### Contribution guidelines ###

* Adding more features to the plugin
* Writing tests
* Migrate *Logging Observer* to other platforms (e.g., Eclipse, or as a stand-alone tool)
* Providing coding or feature suggestions

### Contact ###

* Send messages to the repo owner 
* Send email to moose\<dot\>researchlab\<at\>gmail.com
