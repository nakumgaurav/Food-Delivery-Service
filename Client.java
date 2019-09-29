// Client-side implementation of the online food ordering service

import java.io.*;
import java.net.Socket;
import java.text.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.concurrent.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import javax.swing.border.*;

@SuppressWarnings("unchecked")
abstract class DefaultGUI implements ActionListener, DocumentListener{
	// Socket variables
	protected DataInputStream dis;
	protected ObjectOutputStream outObjectStream;
	protected Socket socket;
	
	// GUI variables
	private JFrame mainFrame;    	// mainframe object
	private JPanel cards;        	// panel for holding all panels in the application
	private JPanel cardInvoice;  	// panel for holding invoice/stock info
	private JPanel invoicePanel; 	// panel for showing invoice
	private JPanel stockPanel;  	// panel for holding stock availability info
	private JTextField tfName;  	// textfield for taking name
	private JTextField tfAddrr; 	// textfield for taking address
	private JLabel stockLabel1;	 	// to display stock of cookies message
	private JLabel stockLabel2;		// to display stock of snacks message
	private JLabel stockLabel3;		// to display reorder message
	private JLabel timeLabel;		// to display delivery time
	private JLabel jLabel2;			// to display date on invoice
	private JLabel jLabel4; 		// name label on invoice
	private JLabel jLabel6;			// address label on invoice
	private JLabel jLabel9;			// Total price label on invoice
	private JTable orderTable;		// table for holding order data
	private JLabel[][] labelArray;  // holds all the items' labels in invoice
	
	private int totPrice;	// variable for holding total price of the order items
	private int delTime;	// variable for holding the delivery time
	private int prepTime;	// variable for holding the preparation time for tea/coffee

	private HashMap<String, ArrayList<String>> orderData;	// hashmap for sending final order data to server
	
	// The OrderInfo class is a subclass of SwingWorker. In the background we send the order data to the server
	// and receive delivery time and availabilty info back from the server, while also displaying the invoice
	private OrderInfo ordInfo;	// object of class OrderInfo
	
