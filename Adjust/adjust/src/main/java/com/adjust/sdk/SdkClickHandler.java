package com.adjust.sdk;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import com.adjust.sdk.threading.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Abdullah on 3/8/16.
 */
public class SdkClickHandler implements ISdkClickHandler {
    private ILogger logger;
    private boolean paused;
    private List<ActivityPackage> packageQueue;
    private BackoffStrategy backoffStrategy;

    public SdkClickHandler(boolean startsSending) {
        init(startsSending);
        this.logger = AdjustFactory.getLogger();
        this.backoffStrategy = AdjustFactory.getSdkClickBackoffStrategy();
    }

    @Override
    public void init(boolean startsSending) {
        this.paused = !startsSending;
        this.packageQueue = new ArrayList<>();
    }

    @Override
    public void pauseSending() {
        paused = true;
    }

    @Override
    public void resumeSending() {
        paused = false;

        sendNextSdkClick();
    }

    @Override
    public void sendSdkClick(final ActivityPackage sdkClick) {
        ThreadManager.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                packageQueue.add(sdkClick);
                logger.debug("Added sdk_click %d", packageQueue.size());
                logger.verbose("%s", sdkClick.getExtendedString());
                sendNextSdkClick();
            }
        });
    }

    private void sendNextSdkClick() {
        ThreadManager.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                if (paused) {
                    return;
                }

                if (packageQueue.isEmpty()) {
                    return;
                }

                ActivityPackage sdkClickPackage = packageQueue.get(0);

                int retries = sdkClickPackage.getRetries();

                if (retries > 0) {
                    long waitTimeMilliSeconds = Util.getWaitingTime(retries, backoffStrategy);

                    double waitTimeSeconds = waitTimeMilliSeconds / 1000.0;
                    String secondsString = Util.SecondsDisplayFormat.format(waitTimeSeconds);

                    logger.verbose("Sleeping for %s seconds before retrying sdk_click for the %d time", secondsString, retries);
                    SystemClock.sleep(waitTimeMilliSeconds);
                }

                sendSdkClickInternal(sdkClickPackage);

                packageQueue.remove(0);
                sendNextSdkClick();
            }
        });
    }

    private void sendSdkClickInternal(ActivityPackage sdkClickPackage) {
        String targetURL = Constants.BASE_URL + sdkClickPackage.getPath();

        try {
            HttpsURLConnection connection = Util.createPOSTHttpsURLConnection(
                    targetURL,
                    sdkClickPackage.getClientSdk(),
                    sdkClickPackage.getParameters(),
                    packageQueue.size() - 1);

            ResponseData responseData = Util.readHttpResponse(connection, sdkClickPackage);

            if (responseData.jsonResponse == null) {
                retrySending(sdkClickPackage);
            }
        } catch (UnsupportedEncodingException e) {
            logErrorMessage(sdkClickPackage, "Sdk_click failed to encode parameters", e);
        } catch (SocketTimeoutException e) {
            logErrorMessage(sdkClickPackage, "Sdk_click request timed out. Will retry later", e);
            retrySending(sdkClickPackage);
        } catch (IOException e) {
            logErrorMessage(sdkClickPackage, "Sdk_click request failed. Will retry later", e);
            retrySending(sdkClickPackage);
        } catch (Throwable e) {
            logErrorMessage(sdkClickPackage, "Sdk_click runtime exception", e);
        }
    }

    private void retrySending(ActivityPackage sdkClickPackage) {
        int retries = sdkClickPackage.increaseRetries();

        logger.error("Retrying sdk_click package for the %d time", retries);
        sendSdkClick(sdkClickPackage);
    }

    private void logErrorMessage(ActivityPackage sdkClickPackage, String message, Throwable throwable) {
        final String packageMessage = sdkClickPackage.getFailureMessage();
        final String reasonString = Util.getReasonString(message, throwable);
        String finalMessage = String.format("%s. (%s)", packageMessage, reasonString);
        logger.error(finalMessage);
    }
}
