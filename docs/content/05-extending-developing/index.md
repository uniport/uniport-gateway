# Extending & Developing

## JSON Schemas for configuration files

Starting with portal-gateway version `8.0.0`, we provide JSON schemas that can be used to validate your configuration files.

### Usage

There are two JSON schemas, `portalGatewayStaticSchema.json` for the static and `portalGatewayDynamicSchema.json` for the dynamic configuration file. Both files are included in the following artifact:

```xml
<dependency>
    <groupId>com.inventage.portal.gateway</groupId>
    <artifactId>config-schemas</artifactId>
    <version>${portal-gateway.version}</version>
</dependency>
```

You can either manually extract schemas, or use a maven plugin to automate their extraction (for example, `maven-depedency-plugin`).

### Helpful links

- [IntelliJ: Using custom JSON schemas](https://www.jetbrains.com/help/idea/json.html#ws_json_schema_add_custom)
- [About JSON Schemas](https://json-schema.org/)
- [JSON Schema Validator](https://www.jsonschemavalidator.net/)

## Code Quality

Das Uniport Projekt wendet verschiedene Code Quality Tools an, um immer ein saubere Codebase zu haben. Dabei wird ein konsistentes Format der ganzen Codebase, das Einhalten eines hohen Java Coding Standards, und das Erkennen von Bugs durch statische Analyse angestrebt. Dazu kommen verschiedenste Tools zum Einsatz, um einerseits eine einfache Integration der Code Quality Tools in den Workflow des Uniport-Teams zu ermöglichen und andererseits die Code Qualität automatisiert zu überprüfen.

Die Code Quality Tool Settings werden dabei in einem zentralen Repo (`code-style-settings`) verwaltet und von den einzelnen Projekten jeweils heruntergeladen.

**Wichtig**: Bei jeder Änderung an den globalen Settings im zentralen Repo, muss das Uniport-Team darüber informiert werden.

### Formatierung

Für eine konsistente Formatierung des Source-Codes wird der Eclipse Code Formatter eingesetzt. Für die verschiedene IDE's gibt es jeweils ein Plugin, um das Format automatische anzuwenden:

- Intellij: [Adapter for Eclipse Code Formatter](https://plugins.jetbrains.com/plugin/6546-adapter-for-eclipse-code-formatter)
- VScode: [Redhat Java](https://marketplace.visualstudio.com/items?itemName=redhat.java)

Damit die Source-Codes einerseits automatisch bei jedem `mvn install` formatiert wird und andererseits bei einem Commit überprüft werden kann, ob die zu committende Codebase die korrekte Formatierung hat, wird das [Spotless Maven Plugin](https://github.com/diffplug/spotless) von diffplug eingesetzt. Zudem kann dieses Plugin nicht nur Java Files mit dem Eclipse Code Formatter formatieren, sondern auch JSON und Pom Files. Weiter kann es auch analog zur bekannten `editorconfig` auch simple Formatierung (wie z.B. intendation, spaces vs tabs, trim trailing whitespaces etc.) auf textbasierte Files anwenden.

Die Formatierung wird vor jedem Kompilieren des Source-Codes angewendet. Zudem kann die Formatierung auch manuell mit `mvn spotless:apply` angewendet werden. Weiter werden per Default nur dirty Files formattiert und überprüft. Das soll verhindern, dass die ganze Codebase bei einem kleine Featuren einen Diff aufweist.

Wie bereits angedeutet, wird die Formatierung bei jedem Commit überprüft. Dazu wird ein weiteres Maven Plugin angewendet, das ein Pre-Commit Hook installiert. Dazu mehr unter [Pre-Commit Hooks](#pre-commit-hooks).

Da jedes Projekt spezielle Files enthalten kann, die nicht formatiert werden sollten oder aber auch Files enthalten, die von der geteilten Konfiguration nicht automatisch formatiert werden, kann dies jeweils pro Projekt eingestellt werden. Die Konfiguration des Spotless Maven Plugins beinhaltet jeweils `<includes>` und `<excludes>` Tags, um genau diese individuelle Konfiguration zu erreichen.

Die verwendete Config des Spotless Maven Plugins:

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.35.0</version>
    <!-- See https://github.com/diffplug/spotless/tree/maven/2.35.0/plugin-maven -->
    <configuration>
        <!-- limit format enforcement to just the files changed by this feature branch -->
        <ratchetFrom>origin/master</ratchetFrom>
        <formats>
            <!-- you can define as many formats as you want, each is independent -->
            <format>
                <includes>
                    <include>**/*.md</include>
                    <include>.gitignore</include>
                    <include>.jenkins/Jenkinsfile.build</include>
                </includes>

                <trimTrailingWhitespace/>
                <endWithNewline/>
                <indent>
                    <spaces>true</spaces>
                    <spacesPerTab>4</spacesPerTab>
                </indent>
            </format>
        </formats>
        <java>
            <includes>
                <include>**/src/main/java/**/*.java</include>
                <include>**/src/test/java/**/*.java</include>
            </includes>

            <importOrder/>
            <removeUnusedImports/>

            <eclipse>
                <file>${maven.multiModuleProjectDirectory}/.code-style-settings/portal-java-formatter.xml</file>
            </eclipse>
        </java>
        <pom>
            <includes>
                <include>**/pom.xml</include>
            </includes>
            <sortPom>
                <nrOfIndentSpace>4</nrOfIndentSpace>
                <expandEmptyElements>false</expandEmptyElements>
            </sortPom>
        </pom>
        <json>
            <includes>
                <include>**/*.json</include>
            </includes>

            <gson>
                <version>2.8.1</version>
                <indentSpaces>4</indentSpaces>
                <sortByKeys>false</sortByKeys>
            </gson>
        </json>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>apply</goal>
            </goals>
            <phase>process-sources</phase>
        </execution>
    </executions>
</plugin>
```

### Coding Standart

Um einen hohen Java Coding Standard aufrechtzuerhalten, wird das [Checkstyle Tool](https://checkstyle.org/) angewendet. Auch hier gibt es für die verschiedenen IDE's jeweils ein Plugin, um die Vorschläge direkt im Source-Code anzuzeigen:

- Intellij: [Checkstyle-IDEA](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea)
- VScode: [VSCode Checkstyle](https://marketplace.visualstudio.com/items?itemName=shengchen.vscode-checkstyle)

Die Checkstyle können manuell mit `mvn checkstyle:check` durchgeführt werden.

Auch hier wird durch ein Pre-Commit Hook jeweils überprüft, dass keine Coding Standards verletzt werden.

Falls auch hier der Wunsch vorhanden ist, einzelne Checkstyle zu ignorieren, kann dies entweder über das global `suppressions.xml` im zentralen Repo oder sogar pro Projekt über das `suppressions-specific.xm` eingestellt werden.

Die verwendete Config des Checkstyle Maven Plugins:

```xml
<plugin>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.2.1</version>
    <configuration>
        <consoleOutput>true</consoleOutput>
        <configLocation>${maven.multiModuleProjectDirectory}/.code-style-settings/checkstyle/config.xml</configLocation>
        <suppressionsLocation>${maven.multiModuleProjectDirectory}/.code-style-settings/checkstyle/suppressions.xml</suppressionsLocation>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <!-- Keep same version as used by the IntelliJ plugin, see IntelliJ IDEA > Preferences > Tools > Checkstyle -->
            <version>10.7.0</version>
        </dependency>
    </dependencies>
</plugin>
```

### Statische Analyse zur Bug-Decetion

Die statische Analyse des Source-Codes findet in der Build Pipeline des Projektes statt und wird durch das [Spotbugs Maven Plugin](https://spotbugs.github.io/) durchgeführt. Ebenfalls werden die gefundenen Bugs auch in einem separaten Bericht, der in der Übersicht der Build Pipeline abrufbar ist, aufgelistet.

Die verwendete Config des Spotbugs Maven Plugins:

```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.7.3.2</version>
    <configuration>
        <failOnError>false</failOnError>
        <xmlOutput>true</xmlOutput>
        <excludeFilterFiles>
            <excludeFilterFile>${maven.multiModuleProjectDirectory}/.code-style-settings/spotbugs/exclude.xml</excludeFilterFile>
            <excludeFilterFile>${maven.multiModuleProjectDirectory}/.code-style-settings/spotbugs/exclude-specific.xml</excludeFilterFile>
        </excludeFilterFiles>
    </configuration>
</plugin>
```

### Pre-Commit Hooks

Git Hooks sind eine lokal Einstellung und werden nicht Upstream gepusht. Daher bedarf es hier ein separates Plugins, das in diesem Fall den Pre-Commit Hooks installiert. Zur Anwendung kommt hier das [Git Build Hook Maven Plugin](https://github.com/rudikershaw/git-build-hook), welches nur ein Pfad zu einem File erwartet, das als Pre-Commit Hooks ausgeführt werden soll. Das File ist ein Bash-Script und enthält dabei nur die beiden Commands, die bereist in den beiden vorangehenden Abschnitten besprochen wurden:

```bash
mvn spotless:check \
    checkstyle:check
```

Zu beachten ist hier, dass bei der Anwendung des Pre-Commit Hooks das Committen einige Sekunden länger beanspruchen kann, damit die beiden Checks auch ausgeführt werden können. Es ist zudem nur mögliche etwas zu Committen, wenn die beiden Checks erfolgreich durchgeführt wurden.

Die verwendete Config des Git Build Hook Maven Plugins:

```xml
<plugin>
    <groupId>com.rudikershaw.gitbuildhook</groupId>
    <artifactId>git-build-hook-maven-plugin</artifactId>
    <version>3.4.1</version>
    <configuration>
        <installHooks>
            <pre-commit>.code-style-settings/hooks/pre-commit</pre-commit>
            <pre-push>.code-style-settings/hooks/pre-push</pre-push>
        </installHooks>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>install</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
