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
public final class AddMediaViewModel_Factory implements Factory<AddMediaViewModel> {
  private final Provider<MediaRepository> repositoryProvider;

  public AddMediaViewModel_Factory(Provider<MediaRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public AddMediaViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static AddMediaViewModel_Factory create(Provider<MediaRepository> repositoryProvider) {
    return new AddMediaViewModel_Factory(repositoryProvider);
  }

  public static AddMediaViewModel newInstance(MediaRepository repository) {
    return new AddMediaViewModel(repository);
  }
}
