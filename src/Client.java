import java.io.*;
import java.net.*;
import java.awt.*;
import java.security.PublicKey;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

/**
 * Class: Client
 * Code for the client to function properly.
 * @author Nick Accuardi
 * @author Alex Hadi
 * @author Mitchell Nguyen
 * @author Patrick Maloney
 */
public class Client extends JFrame{
    private JTextField userText;  // Where the user types.
    private JTextPane chatWindow; // Where the history is displayed
    private JButton imageButton; // Button to send images.

    // Streams to send data to and from the client.
    private ObjectOutputStream output;
    private ObjectInputStream input;

    private String message = ""; // The current message.
    private String serverIP; // The IP address.
    private String name;
    private Socket connection; // The Socket for sending data.
    private Encryptor myEncryptor = new Encryptor(); // For encrypting and decrypting messages.

    // My PublicKey & the server's PublicKey.
    private PublicKey myPublicKey;
    private PublicKey serverPublicKey;

    /**
     * Constructor: Client
     * Initializes the Client GUI.
     */
    public Client(){//feed into it the IP of what we want to talk to.
        super("IM_Client");
        myPublicKey = myEncryptor.getPublicKey();
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
        setSize(500,500);
        setVisible(true);
        chatWindow.setEditable(false);

        //place add-image button
        imageButton = new JButton();
        setVisible(true);
        imageButton.setText("SEND IMAGE");
        imageButton.setEnabled(false);
        imageButton.addActionListener(
                e -> {
                    String imagePath = openImageDialogAndReturnPath();
                    if (imagePath == null) return;
                    try {
                        sendImage(new ImageIcon(ImageIO.read(new File(imagePath))));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
        );
        add(imageButton, BorderLayout.EAST);
    }

    /**
     * Method: startRunning
     * Connects to the server.
     */
    public void startRunning(){
        try {
            connectToTheServer();
            setUpInAndOutStreams();
            whileChatting();
        } catch (EOFException eofException){
            showMessage("\n Client Terminated Connection.");
        } catch (IOException ioException){
            ioException.printStackTrace();
        } finally {
            shutEverythingDown();
        }
    }

    /**
     * Method: connectToTheServer
     * This is what handles connecting to the server.
     */
    private void connectToTheServer() {
        showMessage("Attempting to connect to the server...\n");
        try {
            connection = new Socket(InetAddress.getByName(serverIP),6789);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        showMessage("\nConnected to: " + connection.getInetAddress().getHostName());
    }

    /**
     * Method: setUpInAndOutStreams
     * Sets up the paths for the payloads to follow.
     */
    private void setUpInAndOutStreams() {
        try {
            output = new ObjectOutputStream(connection.getOutputStream());
            output.flush();
            input = new ObjectInputStream(connection.getInputStream());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        exchangeKeys();
        showMessage("\nIn and out streams are connected.\n");
    }

    //this runs while the user is chatting
    private void whileChatting() throws IOException{
        ableToType(true);
        do {
            try {
                boolean isImage = input.readBoolean();
                ImageIcon image;
                if (isImage) {
                    image = (ImageIcon)input.readObject();

                    // send Server's message with an image
                    showMessage("\n" + "Server" + " - ");
                    showIconOnChatWindow(image);
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
            appendString("\n An error has occurred while send a message");
        }
    }

    private void sendImage(ImageIcon imageToSend) {
        try {
            output.writeBoolean(true);
            output.writeObject(imageToSend);
            output.flush();

            showMessage("\n"+name+" - ");
            showIconOnChatWindow(imageToSend);
        } catch (Exception e){
            appendString("\n ERROR: IMAGE UNABLE TO BE SENT");
        }
    }

    private void showIconOnChatWindow(final ImageIcon icon) {
        SwingUtilities.invokeLater(
                () -> chatWindow.insertIcon(getScaledIcon(icon))
        );
    }

    private ImageIcon getScaledIcon(ImageIcon icon) {
        double scaleFactor = 200.0 / (float)icon.getIconHeight();
        int width = (int)(scaleFactor * icon.getIconWidth());
        Image scaledImage = icon.getImage().getScaledInstance(width, 200, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
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

    /**
     * Method: exchangeKeys
     * PublicKeys are exchanged between client and server.
     */
    private void exchangeKeys() {
        try {
            // Client writes the key out first.
            output.writeObject(myPublicKey);
            Object o = input.readObject();
            if (o instanceof PublicKey) {
                serverPublicKey = (PublicKey)o;
            }
        }
        catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method: appendString
     * Adds a given string to the chat window
     * @param str The string to append.
     */
    private void appendString(String str) {
        // Need StyledDocument to insert the string.
        StyledDocument styledDocument = chatWindow.getStyledDocument();
        try {
            styledDocument.insertString(styledDocument.getLength(), str, null);
        }
        catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method: openImageDialogAndReturnPath
     * Opens a JFileChooser and returns the path to the image.
     * @return The string that represents the image path.
     */
    private String openImageDialogAndReturnPath() {
        // Frame and file chooser are instantiated.
        JFrame imageDialogFrame = new JFrame();
        JFileChooser imageChooser = new JFileChooser();

        // Filter out only images.
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
                "JPG, JPEG, & PNG images", "jpg", "jpeg", "png");
        imageChooser.setFileFilter(imageFilter);

        // Wait for either approval from user or cancel operation.
        switch (imageChooser.showOpenDialog(imageDialogFrame)) {
            case JFileChooser.APPROVE_OPTION:
                System.out.println("This image opened: " + imageChooser.getSelectedFile().getName());
                return imageChooser.getSelectedFile().getPath();
            case JFileChooser.CANCEL_OPTION:
                System.out.println("Open image operation cancelled.");
                break;
        }
        return null;
    }
}
