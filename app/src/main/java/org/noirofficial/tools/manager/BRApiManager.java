package org.noirofficial.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.noirofficial.NoirWallet;
import org.noirofficial.presenter.entities.CurrencyEntity;
import org.noirofficial.tools.sqlite.CurrencyDataSource;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.tools.util.Utils;
import okhttp3.Request;
import okhttp3.Response;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BRApiManager {
    private static final String TAG = BRApiManager.class.getName();

    private static BRApiManager instance;

    public static BRApiManager getInstance() {

        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context) {
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = fetchRates(context);
            //Previously the fee per kb was updated here
            //No need, this is just for rates, and fee/kb will be updated when sending
            if (arr != null) {
                int length = arr.length();
                for (int i = 0; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.rate = (float) tmpObj.getDouble("rate");
                        String selectedISO = BRSharedPrefs.getIso(context);
                        if (tmp.code.equalsIgnoreCase(selectedISO)) {
                            BRSharedPrefs.putIso(context, tmp.code);
                            BRSharedPrefs.putCurrencyListPosition(context, i);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    set.add(tmp);
                }
            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List tempList = new ArrayList<>(set);
        Collections.reverse(tempList);
        return new LinkedHashSet<>(set);
    }

    public void asyncUpdateCurrencyData(final Context context) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            Set<CurrencyEntity> tmp = getCurrencies((Activity) context);
            CurrencyDataSource.getInstance(context).putCurrencies(tmp);
        });
    }

//    public void asyncUpdateFeeData(final Context context) {
//        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
//                () -> updateFeePerKb(context));
//    }

    private static JSONArray fetchRates(Activity activity) {
        String jsonString = urlGET(activity, "https://" + NoirWallet.HOST + "/price/");
        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            jsonArray = new JSONArray(jsonString);

        } catch (JSONException ignored) {
        }
        return jsonArray;
    }

//    public static boolean updateFeePerKb(Context app) {
//        String jsonString = urlGET(app, DigiByte.FEE_URL);
//        long fee = 0;
//        long economyFee = 0;
//        try {
//            JSONArray array = new JSONArray(jsonString);
//            for (int i = 0; i < array.length(); i++) {
//                JSONObject feeObject = array.getJSONObject(i);
//                switch(feeObject.getString("level")) {
//                    case "economy":
//                        economyFee = feeObject.getLong("feePerKb");
//                        break;
//                    case "normal":
//                        fee = feeObject.getLong("feePerKb");
//                        break;
//                }
//            }
//            BRSharedPrefs.putFeePerKb(app, fee);
//            BRSharedPrefs.putEconomyFeePerKb(app, economyFee);
//            BRWalletManager.getInstance().setFeePerKb(economyFee, false); //todo improve that
// logic
//            BRSharedPrefs.putFeeTime(app, System.currentTimeMillis()); //store the time of the
// last successful fee fetch
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }

    public String getBlockInfo(Context app, String blockUrl) {
        return urlGET(app, blockUrl);
    }

    private static String urlGET(Context app, String myURL) {
        Request request = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get().build();
        String response = null;
        Response resp = APIClient.getInstance(app).sendRequest(request);

        try {
            if (resp == null) {
                Log.e(TAG, "urlGET: " + myURL + ", resp is null");
                return null;
            }
            response = resp.body().string();
            String strDate = resp.header("date");
            if (strDate == null) {
                Log.e(TAG, "urlGET: strDate is null!");
                return response;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } finally {
            if (resp != null) resp.close();

        }
        return response;
    }
}