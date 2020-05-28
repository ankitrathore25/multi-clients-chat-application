# multi-clients-chat-application

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


References:
https://www.youtube.com/watch?v=rd272SCl-XE
https://www.youtube.com/watch?v=ZzZeteJGncY
