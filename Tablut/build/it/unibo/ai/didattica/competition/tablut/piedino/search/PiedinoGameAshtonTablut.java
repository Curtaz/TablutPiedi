package it.unibo.ai.didattica.competition.tablut.piedino.search;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import it.unibo.ai.didattica.competition.tablut.piedino.search.heuristics.BlackHeuristics;
//import it.unibo.ai.didattica.competition.tablut.piedino.search.heuristics.BlackHeuristicsBrainmates;
import it.unibo.ai.didattica.competition.tablut.piedino.search.heuristics.Heuristics;
import it.unibo.ai.didattica.competition.tablut.piedino.search.heuristics.WhiteHeuristics;
//import it.unibo.ai.didattica.competition.tablut.piedino.search.heuristics.WhiteHeuristicsBrainmates;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.Game;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;
import it.unibo.ai.didattica.competition.tablut.exceptions.*;

/**
 * 
 * Game engine inspired by the Ashton Rules of Tablut
 * 
 * 
 * @author A. Piretti, Andrea Galassi
 * Edited by Tommaso Cortecchia 
 *
 */
public class PiedinoGameAshtonTablut implements Game, Cloneable, aima.core.search.adversarial.Game<State, Action, State.Turn>{

	/**
	 * Number of repeated states that can occur before a draw
	 */
	private int repeated_moves_allowed;

	/**
	 * Number of states kept in memory. negative value means infinite.
	 */
	private int cache_size;
	/**
	 * Counter for the moves without capturing that have occurred
	 */
	private int movesWithutCapturing;
	private String gameLogName;
	private File gameLog;
	private FileHandler fh;
	private Logger loggGame;
	private List<String> citadels;
	// private List<String> strangeCitadels;
	private List<State> drawConditions;

	public PiedinoGameAshtonTablut(int repeated_moves_allowed, int cache_size, String logs_folder, String whiteName,
			String blackName) {
		this(new StateTablut(), repeated_moves_allowed, cache_size, logs_folder, whiteName, blackName);
		
	}

