# Maven Cheat Sheet

## Create New Project

```bash
# Create new project
mvn archetype:generate \
    -DgroupId=xyz.chambaz \
    -DartifactId=fosyma \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false

# Go to project
cd fosyma
```

## Run Your Code

```bash
# Run your app (replace App with your main class)
mvn compile exec:java -Dexec.mainClass="xyz.chambaz.App"

# Run with arguments
mvn compile exec:java -Dexec.mainClass="xyz.chambaz.App" -Dexec.args="arg1 arg2"

# Run quietly (no Maven output)
mvn -q compile exec:java

# Build the jar
mvn package

# Clean and rebuild
mvn clean package
```

## Add Dependencies

```xml
# Add to pom.xml inside <dependencies>...</dependencies>
<dependency>
    <groupId>org.example</groupId>
    <artifactId>library-name</artifactId>
    <version>1.2.3</version>
</dependency>
```

## Make Running Easier

Add this to your pom.xml after `</dependencies>`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <mainClass>xyz.chambaz.App</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Then you can just run:

```bash
mvn compile exec:java
```

## Troubleshooting

```bash
# Clean everything
mvn clean

# Show what's wrong
mvn -X compile

# Check dependencies
mvn dependency:tree
```

Find dependencies: https://mvnrepository.com
