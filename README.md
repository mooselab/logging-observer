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
