package it.unibo.ai.didattica.competition.tablut.piedino.search;

import it.unibo.ai.didattica.competition.tablut.domain.Action;

import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import aima.core.search.adversarial.IterativeDeepeningAlphaBetaSearch;


public class Player  extends IterativeDeepeningAlphaBetaSearch<State,Action,Turn>{
	
	private boolean debug;
	public Player(aima.core.search.adversarial.Game<State, Action, Turn> game, double utilMin, double utilMax,
			int time,boolean debug) {
		super(game, utilMin, utilMax, time);
		this.debug=debug;
		this.setLogEnabled(debug);
	}
	
	public Player(aima.core.search.adversarial.Game<State, Action, Turn> game, double utilMin, double utilMax,
			int time) {
		this(game, utilMin, utilMax, time,false);
	}
	
	/**
     * Method that estimates the value for (not necessarily
     * terminal) states. This implementation returns the utility value for
     * terminal states and heuristic value for non-terminal
     * states.
     */
    @Override
    protected double eval(State state, Turn player) {
        // needed to make heuristicEvaluationUsed = true, if the state evaluated isn't terminal
        super.eval(state, player);
        // return heuristic value for given state
        //State.Turn turn=state.getTurn();
		//System.out.println(turn.toString()+" - "+(turn.equals(Turn.BLACKWIN)||turn.equals(Turn.WHITEWIN)||turn.equals(Turn.DRAW))+" - "+game.getUtility(state, player));;
        return game.getUtility(state, player);
    } 
    
    @Override
 	public Action makeDecision(State state) {
 		Action a= super.makeDecision(state);
 		if (debug)
 	        System.out.println("Explored a total of " + getMetrics().get(METRICS_NODES_EXPANDED) + " nodes, reaching a depth limit of " + getMetrics().get(METRICS_MAX_DEPTH));
 		return a;
 	}	
	
}