	public PiedinoGameAshtonTablut(State state, int repeated_moves_allowed, int cache_size, String logs_folder,
			String whiteName, String blackName) {
		super();
		this.repeated_moves_allowed = repeated_moves_allowed;
		this.cache_size = cache_size;
		this.movesWithutCapturing = 0;

		Path p = Paths.get(logs_folder + File.separator + "_" + whiteName + "_vs_" + blackName + "_"
				+ new Date().getTime() + "_gameLog.txt");
		p = p.toAbsolutePath();
		this.gameLogName = p.toString();
		File gamefile = new File(this.gameLogName);
		try {
			File f = new File(logs_folder);
			f.mkdirs();
			if (!gamefile.exists()) {
				gamefile.createNewFile();
			}
			this.gameLog = gamefile;
			fh = null;
			fh = new FileHandler(gameLogName, true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		this.loggGame = Logger.getLogger("GameLog");
		loggGame.addHandler(this.fh);
		this.fh.setFormatter(new SimpleFormatter());
		loggGame.setLevel(Level.FINE);
		loggGame.fine("Players:\t" + whiteName + "\tvs\t" + blackName);
		loggGame.fine("Repeated moves allowed:\t" + repeated_moves_allowed + "\tCache:\t" + cache_size);
		loggGame.fine("Inizio partita");
		loggGame.fine("Stato:\n" + state.toString());
		drawConditions = new ArrayList<State>();
		
		this.citadels = new ArrayList<String>();
		// this.strangeCitadels = new ArrayList<String>();
		this.citadels.add("a4");
		this.citadels.add("a5");
		this.citadels.add("a6");
		this.citadels.add("b5");
		this.citadels.add("d1");
		this.citadels.add("e1");
		this.citadels.add("f1");
		this.citadels.add("e2");
		this.citadels.add("i4");
		this.citadels.add("i5");
		this.citadels.add("i6");
		this.citadels.add("h5");
		this.citadels.add("d9");
		this.citadels.add("e9");
		this.citadels.add("f9");
		this.citadels.add("e8");
		// this.strangeCitadels.add("e1");
		// this.strangeCitadels.add("a5");
		// this.strangeCitadels.add("i5");
		// this.strangeCitadels.add("e9");
	}

	@Override
	public State checkMove(State state, Action a)
			throws BoardException, ActionException, StopException, PawnException, DiagonalException, ClimbingException,
			ThroneException, OccupitedException, ClimbingCitadelException, CitadelException {
		
		checkPossibleMove(state.clone(),a);
		// se sono arrivato qui, muovo la pedina
		state = this.movePawn(state, a);

		// a questo punto controllo lo stato per eventuali catture
		if (state.getTurn().equalsTurn("W")) {
			state = this.checkCaptureBlack(state, a);
		} else if (state.getTurn().equalsTurn("B")) {
			state = this.checkCaptureWhite(state, a);
		}

		// if something has been captured, clear cache for draws
		if (this.movesWithutCapturing == 0) {
			this.drawConditions.clear();
			this.loggGame.fine("Capture! Draw cache cleared!");
		}
		// controllo pareggio
		int trovati = 0;
		for (State s : drawConditions) {

			//System.out.println(s.toString());

			if (s.equals(state)) {
				// DEBUG: //
				// System.out.println("UGUALI:");
				// System.out.println("STATO VECCHIO:\t" + s.toLinearString());
				// System.out.println("STATO NUOVO:\t" +
				// state.toLinearString());

				trovati++;
				if (trovati > repeated_moves_allowed) {
					state.setTurn(State.Turn.DRAW);
					this.loggGame.fine("Partita terminata in pareggio per numero di stati ripetuti");
					break;
				}
			} else {
				// DEBUG: //
				// System.out.println("DIVERSI:");
				// System.out.println("STATO VECCHIO:\t" + s.toLinearString());
				// System.out.println("STATO NUOVO:\t" +
				// state.toLinearString());
			}
		}
		if (trovati > 0) {
			this.loggGame.fine("Equal states found: " + trovati);
		}
		if (cache_size >= 0 && this.drawConditions.size() > cache_size) {
			this.drawConditions.remove(0);
		}
		this.drawConditions.add(state.clone());
		this.loggGame.fine("Current draw cache size: " + this.drawConditions.size());

		this.loggGame.fine("Stato:\n" + state.toString());
		//System.out.println("Stato:\n" + state.toString());

		return state;
	}

	private State checkCaptureWhite(State state, Action a) {
		// controllo se mangio a destra
		if (a.getColumnTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("B")
				&& (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("W")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("T")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("K")
						|| (this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))
								&& !(a.getColumnTo() + 2 == 8 && a.getRowTo() == 4)
								&& !(a.getColumnTo() + 2 == 4 && a.getRowTo() == 0)
								&& !(a.getColumnTo() + 2 == 4 && a.getRowTo() == 8)
								&& !(a.getColumnTo() + 2 == 0 && a.getRowTo() == 4)))) {
			state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
			this.movesWithutCapturing = -1;
			this.loggGame.fine("Pedina nera rimossa in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
		}
		// controllo se mangio a sinistra
		if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("B")
				&& (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("W")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("T")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("K")
						|| (this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))
								&& !(a.getColumnTo() - 2 == 8 && a.getRowTo() == 4)
								&& !(a.getColumnTo() - 2 == 4 && a.getRowTo() == 0)
								&& !(a.getColumnTo() - 2 == 4 && a.getRowTo() == 8)
								&& !(a.getColumnTo() - 2 == 0 && a.getRowTo() == 4)))) {
			state.removePawn(a.getRowTo(), a.getColumnTo() - 1);
			this.movesWithutCapturing = -1;
			this.loggGame.fine("Pedina nera rimossa in: " + state.getBox(a.getRowTo(), a.getColumnTo() - 1));
		}
		// controllo se mangio sopra
		if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("B")
				&& (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("W")
						|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("T")
						|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("K")
						|| (this.citadels.contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))
								&& !(a.getColumnTo() == 8 && a.getRowTo() - 2 == 4)
								&& !(a.getColumnTo() == 4 && a.getRowTo() - 2 == 0)
								&& !(a.getColumnTo() == 4 && a.getRowTo() - 2 == 8)
								&& !(a.getColumnTo() == 0 && a.getRowTo() - 2 == 4)))) {
			state.removePawn(a.getRowTo() - 1, a.getColumnTo());
			this.movesWithutCapturing = -1;
			this.loggGame.fine("Pedina nera rimossa in: " + state.getBox(a.getRowTo() - 1, a.getColumnTo()));
		}
		// controllo se mangio sotto
		if (a.getRowTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("B")
				&& (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("W")
						|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("T")
						|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("K")
						|| (this.citadels.contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))
								&& !(a.getColumnTo() == 8 && a.getRowTo() + 2 == 4)
								&& !(a.getColumnTo() == 4 && a.getRowTo() + 2 == 0)
								&& !(a.getColumnTo() == 4 && a.getRowTo() + 2 == 8)
								&& !(a.getColumnTo() == 0 && a.getRowTo() + 2 == 4)))) {
			state.removePawn(a.getRowTo() + 1, a.getColumnTo());
			this.movesWithutCapturing = -1;
			this.loggGame.fine("Pedina nera rimossa in: " + state.getBox(a.getRowTo() + 1, a.getColumnTo()));
		}
		// controllo se ho vinto
		if (a.getRowTo() == 0 || a.getRowTo() == state.getBoard().length - 1 || a.getColumnTo() == 0
				|| a.getColumnTo() == state.getBoard().length - 1) {
			if (state.getPawn(a.getRowTo(), a.getColumnTo()).equalsPawn("K")) {
				state.setTurn(State.Turn.WHITEWIN);
				this.loggGame.fine("Bianco vince con re in " + a.getTo());
			}
		}
		// TODO: implement the winning condition of the capture of the last
		// black checker

		this.movesWithutCapturing++;
		return state;
	}

	private State checkCaptureBlackKingLeft(State state, Action a) {
		// ho il re sulla sinistra
		if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("K")) {
			// re sul trono
			if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e5")) {
				if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 3).equalsPawn("B")
						&& state.getPawn(5, 4).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() - 1));
				}
			}
			// re adiacente al trono
			if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e4")) {
				if (state.getPawn(2, 4).equalsPawn("B") && state.getPawn(3, 3).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() - 1));
				}
			}
			if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("f5")) {
				if (state.getPawn(5, 5).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() - 1));
				}
			}
			if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e6")) {
				if (state.getPawn(6, 4).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() - 1));
				}
			}
			// sono fuori dalle zone del trono
			if (!state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e5")
					&& !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e6")
					&& !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e4")
					&& !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("f5")) {
				if (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("B")
						|| this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() - 1));
				}
			}
		}
		return state;
	}

	private State checkCaptureBlackKingRight(State state, Action a) {
		// ho il re sulla destra
		if (a.getColumnTo() < state.getBoard().length - 2
				&& (state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("K"))) {
			// re sul trono
			if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e5")) {
				if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
						&& state.getPawn(5, 4).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
				}
			}
			// re adiacente al trono
			if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e4")) {
				if (state.getPawn(2, 4).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
				}
			}
			if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e6")) {
				if (state.getPawn(5, 5).equalsPawn("B") && state.getPawn(6, 4).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
				}
			}
			if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("d5")) {
				if (state.getPawn(3, 3).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
				}
			}
			// sono fuori dalle zone del trono
			if (!state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("d5")
					&& !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e6")
					&& !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e4")
					&& !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e5")) {
				if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("B")
						|| this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
				}
			}
		}
		return state;
	}

	private State checkCaptureBlackKingDown(State state, Action a) {
		// ho il re sotto
		if (a.getRowTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("K")) {
			//System.out.println("Ho il re sotto");
			// re sul trono
			if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e5")) {
				if (state.getPawn(5, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
						&& state.getPawn(4, 3).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() + 1, a.getColumnTo()));
				}
			}
			// re adiacente al trono
			if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e4")) {
				if (state.getPawn(3, 3).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() + 1, a.getColumnTo()));
				}
			}
			if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("d5")) {
				if (state.getPawn(4, 2).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() + 1, a.getColumnTo()));
				}
			}
			if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("f5")) {
				if (state.getPawn(4, 6).equalsPawn("B") && state.getPawn(5, 5).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() + 1, a.getColumnTo()));
				}
			}
			// sono fuori dalle zone del trono
			if (!state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("d5")
					&& !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e4")
					&& !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("f5")
					&& !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e5")) {
				if (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("B")
						|| this.citadels.contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() + 1, a.getColumnTo()));
				}
			}
		}
		return state;
	}

	private State checkCaptureBlackKingUp(State state, Action a) {
		// ho il re sopra
		if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("K")) {
			// re sul trono
			if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e5")) {
				if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
						&& state.getPawn(4, 3).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() - 1, a.getColumnTo()));
				}
			}
			// re adiacente al trono
			if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e6")) {
				if (state.getPawn(5, 3).equalsPawn("B") && state.getPawn(5, 5).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() - 1, a.getColumnTo()));
				}
			}
			if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("d5")) {
				if (state.getPawn(4, 2).equalsPawn("B") && state.getPawn(3, 3).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() - 1, a.getColumnTo()));
				}
			}
			if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("f5")) {
				if (state.getPawn(4, 6).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() - 1, a.getColumnTo()));
				}
			}
			// sono fuori dalle zone del trono
			if (!state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("d5")
					&& !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e4")
					&& !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("f5")
					&& !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e5")) {
				if (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("B")
						|| this.citadels.contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))) {
					state.setTurn(State.Turn.BLACKWIN);
					this.loggGame
							.fine("Nero vince con re catturato in: " + state.getBox(a.getRowTo() - 1, a.getColumnTo()));
				}
			}
		}
		return state;
	}

	private State checkCaptureBlackPawnRight(State state, Action a) {
		// mangio a destra
		if (a.getColumnTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("W")) {
			if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("B")) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				this.movesWithutCapturing = -1;
				this.loggGame.fine("Pedina bianca rimossa in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
			}
			if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("T")) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				this.movesWithutCapturing = -1;
				this.loggGame.fine("Pedina bianca rimossa in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
			}
			if (this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				this.movesWithutCapturing = -1;
				this.loggGame.fine("Pedina bianca rimossa in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
			}
			if (state.getBox(a.getRowTo(), a.getColumnTo() + 2).equals("e5")) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				this.movesWithutCapturing = -1;
				this.loggGame.fine("Pedina bianca rimossa in: " + state.getBox(a.getRowTo(), a.getColumnTo() + 1));
			}

		}

		return state;
	}

	private State checkCaptureBlackPawnLeft(State state, Action a) {
		// mangio a sinistra
		if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("W")
				&& (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("B")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("T")
						|| this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))
						|| (state.getBox(a.getRowTo(), a.getColumnTo() - 2).equals("e5")))) {
			state.removePawn(a.getRowTo(), a.getColumnTo() - 1);
			this.movesWithutCapturing = -1;
			this.loggGame.fine("Pedina bianca rimossa in: " + state.getBox(a.getRowTo(), a.getColumnTo() - 1));
		}
		return state;
	}

	private State checkCaptureBlackPawnUp(State state, Action a) {
		// controllo se mangio sopra
		if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("W")
				&& (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("B")
						|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("T")
						|| this.citadels.contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))
						|| (state.getBox(a.getRowTo() - 2, a.getColumnTo()).equals("e5")))) {
			state.removePawn(a.getRowTo() - 1, a.getColumnTo());
			this.movesWithutCapturing = -1;
			this.loggGame.fine("Pedina bianca rimossa in: " + state.getBox(a.getRowTo() - 1, a.getColumnTo()));
		}
		return state;
	}

	private State checkCaptureBlackPawnDown(State state, Action a) {
		// controllo se mangio sotto
		if (a.getRowTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("W")
				&& (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("B")
						|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("T")
						|| this.citadels.contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))
						|| (state.getBox(a.getRowTo() + 2, a.getColumnTo()).equals("e5")))) {
			state.removePawn(a.getRowTo() + 1, a.getColumnTo());
			this.movesWithutCapturing = -1;
			this.loggGame.fine("Pedina bianca rimossa in: " + state.getBox(a.getRowTo() + 1, a.getColumnTo()));
		}
		return state;
	}

	private State checkCaptureBlack(State state, Action a) {

		this.checkCaptureBlackPawnRight(state, a);
		this.checkCaptureBlackPawnLeft(state, a);
		this.checkCaptureBlackPawnUp(state, a);
		this.checkCaptureBlackPawnDown(state, a);
		this.checkCaptureBlackKingRight(state, a);
		this.checkCaptureBlackKingLeft(state, a);
		this.checkCaptureBlackKingDown(state, a);
		this.checkCaptureBlackKingUp(state, a);

		this.movesWithutCapturing++;
		return state;
	}

	private State movePawn(State state, Action a) {
		State.Pawn pawn = state.getPawn(a.getRowFrom(), a.getColumnFrom());
		State.Pawn[][] newBoard = state.getBoard();
		// State newState = new State();
		this.loggGame.fine("Movimento pedina");
		// libero il trono o una casella qualunque
		if (a.getColumnFrom() == 4 && a.getRowFrom() == 4) {
			newBoard[a.getRowFrom()][a.getColumnFrom()] = State.Pawn.THRONE;
		} else {
			newBoard[a.getRowFrom()][a.getColumnFrom()] = State.Pawn.EMPTY;
		}

		// metto nel nuovo tabellone la pedina mossa
		newBoard[a.getRowTo()][a.getColumnTo()] = pawn;
		// aggiorno il tabellone
		state.setBoard(newBoard);
		// cambio il turno
		if (state.getTurn().equalsTurn(State.Turn.WHITE.toString())) {
			state.setTurn(State.Turn.BLACK);
		} else {
			state.setTurn(State.Turn.WHITE);
		}

		return state;
	}

	@SuppressWarnings("unused")
	private void setMovesWithutCapturing(int movesWithutCapturing) {
		this.movesWithutCapturing = movesWithutCapturing;
	}

	private List<Action> sortActionList (List<Action> actions) {
		
//		if (actions.size()>0 && actions.get(0).getTurn().equals(Turn.WHITE))
//			Collections.reverse(actions);
		return actions;
	}

	private void checkPossibleMove(State state, Action a)
			throws BoardException, ActionException, StopException, PawnException, DiagonalException, ClimbingException,
			ThroneException, OccupitedException, ClimbingCitadelException, CitadelException {
		this.loggGame.fine(a.toString());
		// controllo la mossa
		if (a.getTo().length() != 2 || a.getFrom().length() != 2) {
			this.loggGame.warning("Formato mossa errato");
			throw new ActionException(a);
		}
		int columnFrom = a.getColumnFrom();
		int columnTo = a.getColumnTo();
		int rowFrom = a.getRowFrom();
		int rowTo = a.getRowTo();
	
		// controllo se sono fuori dal tabellone
		if (columnFrom > state.getBoard().length - 1 || rowFrom > state.getBoard().length - 1
				|| rowTo > state.getBoard().length - 1 || columnTo > state.getBoard().length - 1 || columnFrom < 0
				|| rowFrom < 0 || rowTo < 0 || columnTo < 0) {
			this.loggGame.warning("Mossa fuori tabellone");
			throw new BoardException(a);
		}
	
		// controllo che non vada sul trono
		if (state.getPawn(rowTo, columnTo).equalsPawn(State.Pawn.THRONE.toString())) {
			this.loggGame.warning("Mossa sul trono");
			throw new ThroneException(a);
		}
	
		// controllo la casella di arrivo
		if (!state.getPawn(rowTo, columnTo).equalsPawn(State.Pawn.EMPTY.toString())) {
			this.loggGame.warning("Mossa sopra una casella occupata");
			throw new OccupitedException(a);
		}
		if (this.citadels.contains(state.getBox(rowTo, columnTo))
				&& !this.citadels.contains(state.getBox(rowFrom, columnFrom))) {
			this.loggGame.warning("Mossa che arriva sopra una citadel");
			throw new CitadelException(a);
		}
		if (this.citadels.contains(state.getBox(rowTo, columnTo))
				&& this.citadels.contains(state.getBox(rowFrom, columnFrom))) {
			if (rowFrom == rowTo) {
				if (columnFrom - columnTo > 5 || columnFrom - columnTo < -5) {
					this.loggGame.warning("Mossa che arriva sopra una citadel");
					throw new CitadelException(a);
				}
			} else {
				if (rowFrom - rowTo > 5 || rowFrom - rowTo < -5) {
					this.loggGame.warning("Mossa che arriva sopra una citadel");
					throw new CitadelException(a);
				}
			}
	
		}
	
		// controllo se cerco di stare fermo
		if (rowFrom == rowTo && columnFrom == columnTo) {
			this.loggGame.warning("Nessuna mossa");
			throw new StopException(a);
		}
	
		// controllo se sto muovendo una pedina giusta
		if (state.getTurn().equalsTurn(State.Turn.WHITE.toString())) {
			if (!state.getPawn(rowFrom, columnFrom).equalsPawn("W")
					&& !state.getPawn(rowFrom, columnFrom).equalsPawn("K")) {
				this.loggGame.warning("Giocatore " + a.getTurn() + " cerca di muovere una pedina avversaria");
				throw new PawnException(a);
			}
		}
		if (state.getTurn().equalsTurn(State.Turn.BLACK.toString())) {
			if (!state.getPawn(rowFrom, columnFrom).equalsPawn("B")) {
				this.loggGame.warning("Giocatore " + a.getTurn() + " cerca di muovere una pedina avversaria");
				throw new PawnException(a);
			}
		}
	
		// controllo di non muovere in diagonale
		if (rowFrom != rowTo && columnFrom != columnTo) {
			this.loggGame.warning("Mossa in diagonale");
			throw new DiagonalException(a);
		}
	
		// controllo di non scavalcare pedine
		if (rowFrom == rowTo) {
			if (columnFrom > columnTo) {
				for (int i = columnTo; i < columnFrom; i++) {
					if (!state.getPawn(rowFrom, i).equalsPawn(State.Pawn.EMPTY.toString())) {
						if (state.getPawn(rowFrom, i).equalsPawn(State.Pawn.THRONE.toString())) {
							this.loggGame.warning("Mossa che scavalca il trono");
							throw new ClimbingException(a);
						} else {
							this.loggGame.warning("Mossa che scavalca una pedina");
							throw new ClimbingException(a);
						}
					}
					if (this.citadels.contains(state.getBox(rowFrom, i))
							&& !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
						this.loggGame.warning("Mossa che scavalca una citadel");
						throw new ClimbingCitadelException(a);
					}
				}
			} else {
				for (int i = columnFrom + 1; i <= columnTo; i++) {
					if (!state.getPawn(rowFrom, i).equalsPawn(State.Pawn.EMPTY.toString())) {
						if (state.getPawn(rowFrom, i).equalsPawn(State.Pawn.THRONE.toString())) {
							this.loggGame.warning("Mossa che scavalca il trono");
							throw new ClimbingException(a);
						} else {
							this.loggGame.warning("Mossa che scavalca una pedina");
							throw new ClimbingException(a);
						}
					}
					if (this.citadels.contains(state.getBox(rowFrom, i))
							&& !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
						this.loggGame.warning("Mossa che scavalca una citadel");
						throw new ClimbingCitadelException(a);
					}
				}
			}
		} else {
			if (rowFrom > rowTo) {
				for (int i = rowTo; i < rowFrom; i++) {
					if (!state.getPawn(i, columnFrom).equalsPawn(State.Pawn.EMPTY.toString())) {
						if (state.getPawn(i, columnFrom).equalsPawn(State.Pawn.THRONE.toString())) {
							this.loggGame.warning("Mossa che scavalca il trono");
							throw new ClimbingException(a);
						} else {
							this.loggGame.warning("Mossa che scavalca una pedina");
							throw new ClimbingException(a);
						}
					}
					if (this.citadels.contains(state.getBox(i, columnFrom))
							&& !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
						this.loggGame.warning("Mossa che scavalca una citadel");
						throw new ClimbingCitadelException(a);
					}
				}
			} else {
				for (int i = rowFrom + 1; i <= rowTo; i++) {
					if (!state.getPawn(i, columnFrom).equalsPawn(State.Pawn.EMPTY.toString())) {
						if (state.getPawn(i, columnFrom).equalsPawn(State.Pawn.THRONE.toString())) {
							this.loggGame.warning("Mossa che scavalca il trono");
							throw new ClimbingException(a);
						} else {
							this.loggGame.warning("Mossa che scavalca una pedina");
							throw new ClimbingException(a);
						}
					}
					if (this.citadels.contains(state.getBox(i, columnFrom))
							&& !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
						this.loggGame.warning("Mossa che scavalca una citadel");
						throw new ClimbingCitadelException(a);
					}
				}
			}
		}
	}

	public File getGameLog() {
		return gameLog;
	}

	public int getMovesWithutCapturing() {
		return movesWithutCapturing;
	}

	public int getRepeated_moves_allowed() {
		return repeated_moves_allowed;
	}

	public int getCache_size() {
		return cache_size;
	}

	public List<State> getDrawConditions() {
		return drawConditions;
	}

	public void clearDrawConditions() {
		drawConditions.clear();
	}
	

	@Override
	public void endGame(State state) {
		this.loggGame.fine("Stato:\n"+state.toString());
	}

	@Override
	public State getInitialState() {
		return null;
	}

	@Override
	public State.Turn getPlayer(State state) {
		return state.getTurn();
	}

	@Override
	public State.Turn[] getPlayers() {
		State.Turn []retval={State.Turn.BLACK,State.Turn.WHITE};
		return retval;
	}

	/*
	 * FINISH SORT LAMBDA
	 */
	@Override
	public List<Action> getActions(State state) {
		State.Turn turn = state.getTurn();

		int rowIncr[] = {1, 0, -1, 0}; //rowIncr from aima library 
		int colIncr[] = {0, 1, 0, -1};


		List<Action> possibleActions = new ArrayList<Action>();

		for (int i = 0; i < state.getBoard().length; i++) { //rows of the board
			for (int j = 0; j < state.getBoard().length; j++) { //col of the board

				// if pawn color  is equal of turn color
				if (state.getPawn(i, j).toString().equals(turn.toString()) ||
						(state.getPawn(i, j).equals(State.Pawn.KING) && turn.equals(State.Turn.WHITE)) ) {

					for(int k=0; k < 4 ;k++) { //4 length of rowIncr, colIncr
						int rIncr = rowIncr[k];
						int cIncr = colIncr[k];
						
						int rBound = rIncr > 0 ? state.getBoard().length : -1;
						int cBound = cIncr > 0 ? state.getBoard().length : -1;

						int row = i + rIncr;
						int col = j + cIncr;
						
						boolean uscito=!citadels.contains(state.getBox(i, j)); // true if the pawn is not on a citadel 
						// search on top of pawn
						while(row != rBound && col != cBound) {
						
							if (!citadels.contains(state.getBox(row,col))) uscito=true;
							// break if pawn is out of citadels and it is moving on a citadel
							else if (uscito) break;
							
							if (!citadels.contains(state.getBox(i, j)) && citadels.contains(state.getBox(row, col))) {
								break;
							}

							// check if we are moving on a empty cell
							else if (state.getPawn(row, col).equalsPawn(State.Pawn.EMPTY.toString())) {

								String from = state.getBox(i, j);
								String to = state.getBox(row, col);

								Action action = null;
								try {
									action = new Action(from, to, turn);
								} catch (IOException e) {
									e.printStackTrace();
								}

								// check if action is admissible and if it is, add it to list possibleActions
								try {
									this.checkPossibleMove(state.clone(), action);
									possibleActions.add(action);
								} catch (Exception e) {
									//Do nothing
								}
							} else {
								// there is a pawn in the same cell and it cannot be crossed
								break;
							}
							
							row += rIncr;
							col += cIncr;
						}
					}

				}
			}
		}
		return sortActionList(possibleActions);
	}


	@Override
	public boolean isTerminal(State state) {
		Turn turn=state.getTurn();
		return turn.equals(Turn.BLACKWIN)||turn.equals(Turn.WHITEWIN)||turn.equals(Turn.DRAW);
	}

	@Override
	public State getResult(State state, Action action) {
	
//		try{
//			return checkMove(state.clone(),action);
//		}
//		catch(Exception e) {
//			e.printStackTrace();
//			return null;
//		}

		state = this.movePawn(state.clone(), action);

		if (state.getTurn().equalsTurn("W")) {
			state = this.checkCaptureBlack(state, action);
		} else if (state.getTurn().equalsTurn("B")) {
			state = this.checkCaptureWhite(state, action);
		}
		return state;
	}

	@Override
	public double getUtility(State state, Turn turn) {
		// if it is a terminal state
		if ((turn.equals(State.Turn.BLACK) && state.getTurn().equals(State.Turn.BLACKWIN))
				|| (turn.equals(State.Turn.WHITE) && state.getTurn().equals(State.Turn.WHITEWIN)))
			return Double.POSITIVE_INFINITY;
		else if ((turn.equals(State.Turn.BLACK) && state.getTurn().equals(State.Turn.WHITEWIN))
				|| (turn.equals(State.Turn.WHITE) && state.getTurn().equals(State.Turn.BLACKWIN)))
			return Double.NEGATIVE_INFINITY;

		//ONLY FOR DEBUG PURPOSES
		boolean brainmates=false;
		if(brainmates) {
			if (turn.equals(State.Turn.WHITE)) {
				Heuristics heu = new WhiteHeuristics();
//				return heu.evaluateState(state);
				   //WhiteHeuristicsBrainmates heu=new WhiteHeuristicsBrainmates(state);
				return heu.evaluateState(state);
			} else {
				Heuristics heu = new BlackHeuristics();
				return heu.evaluateState(state);
//				BlackHeuristicsBrainmates heu=new BlackHeuristicsBrainmates(state);
//				return heu.evaluateState();
			}
		}
		
		// if it isn't a terminal state
		Heuristics heuristics = null;
		if (turn.equals(State.Turn.WHITE)) {
			heuristics = new WhiteHeuristics();
		} else {
			heuristics = new BlackHeuristics();
		}
		return  heuristics.evaluateState(state);		
	}
}




