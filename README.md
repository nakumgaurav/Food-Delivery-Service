# Food-Delivery-Service
A distributed GUI application for room delivery of snacks and coffee in college created using Java Swing.

Delivery Time: It is assumed that there is a single tea/coffee machine. Since the orders are to be delivered FCFS, the time of each order
(the time of preparation of tea/coffee) is added to the next orders to get the delivery time of the current order.

The stock of the limited items is initialized to 100 and their thresholds to 10

The file Sales.csv contains the sales details of each order
The file SalesDaily.csv shows the quantity of each item sold on a day and the total revenue on that day
The file SalesMonthly.csv shows the quantity of each item sold in a Month and the total revenue in that month
The file StockCheck.csv would contain those items whose stock drops below the threshold set in the code (=10)

To quickly test the SalesDaily and SalesMonthly functionalities, uncomment the files adding a day and month in the constructor of the ClientHandler class in Server.java (lines 132, 133)
