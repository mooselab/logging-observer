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
* Build the *Logging Observer* plugin locally
  - Configuration
  - Dependencies
  - Install

### Contribution guidelines ###

* Adding more features to the plugin
* Writing tests
* Migrate *Logging Observer* to other platforms (e.g., Eclipse, or as a standard-alone tool)
* Providing coding or feature suggestions

### Contact ###

* Send messages to the repo owner 
* Send email to moose\<dot\>researchlab\<at\>gmail.com
