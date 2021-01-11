package org.noirofficial.tools.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {DigiTransaction.class, AssetName.class}, version = 23)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();

    abstract AssetNameDao assetNameDao();
}