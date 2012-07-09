package com.roylaurie.subcomm.ui.web.client.applet;

import java.applet.Applet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.roylaurie.subcomm.client.SubcommClient;
import com.roylaurie.subcomm.client.netchat.SubcommNetchatClient;

public final class SubcommUIWebClientApplet extends Applet {
    private static final Logger LOG = Logger.getLogger(SubcommUIWebClientApplet.class.getCanonicalName());
    private static final long serialVersionUID = 1L;
    
    /* @var Map<String, SubcommClient> mClientMap Maps uri => client object. */
    private final Map<String, SubcommClient> mClientMap = new HashMap<String, SubcommClient>();
    /* @var Map<String, SubcommClient> mExceptionMap Maps uri => exception object. */
    private final Map<String, Exception> mExceptionMap = new HashMap<String, Exception>();
    
    public String connect(String id, String hostname, int port, String username, String password) {
        final String uri = username + '@' + hostname + ':' + port + '#' + id;
        synchronized(mClientMap) {
            if (mClientMap.containsKey(id))
                return uri;
            
            mClientMap.put(uri, null);
        }

        final SubcommClient client = new SubcommNetchatClient(hostname, port, username, password);
        Thread connectThread = new ConnectThread(this, uri, client);
        connectThread.start();
        return uri;
    }
    
    /**
     * Retrieves the client for the specified uri, if any.
     * @param String uri
     * @return SubcommClient NULL if no client found
     */
    public SubcommClient getClient(String uri) {
        synchronized(mClientMap) {
            return ( mClientMap.get(uri) );
        }
    }
    
    /**
     * Retrieves the next thrown exception, if any, for the specified client.
     * @param String uri The client URI.
     * @return Exception NULL if no exceptions thrown
     */
    public Exception nextClientException(String uri) {
        synchronized(mExceptionMap) {
            return mExceptionMap.remove(uri);
        }
    }
    
    /* package */ void notifyConnectionComplete(String uri, SubcommClient client) {
        synchronized(mClientMap) {
            // prevent leaked connections by bounds checking
            if (!mClientMap.containsKey(uri))
                throw new IllegalArgumentException("Unknown uri `" + uri + "` for notification of completion.");
            
            SubcommClient queuedClient = mClientMap.get(uri);
            if (queuedClient != null && queuedClient.connected())
                throw new IllegalArgumentException("Connection `" + uri + "` already notified for completion.");
            
            mClientMap.put(uri, client);
        }
    }
    
    /* package */ void notifyConnectionFailed(String uri, SubcommClient client, Exception e) {
        synchronized(mExceptionMap) {
            mExceptionMap.put(uri, e);
        }
    }
    
    /**
     * Quietly disconnects the client for the given uri.
     * @param String uri
     */
    public void disconnect(String uri) {
        final SubcommClient client;
        synchronized(mClientMap) {            
            synchronized(mExceptionMap) {
                client = mClientMap.remove(uri);
                mExceptionMap.remove(uri);
            }
        }
        

        if (client == null)
            return;
        
        client.disconnect();
    }
    
    @Override
    public void stop() {
        LOG.info("STOP");
        super.stop();
        synchronized(mClientMap) {
            for (SubcommClient client : mClientMap.values()) {
                final String uri = client.getUsername() + '@' + client.getUsername() + ':' + client.getUsername();
                client.disconnect();
                LOG.info("Disconnected client `" + uri + "`.");
            }
            
            mClientMap.clear();
        }
    }
}
