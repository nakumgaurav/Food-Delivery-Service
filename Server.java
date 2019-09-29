// Server-side implementation of the online food ordering service

import java.net.*;
import java.io.*;
import java.time.LocalDate;
import java.text.*;
import java.util.concurrent.Semaphore;

import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

// add this time to subsequent orders
class SharedResources{
	// the currTime variable stores the time at which a client places an order
	// (updated in the ClientHandler class). It is used to update the delivery
	// times of subsequent client requests
	static long currTime = System.currentTimeMillis();

	// creating a Semaphore object for time and stock checking
    // with number of permits 1
	static Semaphore sem = new Semaphore(1);
	// semaphore for Sales.csv
	static Semaphore semFile = new Semaphore(1);
	// semaphore for SalesDaily.csv
	static Semaphore semFileDaily = new Semaphore(1);
	// semaphore for SalesMonthly.csv
	static Semaphore semFileMonthly = new Semaphore(1);
	// semaphore for StockCheck.csv
	static Semaphore semStockCheck = new Semaphore(1);
	
	// stock of cookies and snacks is limited
	static int stockCookie = 100;
	static int stockSnacks = 100;
	// if the stock of snacks/cookies falls below the threshold level, they are added to
	// the StockCheck.csv file
	final static int stockThreshold = 10;
	
	// list of all items on the menu
	static ArrayList<String> items = new ArrayList<>(Arrays.asList("Tea", "Coffee", "Choco Cookie", "Snacks"));
}



public class Server
{
	public static void main(String[] args) throws IOException
	{		
		
//		ArrayList<String> items = new ArrayList<>(Arrays.asList("te", "123"));
		ExecutorService pool = Executors.newCachedThreadPool();

		// Create CSVReader and CSVWriter objects for handling the sales list file
		String filename = "Sales.csv";
		String filenameDaily = "SalesDaily.csv";
		String filenameMonthly = "SalesMonthly.csv";
		String filenameStock = "StockCheck.csv";
		
		// server is listening on port 9000
		ServerSocket ss = new ServerSocket(9000);
		System.out.println("Server Ready. Waiting for a Client Request");
		
		// get client requests
		while(true) {
			Socket s = null;
			try {
				// socket object to receive incoming client requests
				s = ss.accept();
				System.out.println("A new client is connected : " + s);

				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
//				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());

				// Create a task corresponding to this client request
				Runnable newClientTask = new ClientHandler(s, ois, dos, filename, filenameDaily, filenameMonthly, filenameStock);
				
				System.out.println("Creating a new thread for this client");
				
				// launch the client request in new thread
				pool.execute(newClientTask);
			}
			catch (Exception e){
				s.close();
				e.printStackTrace();
			}
		}
		
	}
}

class ClientHandler implements Runnable{
	// socket stream objects
	private ObjectInputStream ois;
	private DataOutputStream dos;
//	final ObjectOutputStream oos;
	private Socket s;
	
	// names of files to be updated
	private String filename;
	private String filenameDaily;
	private String filenameMonthly;
	private String filenameStock;

	// HashMap to receive OrderData from the client
	private HashMap<String, ArrayList<String>> orderData;
	
	// date at which order is placed
	private LocalDate date;
	
    //Delimiter used in CSV file
    private static final String COMMA_DELIMITER = ", ";
    private static final String NEW_LINE_SEPARATOR = "\n";
    
    private int reached = 0;

    public ClientHandler(Socket s, ObjectInputStream ois, DataOutputStream dos, String filename, String filenameDaily, String filenameMonthly, String filenameStock){
    	this.s = s;
    	this.ois = ois;
//    	this.oos = oos;
    	this.dos = dos;
    	this.filename = filename;
    	this.filenameDaily = filenameDaily;
    	this.filenameMonthly = filenameMonthly;
    	this.filenameStock = filenameStock;
    	this.date = LocalDate.now();
    	
    	// if needed to check the updation of SalesDaily and Sales Monthly files,
    	// uncomment these lines
//    	date = date.plusDays(1);
//		date = date.plusMonths(1);

    }
    
