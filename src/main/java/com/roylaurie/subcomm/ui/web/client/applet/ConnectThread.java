package com.roylaurie.subcomm.ui.web.client.applet;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;

import com.roylaurie.subcomm.client.SubcommClient;

public final class ConnectThread extends Thread {
    public static final String SUBCOMM_CONNECT = "SubcommConnect";
    private static final String EMPTY_STR = "";
    private static final Logger LOG = Logger.getLogger(ConnectThread.class.getCanonicalName()); 
    
    private final SubcommUIWebClientApplet mApplet;
    private final String mUri;
    private final SubcommClient mClient;
    
    public ConnectThread(SubcommUIWebClientApplet applet, String uri, SubcommClient client) {
        mApplet = applet;
        mUri = uri;
        mClient = client;
    }

    @Override
    public void run() {
        try {
            LOG.info("Connecting to `" + mUri + "`.");
            
            AccessController.doPrivileged(
                new PrivilegedAction<String>() {
                    public String run() {
                        try {
                            mClient.connect();
                            if (!mClient.connected()) {
                                mApplet.notifyConnectionFailed(
                                    mUri,
                                    mClient,
                                    new IOException("Client not connected after connect()!")
                                );
                            } 
                        } catch (Exception e) {
                            mApplet.notifyConnectionFailed(mUri, mClient, e);
                            mClient.disconnect();
                        }

                        return EMPTY_STR;
                    }
                }
            );

            mClient.joinDefaultArena();
            mApplet.notifyConnectionComplete(mUri, mClient);
        } catch (Exception e) {
            mApplet.notifyConnectionFailed(mUri, mClient, e);
            if (mClient != null) {
                mClient.disconnect();
            }
        }
    }
}
