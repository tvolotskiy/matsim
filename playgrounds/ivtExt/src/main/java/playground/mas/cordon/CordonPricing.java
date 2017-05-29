package playground.mas.cordon;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import playground.mas.MASConfigGroup;

import java.util.Collection;

public class CordonPricing {
    final private CordonState outerState;
    final private CordonState innerState;

    final private Collection<Id<Link>> outerLinkIds;
    final private Collection<Id<Link>> innerLinkIds;

    final private MASConfigGroup masConfigGroup;
    final private Network network;

    public CordonPricing(MASConfigGroup masConfigGroup, Network network, CordonState innerState, CordonState outerState, Collection<Id<Link>> innerLinkIds, Collection<Id<Link>> outerLinkIds) {
        this.innerState = innerState;
        this.outerState = outerState;
        this.innerLinkIds = innerLinkIds;
        this.outerLinkIds = outerLinkIds;

        this.masConfigGroup = masConfigGroup;
        this.network = network;
    }

    public boolean isAffectingLink(Id<Link> linkId) {
        return outerLinkIds.contains(linkId) || innerLinkIds.contains(linkId);
    }

    public double getFee(Id<Link> linkId, ChargeType chargeType, double time) {
        return getFee(network.getLinks().get(linkId), chargeType, time);
    }

    public double getFee(Link link, ChargeType chargeType, double time) {
        double fee = 0.0;

        if (outerLinkIds.contains(link.getId()) && outerState.isCordonActive(time)) {
            switch (chargeType) {
                case AV_SOLO: fee = masConfigGroup.getAVSoloCordonFee(); break;
                case AV_POOL: fee = masConfigGroup.getAVPoolCordonFee(); break;
                case EV: fee = masConfigGroup.getEVCordonFee(); break;
                case CAR: fee = masConfigGroup.getCarCordonFee(); break;
            }
        }

        if (innerLinkIds.contains(link.getId()) && innerState.isCordonActive(time)) {
            switch (chargeType) {
                case CAR:
                case AV_SOLO:
                    fee += masConfigGroup.getInnerCordonFeePerKm() * link.getLength() / 1000.0;
            }
        }

        return fee;
    }
}
