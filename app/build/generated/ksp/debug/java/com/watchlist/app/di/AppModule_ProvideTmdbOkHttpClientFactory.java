package com.watchlist.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
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
public final class AppModule_ProvideTmdbOkHttpClientFactory implements Factory<OkHttpClient> {
  @Override
  public OkHttpClient get() {
    return provideTmdbOkHttpClient();
  }

  public static AppModule_ProvideTmdbOkHttpClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OkHttpClient provideTmdbOkHttpClient() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTmdbOkHttpClient());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideTmdbOkHttpClientFactory INSTANCE = new AppModule_ProvideTmdbOkHttpClientFactory();
  }
}
