package com.watchlist.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.watchlist.app.data.local.entities.MediaItemEntity;
import com.watchlist.app.data.local.entities.MediaType;
import com.watchlist.app.data.local.entities.WatchStatus;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MediaItemDao_Impl implements MediaItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MediaItemEntity> __insertionAdapterOfMediaItemEntity;

  private final EntityDeletionOrUpdateAdapter<MediaItemEntity> __deletionAdapterOfMediaItemEntity;

  private final EntityDeletionOrUpdateAdapter<MediaItemEntity> __updateAdapterOfMediaItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public MediaItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMediaItemEntity = new EntityInsertionAdapter<MediaItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `media_items` (`id`,`title`,`originalTitle`,`posterPath`,`backdropPath`,`overview`,`mediaType`,`watchStatus`,`rating`,`totalEpisodes`,`watchedEpisodes`,`currentSeason`,`genre`,`year`,`platform`,`tmdbId`,`addedAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MediaItemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getOriginalTitle());
        statement.bindString(4, entity.getPosterPath());
        statement.bindString(5, entity.getBackdropPath());
        statement.bindString(6, entity.getOverview());
        statement.bindString(7, __MediaType_enumToString(entity.getMediaType()));
        statement.bindString(8, __WatchStatus_enumToString(entity.getWatchStatus()));
        statement.bindDouble(9, entity.getRating());
        statement.bindLong(10, entity.getTotalEpisodes());
        statement.bindLong(11, entity.getWatchedEpisodes());
        statement.bindLong(12, entity.getCurrentSeason());
        statement.bindString(13, entity.getGenre());
        statement.bindLong(14, entity.getYear());
        statement.bindString(15, entity.getPlatform());
        statement.bindLong(16, entity.getTmdbId());
        statement.bindLong(17, entity.getAddedAt());
        statement.bindLong(18, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfMediaItemEntity = new EntityDeletionOrUpdateAdapter<MediaItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `media_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MediaItemEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfMediaItemEntity = new EntityDeletionOrUpdateAdapter<MediaItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `media_items` SET `id` = ?,`title` = ?,`originalTitle` = ?,`posterPath` = ?,`backdropPath` = ?,`overview` = ?,`mediaType` = ?,`watchStatus` = ?,`rating` = ?,`totalEpisodes` = ?,`watchedEpisodes` = ?,`currentSeason` = ?,`genre` = ?,`year` = ?,`platform` = ?,`tmdbId` = ?,`addedAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MediaItemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getOriginalTitle());
        statement.bindString(4, entity.getPosterPath());
        statement.bindString(5, entity.getBackdropPath());
        statement.bindString(6, entity.getOverview());
        statement.bindString(7, __MediaType_enumToString(entity.getMediaType()));
        statement.bindString(8, __WatchStatus_enumToString(entity.getWatchStatus()));
        statement.bindDouble(9, entity.getRating());
        statement.bindLong(10, entity.getTotalEpisodes());
        statement.bindLong(11, entity.getWatchedEpisodes());
        statement.bindLong(12, entity.getCurrentSeason());
        statement.bindString(13, entity.getGenre());
        statement.bindLong(14, entity.getYear());
        statement.bindString(15, entity.getPlatform());
        statement.bindLong(16, entity.getTmdbId());
        statement.bindLong(17, entity.getAddedAt());
        statement.bindLong(18, entity.getUpdatedAt());
        statement.bindLong(19, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM media_items WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertItem(final MediaItemEntity item,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMediaItemEntity.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteItem(final MediaItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMediaItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateItem(final MediaItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMediaItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MediaItemEntity>> getAllItems() {
    final String _sql = "SELECT * FROM media_items ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"media_items"}, new Callable<List<MediaItemEntity>>() {
      @Override
      @NonNull
      public List<MediaItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfOriginalTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTitle");
          final int _cursorIndexOfPosterPath = CursorUtil.getColumnIndexOrThrow(_cursor, "posterPath");
          final int _cursorIndexOfBackdropPath = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropPath");
          final int _cursorIndexOfOverview = CursorUtil.getColumnIndexOrThrow(_cursor, "overview");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfWatchStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "watchStatus");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfTotalEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalEpisodes");
          final int _cursorIndexOfWatchedEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedEpisodes");
          final int _cursorIndexOfCurrentSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSeason");
          final int _cursorIndexOfGenre = CursorUtil.getColumnIndexOrThrow(_cursor, "genre");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfTmdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "tmdbId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MediaItemEntity> _result = new ArrayList<MediaItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MediaItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpOriginalTitle;
            _tmpOriginalTitle = _cursor.getString(_cursorIndexOfOriginalTitle);
            final String _tmpPosterPath;
            _tmpPosterPath = _cursor.getString(_cursorIndexOfPosterPath);
            final String _tmpBackdropPath;
            _tmpBackdropPath = _cursor.getString(_cursorIndexOfBackdropPath);
            final String _tmpOverview;
            _tmpOverview = _cursor.getString(_cursorIndexOfOverview);
            final MediaType _tmpMediaType;
            _tmpMediaType = __MediaType_stringToEnum(_cursor.getString(_cursorIndexOfMediaType));
            final WatchStatus _tmpWatchStatus;
            _tmpWatchStatus = __WatchStatus_stringToEnum(_cursor.getString(_cursorIndexOfWatchStatus));
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final int _tmpTotalEpisodes;
            _tmpTotalEpisodes = _cursor.getInt(_cursorIndexOfTotalEpisodes);
            final int _tmpWatchedEpisodes;
            _tmpWatchedEpisodes = _cursor.getInt(_cursorIndexOfWatchedEpisodes);
            final int _tmpCurrentSeason;
            _tmpCurrentSeason = _cursor.getInt(_cursorIndexOfCurrentSeason);
            final String _tmpGenre;
            _tmpGenre = _cursor.getString(_cursorIndexOfGenre);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final int _tmpTmdbId;
            _tmpTmdbId = _cursor.getInt(_cursorIndexOfTmdbId);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MediaItemEntity(_tmpId,_tmpTitle,_tmpOriginalTitle,_tmpPosterPath,_tmpBackdropPath,_tmpOverview,_tmpMediaType,_tmpWatchStatus,_tmpRating,_tmpTotalEpisodes,_tmpWatchedEpisodes,_tmpCurrentSeason,_tmpGenre,_tmpYear,_tmpPlatform,_tmpTmdbId,_tmpAddedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MediaItemEntity>> getItemsByType(final MediaType type) {
    final String _sql = "SELECT * FROM media_items WHERE mediaType = ? ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, __MediaType_enumToString(type));
    return CoroutinesRoom.createFlow(__db, false, new String[] {"media_items"}, new Callable<List<MediaItemEntity>>() {
      @Override
      @NonNull
      public List<MediaItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfOriginalTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTitle");
          final int _cursorIndexOfPosterPath = CursorUtil.getColumnIndexOrThrow(_cursor, "posterPath");
          final int _cursorIndexOfBackdropPath = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropPath");
          final int _cursorIndexOfOverview = CursorUtil.getColumnIndexOrThrow(_cursor, "overview");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfWatchStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "watchStatus");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfTotalEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalEpisodes");
          final int _cursorIndexOfWatchedEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedEpisodes");
          final int _cursorIndexOfCurrentSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSeason");
          final int _cursorIndexOfGenre = CursorUtil.getColumnIndexOrThrow(_cursor, "genre");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfTmdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "tmdbId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MediaItemEntity> _result = new ArrayList<MediaItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MediaItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpOriginalTitle;
            _tmpOriginalTitle = _cursor.getString(_cursorIndexOfOriginalTitle);
            final String _tmpPosterPath;
            _tmpPosterPath = _cursor.getString(_cursorIndexOfPosterPath);
            final String _tmpBackdropPath;
            _tmpBackdropPath = _cursor.getString(_cursorIndexOfBackdropPath);
            final String _tmpOverview;
            _tmpOverview = _cursor.getString(_cursorIndexOfOverview);
            final MediaType _tmpMediaType;
            _tmpMediaType = __MediaType_stringToEnum(_cursor.getString(_cursorIndexOfMediaType));
            final WatchStatus _tmpWatchStatus;
            _tmpWatchStatus = __WatchStatus_stringToEnum(_cursor.getString(_cursorIndexOfWatchStatus));
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final int _tmpTotalEpisodes;
            _tmpTotalEpisodes = _cursor.getInt(_cursorIndexOfTotalEpisodes);
            final int _tmpWatchedEpisodes;
            _tmpWatchedEpisodes = _cursor.getInt(_cursorIndexOfWatchedEpisodes);
            final int _tmpCurrentSeason;
            _tmpCurrentSeason = _cursor.getInt(_cursorIndexOfCurrentSeason);
            final String _tmpGenre;
            _tmpGenre = _cursor.getString(_cursorIndexOfGenre);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final int _tmpTmdbId;
            _tmpTmdbId = _cursor.getInt(_cursorIndexOfTmdbId);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MediaItemEntity(_tmpId,_tmpTitle,_tmpOriginalTitle,_tmpPosterPath,_tmpBackdropPath,_tmpOverview,_tmpMediaType,_tmpWatchStatus,_tmpRating,_tmpTotalEpisodes,_tmpWatchedEpisodes,_tmpCurrentSeason,_tmpGenre,_tmpYear,_tmpPlatform,_tmpTmdbId,_tmpAddedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MediaItemEntity>> getItemsByTypeAndStatus(final MediaType type,
      final WatchStatus status) {
    final String _sql = "SELECT * FROM media_items WHERE mediaType = ? AND watchStatus = ? ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, __MediaType_enumToString(type));
    _argIndex = 2;
    _statement.bindString(_argIndex, __WatchStatus_enumToString(status));
    return CoroutinesRoom.createFlow(__db, false, new String[] {"media_items"}, new Callable<List<MediaItemEntity>>() {
      @Override
      @NonNull
      public List<MediaItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfOriginalTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTitle");
          final int _cursorIndexOfPosterPath = CursorUtil.getColumnIndexOrThrow(_cursor, "posterPath");
          final int _cursorIndexOfBackdropPath = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropPath");
          final int _cursorIndexOfOverview = CursorUtil.getColumnIndexOrThrow(_cursor, "overview");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfWatchStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "watchStatus");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfTotalEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalEpisodes");
          final int _cursorIndexOfWatchedEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedEpisodes");
          final int _cursorIndexOfCurrentSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSeason");
          final int _cursorIndexOfGenre = CursorUtil.getColumnIndexOrThrow(_cursor, "genre");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfTmdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "tmdbId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MediaItemEntity> _result = new ArrayList<MediaItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MediaItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpOriginalTitle;
            _tmpOriginalTitle = _cursor.getString(_cursorIndexOfOriginalTitle);
            final String _tmpPosterPath;
            _tmpPosterPath = _cursor.getString(_cursorIndexOfPosterPath);
            final String _tmpBackdropPath;
            _tmpBackdropPath = _cursor.getString(_cursorIndexOfBackdropPath);
            final String _tmpOverview;
            _tmpOverview = _cursor.getString(_cursorIndexOfOverview);
            final MediaType _tmpMediaType;
            _tmpMediaType = __MediaType_stringToEnum(_cursor.getString(_cursorIndexOfMediaType));
            final WatchStatus _tmpWatchStatus;
            _tmpWatchStatus = __WatchStatus_stringToEnum(_cursor.getString(_cursorIndexOfWatchStatus));
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final int _tmpTotalEpisodes;
            _tmpTotalEpisodes = _cursor.getInt(_cursorIndexOfTotalEpisodes);
            final int _tmpWatchedEpisodes;
            _tmpWatchedEpisodes = _cursor.getInt(_cursorIndexOfWatchedEpisodes);
            final int _tmpCurrentSeason;
            _tmpCurrentSeason = _cursor.getInt(_cursorIndexOfCurrentSeason);
            final String _tmpGenre;
            _tmpGenre = _cursor.getString(_cursorIndexOfGenre);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final int _tmpTmdbId;
            _tmpTmdbId = _cursor.getInt(_cursorIndexOfTmdbId);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MediaItemEntity(_tmpId,_tmpTitle,_tmpOriginalTitle,_tmpPosterPath,_tmpBackdropPath,_tmpOverview,_tmpMediaType,_tmpWatchStatus,_tmpRating,_tmpTotalEpisodes,_tmpWatchedEpisodes,_tmpCurrentSeason,_tmpGenre,_tmpYear,_tmpPlatform,_tmpTmdbId,_tmpAddedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getItemById(final long id,
      final Continuation<? super MediaItemEntity> $completion) {
    final String _sql = "SELECT * FROM media_items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MediaItemEntity>() {
      @Override
      @Nullable
      public MediaItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfOriginalTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTitle");
          final int _cursorIndexOfPosterPath = CursorUtil.getColumnIndexOrThrow(_cursor, "posterPath");
          final int _cursorIndexOfBackdropPath = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropPath");
          final int _cursorIndexOfOverview = CursorUtil.getColumnIndexOrThrow(_cursor, "overview");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfWatchStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "watchStatus");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfTotalEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalEpisodes");
          final int _cursorIndexOfWatchedEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedEpisodes");
          final int _cursorIndexOfCurrentSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSeason");
          final int _cursorIndexOfGenre = CursorUtil.getColumnIndexOrThrow(_cursor, "genre");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfTmdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "tmdbId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final MediaItemEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpOriginalTitle;
            _tmpOriginalTitle = _cursor.getString(_cursorIndexOfOriginalTitle);
            final String _tmpPosterPath;
            _tmpPosterPath = _cursor.getString(_cursorIndexOfPosterPath);
            final String _tmpBackdropPath;
            _tmpBackdropPath = _cursor.getString(_cursorIndexOfBackdropPath);
            final String _tmpOverview;
            _tmpOverview = _cursor.getString(_cursorIndexOfOverview);
            final MediaType _tmpMediaType;
            _tmpMediaType = __MediaType_stringToEnum(_cursor.getString(_cursorIndexOfMediaType));
            final WatchStatus _tmpWatchStatus;
            _tmpWatchStatus = __WatchStatus_stringToEnum(_cursor.getString(_cursorIndexOfWatchStatus));
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final int _tmpTotalEpisodes;
            _tmpTotalEpisodes = _cursor.getInt(_cursorIndexOfTotalEpisodes);
            final int _tmpWatchedEpisodes;
            _tmpWatchedEpisodes = _cursor.getInt(_cursorIndexOfWatchedEpisodes);
            final int _tmpCurrentSeason;
            _tmpCurrentSeason = _cursor.getInt(_cursorIndexOfCurrentSeason);
            final String _tmpGenre;
            _tmpGenre = _cursor.getString(_cursorIndexOfGenre);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final int _tmpTmdbId;
            _tmpTmdbId = _cursor.getInt(_cursorIndexOfTmdbId);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new MediaItemEntity(_tmpId,_tmpTitle,_tmpOriginalTitle,_tmpPosterPath,_tmpBackdropPath,_tmpOverview,_tmpMediaType,_tmpWatchStatus,_tmpRating,_tmpTotalEpisodes,_tmpWatchedEpisodes,_tmpCurrentSeason,_tmpGenre,_tmpYear,_tmpPlatform,_tmpTmdbId,_tmpAddedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MediaItemEntity>> searchItems(final String query) {
    final String _sql = "SELECT * FROM media_items WHERE title LIKE '%' || ? || '%' ORDER BY title ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"media_items"}, new Callable<List<MediaItemEntity>>() {
      @Override
      @NonNull
      public List<MediaItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfOriginalTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTitle");
          final int _cursorIndexOfPosterPath = CursorUtil.getColumnIndexOrThrow(_cursor, "posterPath");
          final int _cursorIndexOfBackdropPath = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropPath");
          final int _cursorIndexOfOverview = CursorUtil.getColumnIndexOrThrow(_cursor, "overview");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfWatchStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "watchStatus");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfTotalEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalEpisodes");
          final int _cursorIndexOfWatchedEpisodes = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedEpisodes");
          final int _cursorIndexOfCurrentSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSeason");
          final int _cursorIndexOfGenre = CursorUtil.getColumnIndexOrThrow(_cursor, "genre");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfTmdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "tmdbId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MediaItemEntity> _result = new ArrayList<MediaItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MediaItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpOriginalTitle;
            _tmpOriginalTitle = _cursor.getString(_cursorIndexOfOriginalTitle);
            final String _tmpPosterPath;
            _tmpPosterPath = _cursor.getString(_cursorIndexOfPosterPath);
            final String _tmpBackdropPath;
            _tmpBackdropPath = _cursor.getString(_cursorIndexOfBackdropPath);
            final String _tmpOverview;
            _tmpOverview = _cursor.getString(_cursorIndexOfOverview);
            final MediaType _tmpMediaType;
            _tmpMediaType = __MediaType_stringToEnum(_cursor.getString(_cursorIndexOfMediaType));
            final WatchStatus _tmpWatchStatus;
            _tmpWatchStatus = __WatchStatus_stringToEnum(_cursor.getString(_cursorIndexOfWatchStatus));
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final int _tmpTotalEpisodes;
            _tmpTotalEpisodes = _cursor.getInt(_cursorIndexOfTotalEpisodes);
            final int _tmpWatchedEpisodes;
            _tmpWatchedEpisodes = _cursor.getInt(_cursorIndexOfWatchedEpisodes);
            final int _tmpCurrentSeason;
            _tmpCurrentSeason = _cursor.getInt(_cursorIndexOfCurrentSeason);
            final String _tmpGenre;
            _tmpGenre = _cursor.getString(_cursorIndexOfGenre);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final int _tmpTmdbId;
            _tmpTmdbId = _cursor.getInt(_cursorIndexOfTmdbId);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MediaItemEntity(_tmpId,_tmpTitle,_tmpOriginalTitle,_tmpPosterPath,_tmpBackdropPath,_tmpOverview,_tmpMediaType,_tmpWatchStatus,_tmpRating,_tmpTotalEpisodes,_tmpWatchedEpisodes,_tmpCurrentSeason,_tmpGenre,_tmpYear,_tmpPlatform,_tmpTmdbId,_tmpAddedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> countByType(final MediaType type) {
    final String _sql = "SELECT COUNT(*) FROM media_items WHERE mediaType = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, __MediaType_enumToString(type));
    return CoroutinesRoom.createFlow(__db, false, new String[] {"media_items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> countByStatus(final WatchStatus status) {
    final String _sql = "SELECT COUNT(*) FROM media_items WHERE watchStatus = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, __WatchStatus_enumToString(status));
    return CoroutinesRoom.createFlow(__db, false, new String[] {"media_items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __MediaType_enumToString(@NonNull final MediaType _value) {
    switch (_value) {
      case MOVIE: return "MOVIE";
      case SERIES: return "SERIES";
      case ANIME: return "ANIME";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private String __WatchStatus_enumToString(@NonNull final WatchStatus _value) {
    switch (_value) {
      case WATCHING: return "WATCHING";
      case COMPLETED: return "COMPLETED";
      case PLANNED: return "PLANNED";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private MediaType __MediaType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "MOVIE": return MediaType.MOVIE;
      case "SERIES": return MediaType.SERIES;
      case "ANIME": return MediaType.ANIME;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private WatchStatus __WatchStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "WATCHING": return WatchStatus.WATCHING;
      case "COMPLETED": return WatchStatus.COMPLETED;
      case "PLANNED": return WatchStatus.PLANNED;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
