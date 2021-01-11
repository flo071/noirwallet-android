package com.platform.kvstore;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/25/15.
 * Copyright (c) 2016 breadwallet LLC
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

import static com.platform.sqlite.PlatformSqliteHelper.KV_STORE_TABLE_NAME;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.jniwrappers.BRKey;
import com.platform.sqlite.KVItem;
import com.platform.sqlite.PlatformSqliteHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.noirofficial.NoirWallet;
import org.noirofficial.tools.security.BRKeyStore;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.tools.util.BRConstants;
import org.noirofficial.tools.util.Utils;

public class ReplicatedKVStore {
    private static final String TAG = ReplicatedKVStore.class.getName();
    private static final String KEY_REGEX = "^[^_][\\w-]{1,255}$";
    private static byte[] tempAuthKey;
    private static ReplicatedKVStore instance;
    public final boolean encrypted = true;
    public final boolean encryptedReplication = true;
    // Database fields
//    private SQLiteDatabase readDb;
//    private SQLiteDatabase writeDb;
    private final PlatformSqliteHelper dbHelper;
    private final String[] allColumns = {
            PlatformSqliteHelper.KV_VERSION,
            PlatformSqliteHelper.KV_REMOTE_VERSION,
            PlatformSqliteHelper.KV_KEY,
            PlatformSqliteHelper.KV_VALUE,
            PlatformSqliteHelper.KV_TIME,
            PlatformSqliteHelper.KV_DELETED
    };
    //    private AtomicInteger mOpenCounter = new AtomicInteger();
    private SQLiteDatabase mDatabase;
    //    private Lock dbLock = new ReentrantLock();
    private Context mContext;

    private ReplicatedKVStore(Context context) {
        mContext = context;
        dbHelper = PlatformSqliteHelper.getInstance(context);
    }

    public static ReplicatedKVStore getInstance(Context context) {
        if (instance == null) {
            instance = new ReplicatedKVStore(context);
        }
        return instance;
    }

    /**
     * generate a nonce using microseconds-since-epoch
     */
    public static byte[] getNonce() {
        byte[] nonce = new byte[12];
        ByteBuffer buffer = ByteBuffer.allocate(8);
        long t = System.nanoTime() / 1000;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(t);
        byte[] byteTime = buffer.array();
        System.arraycopy(byteTime, 0, nonce, 4, byteTime.length);
        return nonce;
    }

    /**
     * encrypt some data using self.key
     */
    public static byte[] encrypt(byte[] data, Context app) {
        if (data == null) {
            Log.e(TAG, "encrypt: data is null");
            return null;
        }
        if (app == null) app = NoirWallet.getContext();
        if (app == null) {
            Log.e(TAG, "encrypt: app is null");
            return null;
        }
        if (tempAuthKey == null) retrieveAuthKey(app);
        if (Utils.isNullOrEmpty(tempAuthKey)) {
            Log.e(TAG, "encrypt: authKey is empty: " + (tempAuthKey == null ? null
                    : tempAuthKey.length));
            return null;
        }
        BRKey key = new BRKey(tempAuthKey);
        byte[] nonce = getNonce();
        if (Utils.isNullOrEmpty(nonce) || nonce.length != 12) {
            Log.e(TAG, "encrypt: nonce is invalid: " + (nonce == null ? null : nonce.length));
            return null;
        }
        byte[] encryptedData = key.encryptNative(data, nonce);
        if (Utils.isNullOrEmpty(encryptedData)) {
            Log.e(TAG, "encrypt: encryptNative failed: " + (encryptedData == null ? null
                    : encryptedData.length));
            return null;
        }
        //result is nonce + encryptedData
        byte[] result = new byte[nonce.length + encryptedData.length];
        System.arraycopy(nonce, 0, result, 0, nonce.length);
        System.arraycopy(encryptedData, 0, result, nonce.length, encryptedData.length);
        return result;
    }

