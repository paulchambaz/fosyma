run *ARGS:
  @cd dedale-etu && mvn -q compile exec:java -Dexec.mainClass="eu.su.mas.dedaleEtu.princ.Principal" -Dexec.args="{{ ARGS }}"

build:
  @cd dedale-etu && mvn -q compile

clean:
  mvn clean

compile:
  mvn compile

fmt:
  find . -name '*.java' | xargs google-java-format --replace

package:
  mvn clean package
