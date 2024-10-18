package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.FlatDependency;
import dev.harrel.jarhell.model.Gav;

import java.util.Arrays;
import java.util.List;

class AnalysisChain {
    private final Gav[] chain;
    private final boolean[] links;

    private AnalysisChain(Gav[] chain, boolean[] links) {
        this.chain = chain;
        this.links = links;
    }

    static AnalysisChain start(Gav gav) {
        return new AnalysisChain(new Gav[]{gav}, new boolean[0]);
    }

    AnalysisChain nextNode(FlatDependency dep) {
        Gav[] newChain = Arrays.copyOf(chain, chain.length + 1);
        boolean[] newLinks = Arrays.copyOf(links, links.length + 1);
        newChain[chain.length] = dep.gav();
        newLinks[links.length] = dep.optional();
        return new AnalysisChain(newChain, newLinks);
    }

    CycleData checkCycle() {
        Gav lastGav = chain[chain.length - 1];
        boolean hardLink = true;
        for (int i = links.length - 1; i >= 0; i--) {
            hardLink &= !links[i];
            if (chain[i].equals(lastGav)) {
                return new CycleData(
                    hardLink,
                    Arrays.asList(chain).subList(0, i),
                    Arrays.asList(chain).subList(i, chain.length));
            }
        }
        return null;
    }

    record CycleData(boolean hard, List<Gav> preceding, List<Gav> cycle) {}
}
