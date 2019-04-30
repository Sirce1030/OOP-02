package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {
	/*fields to hold a part of the game state*/
	List<Boolean> rounds;

	Graph<Integer, Transport> graph;

	List<ScotlandYardPlayer> PlayerList;

	int indexOfCurrentPlayer = 0;

	int currentRound = NOT_STARTED;

	Set<Colour> winningPlayers = new HashSet<>();

	int mrXLastRevealLocation = 0;

	private Set<Move> validMove;

//	ScotlandYardPlayer currentPlayer;

	ArrayList<Spectator> spectators = new ArrayList<>();





	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
							 PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							 PlayerConfiguration... restOfTheDetectives) {
		//null check for round and graph


//return a bool for empty rounds/maps with isEmpty() method
		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}

		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty gragh");
		}
//test whether the rounds and the graph are null
		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);
//check if mrX is not black
		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}
//we put mrX, firstDetective, and restOfTheDetectives into a temporary List so we can perform checks compactly in a loop
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		configurations.add(0, mrX);
		configurations.add(1, firstDetective);
//loop through the rest of the detectives
		for (PlayerConfiguration configuration : restOfTheDetectives) {
			configurations.add(requireNonNull(configuration));
		}

//check if the two players have the same location
		Set<Integer> set1 = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (set1.contains(configuration.location)) {
				throw new IllegalArgumentException("Duplicate location");
			}
			set1.add(configuration.location);
		}
//check if the two players have the same color
		Set<Colour> set2 = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (set2.contains(configuration.colour)) {
				throw new IllegalArgumentException("Duplicate colour");
			}
			set2.add(configuration.colour);
		}

//make a set that contains every type of ticket
		Set<Ticket> set3 = new HashSet<>();
		set3.add(BUS);
		set3.add(TAXI);
		set3.add(UNDERGROUND);
		set3.add(SECRET);
		set3.add(DOUBLE);
//compare the list with the player tickets in the hashmap
		for (PlayerConfiguration configuration : configurations) {
			if (!set3.equals(configuration.tickets.keySet())) {
				throw new IllegalArgumentException("missing tickets for players");
			}
		}
//check if the detectives have the secret and double ticket
		if (firstDetective.tickets.get(Ticket.SECRET) > 0 || firstDetective.tickets.get(Ticket.DOUBLE) > 0) {
			throw new IllegalArgumentException("Detectives have secret and double tickets");
		}
		for (PlayerConfiguration Detective : restOfTheDetectives) {
			if (Detective.tickets.get(Ticket.SECRET) > 0 || Detective.tickets.get(Ticket.DOUBLE) > 0) {
				throw new IllegalArgumentException("Detectives have secret and double tickets");
			}
		}
//create a mutable instance of players
		PlayerList = new ArrayList<>();
		PlayerList.add(0, new ScotlandYardPlayer(mrX.player, mrX.colour, mrX.location, mrX.tickets));
		PlayerList.add(1, new ScotlandYardPlayer(firstDetective.player, firstDetective.colour, firstDetective.location, firstDetective.tickets));
		for (PlayerConfiguration Detective : restOfTheDetectives) {
			PlayerList.add(new ScotlandYardPlayer(Detective.player, Detective.colour, Detective.location, Detective.tickets));
		}

	}

	@Override
	public void registerSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (spectators.contains(spectator)) {
			throw new IllegalArgumentException("Spectator already registered");
		} else {
			spectators.add(spectator);
		}
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (!(spectators.contains(spectator))) {
			throw new IllegalArgumentException("Spectator wasn't registered");
		} else {
			spectators.remove(spectator);
		}
	}

