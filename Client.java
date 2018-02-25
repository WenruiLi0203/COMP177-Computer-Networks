/* Client.Java
Author : Wenrui Li
Last editing date: 11/20/2017
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
public class Client {
	private Socket socketClient;
	private PrintWriter out;
	private BufferedReader stdIn;
	private volatile AtomicBoolean isFinished = new AtomicBoolean();
	public String linein=null;
	private boolean auth=false;
	Scanner sysIn = new Scanner(System.in);
	static ArrayList <String> ignore = new ArrayList<String>();
	
	public static void main(String argv[]) throws Exception
    {
		   Client test = new Client();
		   addignore();//Server messages ignored words.
		   test.connectServer();
		   test.options();//Client menu.
		   System.exit(0);

    }
	
	//connect to the server
	public void connectServer() throws Exception {
		stdIn =new BufferedReader(new InputStreamReader(System.in));
		try {
			socketClient = new Socket("localhost",8006);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.out.println("Invalid hostname/port number.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// send client message thread.
		final Thread outThread = new Thread() {
			@Override
			public void run() {
				//System.out.println("Connected to: "+hostName+", "+port);
				try {
					out = new PrintWriter(socketClient.getOutputStream(),true);
					out.println("HELLO");//initialize connection.
					out.flush();
					
				} catch (IOException e) {
					e.printStackTrace();
				} 
			};
		};
		outThread.start();
		
		//receive server message thread.
		final Thread inThread = new Thread() {
			@Override
			public void run() {
				// Use a Scanner to read from the remote server
				Scanner in = null;
				
				try {
					in = new Scanner(socketClient.getInputStream());
					linein = in.nextLine();
					while (!isFinished.get()) {
						// change/ignore received server message printouts.
						if(linein.contains("SIGNIN")) {
							System.out.println(linein.substring(7, linein.length())+" signed in.");
						}else if(linein.contains("SIGNOFF")) {
							System.out.println(linein.substring(8, linein.length())+" signed off.");
						}else if(linein.contains("FROM")) {
							System.out.println("Message from "+ linein.substring(5, linein.length()));
						}
						//ignore
						else if(!ignore.contains(linein)) {
							System.out.println(linein);
						}
						//get next line.
						linein = in.nextLine();
						//check login boolean.
						if(linein.equals("AUTHYES")) {
							auth = true;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		};
		inThread.start();
		Thread.sleep(300);//delay for receiving server messages.
		login();
	}
	
	public void login() throws InterruptedException {
		String LoginName,Password;
	    while(!auth) {
		    System.out.println("Please enter a username:");
		    LoginName = sysIn.nextLine();
		    System.out.println("Please enter a password:");
		    Password=sysIn.nextLine();
		    out.println("AUTH:"+LoginName+":"+Password);
		    Thread.sleep(300);//delay for receiving server messages.
		    if(!auth) {
		    	System.out.println("Incorrect username and/or password.");
		    }
	    }
	    System.out.println("You are now authenticated.");
	}
	
	//client menu
	public void options() throws IOException, InterruptedException {
		String command = null;
		String sUser,sMsg;
		int clientoptions = 0;

    	while(clientoptions!=3) {// 3 = sign off.

    		Thread.sleep(300);//delay for server input.
			optionsmenu();//print out 3 options.
	    	command=stdIn.readLine();

	    	if((!command.equals("1"))&&(!command.equals("2"))&&(!command.equals("3"))) {//input check
	    		System.out.println("Invaid input.");
	    	}else {
	    		clientoptions=Integer.parseInt(command);
	    		
		    	//LIST
		    	if(clientoptions==1) {
		    		out.println("LIST");
		    		System.out.println("Users currently logged in:");
		    	}
		    	//SEND MSG
		    	else if(clientoptions==2) {
		    		System.out.println("User you would like to message:");
		    		sUser=stdIn.readLine();
		    		System.out.println("Message:");
		    		sMsg=stdIn.readLine();
		    		out.println("TO:"+sUser+":"+sMsg);
		    		System.out.println("Message sent.");
		    	}
		    	//SIGN OFF
		    	else if(clientoptions==3) {
		    		System.out.println("Signing off...");
		    		out.println("BYE");
		    	    socketClient.close();
		    	}
	    	}
    	}
	}
	
	//client menu printout
	public void optionsmenu() {
    	System.out.println("Choose an option:");
    	System.out.println("1. List online users");
    	System.out.println("2. Send someone a message");
    	System.out.println("3. Sign off\r");
	}
	
	//ignored terms that should not print out.
	public static void addignore() {
	   ignore.add("HELLO");
	   ignore.add("AUTHYES");
	   ignore.add("AUTHNO");
	}
}

