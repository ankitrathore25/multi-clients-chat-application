package phase2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


public class ClientView2 extends JFrame {

	/*
	 * References: https://www.youtube.com/watch?v=rd272SCl-XE
	 * 			   https://www.youtube.com/watch?v=ZzZeteJGncY
	 * https://examples.javacodegeeks.com/enterprise-java/jms/apache-activemq-hello-world-example/
	 * https://stackoverflow.com/questions/11839246/how-to-convert-timestamp-to-date-in-java
	 * */
	private static final long serialVersionUID = 1L;
	private JFrame frame;
	private JTextField clientTypingBoard;
	private JList clientActiveUsersList;
	private JTextArea clientMessageBoard;
	private JButton clientKillProcessBtn;
	private JRadioButton oneToNRadioBtn;
	private JRadioButton broadcastBtn;

	DataInputStream inputStream;
	DataOutputStream outStream;
	DefaultListModel<String> dm;
	String id, clientIds = "";

	/**
	 * Launch the application.
	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					ClientView window = new ClientView();
//					window.frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}

	/**
	 * Create the application.
	 */

	public ClientView2() {
		initialize();
	}

	public ClientView2(String id, Socket s) { // constructor call, it will initialize required variables
		initialize(); // initilize UI components
		this.id = id;
		try {
			frame.setTitle("Client View - " + id); // set title of UI
			dm = new DefaultListModel<String>(); // default list used for showing active users on UI
			clientActiveUsersList.setModel(dm);// show that list on UI component JList named clientActiveUsersList
			inputStream = new DataInputStream(s.getInputStream()); // initilize input and output stream
			outStream = new DataOutputStream(s.getOutputStream());
			new Read().start(); // create a new thread for reading the messages
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	class Read extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					String m = inputStream.readUTF();  // read message from server, this will contain :;.,/=<comma seperated clientsIds>
					System.out.println("inside read thread : " + m); // print message for testing purpose
					if (m.contains(":;.,/=")) { // prefix(i know its random)
						m = m.substring(6); // comma separated all active user ids
						dm.clear(); // clear the list before inserting fresh elements
						StringTokenizer st = new StringTokenizer(m, ","); // split all the clientIds and add to dm below
						while (st.hasMoreTokens()) {
							String u = st.nextToken();
							System.out.println("username ===>>> "+u);
							if (!id.equals(u.split("-")[0])) // we do not need to show own user id in the active user list pane
								dm.addElement(u); // add all the active user ids to the defaultList to display on active
													// user pane on client view
						}
					} else {
						clientMessageBoard.append("" + m + "\n"); //otherwise print on the clients message board
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() { // initialize all the components of UI
		frame = new JFrame();
		frame.setBounds(100, 100, 658, 725);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Client View");

		clientMessageBoard = new JTextArea();
		clientMessageBoard.setEditable(false);
		clientMessageBoard.setBounds(12, 25, 364, 543);
		frame.getContentPane().add(clientMessageBoard);

		clientTypingBoard = new JTextField();
		clientTypingBoard.setHorizontalAlignment(SwingConstants.LEFT);
		clientTypingBoard.setBounds(12, 581, 364, 84);
		frame.getContentPane().add(clientTypingBoard);
		clientTypingBoard.setColumns(10);

		JButton clientSendMsgBtn = new JButton("Send");
		clientSendMsgBtn.addActionListener(new ActionListener() { // action to be taken on send message button
			public void actionPerformed(ActionEvent e) {
				String textAreaMessage = clientTypingBoard.getText(); // get the message from textbox
				if (textAreaMessage != null && !textAreaMessage.isEmpty()) {  // only if message is not empty then send it further otherwise do nothing
					try {
						String messageToBeSentToServer = "";
						String cast = "broadcast"; // this will be an identifier to identify type of message
						int flag = 0; // flag used to check whether used has selected any client or not for multicast 
						if (oneToNRadioBtn.isSelected()) { // if 1-to-N is selected then do this
							cast = "multicast"; 
							List<String> clientList = clientActiveUsersList.getSelectedValuesList(); // get all the users selected on UI
							if (clientList.size() == 0) // if no user is selected then set the flag for further use
								flag = 1;
							for (String selectedUsr : clientList) { // append all the usernames selected in a variable
								if (clientIds.isEmpty())
									clientIds += selectedUsr.split("-")[0];
								else
									clientIds += "," + selectedUsr.split("-")[0];
							}
							messageToBeSentToServer = cast + ":" + clientIds + ":" + textAreaMessage; // prepare message to be sent to server
						} else {
							messageToBeSentToServer = cast + ":" + textAreaMessage; // in case of broadcast we don't need to know userIds
						}
						Date date = java.util.Calendar.getInstance().getTime();
						if (cast.equalsIgnoreCase("multicast")) { 
							if (flag == 1) { // for multicast check if no user was selected then prompt a message dialog
								JOptionPane.showMessageDialog(frame, "No user selected");
							} else { // otherwise just send the message to the user
								outStream.writeUTF(messageToBeSentToServer);
								clientTypingBoard.setText("");
								clientMessageBoard.append("<You sent msg to " + clientIds + "> " + textAreaMessage + "\n" + new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(date)+"\n\n"); //show the sent message to the sender's message board
							}
						} else { // in case of broadcast
							outStream.writeUTF(messageToBeSentToServer);
							clientTypingBoard.setText("");
							clientMessageBoard.append("<You sent msg to All> " + textAreaMessage + "\n" + new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(date)+"\n\n");
						}
						clientIds = ""; // clear the all the client ids 
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(frame, "User does not exist anymore."); // if user doesn't exist then show message
					}
				}
			}
		});
		clientSendMsgBtn.setBounds(388, 581, 106, 84);
		frame.getContentPane().add(clientSendMsgBtn);

		clientActiveUsersList = new JList();
		clientActiveUsersList.setToolTipText("Active Users");
		clientActiveUsersList.setBounds(388, 62, 243, 401);
		frame.getContentPane().add(clientActiveUsersList);

		clientKillProcessBtn = new JButton("Kill Process");
		clientKillProcessBtn.addActionListener(new ActionListener() { // kill process event
			public void actionPerformed(ActionEvent e) {
				try {
					outStream.writeUTF("exit"); // closes the thread and show the message on server and client's message
												// board
					clientMessageBoard.append("You are disconnected now.\n");
					frame.dispose(); // close the frame 
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		clientKillProcessBtn.setBounds(506, 581, 125, 84);
		frame.getContentPane().add(clientKillProcessBtn);

		JLabel lblNewLabel = new JLabel("Users");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel.setBounds(388, 43, 95, 16);
		frame.getContentPane().add(lblNewLabel);

		oneToNRadioBtn = new JRadioButton("1 to N");
		oneToNRadioBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clientActiveUsersList.setEnabled(true);
			}
		});
		oneToNRadioBtn.setSelected(true);
		oneToNRadioBtn.setBounds(453, 24, 72, 25);
		frame.getContentPane().add(oneToNRadioBtn);

		broadcastBtn = new JRadioButton("Broadcast");
		broadcastBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clientActiveUsersList.setEnabled(false);
			}
		});
		broadcastBtn.setBounds(529, 24, 107, 25);
		frame.getContentPane().add(broadcastBtn);

		ButtonGroup btngrp = new ButtonGroup();
		btngrp.add(oneToNRadioBtn);
		btngrp.add(broadcastBtn);
		
		JButton btnNewButton = new JButton("Check Messages");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Checking messages start.............");
				
				try {
					outStream.writeUTF("checkmessage:"+id); // send message to the server for showing the messages from queue
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				System.out.println("check messages end.............");
			}
		});
		btnNewButton.setBounds(388, 476, 243, 92);
		frame.getContentPane().add(btnNewButton);

		frame.setVisible(true);
	}
}
