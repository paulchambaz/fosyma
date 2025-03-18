package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.ArrayDeque;
import java.util.Deque;

class Memory {
    private final Deque<Integer> hashDeque;
    private final int maxSize;

    public Memory(int maxSize) {
        this.maxSize = maxSize;
        this.hashDeque = new ArrayDeque<>(maxSize);
    }

    public Integer addHash(SerializableKnowledge knowledge) {
        Integer hash = knowledge.hashCode();
        
        if (hashDeque.size() >= maxSize) {
            hashDeque.removeFirst();
        }
        
        hashDeque.addLast(hash);
        return hash;
    }

    public Integer getLatestHash() {
        if (hashDeque.isEmpty()) {
            return null;
        }
        return hashDeque.getLast();
    }

    public boolean hasHash(int hash) {
        return this.hashDeque.contains(hash);
    }
}