    /**
     * decrypt some data using key
     */
    public static byte[] decrypt(byte[] data, Context app) {
        if (data == null || data.length <= 12) {
            Log.e(TAG, "decrypt: failed to decrypt: " + (data == null ? null : data.length));
            return null;
        }
        if (app == null) app = NoirWallet.getContext();
        if (app == null) return null;
        if (tempAuthKey == null) {
            retrieveAuthKey(app);
        }
        BRKey key = new BRKey(tempAuthKey);
        //12 bytes is the nonce
        return key.decryptNative(Arrays.copyOfRange(data, 12, data.length),
                Arrays.copyOfRange(data, 0, 12));
    }

    //store the authKey for 10 seconds (expensive operation)
    private static void retrieveAuthKey(Context context) {
        if (Utils.isNullOrEmpty(tempAuthKey)) {
            tempAuthKey = BRKeyStore.getAuthKey(context);
            if (tempAuthKey == null) Log.e(TAG, "retrieveAuthKey: FAILED, still null!");
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (tempAuthKey != null) {
                        Arrays.fill(tempAuthKey, (byte) 0);
                    }
                    tempAuthKey = null;
                }
            });
        }
    }

    public SQLiteDatabase getWritable() {
        if (mDatabase == null || !mDatabase.isOpen()) {
            mDatabase = dbHelper.getWritableDatabase();
        }
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WAL);
        return mDatabase;
    }

    public SQLiteDatabase getReadable() {
        return getWritable();
    }

    /**
     * Set the value of a key locally in the database. If syncImmediately is true (the default) then
     * immediately
     * after successfully saving locally, replicate to server. The `localVer` key must be the same
     * as is currently
     * stored in the database. To create a new key, pass `0` as `localVer`
     */
    public CompletionObject set(long version, String key, byte[] value, long time, int deleted) {
        KVItem entity = new KVItem(version, key, value, time, deleted);
        return set(entity);
    }

    public void set(List<KVItem> kvs) {
        for (KVItem kv : kvs) {
            set(kv);
        }
    }

    public CompletionObject set(KVItem kv) {
        try {
            if (isKeyValid(kv.key)) {
                CompletionObject obj = new CompletionObject(
                        CompletionObject.RemoteKVStoreError.unknown);

                try {
                    obj = _set(kv);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return obj;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
    }

    public void set(KVItem[] kvEntities) {
        for (KVItem kv : kvEntities) {
            set(kv);
        }
    }

    private synchronized CompletionObject _set(KVItem kv) throws Exception {
        Log.d(TAG, "_set: " + kv.key);
        long localVer = kv.version;
        long newVer = 0;
        long time = System.currentTimeMillis();
        String key = kv.key;

        long curVer = _localVersion(key).version;
        if (curVer != localVer) {
            Log.e(TAG, String.format("set key %s conflict: version %d != current version %d", key,
                    localVer, curVer));
            return new CompletionObject(CompletionObject.RemoteKVStoreError.conflict);
        }
        newVer = curVer + 1;
        byte[] encryptionData = encrypted ? encrypt(kv.value, mContext) : kv.value;
        SQLiteDatabase db = getWritable();
        try {
            db.beginTransaction();
            boolean success = insert(new KVItem(newVer, key, encryptionData, time, kv.deleted));
            if (!success) return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "_set: ", e);
        } finally {
            db.endTransaction();
        }
        return new CompletionObject(newVer, time, null);
    }

    private boolean insert(KVItem kv) {
        try {
            SQLiteDatabase db = getWritable();
            ContentValues values = new ContentValues();
            if (kv.version != -1) {
                values.put(PlatformSqliteHelper.KV_VERSION, kv.version);
            }
            values.put(PlatformSqliteHelper.KV_KEY, kv.key);
            values.put(PlatformSqliteHelper.KV_VALUE, kv.value);
            values.put(PlatformSqliteHelper.KV_TIME, kv.time);
            values.put(PlatformSqliteHelper.KV_DELETED, kv.deleted);
            long n = db.insertWithOnConflict(KV_STORE_TABLE_NAME, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
            if (n == -1) {
                //try updating if inserting failed
                n = db.updateWithOnConflict(KV_STORE_TABLE_NAME, values, "key=?",
                        new String[]{kv.key}, SQLiteDatabase.CONFLICT_REPLACE);
            }
            return n != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * get kv by key and version (version can be 0)
     */
    public CompletionObject get(String key, long version) {
        KVItem kv = null;
        Cursor cursor = null;
        long curVer = 0;

        try {
            //if no version, fine the version
            SQLiteDatabase db = getReadable();
            if (version == 0) {
                curVer = _localVersion(key).version;
            } else {
                //if we have a version, check if it's correct
                cursor = db.query(KV_STORE_TABLE_NAME,
                        allColumns, "key = ? AND version = ?",
                        new String[]{key, String.valueOf(version)},
                        null, null, "version DESC", "1");
                if (cursor.moveToNext()) {
                    curVer = cursor.getLong(0);
                } else {
                    curVer = 0;
                }
            }

            //if still 0 then version is non-existent or wrong.
            if (curVer == 0) {
                return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
            }
            if (cursor != null) cursor.close();
            cursor = db.query(KV_STORE_TABLE_NAME,
                    allColumns, "key = ? AND version = ?",
                    new String[]{key, String.valueOf(curVer)},
                    null, null, "version DESC", "1");
            if (cursor.moveToNext()) {
                kv = cursorToKv(cursor);
            }
            if (kv != null) {
                byte[] val = kv.value;
                kv.value = encrypted ? decrypt(val, mContext) : val;
                if (val != null && Utils.isNullOrEmpty(kv.value)) {
                    //decrypting failed
                    Log.e(TAG, "get: Decrypting failed for key: " + key + ", deleting the kv");
                    delete(key, curVer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return kv == null ? new CompletionObject(CompletionObject.RemoteKVStoreError.notFound)
                : new CompletionObject(kv, null);
    }

    /**
     * Gets the local version of the provided key, or 0 if it doesn't exist
     */

    public CompletionObject localVersion(String key) {
        if (isKeyValid(key)) {
            return _localVersion(key);
        } else {
            Log.e(TAG, "Key is invalid: " + key);
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
    }

    private synchronized CompletionObject _localVersion(String key) {
        long version = 0;
        long time = System.currentTimeMillis();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadable();
            String selectQuery = "SELECT " + PlatformSqliteHelper.KV_VERSION + ", "
                    + PlatformSqliteHelper.KV_TIME + " FROM " + KV_STORE_TABLE_NAME
                    + " WHERE key = ? ORDER BY version DESC LIMIT 1";
            cursor = db.rawQuery(selectQuery, new String[]{key});
            if (cursor.moveToNext()) {
                version = cursor.getLong(0);
                time = cursor.getLong(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new CompletionObject(version, time, null);
    }

    public synchronized void deleteAllKVs() {
        try {
            SQLiteDatabase db = getWritable();
            db.delete(KV_STORE_TABLE_NAME, null, null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<KVItem> getRawKVs() {
        List<KVItem> kvs = new ArrayList<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadable();
            String selectQuery =
                    "SELECT kvs.version, kvs.remote_version, kvs.key, kvs.value, kvs.thetime, kvs"
                            + ".deleted FROM "
                            + PlatformSqliteHelper.KV_STORE_TABLE_NAME + " kvs " +
                            "INNER JOIN ( " +
                            "   SELECT MAX(version) AS latest_version, key " +
                            "   FROM " + PlatformSqliteHelper.KV_STORE_TABLE_NAME +
                            "   GROUP BY " + PlatformSqliteHelper.KV_KEY +
                            " ) vermax " +
                            "ON kvs.version = vermax.latest_version " +
                            "AND kvs.key = vermax.key";

            cursor = db.rawQuery(selectQuery, null);

            while (cursor.moveToNext()) {
                KVItem kvItem = cursorToKv(cursor);
                if (kvItem != null) {
                    kvs.add(kvItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return kvs;
    }

    public List<KVItem> getAllTxMdKv() {
        List<KVItem> kvs = new ArrayList<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadable();
            String selectQuery =
                    "SELECT kvs.version, kvs.remote_version, kvs.key, kvs.value, kvs.thetime, kvs"
                            + ".deleted FROM kvStoreTable kvs "
                            +
                            "INNER JOIN ( SELECT MAX(version) AS latest_version, key FROM "
                            + "kvStoreTable where key like 'txn2-%' GROUP BY key ) vermax ON kvs"
                            + ".version = vermax.latest_version AND kvs.key = vermax.key";
            cursor = db.rawQuery(selectQuery, null);
            while (cursor.moveToNext()) {
                KVItem kvItem = cursorToKv(cursor);
                if (kvItem != null) {
                    byte[] val = kvItem.value;
                    kvItem.value = encrypted ? decrypt(val, mContext) : val;
                    kvs.add(kvItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return kvs;
    }

    private KVItem cursorToKv(Cursor cursor) {
        long version = 0;
        String key = null;
        byte[] value = null;
        long time = 0;
        int deleted = 0;
        try {
            version = cursor.getLong(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            key = cursor.getString(2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Utils.isNullOrEmpty(key)) return null;
        try {
            value = cursor.getBlob(3);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            time = cursor.getLong(4);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            deleted = cursor.getInt(5);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new KVItem(version, key, value, time, deleted);
    }

    private List<String> getKeysFromKVEntity(List<KVItem> entities) {
        List<String> keys = new ArrayList<>();
        for (KVItem kv : entities) {
            keys.add(kv.key);
        }
        return keys;
    }

    /**
     * Mark a key as removed locally. If syncImmediately is true (the defualt) then immediately mark
     * the key
     * as removed on the server as well. `localVer` must match the most recent version in the local
     * database.
     */
    public CompletionObject delete(String key, long localVersion) {
        try {
            Log.i(TAG, "kv deleted with key: " + key);
            if (isKeyValid(key)) {
                CompletionObject obj = new CompletionObject(
                        CompletionObject.RemoteKVStoreError.unknown);
                try {
                    obj = _delete(key, localVersion);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return obj;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
    }

    private synchronized CompletionObject _delete(String key, long localVersion) throws Exception {
        if (localVersion == 0) {
            return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
        }
        long newVer = 0;
        long time = System.currentTimeMillis();
        Cursor cursor = null;
        try {
            long curVer = _localVersion(key).version;
            if (curVer != localVersion) {
                Log.e(TAG,
                        String.format("del key %s conflict: version %d != current version %d", key,
                                localVersion, curVer));
                return new CompletionObject(CompletionObject.RemoteKVStoreError.conflict);
            }
            SQLiteDatabase db = getWritable();
            try {
                db.beginTransaction();
                Log.i(TAG, String.format("DEL key: %s ver: %d", key, curVer));
                newVer = curVer + 1;
                cursor = db.query(KV_STORE_TABLE_NAME,
                        new String[]{PlatformSqliteHelper.KV_VALUE}, "key = ? AND version = ?",
                        new String[]{key, String.valueOf(localVersion)},
                        null, null, "version DESC", "1");
                byte[] value = null;
                if (cursor.moveToNext()) {
                    value = cursor.getBlob(0);
                }

                if (Utils.isNullOrEmpty(value)) throw new NullPointerException("cannot be empty");
                KVItem kvToInsert = new KVItem(newVer, key, value, time, 1);
                insert(kvToInsert);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return new CompletionObject(newVer, time, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
    }

    /**
     * validates the key. kvs can not start with a _
     */
    private boolean isKeyValid(String key) {
        Pattern pattern = Pattern.compile(KEY_REGEX);
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return true;
        }
        Log.e(TAG, "checkKey: found illegal patterns, key: " + key);
        return false;
    }
}