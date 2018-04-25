/**
 * Created by Robot Laptop on 4/24/2018.
 */
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

public class Client extends JFrame{

    private JTextField userText;//Where user will be typing the stuff.
    private JTextPane chatWindow;//Where the history will be displayed

    private JButton imageButton;
    private String imagePath;

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
        userText = new JTextField();
        userText.setEditable(false);
        userText.addActionListener(
                e -> {
                    sendMessage(e.getActionCommand());
                    userText.setText("");
                }
        );
        add(userText,BorderLayout.SOUTH);

        //place chat box that at the top of the screen
        chatWindow = new JTextPane();
        add(new JScrollPane(chatWindow),BorderLayout.CENTER);
        setSize(300,150);
        setVisible(true);
        chatWindow.setEditable(false);

        //place add-image button
        imageButton = new JButton();
        setVisible(true);
        imageButton.setEnabled(false);
        imageButton.addActionListener
                (
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                String imagePath = openImageDialog();

                                int i = imagePath.lastIndexOf(".");
                                String imageExtension = imagePath.substring(i+1);


                                BufferedImage bufferedImg;
                                try {
                                    bufferedImg = ImageIO.read(new File(imagePath));
                                } catch (IOException ioe) {
                                    return;
                                }

                                ImageIcon icon = new ImageIcon(bufferedImg);
//                            ChatWindow.insertIcon(icon);
                                sendImage(bufferedImg, imageExtension, icon);



                            }
                        }
                );
        add(imageButton, BorderLayout.EAST);
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
                boolean isImage = input.readBoolean();
                BufferedImage image;
                if (isImage) {
                    image = myEncryptor.getDecryptedImageIcon((byte[])input.readObject());
                    showIcon(new ImageIcon(image));
                }
                else{
                    message = myEncryptor.getDecryptedMessage((byte[]) input.readObject());
                    showMessage("\n" + message);
                }
            } catch (ClassNotFoundException classNotFoundException){
                showMessage("\n Unknown Object Type.");
            }
        } while(!message.equals("SERVER - END"));
    }

    //Displays messages in the history window
    private void showMessage(final String payload){
        SwingUtilities.invokeLater(
                () -> appendString(payload)
        );
    }

    //this will handle sending messages to the server.
    private void sendMessage(String payload){
        try {
            output.writeBoolean(false);
            output.writeObject(myEncryptor.encryptString(name + " - " + payload, serverPublicKey));
            output.flush();
            showMessage("\n" + name + " - "+ payload);
        } catch (IOException ioException){
            appendString("\n An error has occured while send a message");
        }
    }

    private void sendImage(BufferedImage img, String imgPathExtension, ImageIcon icon) {
        try {
            output.writeBoolean(true);
            output.writeObject(myEncryptor.encryptImage(img, imgPathExtension, serverPublicKey));
            output.flush();

            showMessage("\n"+name+" ");
            showIcon(icon);
        } catch (Exception e){
            appendString("\n ERROR: IMAGE UNABLE TO BE SENT");
        }
    }

    private void showIcon(final ImageIcon icon) {
        SwingUtilities.invokeLater(
                () -> chatWindow.insertIcon(icon)
        );

    }

    //Allows use to type messages in the text area
    private void ableToType(final boolean bool){
        SwingUtilities.invokeLater(
                () -> setUIEnabled(bool)
        );
    }

    private void setUIEnabled(boolean enabled) {
        imageButton.setEnabled(enabled);
        userText.setEditable(enabled);
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

    private void appendString(String str) {
        StyledDocument doc = (StyledDocument) chatWindow.getDocument();
        try {
            doc.insertString(doc.getLength(), str, null);
        } catch (BadLocationException e) {
            // uh oh.
        }
    }

    private String openImageDialog() {
        JFrame frame = new JFrame();
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JPG, JPEG, & PNG images", "jpg", "jpeg", "png");
        chooser.setFileFilter(filter);

        int returnVal = chooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("You chose to open this file: " + chooser.getSelectedFile().getName());

            return chooser.getSelectedFile().getPath();

        } else if (returnVal == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        return null;
    }
}
