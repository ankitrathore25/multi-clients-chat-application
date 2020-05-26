package phase3;
/*
 *Author: Ankit Rathore
 *Id:     1001767618
 *netId:  axr7618 
 */
import java.awt.EventQueue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;



public class ServerView3 {
	/*
	 * References: https://www.youtube.com/watch?v=rd272SCl-XE
	 * 			   https://www.youtube.com/watch?v=ZzZeteJGncY
	 * */

	private static final long serialVersionUID = 1L;

	private static Map<String, Socket> allUsersList = new ConcurrentHashMap<>(); // keeps the mapping of all the
																					// usernames used and their socket connections
	private static Set<String> activeUserSet = new HashSet<>(); // this set keeps track of all the active users 

	private static int port = 8818;  // port number to be used

	private JFrame frame; // jframe variable

	private ServerSocket serverSocket; //server socket variable

	private JTextArea serverMessageBoard; // variable for server message board on UI

	private JList allUserNameList;  // variable on UI

	private JList activeClientList; // variable on UI

	private DefaultListModel<String> activeDlm = new DefaultListModel<String>(); // keeps list of active users for display on UI

	private DefaultListModel<String> allDlm = new DefaultListModel<String>(); // keeps list of all users for display on UI

	private Map<String,int[]> vClock = new HashMap<>();
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {  // functions starts here
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerView3 window = new ServerView3();  // object creation
					window.frame.setVisible(true); // make jframe visible
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ServerView3() {
		initialize();  // components of swing app will be initialized here.
		try {
			serverSocket = new ServerSocket(port);  // create a socket for server
			serverMessageBoard.append("Server started on port: " + port + "\n"); // print messages to server message board
			serverMessageBoard.append("Waiting for the clients...\n");
			new ClientAccept().start(); // this will create a thread for client
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class ClientAccept extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Socket clientSocket = serverSocket.accept();  // create a socket for client
					String uName = new DataInputStream(clientSocket.getInputStream()).readUTF(); // this will receive the username sent from client register view
					DataOutputStream cOutStream = new DataOutputStream(clientSocket.getOutputStream()); // create an output stream for client
					if(!uName.equalsIgnoreCase("a")&&!uName.equalsIgnoreCase("b")&&!uName.equalsIgnoreCase("c")) {
						cOutStream.writeUTF("Please choose from a,b or c only.");
					}else if (activeUserSet != null && activeUserSet.contains(uName)) { // if username is in use then we need to prompt user to enter new name
						cOutStream.writeUTF("Username already taken");
					} else {
						allUsersList.put(uName, clientSocket); // add new user to allUserList and activeUserSet
						activeUserSet.add(uName);
						cOutStream.writeUTF(""); // clear the existing message
						activeDlm.addElement(uName); // add this user to the active user JList
						if (!allDlm.contains(uName)) // if username taken previously then don't add to allUser JList otherwise add it
							allDlm.addElement(uName);
						activeClientList.setModel(activeDlm); // show the active and allUser List to the swing app in JList
						allUserNameList.setModel(allDlm);
						serverMessageBoard.append("Client " + uName + " Connected...\n"); // print message on server that new client has been connected.
						new MsgRead(clientSocket, uName).start(); // create a thread to read messages
						new PrepareCLientList().start(); //create a thread to update all the active clients
					}
				} catch (IOException ioex) {  // throw any exception occurs
					ioex.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	class MsgRead extends Thread { // this class reads the messages coming from client and take appropriate actions
		Socket s;
		String Id;
		private MsgRead(Socket s, String uname) { // socket and username will be provided by client
			this.s = s;
			this.Id = uname;
		}

		@Override
		public void run() {
			while (allUserNameList != null && !allUsersList.isEmpty()) {  // if allUserList is not empty then proceed further
				try {
					String message = new DataInputStream(s.getInputStream()).readUTF(); // read message from client
					System.out.println("message read ==> " + message); // just print the message for testing
					String[] msgList = message.split(":"); // I have used my own identifier to identify what action to take on the received message from client
														// i have appended actionToBeTaken:clients_for_receiving_msg:message
					if (msgList[0].equalsIgnoreCase("multicast")) { // if action is multicast then send messages to selected active users
						String from = msgList[1]; //this variable contains list of clients which will receive message
						String to = msgList[2];
						if(!vClock.containsKey(from)) { // condition to select which clock to update.
							int[] val = {0,0,0};
							if(from.equalsIgnoreCase("a")) { // if msg is from a then update the time by 1
								val[0] += 1;
							}else if(from.equalsIgnoreCase("b")) { // similarly if from b update 1
								val[1] += 1;
							}else { // else updte time for c by 1
								val[2] += 1;
							}
							vClock.put(from,val);
						}else { // if message received is not for the first time then update the respective clocks by 1
							if(from.equalsIgnoreCase("a")) {
								vClock.get(from)[0] += 1;
							}else if(from.equalsIgnoreCase("b")) {
								vClock.get(from)[1] += 1;
							}else {
								vClock.get(from)[2] += 1;
							}
						}
						if(!vClock.containsKey(to)) { // similarly we will update clock of receiver client too.
							int[] val = {0,0,0};
							if(to.equalsIgnoreCase("a")) { // this is the case if message is being sent for the first time.
								val[0] = 1;
								val[1] = Math.max(vClock.get(from)[1],val[1]); // compare the time and update the clock
								val[2] = Math.max(vClock.get(from)[2],val[2]);// compare the time and update the clock
							}else if(to.equalsIgnoreCase("b")) {
								val[0] = Math.max(vClock.get(from)[0],val[0]);// compare the time and update the clock
								val[1] = 1;
								val[2] = Math.max(vClock.get(from)[2],val[2]);// compare the time and update the clock
							}else {
								val[0] = Math.max(vClock.get(from)[0],val[0]);// compare the time and update the clock
								val[1] = Math.max(vClock.get(from)[1],val[1]);// compare the time and update the clock
								val[2] = 1;
							}
							vClock.put(to,val);
						}else {
							if(to.equalsIgnoreCase("a")) { // this is the case if message recieved is not for the first time.
								vClock.get(to)[0] += 1;
								vClock.get(to)[1] = Math.max(vClock.get(from)[1],vClock.get(to)[1]);// compare the time and update the clock
								vClock.get(to)[2] = Math.max(vClock.get(from)[2],vClock.get(to)[2]);// compare the time and update the clock
							}else if(to.equalsIgnoreCase("b")) {
								vClock.get(to)[0] = Math.max(vClock.get(from)[0],vClock.get(to)[0]);// compare the time and update the clock
								vClock.get(to)[1] += 1;
								vClock.get(to)[2] = Math.max(vClock.get(from)[2],vClock.get(to)[2]);// compare the time and update the clock
							}else {
								vClock.get(to)[0] = Math.max(vClock.get(from)[0],vClock.get(to)[0]);// compare the time and update the clock
								vClock.get(to)[1] = Math.max(vClock.get(from)[1],vClock.get(to)[1]);// compare the time and update the clock
								vClock.get(to)[2] += 1;
							}
						}
						
						new DataOutputStream(((Socket) allUsersList.get(from)).getOutputStream())
						.writeUTF("<Msg sent to: " + to + " >" + Arrays.toString(vClock.get(from))); // put message in output stream
						new DataOutputStream(((Socket) allUsersList.get(to)).getOutputStream())
						.writeUTF("< " + from + " >" + Arrays.toString(vClock.get(to))); // put message in output stream
						serverMessageBoard.append(from+":"+Arrays.toString(vClock.get(from))+"  ,  "+to+":"+Arrays.toString(vClock.get(to))+"\n");
						
					} else if (msgList[0].equalsIgnoreCase("exit")) { // if a client's process is killed then notify other clients
						activeUserSet.remove(Id); // remove that client from active usre set
						serverMessageBoard.append(Id + " disconnected....\n"); // print message on server message board

						new PrepareCLientList().start(); // update the active and all user list on UI

						Iterator<String> itr = activeUserSet.iterator(); // iterate over other active users
						while (itr.hasNext()) {
							String usrName2 = (String) itr.next();
							if (!usrName2.equalsIgnoreCase(Id)) { // we don't need to send this message to ourself
								try {
									new DataOutputStream(((Socket) allUsersList.get(usrName2)).getOutputStream())
											.writeUTF(Id + " disconnected..."); // notify all other active user for disconnection of a user
								} catch (Exception e) { // throw errors
									e.printStackTrace();
								}
								new PrepareCLientList().start(); // update the active user list for every client after a user is disconnected
							}
						}
						activeDlm.removeElement(Id); // remove client from Jlist for server
						activeClientList.setModel(activeDlm); //update the active user list
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	class PrepareCLientList extends Thread { // it prepares the list of active user to be displayed on the UI
		@Override
		public void run() {
			try {
				String ids = "";
				Iterator itr = activeUserSet.iterator(); // iterate over all active users
				while (itr.hasNext()) { // prepare string of all the users
					String key = (String) itr.next();
					ids += key + ",";
				}
				if (ids.length() != 0) { // just trimming the list for the safe side.
					ids = ids.substring(0, ids.length() - 1);
				}
				itr = activeUserSet.iterator(); 
				while (itr.hasNext()) { // iterate over all active users
					String key = (String) itr.next();
					try {
						new DataOutputStream(((Socket) allUsersList.get(key)).getOutputStream())
								.writeUTF(":;.,/=" + ids); // set output stream and send the list of active users with identifier prefix :;.,/=
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() { //here components of Swing App UI are initilized
		frame = new JFrame();
		frame.setBounds(100, 100, 399, 680);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Server View");

		serverMessageBoard = new JTextArea();
		serverMessageBoard.setEditable(false);
		serverMessageBoard.setBounds(12, 13, 234, 607);
		
		frame.getContentPane().add(serverMessageBoard);
		
		serverMessageBoard.setText("Starting the Server...\n");

		allUserNameList = new JList();
		allUserNameList.setBounds(252, 249, 127, 179);
		frame.getContentPane().add(allUserNameList);

		activeClientList = new JList();
		activeClientList.setBounds(252, 50, 127, 156);
		frame.getContentPane().add(activeClientList);

		JLabel lblNewLabel = new JLabel("All Usernames");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel.setBounds(252, 219, 127, 16);
		frame.getContentPane().add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("Active Users");
		lblNewLabel_1.setBounds(258, 29, 98, 23);
		frame.getContentPane().add(lblNewLabel_1);

	}
}
