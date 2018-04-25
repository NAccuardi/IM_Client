/**
 * Created by Robot Laptop on 4/24/2018.
 */
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
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

    //Constructor
    public Client(){//feed into it the IP of what we want to talk to.
        super("IM_Client");
        getUserInfo();
        userText = new JTextField();
        userText.setEditable(false);
        userText.addActionListener(
                e -> {
                    sendMessage(e.getActionCommand());
                    userText.setText("");
                }
        );
        add(userText,BorderLayout.NORTH);
        chatWindow = new JTextArea();
        add(new JScrollPane(chatWindow),BorderLayout.CENTER);
        setSize(300,150);
        setVisible(true);
        chatWindow.setEditable(false);
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
        showMessage("\nIn and out streams are connected.\n");
    }

    //this runs while the user is chatting
    private void whileChatting() throws IOException{
        ableToType(true);
        do{
            try{
                message = (String) input.readObject();
                showMessage("\n" + message);
            }catch (ClassNotFoundException classNotFoundException){
                showMessage("\n Unknown Object Type.");
            }
        }while(!message.equals("SERVER - END"));
    }

    //Displays messages in the history window
    private void showMessage(final String payload){
        SwingUtilities.invokeLater(
                () -> chatWindow.append(payload)
        );
    }

    //this will handle sending messages to the server.
    private void sendMessage(String payload){
        try{
            output.writeObject(name + " - " + payload);
            output.flush();
            showMessage("\n" + name + " - "+ payload);
        }catch (IOException ioException){
            chatWindow.append("\n An error has occured while send a message");
        }
    }

    //Allows use to type messesges in the text area
    private void ableToType(final boolean bool){
        SwingUtilities.invokeLater(
                () -> userText.setEditable(bool)
        );
    }

    //closes the rogram down at the end.
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

}
