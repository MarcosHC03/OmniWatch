package com.watchlist.app.di;

import com.watchlist.app.data.remote.NewsApiService;
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
public final class AppModule_ProvideNewsApiServiceFactory implements Factory<NewsApiService> {
  private final Provider<OkHttpClient> clientProvider;

  public AppModule_ProvideNewsApiServiceFactory(Provider<OkHttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public NewsApiService get() {
    return provideNewsApiService(clientProvider.get());
  }

  public static AppModule_ProvideNewsApiServiceFactory create(
      Provider<OkHttpClient> clientProvider) {
    return new AppModule_ProvideNewsApiServiceFactory(clientProvider);
  }

  public static NewsApiService provideNewsApiService(OkHttpClient client) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNewsApiService(client));
  }
}
