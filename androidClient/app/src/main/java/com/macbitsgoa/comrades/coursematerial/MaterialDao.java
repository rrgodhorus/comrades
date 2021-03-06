package com.macbitsgoa.comrades.coursematerial;

import android.database.Cursor;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

/**
 * @author aayush singla
 */

@Dao
public interface MaterialDao {

    @Query("SELECT * FROM CourseMaterial WHERE courseId = :courseId ")
    LiveData<List<CourseMaterial>> getCourseMaterial(String courseId);

    @Query("SELECT * FROM CourseMaterial WHERE hashId = :hashId ")
    CourseMaterial checkHashId(String hashId);

    @Query("SELECT * FROM CourseMaterial WHERE courseId = :courseId " +
            "AND fileName LIKE :search OR addedBy LIKE :search")
    LiveData<List<CourseMaterial>> searchMaterial(String courseId, String search);

    @Query("SELECT * FROM CourseMaterial WHERE courseId = :courseId " +
            "AND fileName LIKE :search OR addedBy LIKE :search")
    Cursor searchMaterialCursor(String courseId, String search);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMaterial(CourseMaterial material);

    @Update()
    void updateMaterial(CourseMaterial material);

    @Delete()
    void deleteMaterial(CourseMaterial material);


}
