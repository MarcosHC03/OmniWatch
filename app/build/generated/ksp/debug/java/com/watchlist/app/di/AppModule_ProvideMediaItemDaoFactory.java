package com.watchlist.app.di;

import com.watchlist.app.data.local.WatchListDatabase;
import com.watchlist.app.data.local.dao.MediaItemDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class AppModule_ProvideMediaItemDaoFactory implements Factory<MediaItemDao> {
  private final Provider<WatchListDatabase> dbProvider;

  public AppModule_ProvideMediaItemDaoFactory(Provider<WatchListDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public MediaItemDao get() {
    return provideMediaItemDao(dbProvider.get());
  }

  public static AppModule_ProvideMediaItemDaoFactory create(
      Provider<WatchListDatabase> dbProvider) {
    return new AppModule_ProvideMediaItemDaoFactory(dbProvider);
  }

  public static MediaItemDao provideMediaItemDao(WatchListDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMediaItemDao(db));
  }
}
