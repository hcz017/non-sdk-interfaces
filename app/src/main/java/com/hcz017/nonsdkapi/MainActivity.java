package com.hcz017.nonsdkapi;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvOpName;
    private TextView imsStat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvOpName = findViewById(R.id.operator_name);
        imsStat = findViewById(R.id.ims_reg_sta);
        findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvOpName.setText("");
                imsStat.setText("");
            }
        });
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
        Object defaultDataSubId = getDefaultDataSubId.invoke(null);

        // to get getNetworkOperatorName(subId), @hide
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        Class telephonyManager;
        telephonyManager = Class.forName("android.telephony.TelephonyManager");
        Method getNetworkOperatorName = telephonyManager.getMethod("getNetworkOperatorName",
                int.class);
        getNetworkOperatorName.setAccessible(true);
        Object operatorName = getNetworkOperatorName.invoke(tm, defaultDataSubId);
        tvOpName.setText((String) operatorName);
        Log.i(TAG, "default data sub network operator name : " + operatorName);
    }

    /**
     * Access isImsRegistered one of appcompat_hiddenapi-dark-greylist,
     * but log prints (light greylist, reflection)
     *
     * @param view
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public void isImsRegistered(View view) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        Class telephonyManager;
        telephonyManager = Class.forName("android.telephony.TelephonyManager");
        Method isImsRegistered = telephonyManager.getMethod("isImsRegistered");
        isImsRegistered.setAccessible(true);
        Object imsREgStat = isImsRegistered.invoke(tm);
        imsStat.setText((boolean) imsREgStat ? "true" : "false");
        Log.i(TAG, "isImsRegistered : " + imsREgStat);
    }
}
