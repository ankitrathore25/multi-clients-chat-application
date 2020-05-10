package phase2;
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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;



public class ServerView2 {
	/*
	 * References: https://www.youtube.com/watch?v=rd272SCl-XE
	 * 			   https://www.youtube.com/watch?v=ZzZeteJGncY
	 * https://examples.javacodegeeks.com/enterprise-java/jms/apache-activemq-hello-world-example/
	 * https://stackoverflow.com/questions/11839246/how-to-convert-timestamp-to-date-in-java
	 * */

	private static final long serialVersionUID = 1L;

	private static Map<String, Socket> allUsersList = new ConcurrentHashMap<>(); // keeps the mapping of all the
																					// usernames used and their socket connections
	private static Set<String> activeUserSet = new HashSet<>(); // this set keeps track of all the active users 

	private static Map<String, MessageProducer> userWiseMessageProducerMap = new ConcurrentHashMap<>(); //map to keep ActiveMQ producer for every client
	
	private static Map<String, MessageConsumer> userWiseMessageConsumerMap = new ConcurrentHashMap<>();//map to keep ActiveMQ consumer for every client
	
	private static int port = 8818;  // port number to be used

	private JFrame frame; // jframe variable

	private ServerSocket serverSocket; //server socket variable

	private JTextArea serverMessageBoard; // variable for server message board on UI

	private JList allUserNameList;  // variable on UI

	private JList activeClientList; // variable on UI

	private DefaultListModel<String> activeDlm = new DefaultListModel<String>(); // keeps list of active users for display on UI

	private DefaultListModel<String> allDlm = new DefaultListModel<String>(); // keeps list of all users for display on UI
	
