package eu.su.mas.dedaleEtu.mas.knowledge;

class Memory {
  // TODO: une deque
  private List<Integer> hashList;

  public Memory() {
    this.hashList = new ArrayList<>();
  }

  public Integer addHash(SerializableKnowledge knowledge) {
    Integer hash = knowledge.hashCode()
    hashList.add(hash);
    return hash;
  }

  public Integer getLatestHash() {
    return hashList.get(hashList.size()-1);
  }

  public boolean hasHash(int hash) {
    return this.hashList.contains(hash);
  }
}
