package org.matsim.core.config.groups;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

import java.util.*;

/**
 * This is the actual config group, but we need some decoration to take care of backward
 * compatibility of underscored things.
 * Deleting the underscore version would be the clean way, but this is way to much work to go
 * through all the examples one by one, and it is difficult to convert automatically.
 */
class PlanCalcScoreDelegate extends ReflectiveConfigGroup {
	private static final Logger log = Logger.getLogger(PlanCalcScoreDelegate.class);
	public static final String GROUP_NAME = "planCalcScore";

	private static final String LEARNING_RATE = "learningRate";
	private static final String BRAIN_EXP_BETA = "BrainExpBeta";
	private static final String PATH_SIZE_LOGIT_BETA = "PathSizeLogitBeta";
	private static final String LATE_ARRIVAL = "lateArrival";
	private static final String EARLY_DEPARTURE = "earlyDeparture";
	private static final String PERFORMING = "performing";

	private static final String WAITING  = "waiting";
	private static final String WAITING_PT  = "waitingPt";

	private static final String WRITE_EXPERIENCED_PLANS = "writeExperiencedPlans";

	private static final String MARGINAL_UTL_OF_MONEY = "marginalUtilityOfMoney" ;

	private static final String UTL_OF_LINE_SWITCH = "utilityOfLineSwitch" ;
	private static final String USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION = "usingOldScoringBelowZeroUtilityDuration" ;
	private static final String FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA = "fractionOfIterationsToStartScoreMSA" ;

	/**
	 * can't set this from outside java since for the time being it is not useful there. kai, dec'13
	 */
	private boolean memorizingExperiencedPlans = false ;


	PlanCalcScoreDelegate() {
		super( GROUP_NAME );

		this.addScoringParameters( new PlanCalcScoreDelegate.ScoringParameterSet() );

		this.addModeParams( new ModeParams( TransportMode.car ) );
		this.addModeParams( new ModeParams( TransportMode.pt ) );
		this.addModeParams( new ModeParams( TransportMode.walk ) );
		this.addModeParams( new ModeParams( TransportMode.bike ) );
		this.addModeParams( new ModeParams( TransportMode.ride ) );
		this.addModeParams( new ModeParams( TransportMode.other ) );

		{
			ActivityParams params = new ActivityParams("dummy");
			params.setTypicalDuration(2.*3600.);
			this.addActivityParams(params);
			// (this is there so that an empty config prints out at least one activity type,
			// so that the explanations of this important concept show up e.g.
			// in defaultConfig.xml, created from the GUI.  kai, jul'17
		}

		// yyyyyy find better solution for this. kai, dec'15
		{
			ActivityParams params = new ActivityParams("car interaction") ;
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("pt interaction") ; // need this for self-programmed pseudo pt.  kai, nov'16
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("bike interaction") ;
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("other interaction") ;
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("walk interaction") ;
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
			// bushwhacking_walk---network_walk---bushwhacking_walk
		}
	}

	private double learningRate = 1.0;
	private double brainExpBeta = 1.0;
	private double pathSizeLogitBeta = 1.0;

	private boolean writeExperiencedPlans = false;

	private Double fractionOfIterationsToStartScoreMSA = null ;