	// Initialize all the GUI variables in the constructor
	public DefaultGUI(){
		mainFrame = new JFrame("Tea Stall Service");
		// mainFrame.setLayout(new GridLayout(1,0));
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		cards = new JPanel(new CardLayout());
		cardInvoice = new JPanel();
		invoicePanel = new JPanel();
		stockPanel = new JPanel(new GridLayout(3,1));
		tfName = new JTextField(20);
		tfAddrr = new JTextField(20);
		stockLabel1 = new JLabel();
		stockLabel2 = new JLabel();
		stockLabel3 = new JLabel();
		timeLabel = new JLabel("");
		jLabel2 = new JLabel();
		jLabel4 = new JLabel();
		jLabel6 = new JLabel();
		jLabel9 = new JLabel();
		
		totPrice = 0;
		delTime = 0;
		prepTime = 0;
		orderData = new HashMap<String, ArrayList<String>>();
		labelArray = new JLabel[4][4];
		
		// On client closing the window, send Exit message to the server
		// and close all stream objects, close socket and exit
		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("Exit Client");
				try {
					outObjectStream.writeObject(orderData);
					socket.close();
					outObjectStream.close();
					dis.close();
					System.exit(0);
				} catch(IOException ex) {
					ex.printStackTrace();
				}
			}
		});
	}
	
	public Object[][] constructObject(TableModel myModel){
		int count = 0;
		
		for(int i=0; i<4; i++) {
			if(Integer.parseInt(myModel.getValueAt(i, 2).toString()) > 0) 
				count++;
		}
		
		Object[][] data = new Object[count][3];
	
		int k = 0;
		int l = 0;
		for(int i=0; i<4; i++) {
			if(Integer.parseInt(myModel.getValueAt(i, 2).toString()) > 0) {
				l = 0;
				for(int j=0; j<4; j++) {
					if(j==1)
						continue;
					data[k][l] = myModel.getValueAt(i,j);
					l++;
				}
				k++;
			}
		}
		return data;
	}

	public void addComponentsToPane(Container pane){
		// components of opening panel
		JButton bPlaceOrder = new JButton("PLACE ORDER");
		bPlaceOrder.addActionListener(this);

		JButton bViewMenu = new JButton("MENU");
		bViewMenu.addActionListener(this);

		// Opening panel
		JPanel cardOpen = new JPanel();
		cardOpen.add(bPlaceOrder);
		cardOpen.add(bViewMenu);
		
		/*****************************************************************************************************************/

		// components of menu panel
		String[] columnNames = {"Item", "Rate"};
		Object[][] data = {
			{"Tea", Integer.valueOf(5)},
			{"Coffee", Integer.valueOf(8)},
			{"Choco Cookie (1 pc)", Integer.valueOf(12)},
			{"Snacks", Integer.valueOf(20)}
		};
		
		final JTable menuTable = new JTable(data, columnNames);
		menuTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
		JScrollPane scrollPane = new JScrollPane(menuTable);


		JButton backBtn = new JButton("BACK");
		backBtn.addActionListener(this);

		// backBtn.setSize(80,30);
		// backBtn.setLocation(500, 200);

		// Menu panel
		JPanel cardMenu = new JPanel(new FlowLayout(FlowLayout.CENTER, 700, 10));
		// cardMenu.setLayout(new BoxLayout(cardMenu, BoxLayout.Y_AXIS));
		cardMenu.add(scrollPane);
		cardMenu.add(backBtn);
		
		/*****************************************************************************************************************/

		// components of order panel
		String[] columnNamesOrder = {"Item", "Rate", "Quantity", "Price"};
		Object[][] dataOrder = {
				{"Tea", Integer.valueOf(5), Integer.valueOf(0), Integer.valueOf(0)},
				{"Coffee", Integer.valueOf(8), Integer.valueOf(0), Integer.valueOf(0)},
				{"Choco Cookie", Integer.valueOf(12), Integer.valueOf(0), Integer.valueOf(0)},
				{"Snacks", Integer.valueOf(20), Integer.valueOf(0), Integer.valueOf(0)}
		};
		JLabel lTotPrice = new JLabel();

		orderTable = new JTable(dataOrder, columnNamesOrder);
		orderTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
		orderTable.getModel().addTableModelListener(new TableModelListener(){
			@Override
			public void tableChanged(TableModelEvent evtTable){
				int row = evtTable.getFirstRow();
				int column = evtTable.getColumn();
				
				if(column==2){
					TableModel model = (TableModel)evtTable.getSource();

					String data = model.getValueAt(row, column).toString();
					int quant = Integer.parseInt(data);
					data = model.getValueAt(row, column-1).toString();
					int rate = Integer.parseInt(data);
					int priceInt = quant*rate;
					String price = String.valueOf(priceInt);
					Object newPrice = (Object)price;
					model.setValueAt(newPrice, row, column+1);
					
					// update totPrice
					totPrice = 0;
					for(int i=0; i<4; i++)
						totPrice += Integer.parseInt(model.getValueAt(i, 3).toString());
					lTotPrice.setText("TOTAL PRICE: " + String.valueOf(totPrice));
					
					// update times
					delTime = 0;
					prepTime = 0;
					for(int i=0; i<2; i++)
						delTime += Integer.parseInt(model.getValueAt(i, column).toString());
					prepTime = delTime;
					
					
					// update order data
					for(int i=0; i<4; i++) {
						String item = model.getValueAt(i, column-2).toString();
						String rateL = model.getValueAt(i, column-1).toString();
						String quantL = model.getValueAt(i, column).toString();
						if(quantL.equals("0")) {
							if(orderData.containsKey(item))
								orderData.remove(item);
							continue;
						}
						String priceL = model.getValueAt(i, column+1).toString();
						orderData.put(item, new ArrayList<String>(Arrays.asList(rateL,quantL,priceL)));
//						System.out.println("quantL="+quantL);
					}
					
				}
			}
		});

		JScrollPane scrollPaneOrder = new JScrollPane(orderTable);

		JButton bProceed = new JButton("PROCEED");
		bProceed.addActionListener(this);
		
		JLabel usageMsg = new JLabel("Please edit the Quantity column to place your order");
		
		// Order panel
		JPanel cardOrder = new JPanel(new FlowLayout(FlowLayout.CENTER, 500,10));

		// Order Panel
		cardOrder.add(usageMsg);
		cardOrder.add(scrollPaneOrder);
		cardOrder.add(lTotPrice);
		cardOrder.add(bProceed);

		/*****************************************************************************************************************/	
		
		// Components of UserInfo Panel
		JLabel lName = new JLabel("Enter Your Name:");
		JLabel lAddrr = new JLabel("Enter Your Address:");
		
		tfName.getDocument().addDocumentListener(this);
		tfAddrr.getDocument().addDocumentListener(this);

		// Group Layout panel
		JPanel glPanel = new JPanel();
		GroupLayout layout = new GroupLayout(glPanel);
		glPanel.setLayout(layout);

		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		// create a sequential group for the horizontal axis
		GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
		
	   // The sequential group in turn contains two parallel groups.
	   // One parallel group contains the labels, the other the text fields.
	   // Putting the labels in a parallel group along the horizontal axis
	   // positions them at the same x location.
	   //
	   // Variable indentation is used to reinforce the level of grouping	
		hGroup.addGroup(layout.createParallelGroup().addComponent(lName).addComponent(lAddrr));
		hGroup.addGroup(layout.createParallelGroup().addComponent(tfName).addComponent(tfAddrr));
		layout.setHorizontalGroup(hGroup);
		
		
		// Create a sequential group for the vertical axis.
		GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
		
	   // The sequential group contains two parallel groups that align
	   // the contents along the baseline. The first parallel group contains
	   // the first label and text field, and the second parallel group contains
	   // the second label and text field. By using a sequential group
	   // the labels and text fields are positioned vertically after one another
		vGroup.addGroup(layout.createParallelGroup().addComponent(lName).addComponent(tfName));
		vGroup.addGroup(layout.createParallelGroup().addComponent(lAddrr).addComponent(tfAddrr));
		layout.setVerticalGroup(vGroup);

		// confirm order button
		JButton bConfirm = new JButton("CONFIRM ORDER");
		bConfirm.addActionListener(this);
		
		// back to order panel button
		JButton bckOrder = new JButton("BACK TO ORDER");
		bckOrder.addActionListener(this);
		
		// Name-Address (User info) Panel
		JPanel cardUserInfo = new JPanel();
		cardUserInfo.add(glPanel);
		cardUserInfo.add(bckOrder);
		cardUserInfo.add(bConfirm);
		
		/*****************************************************************************************************************/
		
		// components of Invoice panel

        JLabel jLabel1 = new JLabel("Date");
        JLabel jLabel3 = new JLabel("Name");
        JLabel jLabel5 = new JLabel("Address");
        JLabel jLabel7 = new JLabel("ORDER DETAILS");
        JLabel jLabel8 = new JLabel("TOTAL:");

        JLabel itemLabel = new JLabel("Item");
        JLabel quantLabel = new JLabel("Quantity");
        JLabel priceLabel = new JLabel("Price");
        
        JLabel i1 = new JLabel();
        JLabel q1 = new JLabel();
        JLabel p1 = new JLabel();

        JLabel i2 = new JLabel();
        JLabel q2 = new JLabel();
        JLabel p2 = new JLabel();

        JLabel i3 = new JLabel();
        JLabel q3 = new JLabel();
        JLabel p3 = new JLabel();

        JLabel i4 = new JLabel();
        JLabel q4 = new JLabel();
        JLabel p4 = new JLabel();
        
        labelArray[0][0] = i1; labelArray[0][1] = q1; labelArray[0][2] = p1;
        labelArray[1][0] = i2; labelArray[1][1] = q2; labelArray[1][2] = p2;
        labelArray[2][0] = i3; labelArray[2][1] = q3; labelArray[2][2] = p3;        
        labelArray[3][0] = i4; labelArray[3][1] = q4; labelArray[3][2] = p4;        
       
        layout = new GroupLayout(invoicePanel);
        invoicePanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(85, 85, 85)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 96, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel5))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(itemLabel)
                                    .addComponent(i2, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(i3, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(i4, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(quantLabel)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                            .addComponent(q1)
                                            .addComponent(q2)
                                            .addComponent(q4)
                                            .addComponent(q3))))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(p1)
                                    .addComponent(p2)
                                    .addComponent(p4)
                                    .addComponent(p3)
                                    .addComponent(priceLabel)))
                            .addComponent(jLabel6)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel4, GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)))
                        .addGap(209, 209, 209))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(i1, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(layout.createSequentialGroup()
                .addGap(136, 136, 136)
                .addComponent(timeLabel, GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(265, 265, 265)
                        .addComponent(jLabel8)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(178, 178, 178)
                        .addComponent(jLabel7)))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addGap(18, 18, 18)
                .addComponent(jLabel7)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(itemLabel)
                    .addComponent(quantLabel)
                    .addComponent(priceLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(i1)
                    .addComponent(q1)
                    .addComponent(p1))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(p2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(p3)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(p4))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(i2)
                            .addComponent(q2))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(i3)
                            .addComponent(q3))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(i4)
                            .addComponent(q4))))
                .addGap(23, 23, 23)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(timeLabel, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(65, Short.MAX_VALUE))
        );
		
		invoicePanel.add(jLabel1);
		invoicePanel.add(jLabel2);
		invoicePanel.add(jLabel3);
		invoicePanel.add(jLabel4);
		invoicePanel.add(jLabel5);
		invoicePanel.add(jLabel6);
		invoicePanel.add(jLabel7);
		invoicePanel.add(jLabel8);
		invoicePanel.add(jLabel9);
		invoicePanel.add(itemLabel);
		invoicePanel.add(quantLabel);
		invoicePanel.add(priceLabel);		
        for(int i=0; i<4; i++){
			for(int j=0; j<3; j++){
				invoicePanel.add(labelArray[i][j]);
			}
		}
		invoicePanel.add(timeLabel);        

		// set title for the invoice
		int w = 80;
		String pad ="";
		String pad2 = "";
		for (int i=0; i!=w; i++) pad +=" ";
		for (int i=0; i!=w+20; i++) pad2 +=" ";
		pad = String.format("%"+w+"s", pad);		
		Border border = BorderFactory.createTitledBorder(pad+"INVOICE"+pad2);
		invoicePanel.setBorder(border);
        
