package contrib.baseline.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import contrib.baseline.lib.ActivityAnalyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static contrib.baseline.preparation.IVTConfigCreator.*;

/**
 * Creates preferences for a given population.
 *
 * This class is based on the class playground.stahela.matsim2030.CreatePrefs.java
 *
 * @author boescpa,balac
 */
public class PrefsCreator {

    private final static Logger log = Logger.getLogger(PrefsCreator.class);
    private final static Random random = new Random(37835409);

    public enum actCharacteristics {
        HOME, REMOTE_HOME, WORK, REMOTE_WORK, EDUCATION, LEISURE, SHOP, ESCORT_KIDS, ESCORT_OTHER;

        public double getMinDur() {
            double minDurInMins = 0;
            // default 30min, only for obviously possibly shorter activities the min duration is different
            switch (this) {
                case HOME: minDurInMins = 30; break;
                case REMOTE_HOME: minDurInMins = 30; break;
                case WORK: minDurInMins = 30; break;
                case REMOTE_WORK: minDurInMins = 15; break;
                case EDUCATION: minDurInMins = 30; break;
                case LEISURE: minDurInMins = 15; break;
                case SHOP: minDurInMins = 5; break;
                case ESCORT_KIDS: minDurInMins = 1; break;
                case ESCORT_OTHER: minDurInMins = 1; break;
            }
            // return the value in seconds
            return minDurInMins * 60;
        }

        public String getAbbr() {
            switch (this) {
                case HOME: return "-h-";
                case REMOTE_HOME: return "-rh-";
                case WORK: return "-w-";
                case REMOTE_WORK: return "-rw-";
                case EDUCATION: return "-e-";
                case LEISURE: return "-l-";
                case SHOP: return "-s-";
                case ESCORT_KIDS: return "-k-";
                case ESCORT_OTHER: return "-o-";
                default: return "";
            }
        }
    }

    public static void main(final String[] args) {
        final String pathToInputPopulation = args[0];
        final String pathToInputPrefs = args[1];
        final String pathToOutputPopulation = args[2];
        final String pathToOutputPrefs = args[3];

        ObjectAttributes prefs = getObjectAttributes(pathToInputPrefs);

        Population population = getPopulation(pathToInputPopulation);
        createPrefsBasedOnPlans(population, prefs);

        ObjectAttributesXmlWriter attributesXmlWriterWriter = new ObjectAttributesXmlWriter(prefs);
        attributesXmlWriterWriter.writeFile(pathToOutputPrefs);

        new PopulationWriter(population).write(pathToOutputPopulation);
    }

    public static ObjectAttributes createPrefsBasedOnPlans(final Population population) {
        ObjectAttributes prefs = new ObjectAttributes();
        createPrefsBasedOnPlans(population, prefs);
        return prefs;
    }

