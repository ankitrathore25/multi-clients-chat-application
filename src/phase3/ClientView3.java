package phase3;
/*
 *Author: Ankit Rathore
 *Id:     1001767618
 *netId:  axr7618 
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
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
import java.awt.Font;


public class ClientView3 extends JFrame {

	/*
	 * References: https://www.youtube.com/watch?v=rd272SCl-XE
	 * 			   https://www.youtube.com/watch?v=ZzZeteJGncY
	 * */
	private static final long serialVersionUID = 1L;
	private JFrame frame;
	private JList clientActiveUsersList;
	private JTextArea clientMessageBoard;
	private JButton clientKillProcessBtn;
	private int[] vClock = new int[3];
	Set<String> activeClients = new HashSet<>();

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

	public ClientView3() {
		initialize();
	}

	public ClientView3(String id, Socket s) { // constructor call, it will initialize required variables
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
							activeClients.add(u);
							if (!id.equals(u)) // we do not need to show own user id in the active user list pane
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

	class runAlgo extends Thread { // updating the clock algo will be run in a separate thread.
		@Override
		public void run() {
			if(activeClients.size()==3) {
				for(int w=0;w<50;w++) {
					try {
						Random rand = new Random(); 
						int r = rand.nextInt(20) + 2; // we choose a random number and make thread sleep for that numbe of seconds.
						Thread.sleep(r*1000);
						
						double r2 = Math.random(); // this is used to choose a client randomly.
						if(id.equalsIgnoreCase("a")) {
							clientIds += r2>0.5?"b":"c"; // choosing a client
						}else if(id.equalsIgnoreCase("b")) {
							clientIds += r2>0.5?"a":"c";
						}else {
							clientIds += r2>0.5?"b":"a";
						}
						String messageToBeSentToServer = "multicast:"+id+":"+clientIds; // creating message to be sent to server for forwarding
						System.out.println("message to be sent "+messageToBeSentToServer);
						outStream.writeUTF(messageToBeSentToServer);
						clientIds = ""; // clear the all the client ids 
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(frame, "User does not exist anymore."); // if user doesn't exist then show message
					}
				}
			}
		}
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() { // initialize all the components of UI
		frame = new JFrame();
		frame.setBounds(100, 100, 518, 705);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Client View");

		clientMessageBoard = new JTextArea();
		clientMessageBoard.setEditable(false);
		clientMessageBoard.setBounds(12, 25, 284, 620);
		frame.getContentPane().add(clientMessageBoard);

		JButton clientSendMsgBtn = new JButton("START");
		clientSendMsgBtn.setFont(new Font("Bell MT", Font.PLAIN, 30));
		clientSendMsgBtn.addActionListener(new ActionListener() { // action to be taken on send message button
			public void actionPerformed(ActionEvent e) {
				new runAlgo().start();
			}
		});
		clientSendMsgBtn.setBounds(308, 390, 186, 118);
		frame.getContentPane().add(clientSendMsgBtn);

		clientActiveUsersList = new JList();
		clientActiveUsersList.setToolTipText("Active Users");
		clientActiveUsersList.setBounds(308, 57, 186, 319);
		frame.getContentPane().add(clientActiveUsersList);

		clientKillProcessBtn = new JButton("KILL");
		clientKillProcessBtn.setToolTipText("");
		clientKillProcessBtn.setFont(new Font("Bell MT", Font.PLAIN, 30));
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
		clientKillProcessBtn.setBounds(308, 521, 186, 124);
		frame.getContentPane().add(clientKillProcessBtn);

		JLabel lblNewLabel = new JLabel("Active Users");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel.setBounds(350, 28, 95, 16);
		frame.getContentPane().add(lblNewLabel);

		ButtonGroup btngrp = new ButtonGroup();

		frame.setVisible(true);
	}
}
