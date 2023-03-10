package com.redhat.demo;

import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemTracking {

    private TreeMap<Long, Boolean> missing = new TreeMap<>();
    private Long last = 1L;
    private Long duplicateCount = 0L;

    public void track(Long key) {
        // key can be `null` if the incoming record has no key

        // reset message
        if (key == 1) {
            last = 1L;
            duplicateCount = 0L;
            missing.clear();
        }

        // if there is a jump, prepare a missing list
        // e.g. key = 13 and last = 10, missing = [ {11, false}, {12, false} ]
        for (long i = last+1; i < key; i++) {
            missing.put(i, false);
        }

        // if the key is lower than the last one and not missing, then it's a duplicate
        if (key < last && !missing.containsKey(key))
            duplicateCount++;

        // last is current
        if (key >= last) last=key;

        // in case it was missing, remove from missing list
        missing.remove(key);

        System.out.println(String.format("Current Key: %d, Missing messages: %d, Duplicated msg: %d", key, missing.size(),
                duplicateCount));

    }
}
