package phase1;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


public class LoginClient extends JFrame{

	/*
	 * References: https://www.youtube.com/watch?v=rd272SCl-XE
	 * 			   https://www.youtube.com/watch?v=ZzZeteJGncY
	 * */
	private JFrame frame;
	private JTextField clientUserName;
	private int port = 8818;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) { // main function which will make UI visible
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LoginClient window = new LoginClient();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public LoginClient() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() { // it will initialize the components of UI
		frame = new JFrame();
		frame.setBounds(100, 100, 619, 342);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Client Register");

		clientUserName = new JTextField();
		clientUserName.setBounds(207, 50, 276, 61);
		frame.getContentPane().add(clientUserName);
		clientUserName.setColumns(10);

		JButton clientLoginBtn = new JButton("Connect");
		clientLoginBtn.addActionListener(new ActionListener() { //action will be taken on clicking login button
			public void actionPerformed(ActionEvent e) {
				try {
					String id = clientUserName.getText(); // username entered by user
					Socket s = new Socket("localhost", port); // create a socket
					DataInputStream inputStream = new DataInputStream(s.getInputStream()); // create input and output stream
					DataOutputStream outStream = new DataOutputStream(s.getOutputStream());
					outStream.writeUTF(id); // send username to the output stream
					
					String msgFromServer = new DataInputStream(s.getInputStream()).readUTF(); // receive message on socket
					if(msgFromServer.equals("Username already taken")) {//if server sent this message then prompt user to enter other username
						JOptionPane.showMessageDialog(frame,  "Username already taken\n"); // show message in other dialog box
					}else {
						new ClientView(id, s); // otherwise just create a new thread of Client view and close the register jframe
						frame.dispose();
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		
		clientLoginBtn.setFont(new Font("Tahoma", Font.PLAIN, 17));
		clientLoginBtn.setBounds(207, 139, 132, 61);
		frame.getContentPane().add(clientLoginBtn);

		JLabel lblNewLabel = new JLabel("Username");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setBounds(44, 55, 132, 47);
		frame.getContentPane().add(lblNewLabel);
	}

	
}
