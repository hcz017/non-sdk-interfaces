package com.hcz017.nonsdkapi;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import me.weishu.reflection.Reflection;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvOpName;
    private TextView imsStat;
    private TextView callState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvOpName = findViewById(R.id.operator_name);
        imsStat = findViewById(R.id.ims_reg_sta);
        callState = findViewById(R.id.pack_access);

        findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvOpName.setText("");
                imsStat.setText("");
                callState.setText("");
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);
    }

    /**
     * Access getNetworkOperatorName, seems not in any lsit,
     * but log prints (light greylist, reflection)
     *
     * @param view
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public void getNetworkOperatorName(View view) throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Object defaultDataSubId = getDefaultDataSubId();

        // to get getNetworkOperatorName(subId), @hide
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        Class telephonyManager;
        telephonyManager = Class.forName("android.telephony.TelephonyManager");
        Method getNetworkOperatorName = telephonyManager.getMethod("getNetworkOperatorName",
                int.class);
        Object operatorName = getNetworkOperatorName.invoke(tm, (int) defaultDataSubId);
        tvOpName.setText((String) operatorName);
        Log.i(TAG, "default data sub network operator name : " + operatorName);
    }

    /**
     * Access isImsRegistered one of appcompat_hiddenapi-dark-greylist,
     *
     * @param view
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public void isImsRegistered(View view) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Object defaultDataSubId = getDefaultDataSubId();

        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        Class telephonyManager;
        telephonyManager = Class.forName("android.telephony.TelephonyManager");
        Method isImsRegistered = telephonyManager.getMethod("isImsRegistered", int.class);
        Object imsREgStat = isImsRegistered.invoke(tm, defaultDataSubId);
        imsStat.setText((boolean) imsREgStat ? "true" : "false");
        Log.i(TAG, "isImsRegistered : " + imsREgStat);
//        Log.i(TAG, "tm.isImsRegistered : " + tm.isImsRegistered((int) defaultDataSubId));
        hasCarrierPrivileges((int) defaultDataSubId);

    }

    // appcompat_hiddenapi-dark-greylist
    private void hasCarrierPrivileges(int defaultDataSubId) {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        Log.i(TAG, "hasCarrierPrivileges : " + tm.hasCarrierPrivileges(defaultDataSubId));
    }

    public void isPhoneIdle(View view) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Class inProgressCallSession;
        inProgressCallSession = Class.forName("android.telephony.PreciseCallState");
        Method isPhoneIdle = inProgressCallSession.getMethod("getBackgroundCallState");
        Object imsREgStat = isPhoneIdle.invoke(inProgressCallSession);
        callState.setText((String) imsREgStat);
        Log.i(TAG, "call state : " + imsREgStat);
    }

    /**
     *  Access isPackageAccessible one of appcompat_hiddenapi-blacklist,
     *  but not print any warning log
     * @param view
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public void isPackageAccessible(View view) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Class inProgressCallSession;
        inProgressCallSession = Class.forName("sun.reflect.misc.ReflectUtil");
        Method isPhoneIdle = inProgressCallSession.getMethod("isPackageAccessible", Class.class);
        Object imsREgStat = isPhoneIdle.invoke(inProgressCallSession, Activity.class);
        callState.setText((boolean) imsREgStat ? "true" : "false");
        Log.i(TAG, "call state : " + imsREgStat);
    }

    private Object getDefaultDataSubId() throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        // get defaultDataSubId  @hide
        Class subscriptionManager;
        subscriptionManager = Class.forName("android.telephony.SubscriptionManager");
        Method getDefaultDataSubId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // this API opened on Android P
            getDefaultDataSubId = subscriptionManager.getMethod("getDefaultDataSubscriptionId");
        } else {
            getDefaultDataSubId = subscriptionManager.getMethod("getDefaultDataSubId");
        }
        return getDefaultDataSubId.invoke(subscriptionManager);
    }
}
