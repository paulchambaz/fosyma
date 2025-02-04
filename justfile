run *ARGS:
  @cd dedale-etu && mvn -q compile exec:java -Dexec.mainClass="eu.su.mas.dedaleEtu.princ.Principal" -Dexec.args="{{ ARGS }}"

clean:
  mvn clean

compile:
  mvn compile

package:
  mvn clean package