    // updates the sales list files
    private class FileUpdater implements Runnable{
    	private HashMap<String, ArrayList<String>> fileData;
    	public FileUpdater(HashMap<String, ArrayList<String>> fileData) {
    		this.fileData = fileData;
    	}
    	
    	public int itemDecode(String item) {
    		if(item.equals("Tea"))
    			return 1;
    		if(item.equals("Coffee"))
    			return 2;
    		if(item.equals("Choco Cookie"))
    			return 3;
    		if(item.equals("Snacks"))
    			return 4;
    		
    		return -1;
    	}
    	
    	@Override
    	public void run() {
    		String strDate = date.toString();
    		System.out.println("Updating files in background...");
    		try {
    		// Update the sales file (Sales.csv)
    		SharedResources.semFile.acquire();
    		try {
    	    	FileWriter filewriter = new FileWriter(filename, true); // setting the append option to true
    			
    			System.out.println("Now updating Sales.csv");
	    		for(String item:SharedResources.items) {
	    			if(fileData.containsKey(item)) {
//	    				System.out.println("Now adding date to file");
	    				filewriter.append(strDate);
			    		filewriter.append(COMMA_DELIMITER);
//			    		System.out.println("Now adding name to file");
			    		filewriter.append(fileData.get("Name").get(0));

			    		filewriter.append(COMMA_DELIMITER);
//			    		System.out.println("Now adding addrr to file");
			    		filewriter.append(fileData.get("Addrr").get(0));
			    		
			    		filewriter.append(COMMA_DELIMITER);
//			    		System.out.println("Now adding" + item +  "to file");
			    		filewriter.append(item);
			    		
			    		filewriter.append(COMMA_DELIMITER);
//			    		System.out.println("Now adding rate to file");
			    		filewriter.append(fileData.get(item).get(0));
			    		
			    		filewriter.append(COMMA_DELIMITER);
//			    		System.out.println("Now adding quant to file");
			    		filewriter.append(fileData.get(item).get(1));
			    		
			    		filewriter.append(COMMA_DELIMITER);
//			    		System.out.println("Now adding price to file");
			    		filewriter.append(fileData.get(item).get(2));
			    		
			    		filewriter.append(NEW_LINE_SEPARATOR);
	    			}
	    		}   
	    		filewriter.close();
    		}
    		catch(IOException e) {System.out.println("IOERROR");}
    		SharedResources.semFile.release();
    		} catch(InterruptedException e) {}
/***********************************************************************************************************************************************************/ 
    		try {
    		// Update the daily sales file
    		SharedResources.semFileDaily.acquire();
    		try {
	    	BufferedReader readerDaily= new BufferedReader(new FileReader(filenameDaily));		    			
    			
    		System.out.println("Now updating SalesDaily.csv");
    		// create a temp buffered reader, since we need to read the daily sales file twice
    		File inputFile = new File("SalesDaily.csv");
    		BufferedReader tempReader = new BufferedReader(new FileReader(inputFile));
    		
    		// check if the date of the current order is the same as the last order
    		tempReader.readLine(); // skip the header line

    		String lastLine = "";
    		String sCurrentLine;
    		// while you reach the last line (date of last order)
    		while((sCurrentLine = tempReader.readLine()) != null) {
    			lastLine = sCurrentLine;
    		}
    		
    		// create a new temporary file and write all lines from SalesDaily.csv file
    		// to temp file except the last one (since it may be modified)
    		File tempFile = new File("myTemp.csv");
    		BufferedWriter tempWriter = new BufferedWriter(new FileWriter(tempFile));
    		
    		while((sCurrentLine = readerDaily.readLine()) != null && !sCurrentLine.equals(lastLine)) {
    			tempWriter.write(sCurrentLine + System.getProperty("line.separator"));
    		}
    		
    		if(lastLine.length()>0) {
    			lastLine = lastLine.trim();
    			String[] arrLine = lastLine.split(",");
    			LocalDate lastDate = LocalDate.parse(arrLine[0]);

    			// if the date of the current order is the same as the last order
    			if(date.equals(lastDate)) {
    				// update the array arrLine
    				for(String item:SharedResources.items) {
    	    			if(fileData.containsKey(item)) {
    	    				int itemCode = itemDecode(item);
    	    				arrLine[itemCode] = String.valueOf(Integer.parseInt(arrLine[itemCode].trim()) +  Integer.parseInt(fileData.get(item).get(1)));
    	    			}
    				}
    				arrLine[5] = String.valueOf(Integer.parseInt(arrLine[5].trim()) + Integer.parseInt(fileData.get("totPrice").get(0)));
    				
    				lastLine = "";
    				// write the updated results back to the file
    				for(int i=0; i<5; i++)
    					lastLine += arrLine[i].trim() + COMMA_DELIMITER;
    				lastLine += arrLine[5].trim();
    				
//    				System.out.println("My new last line =" + lastLine);
    				tempWriter.write(lastLine + System.getProperty("line.separator"));
        		}
    			
    			// else create a new entry for the current date
    			else {
    				// first write the lastLine to the temp file
    				tempWriter.write(lastLine + System.getProperty("line.separator"));
    				
    				// now make the new last line
    				lastLine = strDate + COMMA_DELIMITER;
    				for(String item:SharedResources.items) {
    	    			if(fileData.containsKey(item)) {
    	    				lastLine += fileData.get(item).get(1) + COMMA_DELIMITER;
    	    			}
    	    			else{
    	    				lastLine += "0" + COMMA_DELIMITER;
    	    			}
    				}
    				lastLine += fileData.get("totPrice").get(0);
    				
    				tempWriter.write(lastLine + System.getProperty("line.separator"));
    			}
   
    		}
    			
    		// else, this is the first entry in the file
    		else {
				// now make the new last line
				lastLine = strDate + COMMA_DELIMITER;
				for(String item:SharedResources.items) {
	    			if(fileData.containsKey(item)) {
	    				lastLine += fileData.get(item).get(1) + COMMA_DELIMITER;
	    			}
	    			else{
	    				lastLine += "0" + COMMA_DELIMITER;
	    			}
				}
				lastLine += fileData.get("totPrice").get(0);
    			
    			tempWriter.write(lastLine + System.getProperty("line.separator"));
    		}
    		tempWriter.close();
    		tempReader.close();
    		readerDaily.close();
    		
    		inputFile.delete();
    		boolean success = tempFile.renameTo(inputFile);
    		}
    		catch(FileNotFoundException e1) {System.out.println("File not found!");}
    		catch(IOException e2) {System.out.println("IO Exception!");e2.printStackTrace();}
    		
    		SharedResources.semFileDaily.release();
    		} catch(InterruptedException e) {}
   /***********************************************************************************************************************************************************/ 		
    		try {
    		// update the monthly sales file
    		SharedResources.semFileMonthly.acquire();
    		try {
			BufferedReader readerMonthly = new BufferedReader(new FileReader(filenameMonthly));
			
			System.out.println("Now updating SalesMonthly.csv");
    		// create a temp buffered reader, since we need to read the daily sales file twice
    		File inputFile2 = new File("SalesMonthly.csv");
    		BufferedReader tempReader2 = new BufferedReader(new FileReader(inputFile2));
    		
    		// check if the month of the current order is the same as the last order
    		tempReader2.readLine(); // skip the header line

    		String lastLine = "";
    		String sCurrentLine;
    		// while you reach the last line (date of last order)
    		while((sCurrentLine = tempReader2.readLine()) != null) {
    			lastLine = sCurrentLine;
    		}
    		
    		// create a new temporary file and write all lines from SalesDaily.csv file
    		// to temp file except the last one (since it may be modified)
    		File tempFile2 = new File("myTemp2.csv");
    		BufferedWriter tempWriter2 = new BufferedWriter(new FileWriter(tempFile2));
    		
    		while((sCurrentLine = readerMonthly.readLine()) != null && !sCurrentLine.equals(lastLine)) {
    			tempWriter2.write(sCurrentLine + System.getProperty("line.separator"));
    		}
    		
    		if(lastLine.length()>0) {
    			lastLine = lastLine.trim();
    			String[] arrLine = lastLine.split(",");

    			String lastMonth = arrLine[0].split("-")[1];
    			String[] currArray = strDate.split("-");
    			String currMonth = currArray[1];
    			
    			// if the month of the current order is the same as the last order
    			if(currMonth.equals(lastMonth)) {
    				// update the array arrLine
    				for(String item:SharedResources.items) {
    	    			if(fileData.containsKey(item)) {
    	    				int itemCode = itemDecode(item);
    	    				arrLine[itemCode] = String.valueOf(Integer.parseInt(arrLine[itemCode].trim()) +  Integer.parseInt(fileData.get(item).get(1)));
    	    			}
    				}
    				arrLine[5] = String.valueOf(Integer.parseInt(arrLine[5].trim()) + Integer.parseInt(fileData.get("totPrice").get(0)));
    				
    				lastLine = "";
    				// write the updated results back to the file
    				for(int i=0; i<5; i++)
    					lastLine += arrLine[i].trim() + COMMA_DELIMITER;
    				lastLine += arrLine[5].trim();
    				
    				tempWriter2.write(lastLine + System.getProperty("line.separator"));
        		}
    			
    			// else create a new entry for the current month
    			else {
    				// first write the lastLine to the temp file
    				tempWriter2.write(lastLine + System.getProperty("line.separator"));
    				
    				// now make the new last line
    				lastLine = currArray[0]+"-"+currArray[1] + COMMA_DELIMITER;
    				for(String item:SharedResources.items) {
    	    			if(fileData.containsKey(item)) {
    	    				lastLine += fileData.get(item).get(1) + COMMA_DELIMITER;
    	    			}
    	    			else{
    	    				lastLine += "0" + COMMA_DELIMITER;
    	    			}
    				}
    				lastLine += fileData.get("totPrice").get(0);
    				
    				tempWriter2.write(lastLine + System.getProperty("line.separator"));
    			}
   
    		}
    			
    		// else, this is the first entry in the file
    		else {
    			String[] currArray = strDate.split("-");
    			
				// now make the new last line
				lastLine = currArray[0].trim()+"-"+currArray[1].trim() + COMMA_DELIMITER;
				for(String item:SharedResources.items) {
	    			if(fileData.containsKey(item)) {
	    				lastLine += fileData.get(item).get(1) + COMMA_DELIMITER;
	    			}
	    			else{
	    				lastLine += "0" + COMMA_DELIMITER;
	    			}
				}
				lastLine += fileData.get("totPrice").get(0);
				
				tempWriter2.write(lastLine + System.getProperty("line.separator"));
    		}
    		tempWriter2.close();
    		tempReader2.close();
    		readerMonthly.close();
    		
    		inputFile2.delete();
    		boolean success = tempFile2.renameTo(inputFile2);

    		}
    		catch(FileNotFoundException e1) {System.out.println("File not found!");}
    		catch(IOException e2) {System.out.println("IO Exception!");}
    		
    		SharedResources.semFileMonthly.release();
    		} catch(InterruptedException e) {}
    		
    /***********************************************************************************************************************************************************/
    		try {
	    		SharedResources.semStockCheck.acquire();
    			try {
    				
					File stock = new File(filenameStock);
					BufferedWriter bw = new BufferedWriter(new FileWriter(stock));
					bw.write("Item, Stock" + System.getProperty("line.separator"));
					
		    		// Update the StockCheck.csv file if required
		    		if(SharedResources.stockCookie < SharedResources.stockThreshold) {
		    			System.out.println("Now updating StockCheck.csv");
		    			// add Choco Cookies to StockCheck file
		    			bw.write("Choco Cookies, "+ SharedResources.stockCookie + System.getProperty("line.separator"));
		    		}
		
		    		if(SharedResources.stockSnacks < SharedResources.stockThreshold) {
		    			// add Snacks to StockCheck file
		    			bw.write("Snacks, " + SharedResources.stockSnacks);
		    		}
		    		bw.close();
		    		}
    			catch(FileNotFoundException e1) {System.out.println("File not found!");}
    			catch(IOException e2) {System.out.println("IO Exception!");}
    		
				SharedResources.semStockCheck.release();
    	}catch (InterruptedException e) {}
    		System.out.println("Updated Files!");
    	}// run() ends
}// FileUpdater class ends

    
    
