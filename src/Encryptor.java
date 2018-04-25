import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.security.*;

/**
 * Class: Encryptor
 * Allows messages to be encrypted and decrypted.
 *
 * @author Alex Hadi
 * @version April 24, 2018
 */
public class Encryptor {
    // This client's specific public and private keys.
    private PublicKey publicKey;
    private PrivateKey privateKey;

    // Constant for encryption and decryption cipher.
    private static final String CIPHER = "RSA/ECB/PKCS1Padding";

    /**
     * Constructor: Encryptor
     * Class is initialized with IP & port. PublicKey & PrivateKey are set.
     */
    public Encryptor() {
        // KeyPair initialized. Both PublicKey and PrivateKey are retrieved.
        KeyPair keyPair = getKeyPair();
        if (keyPair == null) {
            System.out.println("Could not get the KeyPair!");
            return;
        }
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    /**
     * Method: getKeyPair
     * Helper method to retrieve a KeyPair object. Uses RSA & SHA1PRNG.
     *
     * @return The KeyPair object that contains the PrivateKey and PublicKey.
     */
    private KeyPair getKeyPair() {
        KeyPairGenerator keyPairGenerator;
        SecureRandom secureRandom;

        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }

        // 1024 bits set as the key size.
        keyPairGenerator.initialize(1024, secureRandom);
        return keyPairGenerator.generateKeyPair();
    }

    public BufferedImage getDecryptedImageIcon(byte[] encryptedImage) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            return null;
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
        }
        catch (InvalidKeyException e) {
            return null;
        }

        byte[] decryptedImageBytes;
        try {
            decryptedImageBytes = cipher.doFinal(encryptedImage);
        }
        catch (IllegalBlockSizeException | BadPaddingException e) {
            return null;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(decryptedImageBytes);
        try {
            return ImageIO.read(bais);
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     * Method: getDecryptedMessage
     * Retrieves encrypted encryption from the server, decrypts it, and returns as a String.
     *
     * @return The encryption as a String.
     */
    public String getDecryptedMessage(byte[] encryptedMessage) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            return null;
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
        }
        catch (InvalidKeyException e) {
            return null;
        }

        byte[] decryptedMessage;
        try {
            decryptedMessage = cipher.doFinal(encryptedMessage);
        }
        catch (IllegalBlockSizeException | BadPaddingException e) {
            return null;
        }

        // To convert the byte[] to a String
        return new String(decryptedMessage);
    }

    public byte[] encryptImage(BufferedImage image, String imageExtension, PublicKey encryptionKey) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] imageBytes;
        try {
            ImageIO.write(image, imageExtension, baos);
            baos.flush();
            imageBytes = baos.toByteArray();
            baos.close();
        }
        catch (IOException e) {
            return null;
        }

        return getEncryptedBytes(imageBytes, encryptionKey);
    }

    /**
     * Method: encryptString
     * Private helper method that encrypts a string and returns it as a byte array.
     *
     * @param message The String that represents the encryption.
     * @return The encrypted string as a byte[].
     */
    public byte[] encryptString(String message, PublicKey encryptionKey) {
        byte[] messageBytes = message.getBytes();
        return getEncryptedBytes(messageBytes, encryptionKey);
    }

    private byte[] getEncryptedBytes(byte[] bytes, PublicKey encryptionKey) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            return null;
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        }
        catch (InvalidKeyException e) {
            return null;
        }

        try {
            return cipher.doFinal(bytes);
        }
        catch (IllegalBlockSizeException | BadPaddingException e) {
            return null;
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
