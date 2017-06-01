package playground.mas.zurich;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class RunConvertConfig {
    static public void main(String[] args) {
        Config config = ConfigUtils.loadConfig("/home/sebastian/scenarios/mas/sioux/config.xml");
        new ConfigWriter(config).write("/home/sebastian/scenarios/mas/sioux/config_updated.xml");
    }
}
