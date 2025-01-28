run *ARGS:
  @mvn -q compile exec:java -Dexec.args="{{ ARGS }}"

debug-run *ARGS:
  mvn compile exec:java -Dexec.args="{{ ARGS }}"

clean:
  mvn clean

compile:
  mvn compile

package:
  mvn clean package
