package com.rcr2;

import com.rcr2.StateNodeTree.StateNode;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StateTest {

    @Test
    public void testFind() {
        StateNodeTree tree = new StateNodeTree();
        Set<String> dependencies1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        StateNode stateNodeABC = tree.find(dependencies1);

        assert stateNodeABC != null;
        assert "c".equals(stateNodeABC.arg);
        assert stateNodeABC.parent != null;
        assert "b".equals(stateNodeABC.parent.arg);

        assert tree.abstractionEntries.size() == 3;
        assert tree.abstractionEntries.get("[c]") != null;
        assert tree.abstractionEntries.get("[c]").get("[a, b]") != null;
        assert tree.abstractionEntries.get("[c]").get("[a, b]").equals(stateNodeABC);

        Set<String> dependencies2 = new HashSet<>(Arrays.asList("a", "b", "d"));
        StateNode stateNodeABD = tree.find(dependencies2);

        assert stateNodeABD != null;
        assert tree.abstractionEntries.get("[d]").get("[a, b]") != null;
        assert tree.abstractionEntries.get("[d]").get("[a, b]").equals(stateNodeABD);

        Set<String> dependencies3 = new HashSet<>(Arrays.asList("1", "b", "c"));
        StateNode stateNode1BC = tree.find(dependencies3);

        assert stateNode1BC != null;
        Set<StateNode> related = tree.abstractlyRelated(dependencies3, Arrays.asList("a","d"));

        assert related.contains(stateNodeABC);
        assert related.contains(stateNode1BC.parent);
    }

}
