/**
 * Created by Robot Laptop on 4/24/2018.
 */
import  javax.swing.JFrame;


public class ClientTest {
    public static void main(String[] args){
        Client testClient;
        testClient = new Client();//for testing I am connecting to my local host
        testClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testClient.startRunning();
    }
}
