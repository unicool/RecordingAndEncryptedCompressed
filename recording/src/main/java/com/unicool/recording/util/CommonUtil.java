package com.unicool.recording.util;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.unicool.recording.model.Itf;
import com.unicool.recording.model.Rsc;

import java.math.BigInteger;
import java.security.SecureRandom;


/*
 *  @项目名：  NationalCar-Android 
 *  @包名：    com.uniool.recording.service
 *  @文件名:   CommonUtil
 *  @创建者:   cjf
 *  @创建时间:  2017/9/30 13:01
 *  @描述：    TODO
 */
public class CommonUtil {

    /**
     * 车机使用序列号，其他设备使用ANDROID_ID
     * 如果AndroidId也为空，生成一个
     *
     * @param context
     * @return
     */
    public static String getAlias(Context context) {
        String alias = Rsc.alias;
        if (TextUtils.isEmpty(alias)) {
            alias = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (TextUtils.isEmpty(alias) || alias.equals("9774d56d682e549c") || alias.length() < 15) {
                synchronized (alias) {
                    //if ANDROID_ID is null, or it's equals to the GalaxyTab generic ANDROID_ID or bad, generates a new one
                    final SecureRandom random = new SecureRandom();
                    alias = new BigInteger(64, random).toString(16);
                }
            }
            Rsc.alias = alias;
            Log.d(Itf.TAG, alias);
        }
        return alias;
    }
}
