package org.noirofficial.tools.database;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.noirofficial.NoirWallet;
import org.noirofficial.tools.list.items.ListItemTransactionData;

public class Database {
    public static Database instance = new Database();
    public TransactionDao transactionsDao;
    public AssetNameDao assetNamesDao;
    private Executor executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    public List<DigiTransaction> transactions = new LinkedList<>();
    public List<AssetName> assetNames = new LinkedList<>();

    public interface TransacionStoreListener {
        void onTransactionsUpdate();
    }

    public void init() {
        AppDatabase database = Room.databaseBuilder(NoirWallet.getContext(),
                AppDatabase.class, "transaction_database")
                .addMigrations(new TransactionsMigration(6, 23)).fallbackToDestructiveMigration().build();
        transactionsDao = database.transactionDao();
        assetNamesDao = database.assetNameDao();
        updateTransactions(null);
        updateAssetNames(null);
    }

    private class TransactionsMigration extends Migration {

        /**
         * Creates a new migration between {@code startVersion} and {@code endVersion}.
         *
         * @param startVersion The start version of the database.
         * @param endVersion   The end version of the database after this migration is applied.
         */
        public TransactionsMigration(int startVersion, int endVersion) {
            super(startVersion, endVersion);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS `asset_names`");
            database.execSQL("CREATE TABLE IF NOT EXISTS `asset_names` (`id` INTEGER NOT NULL, `hash` TEXT, `asset_name` TEXT, PRIMARY KEY(`id`))");
        }
    }

    public void saveTransaction(byte[] txHash, String amount) {
        executor.execute(() -> {
            DigiTransaction digiTransaction = new DigiTransaction();
            digiTransaction.setTxHash(txHash);
            digiTransaction.setTxAmount(amount);
            transactionsDao.insertAll(digiTransaction);
            transactions = transactionsDao.getAll();
        });
    }

    public void saveAssetName(String asset_name, ListItemTransactionData... transactions) {
        if (TextUtils.isEmpty(asset_name)) {
            return;
        }
        executor.execute(() -> {
            for (ListItemTransactionData listItemTransactionData : transactions) {
                AssetName currentAssetName = findAssetNameFromHash(listItemTransactionData.transactionItem.txReversed);
                if (currentAssetName == null || !asset_name.equals(currentAssetName.getAssetName())) {
                    AssetName assetName = new AssetName();
                    assetName.setHash(listItemTransactionData.transactionItem.txReversed);
                    assetName.setAssetName(asset_name);
                    assetNamesDao.insertAll(assetName);
                    assetNames = assetNamesDao.getAll();
                    handler.post(listItemTransactionData::updateAssetName);
                }
            }
        });
    }

    private void updateTransactions(TransacionStoreListener transacionStoreListener) {
        executor.execute(() -> {
            transactions = transactionsDao.getAll();
            handler.post(() -> {
                if (transacionStoreListener != null) {
                    transacionStoreListener.onTransactionsUpdate();
                }
            });
        });
    }

    private void updateAssetNames(TransacionStoreListener transacionStoreListener) {
        executor.execute(() -> {
            assetNames = assetNamesDao.getAll();
            handler.post(() -> {
                if (transacionStoreListener != null) {
                    transacionStoreListener.onTransactionsUpdate();
                }
            });
        });
    }

    public boolean containsTransaction(byte[] txHash) {
        return findTransaction(txHash) != null;
    }

    public DigiTransaction findTransaction(byte[] txHash) {
        for (DigiTransaction digiTransaction : transactions) {
            if (Arrays.equals(txHash, digiTransaction.getTxHash())) {
                return digiTransaction;
            }
        }
        return null;
    }

    public AssetName findAssetNameFromHash(String hash) {
        for (AssetName assetName : assetNames) {
            if (assetName.getHash().equals(hash)) {
                return assetName;
            }
        }
        return null;
    }

    public void deleteNames() {
        assetNamesDao.nukeTable();
    }
}