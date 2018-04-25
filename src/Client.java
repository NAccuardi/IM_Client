/**
 * Created by Robot Laptop on 4/24/2018.
 */
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.swing.*;

public class Client extends JFrame{

    private JTextField userText;//Where user will be typing the stuff.
    private JTextArea chatWindow;//Where the history will be displayed
    private ObjectOutputStream output;//sends things away. from client to server
    private ObjectInputStream input;
    private String message = "";
    private String serverIP;
    private String name;
    private Socket connection;

    private Encryptor myEncryptor = new Encryptor();

    private PublicKey myPublicKey;
    private PrivateKey myPrivateKey;

    private PublicKey serverPublicKey;

    //Constructor
    public Client(){//feed into it the IP of what we want to talk to.
        super("IM_Client");

        myPublicKey = myEncryptor.getPublicKey();
        myPrivateKey = myEncryptor.getPrivateKey();

        getUserInfo();

        //place user input text box at bottom of screen
        userText = new JTextField("Please type in your message here, and then press \"Enter\" to send message.");
        userText.setEditable(false);
        userText.addActionListener(
                e -> {
                    sendMessage(e.getActionCommand());
                    userText.setText("");
                }
        );
        add(userText,BorderLayout.SOUTH);

        //place chat box that at the top of the screen
        chatWindow = new JTextArea();
        add(new JScrollPane(chatWindow),BorderLayout.CENTER);
        setSize(300,150);
        setVisible(true);
        chatWindow.setLineWrap(true);
        chatWindow.setEditable(false);
        chatWindow.setBackground(Color.CYAN);
    }

    //connects to the server here.
    public void startRunning(){
        try{
            connectToTheServer();
            setUpInAndOutStreams();
            whileChatting();
        }catch (EOFException eofException){
            showMessage("\n Client Terminated Connection.");
        }catch (IOException ioException){
            ioException.printStackTrace();
        }finally {
            shutEverythingDown();
        }
    }

    //THis is what handles connecting to the server.
    private void connectToTheServer()throws IOException{
        showMessage("Attempting to connect to the server...\n");
        connection = new Socket(InetAddress.getByName(serverIP),6789);
        showMessage("Connected to: "+connection.getInetAddress().getHostName());

    }

    //Sets up the paths for the payloads to follow
    private void setUpInAndOutStreams()throws IOException{
        output = new ObjectOutputStream(connection.getOutputStream());
        output.flush();
        input = new ObjectInputStream(connection.getInputStream());
        exchangeKeys();
        showMessage("\nIn and out streams are connected.\n");
    }

    //this runs while the user is chatting
    private void whileChatting() throws IOException{
        ableToType(true);
        do {
            try {
                message = myEncryptor.getDecryptedMessage((byte[]) input.readObject());
                showMessage("\n" + message);
            } catch (ClassNotFoundException classNotFoundException){
                showMessage("\n Unknown Object Type.");
            }
        } while(!message.equals("SERVER - END"));
    }

    //Displays messages in the history window
    private void showMessage(final String payload){
        SwingUtilities.invokeLater(
                () -> chatWindow.append(payload)
        );
    }

    //this will handle sending messages to the server.
    private void sendMessage(String payload){
        try {
            output.writeObject(myEncryptor.encryptString(name + " - " + payload, serverPublicKey));
            output.flush();
            showMessage("\n" + name + " - "+ payload);
        } catch (IOException ioException){
            chatWindow.append("\n An error has occured while send a message");
        }
    }

    //Allows use to type messages in the text area
    private void ableToType(final boolean bool){
        SwingUtilities.invokeLater(
                () -> userText.setEditable(bool)
        );
    }

    //closes the program down at the end.
    private void shutEverythingDown(){
        showMessage("\n Closing the program down.");
        ableToType(false);
        try{
            output.close();
            input.close();
            connection.close();
        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    private void getUserInfo() {
        name = JOptionPane.showInputDialog("Enter your screen name:");
        serverIP = JOptionPane.showInputDialog("Enter IP address of the server:");
    }

    private void exchangeKeys() throws IOException{
        output.writeObject(myPublicKey);

        Object o;
        try {
            o = input.readObject();
        }
        catch (ClassNotFoundException e) {
            return;
        }

        if (o instanceof PublicKey) {
            serverPublicKey = (PublicKey)o;
        }
    }
}
