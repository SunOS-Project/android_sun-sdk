/*
 * SPDX-FileCopyrightText: 2024 Paranoid Android
 * SPDX-FileCopyrightText: 2025 Neoteric OS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.internal.util.sun;

import android.hardware.security.keymint.Algorithm;
import android.hardware.security.keymint.KeyParameter;
import android.hardware.security.keymint.KeyParameterValue;
import android.hardware.security.keymint.Tag;
import android.os.Binder;
import android.system.keystore2.Authorization;
import android.system.keystore2.IKeystoreSecurityLevel;
import android.system.keystore2.KeyDescriptor;
import android.system.keystore2.KeyEntryResponse;
import android.system.keystore2.KeyMetadata;
import android.util.Log;

import com.android.internal.util.sun.KeyboxChainGenerator.KeyGenParameters;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @hide
 */
public class KeyboxImitationHooks {

    private static final String TAG = "KeyboxImitationHooks";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static boolean mSuccess = false;

    public static KeyEntryResponse onGetKeyEntry(KeyDescriptor descriptor) {
        if (!KeyProviderManager.isKeyboxAvailable()) {
            return null;
        }

        if (!mSuccess) {
            return null;
        }

        KeyEntryResponse spoofed = KeyboxUtils.retrieve(Binder.getCallingUid(), descriptor.alias);
        if (spoofed != null) {
            dlog("Key entry spoofed");
            return spoofed;
        }

        return null;
    }

    public static KeyMetadata generateKey(IKeystoreSecurityLevel level, KeyDescriptor descriptor, Collection<KeyParameter> args) {
        if (!KeyProviderManager.isKeyboxAvailable()) {
            return null;
        }

        KeyGenParameters params = new KeyGenParameters(args.toArray(new KeyParameter[args.size()]));

        if (params.attestationChallenge == null) {
            return null;
        }

        if (params.algorithm != Algorithm.EC && params.algorithm != Algorithm.RSA) {
            Log.w(TAG, "Unsupported algorithm: " + params.algorithm);
            return null;
        }

        int uid = Binder.getCallingUid();
        try {
            List<Certificate> chain = KeyboxChainGenerator.generateCertChain(uid, descriptor, params);
            if (chain == null || chain.isEmpty()) {
                return null;
            }
            KeyEntryResponse response = buildResponse(level, chain, params, descriptor);
            if (response == null) {
                return null;
            }
            KeyboxUtils.append(uid, descriptor.alias, response);
            mSuccess = true;
            return response.metadata;
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate key", e);
            return null;
        }
    }

    private static KeyEntryResponse buildResponse(
            IKeystoreSecurityLevel level,
            List<Certificate> chain,
            KeyGenParameters params,
            KeyDescriptor descriptor
    ) {
        try {
            KeyEntryResponse response = new KeyEntryResponse();
            KeyMetadata metadata = new KeyMetadata();
            metadata.keySecurityLevel = params.securityLevel;

            KeyboxUtils.putCertificateChain(metadata, chain.toArray(new Certificate[chain.size()]));

            KeyDescriptor d = new KeyDescriptor();
            d.domain = descriptor.domain;
            d.nspace = descriptor.nspace;
            metadata.key = d;

            List<Authorization> authorizations = new ArrayList<>();
            Authorization a;

            for (Integer i : params.purpose) {
                a = new Authorization();
                a.keyParameter = new KeyParameter();
                a.keyParameter.tag = Tag.PURPOSE;
                a.keyParameter.value = KeyParameterValue.keyPurpose(i);
                a.securityLevel = params.securityLevel;
                authorizations.add(a);
            }

            for (Integer i : params.digest) {
                a = new Authorization();
                a.keyParameter = new KeyParameter();
                a.keyParameter.tag = Tag.DIGEST;
                a.keyParameter.value = KeyParameterValue.digest(i);
                a.securityLevel = params.securityLevel;
                authorizations.add(a);
            }

            a = new Authorization();
            a.keyParameter = new KeyParameter();
            a.keyParameter.tag = Tag.ALGORITHM;
            a.keyParameter.value = KeyParameterValue.algorithm(params.algorithm);
            a.securityLevel = params.securityLevel;
            authorizations.add(a);

            a = new Authorization();
            a.keyParameter = new KeyParameter();
            a.keyParameter.tag = Tag.KEY_SIZE;
            a.keyParameter.value = KeyParameterValue.integer(params.keySize);
            a.securityLevel = params.securityLevel;
            authorizations.add(a);

            a = new Authorization();
            a.keyParameter = new KeyParameter();
            a.keyParameter.tag = Tag.EC_CURVE;
            a.keyParameter.value = KeyParameterValue.ecCurve(params.ecCurve);
            a.securityLevel = params.securityLevel;
            authorizations.add(a);

            a = new Authorization();
            a.keyParameter = new KeyParameter();
            a.keyParameter.tag = Tag.NO_AUTH_REQUIRED;
            a.keyParameter.value = KeyParameterValue.boolValue(true); // TODO: copy
            a.securityLevel = params.securityLevel;
            authorizations.add(a);

            // TODO: ORIGIN, OS_VERSION, OS_PATCHLEVEL, VENDOR_PATCHLEVEL, BOOT_PATCHLEVEL,
            // CREATION_DATETIME, USER_ID

            metadata.authorizations = authorizations.toArray(new Authorization[0]);
            response.metadata = metadata;
            response.iSecurityLevel = level;
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Failed to build key entry response", e);
            return null;
        }
    }

    public static void setSuccessFlag(boolean flag) {
        mSuccess = flag;
    }

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
