package playground.mas.cordon;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import playground.mas.MASAttributeUtils;
import playground.mas.MASConfigGroup;

public class CordonPricing {
    final private CordonState outerState;
    final private CordonState innerState;

    final private MASConfigGroup masConfigGroup;
    final private Network network;

    public CordonPricing(MASConfigGroup masConfigGroup, Network network, CordonState innerState, CordonState outerState) {
        this.innerState = innerState;
        this.outerState = outerState;

        this.masConfigGroup = masConfigGroup;
        this.network = network;
    }

    public boolean isAffectingLink(Link link) {
        return MASAttributeUtils.isInnerCordon(link) || MASAttributeUtils.isOuterCordon(link);
    }

    public boolean isAffectingLink(Id<Link> linkId) {
        return isAffectingLink(network.getLinks().get(linkId));
    }

    public double getFee(Id<Link> linkId, ChargeType chargeType, double time) {
        return getFee(network.getLinks().get(linkId), chargeType, time);
    }

    public double getFee(Link link, ChargeType chargeType, double time) {
        double fee = 0.0;

        if (MASAttributeUtils.isOuterCordon(link) && outerState.isCordonActive(time)) {
            switch (chargeType) {
                case AV_SOLO: fee = masConfigGroup.getAVSoloCordonFee(); break;
                case AV_POOL: fee = masConfigGroup.getAVPoolCordonFee(); break;
                case EV: fee = masConfigGroup.getEVCordonFee(); break;
                case CAR: fee = masConfigGroup.getCarCordonFee(); break;
            }
        }

        if (MASAttributeUtils.isInnerCordon(link) && innerState.isCordonActive(time)) {
            switch (chargeType) {
                case CAR:
                case AV_SOLO:
                    fee += masConfigGroup.getInnerCordonFeePerKm() * link.getLength() / 1000.0;
            }
        }

        return fee;
    }
}
