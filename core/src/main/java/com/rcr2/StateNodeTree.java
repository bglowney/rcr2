package com.rcr2;

import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class StateNodeTree {

    final Map<String,Map<String,StateNode>> abstractionEntries = new HashMap<>();
    final StateNode root = new StateNode(null,null);

    StateNode newStateNode(@NotNull StateNode parent, @NotNull List<String> dependencies, int index) {
        StateNode stateNode = new StateNode(dependencies.get(index), parent);
        String before = dependencies.subList(0, index).toString();
        String after = dependencies.subList(index, dependencies.size()).toString();
        abstractionEntries
                .computeIfAbsent(after, a -> new HashMap<>())
                .computeIfAbsent(before, b -> stateNode);

        return stateNode;
    }

    public StateNode find(Set<String> dependencies) {
        return find(root, new ArrayList<>(dependencies), 0);
    }

    private StateNode find(StateNode stateNode, List<String> dependencies, int i) {
        if (dependencies.size() == i)
            return stateNode;

        String next = new ArrayList<>(dependencies).get(i);
        return find(stateNode.children.computeIfAbsent(next, serializedArg -> newStateNode(stateNode, dependencies, i)),
                    dependencies,
                 i + 1);
    }

    public Set<StateNode> abstractlyRelated(Set<String> scopedDependenciesSet, List<String> nonScopedDependencies) {
        List<String> scopedDependencies = new ArrayList<>(scopedDependenciesSet);
        Set<StateNode> related = new HashSet<>();
        if (scopedDependencies.size() >= 2) {
            String after = scopedDependencies.subList(scopedDependencies.size() - 1, scopedDependencies.size()).toString();
            List<String> beforeDependencies = scopedDependencies.subList(0, scopedDependencies.size() - 1);
            for (String nonScoped : nonScopedDependencies) {
                List<String> modified = beforeDependencies.stream().collect(Collectors.toList());
                modified.set(0, nonScoped);

                Map<String,StateNode> m = abstractionEntries.get(after);
                if (m != null) {
                    StateNode relatedNode = m.get(modified.toString());
                    if (relatedNode != null)
                        related.add(relatedNode);
                }
            }
        }
        StateNode stateNode = find(scopedDependenciesSet);
        if (stateNode.parent != null)
            related.add(stateNode.parent);

        return related;
    }

    @RequiredArgsConstructor
    public static class StateNode {
        @NotNull final String arg;
        @NotNull final StateNode parent;
        @NotNull final Map<String, StateNode> children = new HashMap<>();

        String serialize() {
            String s = arg;
            StateNode current = parent;
            while (current != null) {
                s += ":" + current.arg;
                current = current.parent;
            }
            return s;
        }

    }

}