    @Override
    @SuppressWarnings("unchecked")
    public void run() {
		// append the data to the sales list file
    	int i = 0;
    	while(i<1) {
    		try {
//    			System.out.println("i is"+i);
    			// receive the order data from the socket input stream
    			orderData = (HashMap<String, ArrayList<String>>)ois.readObject();
//    			System.out.println("orderData received=" + orderData);
				
				int chocoQuant=0, snackQuant=0;
				if(orderData.containsKey("Choco Cookie")) {
					chocoQuant = Integer.parseInt(orderData.get("Choco Cookie").get(1));
//					System.out.println("chocoQuant="+chocoQuant);
				}
				if(orderData.containsKey("Snacks")) {
					snackQuant = Integer.parseInt(orderData.get("Snacks").get(1));
				}
					//					if(chocoQuant >= 0)
				int clientTime = Integer.parseInt(orderData.get("prepTime").get(0));
//				System.out.println(clientTime);
				
				// acquire the semaphore for updating the shared resources
				SharedResources.sem.acquire();
					long currTime2 = System.currentTimeMillis();
					long d = SharedResources.currTime - currTime2;
					if(d<0)
						SharedResources.currTime = currTime2;

					int snackFlag = 1; int cookieFlag = 1;
					if(snackQuant > 0 && SharedResources.stockSnacks < snackQuant)
						snackFlag = 0;
					
					if(chocoQuant > 0 && SharedResources.stockCookie < chocoQuant)
						cookieFlag = 0;
					
//					String msg = "";
					
					int retTime = -1;
					if(snackFlag==1 && cookieFlag==1) {
						SharedResources.stockSnacks -= snackQuant;
						SharedResources.stockCookie -= chocoQuant;
						SharedResources.currTime += clientTime*60*1000;
//						msg = "DONE";
						float f = Math.round((SharedResources.currTime - currTime2)/60000.0);
						retTime = (int)f;
						
//						// check if tea or coffee has been ordered
//						int teaQuant = 0;
//						if(orderData.containsKey("Tea"))
//							teaQuant = Integer.parseInt(orderData.get("Tea").get(1));
//						int coffeeQuant = 0;
//						if(orderData.containsKey("Coffee"))
//							coffeeQuant = Integer.parseInt(orderData.get("Coffee").get(1));
//						
//						// if neither has been ordered, the order can be dispatched immediately
//						if(teaQuant==0 && coffeeQuant==0)
//							retTime = 0;
					}

//					System.out.println("retTime="+retTime);
					// construct the message to be sent to the client
//					HashMap<String, Integer> msgClient = new HashMap<String, Integer>();
//					msgClient.put("snacks", snackFlag);
//					msgClient.put("cookies", cookieFlag);
//					msgClient.put("time", (int)retTime);
					
					String msgClient = String.format("%d\n%d\n%d\n", snackFlag, cookieFlag, retTime);
					msgClient += date.toString();
					
					// send the message to the client
//					oos.writeObject(msgClient);
					
					dos.writeUTF(msgClient);
				SharedResources.sem.release();
				
				// Update the sales list file
				if(retTime>=0 && reached==0) {
//					System.out.println("Reached HERE Again?");
					FileUpdater obj = new FileUpdater(orderData);
					Thread t = new Thread(obj);
					t.start();
					reached = 1;
				}
				
				System.out.println("cookieStock="+SharedResources.stockCookie);
				System.out.println("snackStock="+SharedResources.stockSnacks);
				i++;
				
			}
    		catch (Exception e) {
    			try{					
	    			System.out.println("Client closed the window");
					System.out.println("Closing this connection");
					this.s.close();
	                System.out.println("Connection closed");
	                }
    			catch(IOException evt){}
    			break;
//    			e.printStackTrace();
    		}
    	}

        try {
            // closing resources
            this.ois.close();
            this.dos.close();
//            this.oos.close();
        }catch(IOException e){
        	
            e.printStackTrace();
        }
    }
 }