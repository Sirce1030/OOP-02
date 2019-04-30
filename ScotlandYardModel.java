package uk.ac.bris.cs.scotlandyard.model;

//1. the gameover can only appear once and when it is done, there is no further interaction. So we put it in the bottom
//there is another case which is different. that is all rounds used.
//Ok, there is no this kind of case
//but if all round is used, we should't let spectator onMovemade first,but GameOver first in mrX round.


//2. our work is not perfect, i believe that there must be else mistakes that we haven't found during coding.
//the things we are doing is just enough passing the tests.
//and we also found some bugs that we still haven't fixed. for example...the SECRET ticket is not included in the test, but revealed location needed not to be checked
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.*;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;
import java.util.*;
import  java.util.List;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.*;


//领悟，java的return都是从collection开始，我说的collection是指对象，然后对这个对象所做的动作反而放在后面
//所以我们可以把java获得数据的过程理解成一种被动的语法
//在写java的过程中，才能真正感受到dispatch，double dispatch，还有visitor parttern的作用
//另外，计算机其实非常聪明..知道许多我们不知道的东西，这也导致到了我会想按照自己的思维来写代码，要学会知道计算机知道的什么。

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor{
	private int indexOfcurrentPlayer = 0;
	private int indexOfpreviousPlayer;
	private int CurrentRound = 0;
	boolean Occupied = false;
	private Set<Move> validMoves;
	private List< Boolean > rounds;
	private int mrXLastRevealLocation = 0;
	private DoubleMove doubleMove;
	/////
	public Set<Colour> winner = new HashSet<>();


	///
	private Graph< Integer, Transport > graph;
	private ArrayList< PlayerConfiguration > configurations = new ArrayList<>();
	///？？有些不理解
	private ArrayList<Spectator> spectators = new ArrayList<>();
	private Set< Integer > set = new HashSet<>();
	private Set< Colour > Colour = new HashSet<>();
	private ArrayList< ScotlandYardPlayer > listOfplayers = new ArrayList<>();//为什么位置变了就不行


	//这里一个大框框，声明要建在这个框框的上面
	public ScotlandYardModel ( List< Boolean > rounds, Graph< Integer, Transport > graph,
							   PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							   PlayerConfiguration... restOfTheDetectives ) {
		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);
		if ( rounds.isEmpty() ) {
			throw new IllegalArgumentException("Empty rounds");
		}
		if ( graph.isEmpty() ) {
			throw new IllegalArgumentException("Empty graph");
		}
		if ( mrX.colour != BLACK ) {
			throw new IllegalArgumentException("MrX should be Black");
		}

//////////////

		for ( PlayerConfiguration configuration : restOfTheDetectives )//add players into an ArrayList
			configurations.add(requireNonNull(configuration));
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);

/*Set<Integer> set = new HashSet<>();
Set<Colour> Colour = new HashSet<>();*/
		//check whether there is duplicate location or colour
		for ( PlayerConfiguration configuration : configurations ) {
			if ( set.contains(configuration.location) ) throw new IllegalArgumentException("Duplicate location");
			if ( Colour.contains(configuration.colour) ) throw new IllegalArgumentException("Duplicate colour");
			Colour.add(configuration.colour);
			set.add(configuration.location);//为什么这里的location和color是final？？
		}
		//check whether detective holds Double or Secrete
		if ( firstDetective.tickets.containsKey(DOUBLE) && firstDetective.tickets.containsKey(SECRET) ) {
			if ( firstDetective.tickets.get(DOUBLE) > 0 || firstDetective.tickets.get(SECRET) > 0 )
				throw new IllegalArgumentException("FirstDective has Double or Secrete");
		}
		for ( PlayerConfiguration restOfTheDetective : restOfTheDetectives ) {
			if ( restOfTheDetective.tickets.containsKey(DOUBLE) || firstDetective.tickets.containsKey(SECRET) )
				if ( restOfTheDetective.tickets.get(DOUBLE) > 0 || restOfTheDetective.tickets.get(SECRET) > 0 ) {
					throw new IllegalArgumentException("restOfTheDetective has Double or Secrete");
				}
		}
		//check every one have nonNull ticket
		for(PlayerConfiguration p : configurations){
			if(!p.tickets.keySet().containsAll(Arrays.asList(Ticket.values())))//turn an Array into a list
				throw new IllegalArgumentException("Someone has no every tickets");
		}
		//将数组转化为collection
