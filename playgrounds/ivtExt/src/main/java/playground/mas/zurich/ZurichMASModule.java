package playground.mas.zurich;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import contrib.baseline.IVTBaselineScoringFunctionFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.PtConstants;
import playground.mas.MASConfigGroup;
import playground.mas.cordon.CordonCharger;
import playground.mas.cordon.MASCordonUtils;
import playground.mas.scoring.MASScoringFunctionFactory;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.zurich_av.ZurichGenerator;
import playground.zurich_av.replanning.ZurichPlanStrategyProvider;

import java.util.Collection;
import java.util.stream.Collectors;

public class ZurichMASModule extends AbstractModule {
    final public static String AV_AREA_LINKS = "av_area_links";

    @Override
    public void install() {
        bind(new TypeLiteral<Collection<Id<Link>>>() {})
                .annotatedWith(Names.named("zurich"))
                .to(Key.get(new TypeLiteral<Collection<Id<Link>>>() {}, Names.named(AV_AREA_LINKS)));

        AVUtils.registerGeneratorFactory(binder(), "ZurichGenerator", ZurichGenerator.ZurichGeneratorFactory.class);
        addPlanStrategyBinding("ZurichModeChoice").toProvider(ZurichPlanStrategyProvider.class);
    }

    @Provides @Singleton
    AVScoringFunctionFactory provideAVScoringFunctionFactory(Scenario scenario, AVConfig config) {
        return new AVScoringFunctionFactory(
                new IVTBaselineScoringFunctionFactory(scenario, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)),
                scenario, config);
    }

    @Provides @Singleton @Named(AV_AREA_LINKS)
    public Collection<Id<Link>> provideAVAreaLinkIds(ZurichMASConfigGroup masConfig, Network network) {
        return MASCordonUtils.findInsideCordonLinks(masConfig.getAVAreaCenterNodeId(), masConfig.getAVAreaRadius(), network)
                .stream()
                .filter(l -> l.getAllowedModes().contains("car"))
                .map(l -> l.getId()).collect(Collectors.toList());
    }
}
