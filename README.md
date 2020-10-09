# multi-clients-chat-application
# [Demo](https://www.youtube.com/watch?v=tf5fUAXckGg)
Multiple clients-server chat application built in java swing. It supports message delivery to the offline clients and vector clock implementation

This project was based on my learning in course CSE-5306 Distributed System.
It was developed in 3 phases. In each phase I have implemented different concepts of Distributed System.

Phase 1:
In this phase, I have implemented simple multiple clients - server application which support Unicasting, Multicasting and Broadcasting of the messages based on different options available on the UI.
 
 Code Structure:

Each phase contains 3 files. 
- ServerView.java - It contains code for server. This class must be run first.
- loginClient.java - It contains code for client's login and run after the server has been started. It can be run as many number of times depending upon number of clients required.
- ClientView.java - It contains code for the client.

Phase 2:
In this phase, I have implemented functionality where a client can send message to offline client too. I have used activemq (messaging queue) to implement this feature.
Messaging queue is java based messaging server (https://activemq.apache.org/).

Steps to Run:
- Download activemq in your computer. Start activemq at local server using command line. Just open cmd/terminal and navigate to the bin folder of the downloaded activemq and run the command `start activemq`.
- Now you can check the queues on the UI of activemq. You can check out other videos on youtube on how to use activemq.
- Start the server first.
- Create as many client as you want by running the client class.

To implement offline client messaging. I am sending all the messages to the activemq first and then fetching from there to send it to the intended clients.

`Assumption` : when a client comes online again then it will first check the message using 'check messages' button before sending or receiving any message.

Phase 3:
In this phase, I have implemented `vector clock` concept. Once clients connect to the server and starting the clock after clicking the start button on their UI, will be able to send vector clock as message to the randomly chosen user.




References:
https://www.youtube.com/watch?v=rd272SCl-XE
https://www.youtube.com/watch?v=ZzZeteJGncY
