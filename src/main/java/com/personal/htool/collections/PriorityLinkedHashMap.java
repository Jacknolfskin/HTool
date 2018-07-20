package com.personal.htool.collections;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带有优先级的有序哈希表
 */
public class PriorityLinkedHashMap<K, V> implements Iterable<Map.Entry<K, V>>{
    private ConcurrentHashMap<Integer, Node<K, V>> priorityMap = new ConcurrentHashMap<>();
    private List<Integer> priorityList = new ArrayList<>();

    public void add(int priority, K key, V value) {
        if (priorityMap.containsKey(priority)) {
            priorityMap.get(priority).getData().put(key, value);
        } else {
            LinkedHashMap<K, V> linkedHashMap = new LinkedHashMap<>();
            linkedHashMap.put(key, value);
            priorityMap.put(priority, new Node<>(priority, linkedHashMap));
            priorityList.add(priority);
            sortPriority();
        }
    }

    public void remove(int priority, K key) {
        if (!priorityMap.containsKey(priority)) {
            return;
        }
        priorityMap.get(priority).getData().remove(key);
    }

    public void putToTail(int priority, K key, V value) {
        if (priorityMap.containsKey(priority)) {
            LinkedHashMap<K, V> linkedHashMap = priorityMap.get(priority).getData();
            if (linkedHashMap.containsKey(key)) {
                linkedHashMap.remove(key);
            }
            linkedHashMap.put(key, value);
        } else {
            add(priority, key, value);
        }
    }

    private void sortPriority() {
        Collections.sort(priorityList, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        });
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new Itor<>();
    }

    private class Itor<K, V> implements Iterator<Map.Entry<K, V>> {
        int priorityCnt;
        int priorityCursor;
        int dataSize;
        int dataCursor;

        Itor() {
            priorityCnt = priorityList.size();
            if (priorityCnt > 0 && priorityMap.containsKey(priorityList.get(0))) {
                dataSize = priorityMap.get(priorityList.get(0)).getData().size();
            }
            priorityCursor = 0;
            dataCursor = 0;
        }

        @Override
        public boolean hasNext() {
            if (priorityCursor < priorityCnt) {
                return true;
            } else if (priorityCursor == priorityCnt){
                return dataCursor < dataSize;
            } else {
                return false;
            }
        }

        @Override
        public Map.Entry<K, V> next() {
            if (!hasNext()) {
                return null;
            }
            LinkedHashMap<K, V> map = (LinkedHashMap<K, V>) priorityMap.get(priorityList.get(priorityCursor)).getData();
            Map.Entry<K, V> entry = null;
            int i = 0;
            for (Map.Entry<K, V> e : map.entrySet()) {
                if (i < dataCursor) {
                    i++;
                    continue;
                }
                entry = e;
                break;
            }
            dataCursor++;
            if (dataCursor == dataSize) {
                priorityCursor++;
                dataCursor = 0;
                if (priorityCursor >= priorityCnt) {
                    dataSize = 0;
                } else {
                    dataSize = priorityMap.get(priorityList.get(priorityCursor)).getData().size();
                }
            }
            return entry;
        }

        @Override
        public void remove() {
        }
    }

    private static class Node<K, V> {
        private int priority;
        private LinkedHashMap<K, V> data;

        public Node(int priority, LinkedHashMap<K, V> data) {
            this.priority = priority;
            this.data = data;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public LinkedHashMap<K, V> getData() {
            return data;
        }

        public void setData(LinkedHashMap<K, V> data) {
            this.data = data;
        }
    }

    public static void main(String[] args) {
        PriorityLinkedHashMap<Integer, Integer> p = new PriorityLinkedHashMap<>();
        p.add(-1, -1, -1);
        p.add(100, 100, 100);
        p.add(2, 2, 2);
        p.add(2, 3, 3);
        p.putToTail(2, 2, 2);
        for (Map.Entry<Integer, Integer> e: p) {
            System.out.println(e.getKey() + "," + e.getValue());
        }
    }
}