    public static void createPrefsBasedOnPlans(final Population population, final ObjectAttributes prefs) {

        for(Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			String personId = p.getId().toString();
			Map<String, Integer> count = new HashMap<>();
			Activity firstActivity = null;
			int size = plan.getPlanElements().size();
			if (size == 1) {
				
				String type = ((Activity) plan.getPlanElements().get(0)).getType();
				
				setDurations(prefs, type + "_" + 1, 24.0 * 3600.0, personId);
				
				((Activity) plan.getPlanElements().get(0)).setType(type + "_1");
				continue;
			}
			//boolean firstlastsame = ((Activity)plan.getPlanElements().get(0)).getType().
			//		equals(((Activity)plan.getPlanElements().get(size - 1)).getType());
			for (PlanElement pe : plan.getPlanElements()) {			
				
				if (pe instanceof Activity && !((Activity) pe).getType().equals("pt interaction")) {
					Activity act = (Activity) pe;
					String type = act.getType();
					
					if (count.containsKey(type))
						count.put(type, count.get(type) + 1);
					else
						count.put(type, 1);
							
					if (plan.getPlanElements().get(0) == pe) {
						//==== handling first activity in the plan =====//

						if (type.equals(((Activity)plan.getPlanElements().get(size - 1)).getType())) {
						
							firstActivity = (Activity) pe;
						}
						else {
							double duration = act.getEndTime();
							setDurations(prefs, type + "_" + 1, duration, personId);							
						}
						
						act.setType(type + "_" + 1);

					}
					else if (plan.getPlanElements().get(size - 1) == pe) {
						//==== handling last activity in the plan =====//
						if (firstActivity != null) {
							//===== agents who do have first and last activity of the same type =====//

							double duration = firstActivity.getEndTime() + 24 * 3600.0 - act.getStartTime();
							if (duration <= 0.0) {
								log.error(
										"For act type " + act.getType() + " of person " + p.getId() + " duration is either zero or negative.");
								throw new RuntimeException();
							}
							
							setDurations(prefs, type + "_" + 1, duration, personId);							
							act.setType(type + "_" + 1);
						}
						else {
							//===== agents who do not have first and last activity of the same type =====//
							double duration = 24 * 3600.0 - ((Activity) pe).getStartTime();
							if (duration <= 0.0) {
								log.error(
										"For act type " + act.getType() + " of person " + p.getId() + " duration is either zero or negative.");	
								throw new RuntimeException();
							}
							
							setDurations(prefs, type + "_" + count.get(type), duration, personId);
							act.setType(type + "_" + count.get(type));							
						}
					}
					else {
						double duration = act.getEndTime() - act.getStartTime();
						
						if (duration <= 0.0) {
							log.error("For act type " + act.getType() + 
									" of person " + p.getId() + " duration is either zero or negative.");	
							throw new RuntimeException();
						}
						setDurations(prefs, type + "_" + count.get(type), duration, personId);
					
						act.setType(type + "_" + count.get(type));							

					}
				}
			}
        }
    }

    /**
     * Sets the preferences for the given activity.
     *  MinDuration according to Enum minDuration.
     *  TypicalDuration to the provided value or, if provided value < minDuration, to minDuration.
     */
    private static void setDurations(ObjectAttributes prefs, String activity, double typicalDuration, String personId) {

        prefs.putAttribute(personId, "typicalDuration_" + activity, typicalDuration);
        prefs.putAttribute(personId, "minimalDuration_" + activity, 0.0);
        prefs.putAttribute(personId, "earliestEndTime_" + activity, 0.0 * 3600.0);
        prefs.putAttribute(personId, "latestStartTime_" + activity, 24.0 * 3600.0);
    }

