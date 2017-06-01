package playground.zurich_av.replanning;

import org.matsim.api.core.v01.network.Link;

public interface ZurichAVLinkChecker {
    boolean isAcceptable(Link link);
}
