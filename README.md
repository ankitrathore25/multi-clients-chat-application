# multi-clients-chat-application
Multiple clients-server chat application built in java swing. It supports message delivery to the offline clients and vector clock implementation.

This project was based on my learning in course CSE-5306 Distributed System.
It was developed in 3 phases. In each phase I have implemented different concepts of Distributed System.

Phase 1:
In this phase, I have implemented simple multiple clients - server application which support Unicasting, Multicasting and Broadcasting of the messages based on different options available on the UI.

 Code Structure:

Each phase contains 3 files. 

ServerView.java - It contains code for server. This class must be run first.

loginClient.java - It contains code for client's login and run after the server has been started. It can be run as many number of times depending upon number of clients required.

ClientView.java - It contains code for the client.



References:
https://www.youtube.com/watch?v=rd272SCl-XE
https://www.youtube.com/watch?v=ZzZeteJGncY