//		JButton bckOrder2 = new JButton("CHANGE ORDER");
//		bckOrder2.addActionListener(this);
		
		stockPanel.add(stockLabel1);
		stockPanel.add(stockLabel2);
		stockPanel.add(stockLabel3);
//		stockPanel.add(bckOrder2);
		/*****************************************************************************************************************/		
		
		// Panel that contains all the cards (other panels)
		cards.add(cardOpen, "BACK");
		cards.add(cardMenu, "MENU");
		cards.add(cardOrder, "PLACE ORDER");
		cards.add(cardUserInfo, "PROCEED");
		cards.add(cardInvoice, "CONFIRM ORDER");

		// add the cards panel to the content pane
		pane.add(cards, BorderLayout.CENTER);
	}
	
	
	public void insertUpdate(DocumentEvent e) {
		orderData.put("Name", new ArrayList<String>(Arrays.asList(tfName.getText())));
		orderData.put("Addrr", new ArrayList<String>(Arrays.asList(tfAddrr.getText())));
	}
	
	public void removeUpdate(DocumentEvent e) {
		orderData.put("Name", new ArrayList<String>(Arrays.asList(tfName.getText())));
		orderData.put("Addrr", new ArrayList<String>(Arrays.asList(tfAddrr.getText())));	
	}
	
	// fired only when the font, size, etc changes
	public void changedUpdate(DocumentEvent e) {
	}

	@Override
	public void actionPerformed(ActionEvent evt){
		CardLayout cl = (CardLayout)(cards.getLayout());
		String action = evt.getActionCommand();
		if(action.equals("BACK TO ORDER")) {
			action = "PLACE ORDER";
		}
//		if(action.equals("CHANGE ORDER")) {
//			action = "PLACE ORDER";
//			cardInvoice.remove(stockPanel);
//		}
		
		if(action.equals("PROCEED")) {
			delTime += 2;
			orderData.put("delTime", new ArrayList<String>(Arrays.asList(String.valueOf(delTime))));
			orderData.put("prepTime", new ArrayList<String>(Arrays.asList(String.valueOf(prepTime))));
			orderData.put("totPrice", new ArrayList<String>(Arrays.asList(String.valueOf(totPrice))));
		}
		
		// if the order has been confirmed, send the order and customer data
		// to the server
		if(action.equals("CONFIRM ORDER")) {
			// run the order info background task
			ordInfo = new OrderInfo();
			ordInfo.execute();
		}
		// System.out.println(action);
		cl.show(cards, action);
	}

	// Background thread class (which extends SwingWorker) is used to send data
	// to the client while the invoice is getting displayed
	// In the background the server keeps updating the appropriate files
	private class OrderInfo extends SwingWorker<Integer, String>{
//		private HashMap<String, Integer> info;
		private String info;
		private ArrayList<String> items;
		
		public OrderInfo() {
			items = new ArrayList<>(Arrays.asList("Tea", "Coffee", "Choco Cookie", "Snacks"));
		}
		
		@Override
		public Integer doInBackground(){
			try {
//				System.out.println("orderData=" + orderData);
				// send the order data to the server
				outObjectStream.writeObject(orderData);
				outObjectStream.flush();
			}
			catch(IOException e1) {e1.printStackTrace();}
			
			try {
				// get the stock info and del time from the server
//				info = (HashMap<String, Integer>)inObjectStream.readObject();
				info = dis.readUTF();
			}
			catch(IOException e2) {e2.printStackTrace();}
//			catch(ClassNotFoundException e3) {e3.printStackTrace();}
			
//			int retTime = info.get("time");
//			int snackFlag = info.get("snacks"); 
//			int cookieFlag = info.get("cookies");
			
			String[] lines = info.split("\n");
			int snackFlag = Integer.parseInt(lines[0]);
			int cookieFlag = Integer.parseInt(lines[1]);
			int retTime = Integer.parseInt(lines[2]);
			String date = lines[3];
			
			
//			System.out.println("snackFlag"+snackFlag);
//			System.out.println("cookieFlag"+cookieFlag);
			
			if(snackFlag==0)
				stockLabel1.setText("Stock of Snacks insufficient!");
			if(cookieFlag==0)
				stockLabel2.setText("Stock of Cookies insufficient!");

			if(snackFlag==0 || cookieFlag==0) {
				stockLabel3.setText( "Please place a new order! Thanks!");
				cardInvoice.add(stockPanel);
			}
			else {
				jLabel2.setText(date);
				jLabel4.setText(tfName.getText());
				jLabel6.setText(tfAddrr.getText());
				
				int l = 0;
				for(String item : items) {
					if(orderData.containsKey(item)) {
						labelArray[l][0].setText(item); 
						labelArray[l][1].setText(orderData.get(item).get(1)); 
						labelArray[l][2].setText(orderData.get(item).get(2));
						l++;
					}
				}
		
				jLabel9.setText(String.valueOf(totPrice));
				cardInvoice.add(invoicePanel);
//				mainFrame.setTitle("INVOICE");
			}
			
			return retTime;
		}

		@Override
		protected void process(List<String> chunks){
//			stockLabel.setText(chunks.get(0));
		}
		
		@Override
		protected void done() {
			try {
				int retTime = get();
				if(retTime>=0) {
					retTime += 2;
					String msg = String.format("The Expected Delivery Time is %d minutes", retTime);
					timeLabel.setText(msg);
				}
			}
            catch (InterruptedException e1){e1.printStackTrace();}
            catch (ExecutionException e2){e2.printStackTrace();}
		}

	}
	
	public void createAndShowGUI(){
		addComponentsToPane(mainFrame.getContentPane());
		// Display the window
		mainFrame.pack();
		mainFrame.setSize(600,400);
		mainFrame.setVisible(true);
	}
}


class ClientSocket extends DefaultGUI{
	public ClientSocket(int port) throws IOException{
		socket = new Socket("localhost", port);
		dis = new DataInputStream(socket.getInputStream());
		outObjectStream = new ObjectOutputStream(socket.getOutputStream());
	}
}

public class Client{
private static final int PORT = 9000;
	
	public static void main(String[] args){
		
		try{
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run(){
					try {
						ClientSocket clientObj = new ClientSocket(PORT);
						clientObj.createAndShowGUI();
					}
					catch(IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
}