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
public final class ReleasesViewModel_Factory implements Factory<ReleasesViewModel> {
  private final Provider<MediaRepository> repositoryProvider;

  public ReleasesViewModel_Factory(Provider<MediaRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ReleasesViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ReleasesViewModel_Factory create(Provider<MediaRepository> repositoryProvider) {
    return new ReleasesViewModel_Factory(repositoryProvider);
  }

  public static ReleasesViewModel newInstance(MediaRepository repository) {
    return new ReleasesViewModel(repository);
  }
}
