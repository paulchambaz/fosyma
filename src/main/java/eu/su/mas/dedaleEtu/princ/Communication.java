package eu.su.mas.dedaleEtu.princ;

import jade.core.AID;
import java.io.Serializable;

public class Communication implements Serializable {
  private AID friend;
  private String protocol;
  private boolean shouldSpeak;

  public Communication(AID friend, String protocol, boolean shouldSpeak) {
    this.friend = friend;
    this.protocol = protocol;
    this.shouldSpeak = shouldSpeak;
  }

  public AID getFriend() {
    return this.friend;
  }

  public String getProtocol() {
    return this.protocol;
  }

  public boolean shouldSpeak() {
    return this.shouldSpeak;
  }

  public boolean shouldAwait() {
    return !this.shouldSpeak;
  }
}
