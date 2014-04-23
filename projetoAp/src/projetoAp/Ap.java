package projetoAp;

import java.net.SocketException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class Ap extends Host {
	
	private static final int BUSY_SWITCHING = -1;
	private static final int CAN_SWITCH = 0;
	
	private static final int CHANNEL_SWITCHING_DELAY = 1000;
	private static final int CHANNEL_SWITCHING_PERIOD = 1000;
	private int NUMBER_OF_CLIENTS;

	private Timer timer;
	private TimerTask switchChannel;
	
	private int psi;
	private int channel;
	private int[] clientResponse;
	private String[] clients;
	
	public Ap(String clients, int port) throws SocketException{
		super(clients, port);

		countClients();
		initClientsResponse();
		
		this.psi = CAN_SWITCH;
		this.channel = 1;
		
		startTimer();
		listen();
	}

	/**
	 * Executes the channel switching logic every <CHANNEL_SWITCHING_PERIOD> milliseconds.
	 */
	private void startTimer(){
		timer = new Timer();
		switchChannel = new TimerTask(){
			@Override
			public void run(){
				if(canBeLocked()){
					startPhase1();
					startPhase2();
				}
			}
		};
		this.timer.schedule(switchChannel, CHANNEL_SWITCHING_DELAY, CHANNEL_SWITCHING_PERIOD); //TODO: randomize period
	}
	
	/**
	 * Corresponds to the part where the AP functions as a client.
	 */
	private void listen(){
		msg = receiveMessage();
		if( msg.startsWith("#lock") ) { 
			if(canBeLocked()){
				psi++;
				reply("#" + channel);
			}
			else{
				reply("#c" + BUSY_SWITCHING );
			}
		}
		else if( msg.startsWith("#unlock") ){
			psi--;
		}
		else if ( msg.startsWith("#c") ){
			int clientIndex = Arrays.binarySearch(clients, dtgReceive.getAddress().getHostAddress() + ":" + dtgReceive.getPort() + "#");
			clientResponse[clientIndex] = Integer.parseInt(msg.substring(2));
		}
		else{
			System.err.println("Stray message: " + msg);
		}
	}	
	
	private void startPhase1(){
		this.psi = BUSY_SWITCHING;
		lockAllChildren();
	}
	
	private void startPhase2(){
		waitForReplies();
		if(allChildrenAreLocked()){
			tryToSwitchChannel();
		}
		unlockAllChildren();
	}

	private void waitForReplies(){
		while(!allChildrenReplied());
	}

	private boolean allChildrenReplied(){
		for(int response : clientResponse){
			if(response == -2){
				return false;
			}
		}
		return true;
	}
	
	private void reply(String toSend){
		sendMessage(toSend, dtgReceive.getAddress().getHostAddress(), dtgReceive.getPort());
	}
	
	private void tryToSwitchChannel(){
		//TODO: the channel switching logic
	}
	
	private void lockAllChildren(){
		resetClientResponse();
		sendBroadcast("#lock");
	}
	
	private void unlockAllChildren(){
		for(int i = 0; i < clientResponse.length; i++){
			if(clientResponse[i] != BUSY_SWITCHING){
				int idx = clients[i].indexOf(":");
				sendMessage("#unlock", clients[i].substring(0, idx), Integer.parseInt(clients[i].substring(idx)));
			}
		}
	}
	
	private boolean allChildrenAreLocked(){
		for(int response : clientResponse){
			if(response == BUSY_SWITCHING){
				return false;
			}
		}
		return true;
	}
	
	private void countClients(){
		clients = clientList.split("#");
		Arrays.sort(clients);
		NUMBER_OF_CLIENTS = clients.length;
	}
		
	private void initClientsResponse(){
		clientResponse = new int[NUMBER_OF_CLIENTS];
		resetClientResponse();
	}
	
	private void resetClientResponse(){
		Arrays.fill(clientResponse, -2);
	}
	
	private boolean canBeLocked(){
		return !(this.psi == BUSY_SWITCHING);
	}
}