	private static String url = ActiveMQConnection.DEFAULT_BROKER_URL;
    
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {  // functions starts here
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerView2 window = new ServerView2();  // object creation
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
	public ServerView2() {
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
					if (activeUserSet != null && activeUserSet.contains(uName)) { // if username is in use then we need to prompt user to enter new name
						cOutStream.writeUTF("Username already taken");
					} else {
						// first we will register a new messagig queue on ActiveMQ
						ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url); // make connection factory
				        Connection connection = connectionFactory.createConnection(); // make connection
				        connection.start(); // start the connection
				        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE); //Create session to send or receive messages.
				        Destination destination = session.createQueue(uName);// destination queue where msg will be stored
				        MessageProducer producer = null;
				        if(!userWiseMessageProducerMap.containsKey(uName)) { //if producer already created then skip creating again
				        	producer = session.createProducer(destination);// producer is used for sending messages to the queue.
				        	userWiseMessageProducerMap.put(uName, producer); // add producer to the map. so that only 1 producer per client is made
						}
				        MessageConsumer consumer = null;
				        if(!userWiseMessageConsumerMap.containsKey(uName)) {// if consumer is already made then skip creating again
				        	consumer = session.createConsumer(destination); // create consumer.
				        	userWiseMessageConsumerMap.put(uName, consumer); // add consumer to the map for future use. this will make sure that only 1 consumer per client exist
				        }
						
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
			
	        System.out.println("Start message read.............");
			while (allUserNameList != null && !allUsersList.isEmpty()) {  // if allUserList is not empty then proceed further
				try {
					String message = new DataInputStream(s.getInputStream()).readUTF(); // read message from client
					System.out.println("message read ==> " + message); // just print the message for testing
					String[] msgList = message.split(":"); // I have used my own identifier to identify what action to take on the received message from client
														// i have appended actionToBeTaken:clients_for_receiving_msg:message
					if (msgList[0].equalsIgnoreCase("multicast")) { // if action is multicast then send messages to selected active users
						String[] sendToList = msgList[1].split(","); //this variable contains list of clients which will receive message
						for (String usr : sendToList) { // for every user send message
							try {
								//intialization of the connection for further use
								MessageProducer producer = null;
								ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
						        Connection connection = connectionFactory.createConnection();
						        connection.start();
						        //Create session to send or receive messages.
						        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
							     // destination queue where msg will be stored
						        Destination destination = session.createQueue(usr);
						        if(userWiseMessageProducerMap.containsKey(usr)) { // if producer already exist then use it otherwise create new producer and add to map
									producer = userWiseMessageProducerMap.get(usr);
								}else {
							        // producer is used for sending messages to the queue.
							        producer = session.createProducer(destination);
								}
						        
						        TextMessage sentMsg = session.createTextMessage("<" + Id + "> " + msgList[2]);
						        
						        //sending message to the queue
						        producer.send(sentMsg);

								if (activeUserSet.contains(usr)) { // check again if user is active then send the message
									// we need to make sure that there must be only one consumer for a queue otherwise it will freeze as new consumer will wait for messages while messages were in older consumer
									MessageConsumer consumer =  userWiseMessageConsumerMap.get(usr);
							        // Here we receive the message from queue.
							        Message receivedMsg = consumer.receive();
							        String rMsg = "";
							        // we will be showing timestamp for every message so we use java util date time.
							        String timestamp = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (receivedMsg.getJMSTimestamp()));
							        if (receivedMsg instanceof TextMessage) {
							            TextMessage textMessage = (TextMessage) receivedMsg; // get the message from the queue
							            rMsg = textMessage.getText() + "\n" + timestamp + "\n";
							            System.out.println("Received message '" + rMsg + "'");
							        }
							        // add message to the client's message dashboard
									new DataOutputStream(((Socket) allUsersList.get(usr)).getOutputStream())
											.writeUTF(rMsg); // put message in output stream
								}
								connection.close();
							} catch (Exception e) { // throw exceptions
								e.printStackTrace();
							}
						}
					} else if (msgList[0].equalsIgnoreCase("broadcast")) { // if broadcast then send message to all active clients
						
						Iterator<String> itr1 = allUsersList.keySet().iterator(); // iterate over all users
						while (itr1.hasNext()) {
							String usrName = (String) itr1.next(); // it is the username
							if (usrName.equalsIgnoreCase(Id)) { // we don't want to send message to ourself i.e. sender
								continue;
							}
							System.out.println("broadcasting message to username:"+usrName);
							// making connections first for further use
							MessageProducer producer = null;
							ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
					        Connection connection = connectionFactory.createConnection();
					        connection.start();
					        //Create session to send or receive messages.
					        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
						     // destination queue where msg will be stored
					        Destination destination = session.createQueue(usrName);
					        if(userWiseMessageProducerMap.containsKey(usrName)) { // use the existing producer for the client
								producer = userWiseMessageProducerMap.get(usrName);
							}else { // if no producer then create one and put in map
						        // producer is used for sending messages to the queue.
						        producer = session.createProducer(destination);
							}
							// create message
					        TextMessage sentMsg = session.createTextMessage("<" + Id + "> " + msgList[1]);
					        
					        //sending message to the queue
					        producer.send(sentMsg);
							
							if (!usrName.equalsIgnoreCase(Id)) { // we don't need to send message to ourself, so we check for our Id
								try {
									if (activeUserSet.contains(usrName)) { // if client is active then send message through output stream
										// make connection to the queue
										MessageConsumer consumer = userWiseMessageConsumerMap.get(usrName);
										Message receivedMsg = consumer.receive(); // receive message from the queue
										// append the date time with the messages
										String timestamp = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (receivedMsg.getJMSTimestamp()));
								        String rMsg = "";
								        if (receivedMsg instanceof TextMessage) {
								            TextMessage textMessage = (TextMessage) receivedMsg; 
								            rMsg = textMessage.getText() + "\n" + timestamp + "\n"; // append time to the message
								            System.out.println("Received message '" + textMessage.getText() + "'");
								        }
								        // show message to the client's dashboard
										new DataOutputStream(((Socket) allUsersList.get(usrName)).getOutputStream())
												.writeUTF(rMsg); // put message in output stream
									}
								} catch (Exception e) {
									e.printStackTrace(); // throw exceptions
								}
							}
						}
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
											.writeUTF(Id + " disconnected...\n"); // notify all other active user for disconnection of a user
								} catch (Exception e) { // throw errors
									e.printStackTrace();
								}
								new PrepareCLientList().start(); // update the active user list for every client after a user is disconnected
							}
						}
						activeDlm.removeElement(Id); // remove client from Jlist for server
						activeClientList.setModel(activeDlm); //update the active user list
					} else if (msgList[0].equalsIgnoreCase("checkmessage")) { // here we will retrieve messages from the queue posted when user was offline
						String id = msgList[1]; // get the username
						  try { 
							  System.out.println("check messages for user:"+id);
							  //make connection to the queue
							  ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL); Connection
							  connection = connectionFactory.createConnection(); connection.start();
							  //Create session to send or receive messages. 
							  Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE); // destination queue where msg will be stored 
							  // use the existing consumer as we want only 1 consumer per queue otherwise there will be problem
							  MessageConsumer consumer = userWiseMessageConsumerMap.get(id);
							  // there is no method to count messages from the queue so we will use enumerator to retrieve all the messages one by one.
							  Queue queue = session.createQueue(id);  // create queue
							  QueueBrowser qb = session.createBrowser(queue); // create queuebrowser for using enumerator upon
							  Enumeration enumeration = qb.getEnumeration(); // create enumerator
							  String outputMsg = "";
							  while(enumeration.hasMoreElements()) { // loop on all the messages available in queue
								  Message m = (Message) enumeration.nextElement(); // get message without removing it from queue.
								  String timestamp = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (m.getJMSTimestamp()));
								  System.out.println("received messsagessss:"+((TextMessage) m).getText());
								  if(outputMsg=="")
									  outputMsg += ((TextMessage) m).getText() + "\n" + timestamp + "\n";
								  else
									  outputMsg += "\n"+((TextMessage) m).getText() + "\n" + timestamp + "\n";
								  // enumerator show the message without deleting it from queue but we need to remove it so will use consumer.receive to remove it.
								  Message mes = consumer.receive();
								  System.out.println("this message was dequeued."+((TextMessage)mes).getText()); 
							  } 
							  if(outputMsg=="") { // if there is no message then show the message
								  new DataOutputStream(((Socket) allUsersList.get(id)).getOutputStream())
									.writeUTF("No messages in the queue.\n");
							  }else {//otherwise just show message to the client's dashboard
								  new DataOutputStream(((Socket) allUsersList.get(id)).getOutputStream())
									.writeUTF(outputMsg);
							  }
							  
						  }catch (Exception e) {
							  e.printStackTrace(); 
						  }
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println("End message read.............");
		}
	}

	class PrepareCLientList extends Thread { // it prepares the list of active user to be displayed on the UI
		@Override
		public void run() {
			try {
				String ids = "";
				for(String username : allUsersList.keySet()) {
					if(activeUserSet.contains(username)) {
						ids += username + "-Active" + ",";
					}else {
						ids += username + "-Inactive" + ",";
					}
				}
				
				if (ids.length() != 0) { // just trimming the list for the safe side.
					ids = ids.substring(0, ids.length() - 1);
				}
				Iterator itr = activeUserSet.iterator(); 
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
		frame.setBounds(100, 100, 651, 343);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Server View");

		serverMessageBoard = new JTextArea();
		serverMessageBoard.setEditable(false);
		serverMessageBoard.setBounds(12, 13, 388, 278);
		frame.getContentPane().add(serverMessageBoard);
		serverMessageBoard.setText("Starting the Server...\n");

		allUserNameList = new JList();
		allUserNameList.setBounds(412, 175, 218, 116);
		frame.getContentPane().add(allUserNameList);

		activeClientList = new JList();
		activeClientList.setBounds(412, 34, 218, 120);
		frame.getContentPane().add(activeClientList);

		JLabel lblNewLabel = new JLabel("All Usernames");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel.setBounds(412, 157, 127, 16);
		frame.getContentPane().add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("Active Users");
		lblNewLabel_1.setBounds(412, 13, 98, 23);
		frame.getContentPane().add(lblNewLabel_1);

	}
}
