package org.noirofficial.tools.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "asset_names")
public class AssetName {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "hash")
    private String hash;

    @ColumnInfo(name = "asset_name")
    private String assetName;

    public int getId() {
        return id;
    }

    public void setId(int uid) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }
}