/* Server.Java
Author : Wenrui Li
Last editing date: 12/1/2017
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
 
public class SocketServer extends Thread {
    public static final int PORT_NUMBER = 8005;
    
    static ServerSocket server = null;
    protected static Socket socket = null;
    
    InputStream in = null;
    OutputStream out = null;
    
    public String getuser=null;
    static SocketServer client;
    
    static ArrayList <String> usernameAndpassword = new ArrayList<String>();
    final static List<SocketServer> clients = new ArrayList<>();
    
    //create server socket and start
    private SocketServer(Socket socket) {
        this.socket = socket;
        System.out.println("New client connected from " + socket.getInetAddress().getHostAddress());
        start();
    }
    
    //main
    public static void main(String[] args) throws IOException {
        System.out.println("SocketServer Example");
        test_user_and_password(); //add test user and password in the server.
        try {
            server = new ServerSocket(PORT_NUMBER);
            while (true) {
                client = new SocketServer(server.accept());
            }
        } catch (IOException ex) {
            System.out.println("Unable to start server.");
        } finally {
            try {
                if (server != null)
                    server.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    //run loop
    public void run() {
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String request;//client input
            
            //HELLO method part
            request = br.readLine();
            inital(request);
            
            //AUTH method part
            request = br.readLine();
            boolean authyes = false;
            authyes = login(request);
            while(!authyes) {
            	request = br.readLine();
            	authyes = login(request);
            }
            
            //SIGNED IN
            request = br.readLine();
            while(!request.equals("BYE")) {
            	if(request.equals("LIST")) {//LIST method part
            		listuser();
            		request = br.readLine();
            	}else if(request.substring(0,3).equals("TO:")) {//SEND message part
            		sendmessage(request);
            		request = br.readLine();
            	}else {
            		break;
            	}
            }
            out.write("BYE\r\n".getBytes());
 
        } catch (IOException ex) {
            System.out.println("Unable to get streams from client");
        } finally {
            try {
            	clients.remove(client);
    			for(SocketServer client : clients) {
    				client.out.write(("SIGNOFF: "+getuser+"\r\n").getBytes());
    			}
                in.close();
                out.close();
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    //add list of usernames and passwords.
	public static void test_user_and_password() {
		usernameAndpassword.add("test1.p000");
		usernameAndpassword.add("test2.p000");
		usernameAndpassword.add("test3.p000");
		usernameAndpassword.add("test4.p000");
	}
	
	//HELLO method 
    public void inital(String request) throws IOException {
    	if(request.equals("HELLO")) {
    		out.write("HELLO\r\n".getBytes());
    	}else {
    		out.write("Connection closed by foreign host.\r\n".getBytes());
    		server.close();
    	}
    }

    //AUTH method
    public boolean login(String request) throws IOException {
    	String[] parts = null;
    	String getpw=null;
    	String authpart = null;
    	try {
    		parts = request.split("\\:");//Separate client input to three parts by ":"
    		authpart = parts[0];
    		getuser = parts[1];
    		getpw = parts[2];
    		
        	if(authpart.equals("AUTH")) {
        		boolean auth = authentication(getuser, getpw);//check username and password
        		if(auth) {
        			out.write("AUTHYES\r\n".getBytes());
        			client.setName(getuser);
        			clients.add(client);
        			//System.out.println(client.getName()+"\r\n");
        			for(SocketServer client : clients) {//print out to all clients that someone sign in.
        				client.out.write(("SIGNIN: "+getuser+"\r\n").getBytes());
        			}
        			//System.out.println("SIGNIN:"+getuser);
        			return true;
        		}else {
        			out.write("AUTHNO\r\n".getBytes());
        			return false;
        		}
        	}
    	}catch(Exception e){
    		out.write("Connection closed by foreign host.\r\n".getBytes());
    		server.close();

        }
    	return false;
    }
    
    //check username and password exist
    public boolean authentication(String username, String password) {
    	String combin = username+"."+password;
    	for(String string : usernameAndpassword) {
    		if(string.equals(combin)) {
    			return true;
    		}
    	}
		return false;
    }
    
    //LIST method
    public void listuser() throws IOException {
    	for(SocketServer client : clients) {
    		out.write(client.getName().getBytes());
    		out.write(",".getBytes());
    	}
    	out.write("\r\n".getBytes());
    	//System.out.println(clients.toString());
    }
    
    //Send message method
    public void sendmessage(String request) throws IOException {
    	String[] parts = null;
    	String user=null;
    	String message = null;
    	int index = -1;
    	try {
    		parts = request.split("\\:");//separate client input to three parts by ":"
    		user = parts[1];
    		message = parts[2];
    		index = findUserIndex(user);//find the clients list index.
    		if(index!=-1) {
    			clients.get(index).out.write(("FROM:"+getuser+":"+message+"\r\n").getBytes());//print out the message at receiver.
    		}
    	}catch(Exception e){
    		out.write("Send Message Error.\r\n".getBytes());
        }
    }
    
    //find client list index by username
    public int findUserIndex(String username) {
    	for(SocketServer client : clients) {
    		if(client.getName().equals(username)) {
    			return clients.indexOf(client);
    		}
    	}
		return -1;
    }

}
