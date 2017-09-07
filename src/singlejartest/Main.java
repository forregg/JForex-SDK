package singlejartest;

import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.Instrument;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;


//java -Djava.library.path=/usr/local/lib -classpath /home/mage/IdeaProjects/JForexSDK/libs/ -jar /home/mage/IdeaProjes/JForexSDK/out/artifacts/JForex_SDK_jar/JForex-SDK.jar


/**
 * This small program demonstrates how to initialize Dukascopy client and start a strategy
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    //url of the DEMO jnlp
    private static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    //user name
    private static String userName = "DEMO3WVmDQ";
    //password
    private static String password = "WVmDQ";

    public static void main(String[] args) throws Exception {
        //get the instance of the IClient interface
        final IClient client = ClientFactory.getDefaultInstance();
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {
            private int lightReconnects = 3;

        	@Override
        	public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
        	}

			@Override
			public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
			}

			@Override
			public void onConnect() {
                LOGGER.info("Connected");
                lightReconnects = 3;
			}

			@Override
			public void onDisconnect() {
                LOGGER.warn("Disconnected");
                if (lightReconnects > 0) {
                    client.reconnect();
                    --lightReconnects;
                } else {
                    try {
                        //sleep for 10 seconds before attempting to reconnect
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    try {
                        client.connect(jnlpUrl, userName, password);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
			}
		});

        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        disableCertificateValidation();
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }

        //subscribe to the instruments
        Set<Instrument> instruments = new HashSet<>();
        instruments.add(Instrument.EURUSD);
        instruments.add(Instrument.GBPUSD);
        instruments.add(Instrument.USDCAD);
        instruments.add(Instrument.AUDUSD);
        instruments.add(Instrument.EURAUD);
        instruments.add(Instrument.AUDCAD);
        instruments.add(Instrument.AUDNZD);
        instruments.add(Instrument.GBPCAD);
        instruments.add(Instrument.EURCHF);
        instruments.add(Instrument.USDCHF);
        instruments.add(Instrument.NZDUSD);
        instruments.add(Instrument.USDJPY);

        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
                
        //start the strategy
        LOGGER.info("Starting strategy");
        client.startStrategy(new ZmqQouteServer());
        LOGGER.info("Starting 2 strategy");
        client.startStrategy(new ZmqOrderManagement());
        //now it's running


    }
    public static void disableCertificateValidation() {
// Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }};

// Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };

// Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {}
    }
}
