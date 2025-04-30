/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.content;

import static org.sun.content.ContextExt.ONLINE_CONFIG_MANAGER_SERVICE;

import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import org.sun.content.IOnlineConfigManagerService;
import org.sun.content.IOnlineConfigurable;
import org.sun.server.SunSystemExService;

public class OnlineConfigController {

    private static final String TAG = "OnlineConfigController";

    private static class InstanceHolder {
        private static OnlineConfigController INSTANCE = new OnlineConfigController();
    }

    public static OnlineConfigController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final Object mLock = new Object();

    private final ArrayList<OnlineConfigurable> mClients = new ArrayList<>();

    private final class OnlineConfigurable {
        final IOnlineConfigurable mConfigurable;
        final IBinder.DeathRecipient mDeathRecipient;

        OnlineConfigurable(IOnlineConfigurable configurable,
                IBinder.DeathRecipient deathRecipient) {
            mConfigurable = configurable;
            mDeathRecipient = deathRecipient;
        }
    }

    private final class OnlineConfigManagerService extends IOnlineConfigManagerService.Stub {
        @Override
        public List<IOnlineConfigurable> getRegisteredClients() {
            synchronized (mLock) {
                List<IOnlineConfigurable> ret = new ArrayList<>();
                for (OnlineConfigurable client : mClients) {
                    ret.add(client.mConfigurable);
                }
                return ret;
            }
        }

        @Override
        public boolean registerOnlineConfigurable(IOnlineConfigurable configurable) {
            final IBinder configurableBinder = configurable.asBinder();
            IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    synchronized (mLock) {
                        for (int i = 0; i < mClients.size(); i++) {
                            if (configurableBinder == mClients.get(i).mConfigurable.asBinder()) {
                                OnlineConfigurable removed = mClients.remove(i);
                                IBinder binder = removed.mConfigurable.asBinder();
                                if (binder != null) {
                                    binder.unlinkToDeath(this, 0);
                                }
                                i--;
                            }
                        }
                    }
                }
            };

            synchronized (mLock) {
                try {
                    configurable.asBinder().linkToDeath(dr, 0);
                    mClients.add(new OnlineConfigurable(configurable, dr));
                } catch (RemoteException e) {
                    // Client died, no cleanup needed.
                    return false;
                }
                return true;
            }
        }

        @Override
        public boolean unregisterOnlineConfigurable(IOnlineConfigurable configurable) {
            boolean found = false;
            final IBinder configurableBinder = configurable.asBinder();
            synchronized (mLock) {
                for (int i = 0; i < mClients.size(); i++) {
                    found = true;
                    OnlineConfigurable onlineConfigurable = mClients.get(i);
                    if (configurableBinder == onlineConfigurable.mConfigurable.asBinder()) {
                        OnlineConfigurable removed = mClients.remove(i);
                        IBinder binder = removed.mConfigurable.asBinder();
                        if (binder != null) {
                            binder.unlinkToDeath(removed.mDeathRecipient, 0);
                        }
                        i--;
                    }
                }
            }
            return found;
        }
    }

    public void initSystemExService(SunSystemExService service) {
        service.publishBinderService(ONLINE_CONFIG_MANAGER_SERVICE, new OnlineConfigManagerService());
    }
}
