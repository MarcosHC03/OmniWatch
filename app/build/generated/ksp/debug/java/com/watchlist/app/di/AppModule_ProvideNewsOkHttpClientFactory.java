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
public final class AppModule_ProvideNewsOkHttpClientFactory implements Factory<OkHttpClient> {
  @Override
  public OkHttpClient get() {
    return provideNewsOkHttpClient();
  }

  public static AppModule_ProvideNewsOkHttpClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OkHttpClient provideNewsOkHttpClient() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNewsOkHttpClient());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideNewsOkHttpClientFactory INSTANCE = new AppModule_ProvideNewsOkHttpClientFactory();
  }
}
