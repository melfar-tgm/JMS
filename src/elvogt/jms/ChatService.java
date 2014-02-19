package elvogt.jms;

import java.net.InetAddress;

import org.apache.activemq.*;


public class ChatService implements javax.jms.MessageListener {
    private static final String APP_TOPIC = "JMSChat";
    private static final String PROPERTY_NAME = "Department";
    private static final String DEFAULT_BROKER_NAME = "tcp://localhost:61616";
    private static final String DEFAULT_PASSWORD = "";

    private javax.jms.Connection connect  = null;
    private javax.jms.Session pubSession  = null;
    private javax.jms.Session subSession  = null;
    private javax.jms.MessageProducer publisher = null;

    /** Create JMS client for publishing and subscribing to messages. */
    private void chatter( String broker, String username, String password, String selection)
    {
    	
        /*
         * Erstellt eine Verbindung mit 3 Parametern
         * Session mit AUTO_ACKNOWLEDGE erstellt
         */
        try
        {
            javax.jms.ConnectionFactory factory;
            factory = new ActiveMQConnectionFactory(username, password, broker);
            connect = factory.createConnection (username, password);
            pubSession = connect.createSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
            subSession = connect.createSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
        }
        catch (javax.jms.JMSException jmse)
        {
            System.err.println("error: Cannot connect to Broker - " + broker);
            jmse.printStackTrace();
            System.exit(1);
        }

        // Create Publisher and Subscriber to 'chat' topics

        try
        {
            javax.jms.Topic topic = pubSession.createTopic (APP_TOPIC);
            // NOTE: The subscriber's message selector will now be set:
            javax.jms.MessageConsumer subscriber = subSession.createConsumer(topic, PROPERTY_NAME + " = \'" + selection +"\'", false);
            subscriber.setMessageListener(this);
            publisher = pubSession.createProducer(topic);
            // Now that setup is complete, start the Connection
            connect.start();
        }
        catch (javax.jms.JMSException jmse)
        {
            jmse.printStackTrace();
            System.exit(1);
        }

        try
        {
            // Read all standard input and send it as a message.

            java.io.BufferedReader stdin =
                new java.io.BufferedReader( new java.io.InputStreamReader( System.in ) );		   
		        System.out.println ("Willkommen im JMSChat\n"
			            	  + "===========================\n"
			            	  + "Sie haben sich erfolgreich mit den Namen" + username + " in "+DEFAULT_BROKER_NAME +" eingeloggt.\n"
							  + "Sie befinden sich im Chatroom: " + selection + " mit den Thema " + APP_TOPIC + ".\n" 	
							  + "Schreiben Sie Text und versenden sie es an andere Teilnehmer indem Sie die Enter-Taste betätigen.\n"
							  + "Der Benutzername und ihre IP-Adresse ihres Computers werden anderen Teilnehmern nach dem Senden einer Nachricht angezeigt.");
		   
            while ( true )
            {
                String s = stdin.readLine();

                if ( s == null || s.equalsIgnoreCase("exit")){
                	System.out.println("Das Programm wurde durch den Befehl Exit beendet.");
                    exit();
                	
                }
                else if ( s.length() > 0 )
                {
                    javax.jms.TextMessage msg = pubSession.createTextMessage();
                    msg.setText( username +" ["+InetAddress.getLocalHost().getHostAddress()+"]: " + s );
                    // NOTE: here we set a property on messages to be published:
                    msg.setStringProperty(PROPERTY_NAME, selection);
                    publisher.send( msg );
                }
            }
        }
        catch ( java.io.IOException ioe )
        {
            ioe.printStackTrace();
        }
        catch ( javax.jms.JMSException jmse )
        {
            jmse.printStackTrace();
        }
    }

    /**
     * Handle the message
     * (as specified in the javax.jms.MessageListener interface).
     */
    public void onMessage( javax.jms.Message aMessage)
    {
        try
        {
            // Cast the message as a text message.
            javax.jms.TextMessage textMessage = (javax.jms.TextMessage) aMessage;

            // This handler reads a single String from the
            // message and prints it to the standard output.
            try
            {
                String string = textMessage.getText();
                System.out.println( string );
            }
            catch (javax.jms.JMSException jmse)
            {
                jmse.printStackTrace();
            }
        }
        catch (java.lang.RuntimeException rte)
        {
            rte.printStackTrace();
        }
    }

    /** Cleanup resources and then exit. */
    private void exit()
    {
        try
        {
            connect.close();
        }
        catch (javax.jms.JMSException jmse)
        {
            jmse.printStackTrace();
        }

        System.exit(0);
    }

    //
    // NOTE: the remainder of this sample deals with reading arguments
    // and does not utilize any JMS classes or code.
    //

    /** Main program entry point. */
    public static void main(String argv[]) {

        // Is there anything to do?
        if (argv.length == 0) {
            printUsage();
            System.exit(1);
        }

        // Values to be read from parameters
        String broker    = DEFAULT_BROKER_NAME;
        String username  = null;
        String password  = DEFAULT_PASSWORD;
        String selection  = null;

        // Check parameters
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];

            // Options
            if (!arg.startsWith("-")) {
                System.err.println ("error: unexpected argument - "+arg);
                printUsage();
                System.exit(1);
            }
            else {
                if (arg.equals("-b")) {
                    if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                        System.err.println("error: missing broker name:port");
                        System.exit(1);
                    }
                    broker = argv[++i];
                    continue;
                }

                if (arg.equals("-u")) {
                    if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                        System.err.println("error: missing user name");
                        System.exit(1);
                    }
                    username = argv[++i];
                    continue;
                }

                if (arg.equals("-p")) {
                    if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                        System.err.println("error: missing password");
                        System.exit(1);
                    }
                    password = argv[++i];
                    continue;
                }

                if (arg.equals("-s")) {
                    if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                        System.err.println("error: missing selection");
                        System.exit(1);
                    }
                    selection = argv[++i];
                    continue;
                }

                if (arg.equals("-h")) {
                    printUsage();
                    System.exit(1);
                }
            }
        }

        // Check values read in.
        if (username == null) {
            System.err.println ("error: user name must be supplied");
            printUsage();
        }

        if (selection == null) {
            System.err.println ("error: selection must be supplied");
            printUsage();
            System.exit(1);
        }

        // Start the JMS client for the "chat".
        ChatService chat = new ChatService();
        chat.chatter (broker, username, password, selection);

    }

    /** Prints the usage. */
    private static void printUsage() {

        StringBuffer use = new StringBuffer();
        use.append("usage: java SelectorChat (options) ...\n\n");
        use.append("options:\n");
        use.append("  -b hostadresse:port of broker \n");
        use.append("  -u name       Einzigartigen Usernamen verwenden. (benötigt)\n");
        use.append("  -p passwort   Passwort für Username festlegen.\n");
        use.append("                Default password: "+DEFAULT_PASSWORD+"\n");
        use.append("  -s selection  Chatroom auswählen/erstellen. (benötigt)\n");
        use.append("  -h            Hilfe\n");
        System.err.println (use);
    }

}