/////////////////////////////////////////////////////////

//ArrayList<ScotlandYardPlayer> listOfplayers = new ArrayList<>();
		listOfplayers.add(new ScotlandYardPlayer(mrX.player, mrX.colour, mrX.location, mrX.tickets));
		listOfplayers.add(new ScotlandYardPlayer(firstDetective.player, firstDetective.colour, firstDetective.location, firstDetective.tickets));
		for ( PlayerConfiguration restOfTheDetective : restOfTheDetectives ) {
			listOfplayers.add(new ScotlandYardPlayer(restOfTheDetective.player, restOfTheDetective.colour, restOfTheDetective.location, restOfTheDetective.tickets));
		}


	}

	@Override//1
	public void registerSpectator ( Spectator spectator ) {
		requireNonNull(spectator);
		if (spectators.contains(spectator)) {
			throw new IllegalArgumentException("Spectator already registered");
		} else {
			spectators.add(spectator);
		}
	}

	@Override//2
	public void unregisterSpectator ( Spectator spectator ) {
		requireNonNull(spectator);
		if (!(spectators.contains(spectator))) {
			throw new IllegalArgumentException("Spectator wasn't registered");
		} else {
			spectators.remove(spectator);
		}
	}

	private Set< TicketMove > validTicketMoves ( ScotlandYardPlayer player,int location ) {
		Set< TicketMove > validMoves = new HashSet<>();
		for ( Edge< Integer, Transport > edge : getGraph().getEdgesFrom(getGraph().getNode(location)) ) {
			boolean occupied = false;
			for ( ScotlandYardPlayer p : listOfplayers ) {
				if ( p.location() == edge.destination().value() && p!=player && p.isDetective()) {
					occupied= p.location() == edge.destination().value();
				}
			}
			Ticket ticket = fromTransport(edge.data());
			if ( player.hasTickets(ticket) && !occupied)
				validMoves.add(new TicketMove(player.colour(), ticket, edge.destination().value()));
			if ( player.hasTickets(SECRET) && !occupied)
				validMoves.add(new TicketMove(player.colour(), SECRET, edge.destination().value()));
		}

		return validMoves;
	}
	private Set<Move> validMoves(ScotlandYardPlayer player) {
		Set<TicketMove> ValidTicketMove = validTicketMoves(player,player.location());
		Set<Move> validMoves = new HashSet<>(ValidTicketMove);//这里要更新validMoves，不然会不断的往里面加...
		if (player.isMrX() && listOfplayers.get(0).hasTickets(DOUBLE) && getCurrentRound() < (getRounds().size())-1) {
			for (TicketMove firstMove : ValidTicketMove) {
				for (TicketMove secondMove : validTicketMoves(player, firstMove.destination())) {
					if (firstMove.ticket() == secondMove.ticket() && !player.hasTickets(firstMove.ticket(), 2)) {
						continue;
					}
					else {
						validMoves.add(new DoubleMove(player.colour(), firstMove, secondMove));
					}
				}
			}
		}
		if (validMoves.isEmpty()&&player.isDetective()) {
			validMoves.add(new PassMove(player.colour())); }
		return validMoves;
	}

	@Override//3
	public void startRotate() {//Consumer 里面有一个default，所以不需要调用，就可以直接accept一次
		if (!isGameOver()) {
			validMoves = validMoves(listOfplayers.get(indexOfcurrentPlayer));
			listOfplayers.get(indexOfcurrentPlayer).player().makeMove(this, listOfplayers.get(indexOfcurrentPlayer).location(), validMoves(listOfplayers.get(indexOfcurrentPlayer)), this); }//notify player to make move
		else { throw new IllegalStateException("Cannot start rotate if game is already over"); } }

	private ScotlandYardPlayer GetpreviousPlayer(){
		return listOfplayers.get(indexOfpreviousPlayer);
	}

	private void OnMoveMadePlayer(Move move){
		for(Spectator s: spectators) {
			s.onMoveMade(this,move);
		}
	}
	private void OnMoveMadeformrX(Move move){
		for(Spectator s :spectators){
			s.onRoundStarted(this, CurrentRound);
			s.onMoveMade(this,move);
		} }
	private void onRotationCompleteforplayer(){
		for(Spectator s :spectators){
			s.onRotationComplete(this); } }
	private void OnGameOver(){
		for(Spectator s : spectators){
			s.onGameOver(this,getWinningPlayers());
		}
	}

	@Override//之前纠结了很长时间不理解为什么makeMove完之后CurrentRound还是0，其实是还不会分析代码，看得太跳跃了，应该要是在accept之后CurrentRound才会增长
	//没有仔细看UML图的顺序,特别在遇到default的时候一定要注意，顺序会乱
	public void accept(Move move) {//move 相当于 Visitor,但是虚拟类,所以accept也很抽象
		requireNonNull(move);
		indexOfpreviousPlayer = indexOfcurrentPlayer;
		if ( !validMoves.contains(requireNonNull(move)) ) {
			throw new IllegalArgumentException("valid moves contains moves not valid");
		}
		if ( indexOfcurrentPlayer < listOfplayers.size() ) {
			++indexOfcurrentPlayer;
		}
		if ( indexOfcurrentPlayer == (listOfplayers.size()) ) {
			indexOfcurrentPlayer = 0;
		}


		move.visit(new MoveVisitor() {//为什么MoveVisitor可以这样新建？move.visit里面的MoveVisitor代表一个class？？class可以作为入参？
			//**怎么改写看起来更简洁

			//疑问：为什么MoveVisitor可以这样新建？move.visit里面的MoveVisitor代表一个class？？class可以作为入参？
			//**提高：怎么改写看起来更简洁，新建一个新的MoveVisitor对象在accept之外
			//学习：这里有个很骚的地方就是MoveVisitor里都是default，这个游戏里大部分都是default，应该是很实用的修饰词，提供了一个奇怪的框架，居然让vistor在model里面更改...而不是在visitor原本的class里面
			//原因/解释：这么写很麻烦，但应该是为了让我们更好的把代码全写在model里面
			@Override
			public void visit ( PassMove move ) {
				OnMoveMadePlayer(move);
				if(0==indexOfpreviousPlayer)
					onRotationCompleteforplayer();}


			//					   @Override
//					   public void visit ( TicketMove move ) {
//						   ScotlandYardPlayer player = listOfplayers.get(indexOfpreviousPlayer);
//						   player.location(move.destination());
//						   if ( player.isMrX() ) {
//							   player.removeTicket(move.ticket());
//							   if ( getRounds().get(getCurrentRound()) ) {
//								   CurrentRound++;
//
//							   } else {
//								   CurrentRound++;
//
//							   }
//						   }}

//don't know where to apply SECRET ticket
			public void visit ( TicketMove move ) {
				GetpreviousPlayer().location(move.destination());
				if(GetpreviousPlayer().isMrX()) {
					GetpreviousPlayer().removeTicket(move.ticket());
					if(getRounds().get(getCurrentRound())||  ! (move.ticket()==SECRET )) {
						mrXLastRevealLocation = GetpreviousPlayer().location();
					}

					CurrentRound++;
					OnMoveMadeformrX(new TicketMove(BLACK,move.ticket(),mrXLastRevealLocation));
					//if all round is used, then we can game over directly


				}
				else {
					GetpreviousPlayer().removeTicket(move.ticket());
					listOfplayers.get(0).addTicket(move.ticket());
					OnMoveMadePlayer(move);
				} }

			@Override
			public void visit ( DoubleMove move ) {

				if (getRounds().get(getCurrentRound()) && getRounds().get(getCurrentRound() + 1)) {                            //reveal reveal
					doubleMove = new DoubleMove(move.colour(), move.firstMove().ticket(), move.firstMove().destination(), move.secondMove().ticket(), move.secondMove().destination());
				}
				if (getRounds().get(getCurrentRound()) && !getRounds().get(getCurrentRound() + 1)) {                        //reveal hidden
					doubleMove =  new DoubleMove(move.colour(), move.firstMove().ticket(), move.firstMove().destination(), move.secondMove().ticket(), move.firstMove().destination());
				}
				if (!getRounds().get(getCurrentRound()) && getRounds().get(getCurrentRound() + 1)) {                            //hidden reveal
					doubleMove = new DoubleMove(move.colour(), move.firstMove().ticket(), mrXLastRevealLocation, move.secondMove().ticket(), move.secondMove().destination());
				}
				if (!getRounds().get(getCurrentRound()) && !getRounds().get(getCurrentRound() + 1)) {                            //hidden hidden
					doubleMove = new DoubleMove(move.colour(), move.firstMove().ticket(), mrXLastRevealLocation, move.secondMove().ticket(), mrXLastRevealLocation);
				}
				GetpreviousPlayer().removeTicket(DOUBLE);
				OnMoveMadePlayer(doubleMove);
				visit(move.firstMove());
				visit(move.secondMove());
			}});
		if (indexOfcurrentPlayer == 0) {
			if ( isGameOver() ) OnGameOver();
			else {
				onRotationCompleteforplayer();
			}
		} else {//make sure no further interaction after detectives win
			if(isGameOver() && !getWinningPlayers().contains(BLACK)) { OnGameOver();}
			else {validMoves = validMoves(listOfplayers.get(indexOfcurrentPlayer));//用来确保GameOver一直在被更新,不然可能会StartRotate不throw
			listOfplayers.get(indexOfcurrentPlayer).player().makeMove(this, listOfplayers.get(indexOfcurrentPlayer).location(), validMoves(listOfplayers.get(indexOfcurrentPlayer)), this);
		}}
	}





	@Override///////////4
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableList(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> listOfColour = new ArrayList<>();
		for(ScotlandYardPlayer player : listOfplayers){
			listOfColour.add(player.colour());
		}
		return Collections.unmodifiableList(listOfColour);
	}
	private boolean Stuck(ScotlandYardPlayer player){
		for(Move v : validMoves(player)){
			if(!(v instanceof PassMove)) return false;
		}
		return true;
	}
	private boolean AllDetectivesStuck(){
		for(ScotlandYardPlayer player : listOfplayers){
			if(player.isDetective() && !Stuck(player)) return false; }
		return true; }

	private boolean mrXCaptured(){
		for(ScotlandYardPlayer player : listOfplayers){
			if(player.isDetective() && player.location()==listOfplayers.get(0).location()) return true;
		}return false;
	}

	private boolean mrXEscape(){
		return (getRounds().size()==CurrentRound && indexOfcurrentPlayer==0)||(getRounds().size()<CurrentRound); }
	private boolean mrXHasNoWay(){
		return (indexOfcurrentPlayer==0 && Stuck(listOfplayers.get(0))&&!(listOfplayers.get(0).tickets().isEmpty()));

	}

	@Override/////////////6
	public Set<Colour> getWinningPlayers() {
		if (winner.isEmpty()) {                                        //only checks for winning players, while set of winning players is empty
			if (mrXEscape() || AllDetectivesStuck()) {                        //if mrX winning conditions
				winner.add(BLACK);                                    //add mrX colour to set of winning players
			}
			if (mrXCaptured() || mrXHasNoWay())                            //if detectives winning conditions
				for (ScotlandYardPlayer p : listOfplayers) {                    //loop through players
					if (p.isDetective()) {                                    //check player is detective
						winner.add(p.colour());                        // add detective colours to set of winning players
					} }
		}
		return Collections.unmodifiableSet(winner);
	}

	@Override///////////5
	public boolean isGameOver() {
		return !getWinningPlayers().isEmpty();
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		for (ScotlandYardPlayer player : listOfplayers) {
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
		for(ScotlandYardPlayer player:listOfplayers){
			if(player.colour() == colour) return Optional.of(player.tickets().get(ticket));
		}
		return Optional.empty();
	}


	@Override
	public Colour getCurrentPlayer() {
		return listOfplayers.get(indexOfcurrentPlayer).colour();
	}

	@Override
	public int getCurrentRound() {
		return CurrentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(graph);
	}


}