    protected static ObjectAttributes createRandomPrefs(final Population population) {
        //////////////////////////////////////////////////////////////////////
        // create prefs

        ObjectAttributes prefs = new ObjectAttributes();
        Counter counter = new Counter(" person # ");
        ActivityAnalyzer activityAnalyzer = new ActivityAnalyzer();
        int nrOfActs, nrWorkActs;
        double timeBudget;
        String actChain;
        boolean education, shop, leisure;

        for (Person p : population.getPersons().values()) {
            counter.incCounter();

            if (p.getSelectedPlan() != null) {
                // reset person
                nrOfActs = 0;
                nrWorkActs = 0;
                timeBudget = 24*3600.0;
                actChain = "";
                education = false;
                shop = false;
                leisure = false;
                // get number of activities and actChain
                for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof Activity) {
                        nrOfActs += 1;
                        Activity act = (Activity) pe;
                        actChain = actChain.concat(act.getType().substring(0, 1));
                        switch (act.getType()) {
                            case WORK:
                                nrWorkActs += 1;
                                break;
                            case EDUCATION:
                                education = true;
                                break;
                            case SHOP:
                                shop = true;
                                break;
                            case LEISURE:
                                leisure = true;
                                break;
                        }
                    }
                    activityAnalyzer.addActChain(actChain);
                }
                // assign durations
                timeBudget = getRandomWorkDurations(prefs, nrOfActs, nrWorkActs, timeBudget, p);
                timeBudget = getRandomEducationDurations(prefs, nrOfActs, timeBudget, education, shop, leisure, p);
                timeBudget = getRandomSecondaryActivityDurations(prefs, timeBudget, shop, leisure, p);
                setTimeBudgetAsHomeDuration(prefs, timeBudget, p);
            } else {
                log.warn("person " +p.getId().toString()+ " has no plan defined");
            }
        }

        counter.printCounter();
        activityAnalyzer.printActChainAnalysis();
        return prefs;
    }

    private static void setTimeBudgetAsHomeDuration(ObjectAttributes prefs, double timeBudget, Person p) {
        // assign remaining timeBudget to home activities
        prefs.putAttribute(p.getId().toString(), "typicalDuration_home", timeBudget);
        prefs.putAttribute(p.getId().toString(), "minimalDuration_home", 0.5 * 3600.0);
        prefs.putAttribute(p.getId().toString(), "earliestEndTime_home", 0.0 * 3600.0);
        prefs.putAttribute(p.getId().toString(), "latestStartTime_home", 24.0 * 3600.0);

        //log.info("person p " +p.getId().toString()+ " has home duration: " +typicalHomeDuration/3600);
    }

    private static double getRandomSecondaryActivityDurations(ObjectAttributes prefs, double timeBudget, boolean shop, boolean leisure, Person p) {
        // draw a duration for secondary activities
        if (shop || leisure) {
            // agent should be home at least for 5-7 hours
            double maxSecActDur = timeBudget-(5*3600.0+random.nextInt(2));
            double typicalShoppingDuration;
            double typicalLeisureDuration;

            // if both act types are reported:
            // both between 0.5 and available time budget
            if (shop && leisure) {
                // agent should be home at least for 5-7 hours
                typicalShoppingDuration = 1800.0 + random.nextInt((int) (maxSecActDur-3600.0));
                typicalLeisureDuration = maxSecActDur - typicalShoppingDuration;

                prefs.putAttribute(p.getId().toString(), "typicalDuration_shop", typicalShoppingDuration);
                prefs.putAttribute(p.getId().toString(), "minimalDuration_shop", 0.5 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "earliestEndTime_shop", 0.0 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "latestStartTime_shop", 24.0 * 3600.0);

                prefs.putAttribute(p.getId().toString(), "typicalDuration_leisure", typicalLeisureDuration);
                prefs.putAttribute(p.getId().toString(), "minimalDuration_leisure", 0.5 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "earliestEndTime_leisure", 0.0 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "latestStartTime_leisure", 24.0 * 3600.0);

                timeBudget -= typicalLeisureDuration;
                timeBudget -= typicalShoppingDuration;
                //log.info("person p " +p.getId().toString()+ " has leisure duration: " +typicalLeisureDuration/3600);
                //log.info("person p " +p.getId().toString()+ " has shop duration: " +typicalShoppingDuration/3600);
            }
            else if (shop && !leisure) {
                typicalShoppingDuration = 1800 + random.nextInt((int) ((maxSecActDur-1800.0)));

                prefs.putAttribute(p.getId().toString(), "typicalDuration_shop", typicalShoppingDuration);
                prefs.putAttribute(p.getId().toString(), "minimalDuration_shop", 0.5 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "earliestEndTime_shop", 0.0 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "latestStartTime_shop", 24.0 * 3600.0);

                timeBudget -= typicalShoppingDuration;
                //log.info("person p " +p.getId().toString()+ " has shop duration: " +typicalShoppingDuration/3600);
            }
            else if (!shop && leisure) {
                typicalLeisureDuration = 1800 + random.nextInt((int) ((maxSecActDur-1800.0)));

                prefs.putAttribute(p.getId().toString(), "typicalDuration_leisure", typicalLeisureDuration);
                prefs.putAttribute(p.getId().toString(), "minimalDuration_leisure", 0.5 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "earliestEndTime_leisure", 0.0 * 3600.0);
                prefs.putAttribute(p.getId().toString(), "latestStartTime_leisure", 24.0 * 3600.0);

                timeBudget -= typicalLeisureDuration;
                //log.info("person p " +p.getId().toString()+ " has leisure duration: " +typicalLeisureDuration/3600);
            }
        }
        return timeBudget;
    }

    private static double getRandomEducationDurations(ObjectAttributes prefs, int nrOfActs, double timeBudget, boolean education, boolean shop, boolean leisure, Person p) {
        // draw a duration for education activities
        if (education) {
            double typicalEducationDuration;
            // less than 4 acts or only one other secondary activity type:
            // 7-9h
            if (nrOfActs < 4 || (leisure && !shop) || (!leisure && shop)) {
                typicalEducationDuration = 25200 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
            }
            // else:
            // 4-6
            else {
                typicalEducationDuration = 14400 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
            }

            prefs.putAttribute(p.getId().toString(), "typicalDuration_education", typicalEducationDuration);
            prefs.putAttribute(p.getId().toString(), "minimalDuration_education", 0.5 * 3600.0);
            prefs.putAttribute(p.getId().toString(), "earliestEndTime_education", 0.0 * 3600.0);
            prefs.putAttribute(p.getId().toString(), "latestStartTime_education", 24.0 * 3600.0);

            timeBudget -= typicalEducationDuration;
            //log.info("person p " +p.getId().toString()+ " has education duration: " +typicalEducationDuration/3600);
        }
        return timeBudget;
    }

    private static double getRandomWorkDurations(ObjectAttributes prefs, int nrOfActs, int nrWorkActs, double timeBudget, Person p) {
        // draw a duration for work activities
        if (nrWorkActs > 0) {
            double typicalWorkDuration = 0;
            // if number of nonWorkActs < 4:
            // 4-7h with 10% prob, 7-9h with 80% prob and 9-12 hours with 10% prob
            if (nrOfActs-nrWorkActs < 4) {
                double prob = random.nextDouble();
                if (prob <= 0.1) {
                    typicalWorkDuration = 14400 + random.nextInt(2)*3600.0 + random.nextDouble()*3600.0;
                }
                else if (prob > 0.1 && prob < 0.9) {
                    typicalWorkDuration = 25200 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
                }
                else if (prob >= 0.9) {
                    typicalWorkDuration = 32400 + random.nextInt(2)*3600.0 + random.nextDouble()*3600.0;
                }
            }
            // if number of nonWorkActs >= 4:
            // 3-8h
            else {
                typicalWorkDuration = 10800 + random.nextInt(4) + random.nextDouble()*3600.0;
            }

            prefs.putAttribute(p.getId().toString(), "typicalDuration_work", typicalWorkDuration);
            prefs.putAttribute(p.getId().toString(), "minimalDuration_work", 0.5 * 3600.0);
            prefs.putAttribute(p.getId().toString(), "earliestEndTime_work", 0.0 * 3600.0);
            prefs.putAttribute(p.getId().toString(), "latestStartTime_work", 24.0 * 3600.0);

            timeBudget -= typicalWorkDuration;
            //log.info("person p " +p.getId().toString()+ " has working duration: " +typicalWorkDuration/3600);
        }
        return timeBudget;
    }

    private static ObjectAttributes getObjectAttributes(String pathToInputPrefs) {
        ObjectAttributes prefs = new ObjectAttributes();
        ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(prefs);
        reader.readFile(pathToInputPrefs);
        return prefs;
    }

    private static Population getPopulation(final String pathToPopFile) {
        //////////////////////////////////////////////////////////////////////
        // read in population

        log.info("Reading plans...");
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader PlansReader = new PopulationReader(scenario);
        PlansReader.readFile(pathToPopFile);
        Population population = scenario.getPopulation();
        log.info("Reading plans...done.");
        log.info("Population size is " +population.getPersons().size());
        return population;
    }

}
