package eu.su.mas.dedaleEtu.princ;

class Communication {
  private String friend;
  private String protocol;
  private boolean shouldSpeak;

  public Communication(String friend, String protocol, boolean shouldSpeak) {
    this.friend = friend;
    this.protocol = protocol;
    this.shouldSpeak = shouldSpeak;
  }

  public String getFriend() { return this.friend; }
  public String getProtocol() { return this.protocol; }

  public boolean shouldSpeak() {
    return this.shouldSpeak;
  }

  public boolean shouldAwait() {
    return !this.shouldSpeak;
  }
}

