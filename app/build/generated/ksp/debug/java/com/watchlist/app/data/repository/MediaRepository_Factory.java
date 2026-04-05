package com.watchlist.app.data.repository;

import com.watchlist.app.data.local.dao.MediaItemDao;
import com.watchlist.app.data.remote.NewsApiService;
import com.watchlist.app.data.remote.TmdbApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class MediaRepository_Factory implements Factory<MediaRepository> {
  private final Provider<MediaItemDao> daoProvider;

  private final Provider<TmdbApiService> tmdbApiProvider;

  private final Provider<NewsApiService> newsApiProvider;

  public MediaRepository_Factory(Provider<MediaItemDao> daoProvider,
      Provider<TmdbApiService> tmdbApiProvider, Provider<NewsApiService> newsApiProvider) {
    this.daoProvider = daoProvider;
    this.tmdbApiProvider = tmdbApiProvider;
    this.newsApiProvider = newsApiProvider;
  }

  @Override
  public MediaRepository get() {
    return newInstance(daoProvider.get(), tmdbApiProvider.get(), newsApiProvider.get());
  }

  public static MediaRepository_Factory create(Provider<MediaItemDao> daoProvider,
      Provider<TmdbApiService> tmdbApiProvider, Provider<NewsApiService> newsApiProvider) {
    return new MediaRepository_Factory(daoProvider, tmdbApiProvider, newsApiProvider);
  }

  public static MediaRepository newInstance(MediaItemDao dao, TmdbApiService tmdbApi,
      NewsApiService newsApi) {
    return new MediaRepository(dao, tmdbApi, newsApi);
  }
}
