package com.watchlist.app.di;

import com.watchlist.app.data.remote.TmdbApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class AppModule_ProvideTmdbApiServiceFactory implements Factory<TmdbApiService> {
  private final Provider<OkHttpClient> clientProvider;

  public AppModule_ProvideTmdbApiServiceFactory(Provider<OkHttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public TmdbApiService get() {
    return provideTmdbApiService(clientProvider.get());
  }

  public static AppModule_ProvideTmdbApiServiceFactory create(
      Provider<OkHttpClient> clientProvider) {
    return new AppModule_ProvideTmdbApiServiceFactory(clientProvider);
  }

  public static TmdbApiService provideTmdbApiService(OkHttpClient client) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTmdbApiService(client));
  }
}
