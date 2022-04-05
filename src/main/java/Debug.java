/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

import java.util.*;
import java.util.stream.Collectors;

public class Debug {
    public static void main(String[] args) {
        Map<Pair<Integer, Integer>, String> map = new HashMap<>();
        map.put(Pair.of(1, 2), "unti");
        map.put(Pair.of(1, 2), "buri");
        map.put(Pair.of(2, 3), "oppai");
        System.out.println(map);
        System.out.println(map.keySet().stream().filter(pair -> pair.equals(Pair.of(1, 2))).collect(Collectors.toSet()));
    }
}

class Pair<L, R> {
    private static final Set<Pair<?, ?>> INSTANCES = new HashSet<>();
    private final L left;
    private final R right;

    private Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public static <L, R> Pair<L, R> of(L left, R right) {
        Pair<L, R> inst = new Pair<>(left, right);
        List<Pair<L, R>> searchedInstances = INSTANCES.stream()
                .filter(pair -> pair.equals(inst))
                .map(pair -> (Pair<L, R>) pair)
                .toList();
        if (searchedInstances.size() > 0) return searchedInstances.get(0);
        INSTANCES.add(inst);
        return inst;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair<?, ?> pair) {
            if (pair.getLeft().equals(this.getLeft()) && pair.getRight().equals(this.getRight())) {
                return true;
            }
        }

        return super.equals(obj);
    }
}