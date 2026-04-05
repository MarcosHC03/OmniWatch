package com.watchlist.app.viewmodel;

import com.watchlist.app.data.repository.MediaRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class MyListViewModel_Factory implements Factory<MyListViewModel> {
  private final Provider<MediaRepository> repositoryProvider;

  public MyListViewModel_Factory(Provider<MediaRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public MyListViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static MyListViewModel_Factory create(Provider<MediaRepository> repositoryProvider) {
    return new MyListViewModel_Factory(repositoryProvider);
  }

  public static MyListViewModel newInstance(MediaRepository repository) {
    return new MyListViewModel(repository);
  }
}