//a method that can get valid moves
    public Set<TicketMove> GetValidMove(ScotlandYardPlayer player, Graph<Integer, Transport>graph, int location) {
		Set<TicketMove> ValidTicketMoves = new HashSet<>();

		Node<Integer> currentNode = graph.getNode(location);

		Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(currentNode);


		//checking whether the location is occupied by the other players
		for (Edge<Integer, Transport> edge : edges) {
			boolean Occupied = false;
			for (ScotlandYardPlayer p : PlayerList) {
				if (p.isDetective() && p.location() == edge.destination().value() && p!=player) {
					Occupied = true;
					break;
				}
			}
			if (Occupied) {
				continue;
			}

			Ticket ticket = fromTransport(edge.data());
			if (player.hasTickets(ticket)) {
				ValidTicketMoves.add(new TicketMove(player.colour(), ticket, edge.destination().value()));
			}
			if (player.hasTickets(SECRET)) {
				ValidTicketMoves.add(new TicketMove(player.colour(), SECRET, edge.destination().value()));
			}
		}
		if (ValidTicketMoves.isEmpty()) {
			return emptySet();
		}
		return ValidTicketMoves;
	}

//a set of moves that are valid
	private Set<Move> validMove(ScotlandYardPlayer player) {
		Set<Move> temp = new HashSet<>();

		Set<TicketMove> ValidTicketMove = GetValidMove(player, graph, player.location());
		Graph<Integer, Transport> graph = getGraph();
		temp.addAll(ValidTicketMove);
		if (player.isMrX() && PlayerList.get(0).hasTickets(DOUBLE) && getCurrentRound() < (getRounds().size())-1) {
			for (TicketMove firstMove : ValidTicketMove) {
				for (TicketMove secondMove : GetValidMove(player, graph, firstMove.destination())) {
					if (firstMove.ticket() == secondMove.ticket() && !player.hasTickets(firstMove.ticket(), 2)) {
						continue;
					}
					else {
						temp.add(new DoubleMove(player.colour(), firstMove, secondMove));
					}
				}
			}
		}
		if (player.isDetective() && temp.isEmpty()) {
			temp.add(new PassMove(player.colour()));
		}
		if (temp.isEmpty()) {
			return emptySet();
		}
		return temp;
	}

	@Override
	public void startRotate() {
		validMove = validMove(GetCurrentScotlandYardPlayer(getCurrentPlayer()));
		if (!isGameOver()) {
//			Colour currentColour = getCurrentPlayer();
//			ScotlandYardPlayer currentPlayer = GetCurrentScotlandYardPlayer(currentColour);
			PlayerList.get(indexOfCurrentPlayer).player().makeMove(this, PlayerList.get(indexOfCurrentPlayer).location(), validMove, this);
		}
		else {
			throw new IllegalStateException("Cannot start rotate if game is already over");
		}
	}

	private void spectatorDoubleMoveUpdating(Move move) {
		for (Spectator s : getSpectators()) {
			s.onMoveMade(this, move);
		}
	}

	private void spectatorMrXMoveUpdating(Move move, int x) {
		for (Spectator s : getSpectators()) {
			s.onRoundStarted(this, x);
		}
		for (Spectator s : getSpectators()) {
			s.onMoveMade(this, move);
		}
	}

	private void spectatorDetectiveUpdating(Move move) {
		for (Spectator s : getSpectators()) {
			s.onMoveMade(this, move);
		}
	}

	private void spectatorOnGameOver() {
		for(Spectator s : getSpectators()) {
			s.onGameOver(this, getWinningPlayers());
		}
	}

	//a getter method for getting the player() from its colour
	public ScotlandYardPlayer GetCurrentScotlandYardPlayer(Colour colour) {
		int i = 0;
		while (i < PlayerList.size()) {
			if (!(PlayerList.get(i).colour().equals(colour))) {
				++i;
			}
			else break;
		}
		return PlayerList.get(i);
	}

	@Override
	public void accept(Move move) {

		if (!validMove.contains(requireNonNull(move))) {
			throw new IllegalArgumentException("valid moves contains moves not valid");
		}
		int prePlayer = indexOfCurrentPlayer;
		if (indexOfCurrentPlayer < PlayerList.size()) {
			++indexOfCurrentPlayer;
		}
		if (indexOfCurrentPlayer == PlayerList.size()) {
			indexOfCurrentPlayer = 0;
		}

//		Colour currentColour = getCurrentPlayer();
//		ScotlandYardPlayer currentPlayer = GetCurrentScotlandYardPlayer(currentColour);

		move.visit(new MoveVisitor() {
			@Override
			public void visit(PassMove move) {
				if (prePlayer == 0) {
					currentRound++;
					spectatorMrXMoveUpdating(move, currentRound);
				}
				if (prePlayer != 0) {
					spectatorDetectiveUpdating(move);
				}
			}

			@Override
			public void visit(TicketMove move) {
				ScotlandYardPlayer player = PlayerList.get(prePlayer);
				player.location(move.destination());
				if (player.isMrX()) {

					player.removeTicket(move.ticket());
					//if it's true,then it's the certain round that mrx need to show its location
					if (getRounds().get(getCurrentRound())) {
						currentRound++;
						mrXLastRevealLocation = move.destination();
						spectatorMrXMoveUpdating(move, currentRound);
					}
					//if not, mrx will remain invisible
					else {
						currentRound++;
						TicketMove masked = new TicketMove(move.colour(), move.ticket(), mrXLastRevealLocation);
						spectatorMrXMoveUpdating(masked, currentRound);
					}
				}
				if (player.isDetective()) {
					player.removeTicket(move.ticket());
					PlayerList.get(0).addTicket(move.ticket());
					spectatorDetectiveUpdating(move);
				}
			}

			@Override
			public void visit(DoubleMove move) {

					if (getRounds().get(getCurrentRound()) && getRounds().get(getCurrentRound() + 1)) {                            //reveal reveal
						PlayerList.get(0).removeTicket(DOUBLE);
						DoubleMove doublemove = new DoubleMove(move.colour(), move.firstMove().ticket(), move.firstMove().destination(), move.secondMove().ticket(), move.secondMove().destination());
						spectatorDoubleMoveUpdating(doublemove);
					}
					if (getRounds().get(getCurrentRound()) && !getRounds().get(getCurrentRound() + 1)) {                        //reveal hidden
						PlayerList.get(0).removeTicket(DOUBLE);
						DoubleMove doublemove = new DoubleMove(move.colour(), move.firstMove().ticket(), move.firstMove().destination(), move.secondMove().ticket(), move.firstMove().destination());
						spectatorDoubleMoveUpdating(doublemove);
					}
					if (!getRounds().get(getCurrentRound()) && getRounds().get(getCurrentRound() + 1)) {                            //hidden reveal
						PlayerList.get(0).removeTicket(DOUBLE);
						DoubleMove doublemove = new DoubleMove(move.colour(), move.firstMove().ticket(), mrXLastRevealLocation, move.secondMove().ticket(), move.secondMove().destination());
						spectatorDoubleMoveUpdating(doublemove);
					}
					if (!getRounds().get(getCurrentRound()) && !getRounds().get(getCurrentRound() + 1)) {                            //hidden hidden
						PlayerList.get(0).removeTicket(DOUBLE);
						DoubleMove doublemove = new DoubleMove(move.colour(), move.firstMove().ticket(), mrXLastRevealLocation, move.secondMove().ticket(), mrXLastRevealLocation);
						spectatorDoubleMoveUpdating(doublemove);
					}


				visit(move.firstMove());
				visit(move.secondMove());
			}
		});

		//update the validMove for the next player
		validMove = validMove(GetCurrentScotlandYardPlayer(getCurrentPlayer()));

		if (indexOfCurrentPlayer == 0) {
			if (isGameOver()) {
				spectatorOnGameOver();
			}
			else {
				for (Spectator s : getSpectators()) {
					s.onRotationComplete(this);
				}
			}
		}

		else {
			if(isGameOver()) {
				spectatorOnGameOver();
			}
			else {
				PlayerList.get(indexOfCurrentPlayer).player().makeMove(this, PlayerList.get(indexOfCurrentPlayer).location(), validMove, this);
			}
		}

	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		return Collections.unmodifiableList(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		List<Colour> colourList = new ArrayList<>();
		for (ScotlandYardPlayer player : PlayerList) {
			colourList.add(player.colour());
		}
		return Collections.unmodifiableList(colourList);
	}

	//check whether all rounds are used up,and every player has made the move in that round
	private boolean MrXEscaped() {
		return ((getCurrentRound() == getRounds().size() && indexOfCurrentPlayer == 0));
	}

	//for isAllDetectivesStuck
	private boolean isPlayerStuck(ScotlandYardPlayer player) {
		for (Move move : validMove(player)) {
			if (!(move instanceof PassMove)) {
				return false;
			}
		}
		return true;
	}

	//check whether all detectives are stuck
	private boolean AllDetectivesStuck() {
		for (ScotlandYardPlayer player : PlayerList) {
			if (player.isDetective() && !(isPlayerStuck(player))) {
				return false;
			}
		}
		return true;
	}

	private boolean isMrXCaptured() {
		int mrXLocation = PlayerList.get(0).location();
		for (ScotlandYardPlayer p : PlayerList) {
			if (p.isDetective() && p.location() == mrXLocation) {
				return true;
			}
		}
		return false;
	}

//	private boolean isPlayerStuck(ScotlandYardPlayer player) {
//		for (Move move : validMove(player)) {
//			if (!(move instanceof PassMove)) {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	private boolean allDetectivesStuck() {
//		for (ScotlandYardPlayer p : PlayerList) {
//			if (p.isDetective() && !(isPlayerStuck(p))) {
//				return false;
//			}
//		}
//		return true;
//	}
//
	private boolean isMrXCornered() {
		return ((indexOfCurrentPlayer == 0) && isPlayerStuck(PlayerList.get(0)) && !(PlayerList.get(0).tickets().isEmpty()));
	}
//
//	private boolean isMrXCaptured() {
//		int mrXLocation = PlayerList.get(0).location();
//		for (ScotlandYardPlayer p : PlayerList) {
//			if (p.isDetective() && p.location() == mrXLocation) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private boolean mrXEscaped() {
//		return (getRounds().size() == getCurrentRound());
//	}

	@Override
	public Set<Colour> getWinningPlayers() {

		if (winningPlayers.isEmpty()) {                                        //only checks for winning players, while set of winning players is empty
			if (MrXEscaped() || AllDetectivesStuck()) {                        //if mrX winning conditions
				winningPlayers.add(BLACK);                                    //add mrX colour to set of winning players
			}
			if (isMrXCaptured() || isMrXCornered())                            //if detectives winning conditions
				for (ScotlandYardPlayer player : PlayerList) {                    //loop through players
					if (player.isDetective()) {                                    //check player is detective
						winningPlayers.add(player.colour());                        // add detective colours to set of winning players
					}
				}
		}
        return Collections.unmodifiableSet(winningPlayers);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		for (ScotlandYardPlayer player : PlayerList) {
			if (player.colour()==colour && player.isMrX()) {
				return Optional.of(mrXLastRevealLocation);
			}
			if (player.colour()==colour && player.isDetective()) {
				return Optional.of(player.location());
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		for (ScotlandYardPlayer player : PlayerList) {
			if (player.colour()==colour) {
				return Optional.of(player.tickets().get(ticket));
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		// TODO
		return !getWinningPlayers().isEmpty();
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		return PlayerList.get(indexOfCurrentPlayer).colour();
	}

	@Override
	public int getCurrentRound() {
		// TODO
		return currentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		// TODO
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		// TODO
		return new ImmutableGraph<Integer, Transport>(graph);
	}
}



