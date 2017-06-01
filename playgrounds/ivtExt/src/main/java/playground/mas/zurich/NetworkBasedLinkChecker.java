package playground.mas.zurich;

import org.matsim.api.core.v01.network.Link;
import playground.mas.MASAttributeUtils;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.zurich_av.replanning.ZurichAVLinkChecker;

public class NetworkBasedLinkChecker implements ZurichAVLinkChecker {
    @Override
    public boolean isAcceptable(Link link) {
        return link.getAllowedModes().contains(AVModule.AV_MODE);
    }
}
