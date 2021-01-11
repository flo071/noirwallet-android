package org.noirofficial;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.JobManager;
import com.facebook.soloader.SoLoader;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.orm.SchemaGenerator;
import com.orm.SugarContext;
import com.orm.SugarDb;

import org.noirofficial.presenter.activities.DisabledActivity;
import org.noirofficial.presenter.activities.LoginActivity;
import org.noirofficial.presenter.activities.utils.AssetVideos;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.database.Database;
import org.noirofficial.tools.manager.BRSharedPrefs;
import org.noirofficial.tools.manager.JobsHelper;
import org.noirofficial.tools.security.BRKeyStore;
import org.noirofficial.tools.util.BRConstants;
import io.fabric.sdk.android.Fabric;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;


/**
 * BreadWallet
 * <p/>
 * Created by Noah Seidman
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class NoirWallet extends Application implements
        Application.ActivityLifecycleCallbacks {
    public static final String HOST = "noirofficial.org";
    //public static final String FEE_URL = "https://go.digibyte.co/bws/api/v2/feelevels";

    private static NoirWallet application;

    public static NoirWallet getContext() {
        return application;
    }

    public boolean isSuspended() {
        return activeActivity == null;
    }

    // TODO: Unfortunately there's some part of the app that use Activity context and need
    // TODO: to be reworked accordingly; see getContext usages. This will not leak activity context
    // TODO: as the reference is removed in the lifecycle callback onPause invokation
    // activities
    private Activity activeActivity;

    public Activity getActivity() {
        return activeActivity;
    }

    static {
        System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Completable.fromRunnable(() -> {
            SugarContext.init(NoirWallet.this);
            AssetVideos.INSTANCE.init(this);
            SoLoader.init(this, false);
            Fabric.with(this, new Crashlytics());
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG);
            try {
                JobManager.create(this).addJobCreator(new JobsHelper.DigiByteJobCreator());
            } catch (Exception e) {
                Crashlytics.logException(e);
                Toast.makeText(this, R.string.recurring_payments_not_supported, Toast.LENGTH_SHORT).show();
            }
            BRSharedPrefs.putFeePerKb(this, 40000);
            Database.instance.init();

            //Useful for dropping and re-creating the recurring payments db
            //SugarContext.terminate();
            SchemaGenerator schemaGenerator = new SchemaGenerator(NoirWallet.this);
            //schemaGenerator.deleteTables(new SugarDb(this).getDB());
            schemaGenerator.createDatabase(new SugarDb(NoirWallet.this).getDB());
        }).subscribeOn(Schedulers.io()).subscribe();
        application = this;
        activeActivity = null;
        registerActivityLifecycleCallbacks(this);
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////// Implementation of ActivityLifecycleCallbacks interface //////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onActivityCreated(Activity anActivity, Bundle aBundle) {
    }

    @Override
    public void onActivityStarted(Activity anActivity) {
    }

    /**
     * This called is used to ensure that no matter what activity is being started or resumed
     * the application will go back to the login screen if the timeout period has been exceeded
     */
    @Override
    public void onActivityResumed(Activity anActivity) {
        activeActivity = anActivity;
        if (!(activeActivity instanceof DisabledActivity)
                && !(activeActivity instanceof LoginActivity)) {
            // lock wallet if 3 minutes passed
            long suspendedTime = BRSharedPrefs.getSuspendTime(anActivity);
            if (suspendedTime != 0 && (System.currentTimeMillis() - suspendedTime >= 180 * 1000)) {
                if (!BRKeyStore.getPinCode(activeActivity).isEmpty()) {
                    BRAnimator.startBreadActivity(activeActivity, true);
                }
            }
        }
        BRSharedPrefs.putSuspendTime(anActivity, 0);
    }

    @Override
    public void onActivityStopped(Activity anActivity) {
    }

    @Override
    public void onActivityDestroyed(Activity anActivity) {
    }

    @Override
    public void onActivityPaused(Activity anActivity) {
        activeActivity = null;
        BRSharedPrefs.putSuspendTime(anActivity, System.currentTimeMillis());
    }

    @Override
    public void onActivitySaveInstanceState(Activity anActivity, Bundle aBundle) {
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
    }
}