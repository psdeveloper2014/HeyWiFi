package net.heywifi.app;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by Andy on 2015-08-01.
 */
public class MyInstanceIDListenerService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        // send new registration token to app server
    }

}