	private boolean usingOldScoringBelowZeroUtilityDuration = false;

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA, "fraction of iterations at which MSA score averaging is started. The matsim theory department " +
				"suggests to use this together with switching off choice set innovation (where a similar switch exists), but it has not been tested yet.") ;
		map.put(USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION, "There used to be a plateau between duration=0 and duration=zeroUtilityDuration. "
				+ "This caused durations to evolve to zero once they were below zeroUtilityDuration, causing problems.  Only use this switch if you need to be "
				+ "backwards compatible with some old results.  (changed nov'13)") ;
		map.put(PERFORMING,"[utils/hr] marginal utility of doing an activity.  normally positive.  also the opportunity cost of " +
				"time if agent is doing nothing.  MATSim separates the resource value of time from the direct (dis)utility of travel time, see, e.g., "
				+ "Boerjesson and Eliasson, TR-A 59 (2014) 144-158.");
		map.put(LATE_ARRIVAL, "[utils/hr] utility for arriving late (i.e. after the latest start time).  normally negative") ;
		map.put(EARLY_DEPARTURE, "[utils/hr] utility for departing early (i.e. before the earliest end time).  Normally negative.  Probably " +
				"implemented correctly, but not tested." );
		map.put(WAITING, "[utils/hr] additional marginal utility for waiting. normally negative. this comes on top of the opportunity cost of time.  Probably " +
				"implemented correctly, but not tested.") ;
		map.put(WAITING_PT, "[utils/hr] additional marginal utility for waiting for a pt vehicle. normally negative. this comes on top of the opportunity cost " +
				"of time. Default: if not set explicitly, it is equal to traveling_pt!!!" ) ;
		map.put(BRAIN_EXP_BETA, "logit model scale parameter. default: 1.  Has name and default value for historical reasons " +
				"(see Bryan Raney's phd thesis).") ;
		map.put(LEARNING_RATE, "new_score = (1-learningRate)*old_score + learningRate * score_from_mobsim.  learning rates " +
				"close to zero emulate score averaging, but slow down initial convergence") ;
		map.put(UTL_OF_LINE_SWITCH, "[utils] utility of switching a line (= transfer penalty).  Normally negative") ;
		map.put(MARGINAL_UTL_OF_MONEY, "[utils/unit_of_money] conversion of money (e.g. toll, distance cost) into utils. Normall positive (i.e. toll/cost/fare are processed as negative amounts of money)." ) ;
		map.put(WRITE_EXPERIENCED_PLANS, "write a plans file in each iteration directory which contains what each agent actually did, and the score it received.");

		return map;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		final Set<String> subpopulations = getScoringParametersPerSubpopulation().keySet();
		if ( subpopulations.size() > 1 && subpopulations.contains( null ) ) {
			log.error( "One cannot define a default subpopulation when using specific subpopulations.");
			log.error( "Subpopulations defined for scoring are "+subpopulations );
			throw new RuntimeException( "Config group "+GROUP_NAME+" has default and specific subpopulations" );
		}
	}

	public PlanCalcScoreConfigGroup.ModeParams getOrCreateModeParams(String modeName) {
		return getScoringParameters( null ).getOrCreateModeParams( modeName );
	}
	public Collection<String> getActivityTypes() {
		return getScoringParameters( null ).getActivityParamsPerType().keySet();
	}

	public Collection<ActivityParams> getActivityParams() {
		return getScoringParameters( null ).getActivityParams();
	}

	public Map<String, ModeParams> getModes() {
		return getScoringParameters( null ).getModes();
	}

	public Map<String, ScoringParameterSet> getScoringParametersPerSubpopulation() {
		@SuppressWarnings("unchecked")
		final Collection<ScoringParameterSet> parameters = (Collection<ScoringParameterSet>) getParameterSets( ScoringParameterSet.SET_TYPE );
		final Map<String, ScoringParameterSet> map = new LinkedHashMap< >();

		for ( ScoringParameterSet pars : parameters ) {
			if ( this.isLocked() ) {
				pars.setLocked();
			}
			map.put( pars.getSubpopulation() , pars );
		}

		return map;
	}


	public double getMarginalUtlOfWaitingPt_utils_hr() {
		return getScoringParameters( null ).getMarginalUtlOfWaitingPt_utils_hr( );
	}

	public void setMarginalUtlOfWaitingPt_utils_hr(double val) {
		getScoringParameters( null ).setMarginalUtlOfWaitingPt_utils_hr( val );
	}

	public ActivityParams getActivityParams(final String actType) {
		return getScoringParameters( null ).getActivityParams( actType );
	}

	public ScoringParameterSet getScoringParameters(String subpopulation) {
		final ScoringParameterSet params = getScoringParametersPerSubpopulation().get( subpopulation );
		// If no config parameters defined for a specific subpopulation,
		// use the ones of the "default" subpopulation
		return params != null ? params : getScoringParametersPerSubpopulation().get( null );
	}

	public ScoringParameterSet getOrCreateScoringParameters(String subpopulation) {
		ScoringParameterSet params = getScoringParametersPerSubpopulation().get( subpopulation );

		if ( params ==null ) {
			params = new ScoringParameterSet( subpopulation );
			this.addScoringParameters( params );
		}

		return params;
	}

	@Override
	public void addParameterSet( final ConfigGroup set ) {
		switch ( set.getName() ) {
			case ActivityParams.SET_TYPE:
				addActivityParams( (ActivityParams) set );
				break;
			case ModeParams.SET_TYPE:
				addModeParams( (ModeParams) set );
				break;
			case ScoringParameterSet.SET_TYPE:
				addScoringParameters( (ScoringParameterSet) set );
				break;
			default:
				throw new IllegalArgumentException( set.getName() );
		}
	}

	public void addScoringParameters(final ScoringParameterSet params) {
		final ScoringParameterSet previous = this.getScoringParameters(params.getSubpopulation());

		if ( previous != null ) {
			log.info("scoring parameters for subpopulation " + previous.getSubpopulation() + " were just overwritten.") ;

			final boolean removed = removeParameterSet( previous );
			if ( !removed ) throw new RuntimeException( "problem replacing scoring params " );
		}

		super.addParameterSet( params );
	}


	public void addModeParams(final ModeParams params) {
		getScoringParameters( null ).addModeParams( params );
	}

	public void addActivityParams(final ActivityParams params) {
		getScoringParameters( null ).addActivityParams( params );
	}



	/* parameter set handling */
	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch ( type ) {
			case PlanCalcScoreConfigGroup.ActivityParams.SET_TYPE:
				return new PlanCalcScoreConfigGroup.ActivityParams();
			case PlanCalcScoreConfigGroup.ModeParams.SET_TYPE:
				return new PlanCalcScoreConfigGroup.ModeParams();
			case ScoringParameterSet.SET_TYPE:
				return new ScoringParameterSet();
			default:
				throw new IllegalArgumentException( type );
		}
	}

	@Override
	protected void checkParameterSet( final ConfigGroup module ) {
		switch ( module.getName() ) {
			case ScoringParameterSet.SET_TYPE:
				if ( !(module instanceof ScoringParameterSet) ) {
					throw new RuntimeException( "wrong class for "+module );
				}
				final String s = ((ScoringParameterSet) module).getSubpopulation();
				if ( getScoringParameters( s ) != null ) {
					throw new IllegalStateException( "already a parameter set for subpopulation "+s );
				}
				break;
			default:
				throw new IllegalArgumentException( module.getName() );
		}
	}


	public boolean isMemorizingExperiencedPlans() {
		return this.memorizingExperiencedPlans ;
	}

	public void setMemorizingExperiencedPlans(boolean memorizingExperiencedPlans) {
		this.memorizingExperiencedPlans = memorizingExperiencedPlans;
	}


	@StringGetter(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA)
	public Double getFractionOfIterationsToStartScoreMSA() {
		return fractionOfIterationsToStartScoreMSA;
	}
	@StringSetter(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA)
	public void setFractionOfIterationsToStartScoreMSA(Double fractionOfIterationsToStartScoreMSA) {
		testForLocked() ;
		this.fractionOfIterationsToStartScoreMSA = fractionOfIterationsToStartScoreMSA;
	}

	@StringGetter( LEARNING_RATE )
	public double getLearningRate() {
		return learningRate;
	}
	@StringSetter( LEARNING_RATE )
	public void setLearningRate(double learningRate) {
		testForLocked() ;
		this.learningRate = learningRate;
	}

	@StringGetter( BRAIN_EXP_BETA )
	public double getBrainExpBeta() {
		return brainExpBeta;
	}

	@StringSetter( BRAIN_EXP_BETA )
	public void setBrainExpBeta(double brainExpBeta) {
		testForLocked() ;
		this.brainExpBeta = brainExpBeta;
	}

	@StringGetter( PATH_SIZE_LOGIT_BETA )
	public double getPathSizeLogitBeta() {
		return pathSizeLogitBeta;
	}

	@StringSetter( PATH_SIZE_LOGIT_BETA )
	public void setPathSizeLogitBeta(double beta) {
		testForLocked() ;
		if ( beta != 0. ) {
			log.warn("Setting pathSizeLogitBeta different from zero is experimental.  KN, Sep'08") ;
		}
		this.pathSizeLogitBeta = beta;
	}


	@StringGetter( USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION )
	public boolean isUsingOldScoringBelowZeroUtilityDuration() {
		return usingOldScoringBelowZeroUtilityDuration;
	}

	@StringSetter( USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION )
	public void setUsingOldScoringBelowZeroUtilityDuration(
			boolean usingOldScoringBelowZeroUtilityDuration) {
		testForLocked() ;
		this.usingOldScoringBelowZeroUtilityDuration = usingOldScoringBelowZeroUtilityDuration;
	}

	@StringGetter( WRITE_EXPERIENCED_PLANS )
	public boolean isWriteExperiencedPlans() {
		return writeExperiencedPlans;
	}

	@StringSetter( WRITE_EXPERIENCED_PLANS )
	public void setWriteExperiencedPlans(boolean writeExperiencedPlans) {
		testForLocked() ;
		this.writeExperiencedPlans = writeExperiencedPlans;
	}

	public double getLateArrival_utils_hr() {
		return getScoringParameters( null ).getLateArrival_utils_hr();
	}

	public double getEarlyDeparture_utils_hr() {
		return getScoringParameters( null ).getEarlyDeparture_utils_hr();
	}

	public double getPerforming_utils_hr() {
		return getScoringParameters( null ).getPerforming_utils_hr();
	}

	public double getMarginalUtilityOfMoney() {
		return getScoringParameters( null ).getMarginalUtilityOfMoney();
	}

	public double getUtilityOfLineSwitch() {
		return getScoringParameters( null ).getUtilityOfLineSwitch();
	}

	public void setLateArrival_utils_hr(double lateArrival) {
		getScoringParameters( null ).setLateArrival_utils_hr(lateArrival);
	}

	public void setEarlyDeparture_utils_hr(double earlyDeparture) {
		getScoringParameters( null ).setEarlyDeparture_utils_hr(earlyDeparture);
	}

	public void setPerforming_utils_hr(double performing) {
		getScoringParameters( null ).setPerforming_utils_hr(performing);
	}

	public void setMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
		getScoringParameters( null ).setMarginalUtilityOfMoney(marginalUtilityOfMoney);
	}

	public void setUtilityOfLineSwitch(double utilityOfLineSwitch) {
		getScoringParameters( null ).setUtilityOfLineSwitch(utilityOfLineSwitch);
	}

	public double getMarginalUtlOfWaiting_utils_hr() {
		return getScoringParameters( null ).getMarginalUtlOfWaiting_utils_hr();
	}
	public void setMarginalUtlOfWaiting_utils_hr(double waiting) {
		getScoringParameters( null ).setMarginalUtlOfWaiting_utils_hr(waiting);
	}


	public static class ScoringParameterSet extends ReflectiveConfigGroup {
		public static final String SET_TYPE = "scoringParameters";

		private ScoringParameterSet( final String subpopulation ) {
			this();
			this.subpopulation = subpopulation;
		}

		ScoringParameterSet() {
			super( SET_TYPE );
		}

		private String subpopulation = null;

		private double lateArrival = -18.0;
		private double earlyDeparture = -0.0;
		private double performing = +6.0;

		private double waiting = -0.0;

		private double marginalUtilityOfMoney = 1.0 ;

		private double utilityOfLineSwitch = - 1 ;

		private Double waitingPt = null ;  // if not actively set by user, it will later be set to "travelingPt".

		@StringGetter( LATE_ARRIVAL )
		public double getLateArrival_utils_hr() {
			return lateArrival;
		}

		@StringSetter( LATE_ARRIVAL )
		public void setLateArrival_utils_hr(double lateArrival) {
			testForLocked() ;
			this.lateArrival = lateArrival;
		}

		@StringGetter( EARLY_DEPARTURE )
		public double getEarlyDeparture_utils_hr() {
			return earlyDeparture;
		}

		@StringSetter( EARLY_DEPARTURE )
		public void setEarlyDeparture_utils_hr(double earlyDeparture) {
			testForLocked() ;
			this.earlyDeparture = earlyDeparture;
		}

		@StringGetter( PERFORMING )
		public double getPerforming_utils_hr() {
			return performing;
		}

		@StringSetter( PERFORMING )
		public void setPerforming_utils_hr(double performing) {
			this.performing = performing;
		}

		@StringGetter( MARGINAL_UTL_OF_MONEY )
		public double getMarginalUtilityOfMoney() {
			return marginalUtilityOfMoney;
		}

		@StringSetter( MARGINAL_UTL_OF_MONEY )
		public void setMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
			testForLocked() ;
			this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		}

		@StringGetter( UTL_OF_LINE_SWITCH )
		public double getUtilityOfLineSwitch() {
			return utilityOfLineSwitch;
		}

		@StringSetter( UTL_OF_LINE_SWITCH )
		public void setUtilityOfLineSwitch(double utilityOfLineSwitch) {
			testForLocked() ;
			this.utilityOfLineSwitch = utilityOfLineSwitch;
		}

		private static int setWaitingCnt=0 ;

		@StringGetter( WAITING )
		public double getMarginalUtlOfWaiting_utils_hr() {
			return this.waiting;
		}

		@StringSetter( WAITING )
		public void setMarginalUtlOfWaiting_utils_hr(final double waiting) {
			testForLocked() ;
			if ( (waiting != 0.) && (setWaitingCnt<1) ) {
				setWaitingCnt++ ;
				log.warn("Setting betaWaiting different from zero is discouraged.  It is probably implemented correctly, " +
						"but there is as of now no indication that it makes the results more realistic." + Gbl.ONLYONCE );
			}
			this.waiting = waiting;
		}

		@StringGetter( "subpopulation" )
		public String getSubpopulation() {
			return subpopulation;
		}

		@StringSetter( "subpopulation" )
		public void setSubpopulation(String subpopulation) {
			//TODO: handle case of default subpopulation
			if ( this.subpopulation != null ) {
				throw new IllegalStateException( "cannot change subpopulation in a scoring parameter set, as it is used for indexing." );
			}

			this.subpopulation = subpopulation;
		}

		@StringGetter( WAITING_PT )
		public double getMarginalUtlOfWaitingPt_utils_hr() {
			return waitingPt != null ? waitingPt :
				this.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling();
		}

		@StringSetter( WAITING_PT )
		public void setMarginalUtlOfWaitingPt_utils_hr( final Double waitingPt ) {
			this.waitingPt = waitingPt;
		}

		/* parameter set handling */
		@Override
		public ConfigGroup createParameterSet(final String type) {
			switch ( type ) {
				case ActivityParams.SET_TYPE:
					return new ActivityParams();
				case ModeParams.SET_TYPE:
					return new ModeParams();
				default:
					throw new IllegalArgumentException( type );
			}
		}

		@Override
		protected void checkParameterSet( final ConfigGroup module ) {
			switch ( module.getName() ) {
				case ActivityParams.SET_TYPE:
					if ( !(module instanceof ActivityParams) ) {
						throw new RuntimeException( "wrong class for "+module );
					}
					final String t = ((ActivityParams) module).getActivityType();
					if ( getActivityParams( t  ) != null ) {
						throw new IllegalStateException( "already a parameter set for activity type "+t );
					}
					break;
				case ModeParams.SET_TYPE:
					if ( !(module instanceof ModeParams) ) {
						throw new RuntimeException( "wrong class for "+module );
					}
					final String m = ((ModeParams) module).getMode();
					if ( getModes().get(m) != null ) {
						throw new IllegalStateException( "already a parameter set for mode "+m );
					}
					break;
				default:
					throw new IllegalArgumentException( module.getName() );
			}
		}

		public Collection<String> getActivityTypes() {
			return this.getActivityParamsPerType().keySet();
		}

		public Collection<ActivityParams> getActivityParams() {
				@SuppressWarnings("unchecked")
				Collection<ActivityParams> collection = (Collection<ActivityParams>) getParameterSets( ActivityParams.SET_TYPE );
				for ( ActivityParams params : collection ) {
					if ( this.isLocked() ) {
						params.setLocked();
					}
				}
				return collection ;
		}

		public Map<String, ActivityParams> getActivityParamsPerType() {
			final Map<String, ActivityParams> map = new LinkedHashMap< >();

			for ( ActivityParams pars : getActivityParams() ) {
				map.put( pars.getActivityType() , pars );
			}

			return map;
		}

		public ActivityParams getActivityParams(final String actType) {
			return this.getActivityParamsPerType().get(actType);
		}

		public ActivityParams getOrCreateActivityParams(final String actType) {
			ActivityParams params = this.getActivityParamsPerType().get(actType);

			if ( params == null ) {
				params = new ActivityParams( actType );
				addActivityParams( params );
			}

			return  params;
		}

		public Map<String, ModeParams> getModes() {
			@SuppressWarnings("unchecked")
			final Collection<ModeParams> modes = (Collection<ModeParams>) getParameterSets( ModeParams.SET_TYPE );
			final Map<String, ModeParams> map = new LinkedHashMap< >();

			for ( ModeParams pars : modes ) {
				if ( this.isLocked() ) {
					pars.setLocked();
				}
				map.put( pars.getMode() , pars );
			}
			if ( this.isLocked() ) {
				return Collections.unmodifiableMap(map) ;
			} else {
				return map ;
			}
		}

		public ModeParams getOrCreateModeParams(String modeName) {
			ModeParams modeParams = getModes().get(modeName);
			if (modeParams == null) {
				modeParams = new ModeParams( modeName );
				addParameterSet(modeParams);
			}
			return modeParams;
		}

		public void addModeParams(final ModeParams params) {
			final ModeParams previous = this.getModes().get( params.getMode() );

			if ( previous != null ) {
				final boolean removed = removeParameterSet( previous );
				if ( !removed ) throw new RuntimeException( "problem replacing mode params " );
				log.info("mode parameters for mode " + previous.getMode() + " were just overwritten.") ;
			}

			super.addParameterSet( params );
		}

		public void addActivityParams(final PlanCalcScoreConfigGroup.ActivityParams params) {
			final PlanCalcScoreConfigGroup.ActivityParams previous = this.getActivityParams( params.getActivityType() );

			if ( previous != null ) {
				if ( previous.getActivityType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					log.error("ERROR: Activity parameters for activity type " + previous.getActivityType() + " were just overwritten. This happens most " +
							"likely because you defined them in the config file and the Controler overwrites them.  Or the other way " +
							"round.  pt interaction has problems, but doing what you are doing here will just cause " +
							"other (less visible) problem. Please take the effort to discuss with the core team " +
							"what needs to be done.  kai, nov'12") ;
				} else {
					log.info("activity parameters for activity type " + previous.getActivityType() + " were just overwritten.") ;
				}

				final boolean removed = removeParameterSet( previous );
				if ( !removed ) throw new RuntimeException( "problem replacing activity params " );
			}

			super.addParameterSet( params );
		}

		/** Checks whether all the settings make sense or if there are some problems with the parameters
		 * currently set. Currently, this checks that for at least one activity type opening AND closing
		 * times are defined. */
		@Override
		public void checkConsistency(Config config) {
			super.checkConsistency(config);
			boolean hasOpeningAndClosingTime = false;
			boolean hasOpeningTimeAndLatePenalty = false ;

			// This cannot be done in ActivityParams (where it would make more sense),
			// because some global properties are also checked
			for ( PlanCalcScoreConfigGroup.ActivityParams actType : this.getActivityParams() ) {
				if ( actType.isScoringThisActivityAtAll() ) {
					// (checking consistency only if activity is scored at all)

					if ((actType.getOpeningTime() != Time.UNDEFINED_TIME) && (actType.getClosingTime() != Time.UNDEFINED_TIME)) {
						hasOpeningAndClosingTime = true;
					}
					if ((actType.getOpeningTime() != Time.UNDEFINED_TIME) && (getLateArrival_utils_hr() < -0.001)) {
						hasOpeningTimeAndLatePenalty = true;
					}
					if ( actType.getOpeningTime()==0. && actType.getClosingTime()>24.*3600-1 ) {
						log.error("it looks like you have an activity type with opening time set to 0:00 and closing " +
								"time set to 24:00. This is most probably not the same as not setting them at all.  " +
								"In particular, activities which extend past midnight may not accumulate scores.") ;
					}
				}
			}
			if (!hasOpeningAndClosingTime && !hasOpeningTimeAndLatePenalty) {
				log.info("NO OPENING OR CLOSING TIMES DEFINED!\n\n\n"
						+"There is no activity type that has an opening *and* closing time (or opening time and late penalty) defined.\n"
						+"This usually means that the activity chains can be shifted by an arbitrary\n"
						+"number of hours without having an effect on the score of the plans, and thus\n"
						+"resulting in wrong results / traffic patterns.\n"
						+"If you are using MATSim without time adaptation, you can ignore this warning.\n\n\n");
			}
			if ( this.getMarginalUtlOfWaiting_utils_hr() != 0.0 ) {
				log.warn( "marginal utl of wait set to: " + this.getMarginalUtlOfWaiting_utils_hr() + ". Setting this different from zero is " +
						"discouraged. The parameter was also abused for pt routing; if you did that, consider setting the new " +
						"parameter waitingPt instead.");
			}
		}

	}
}
