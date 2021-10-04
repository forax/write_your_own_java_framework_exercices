package com.github.forax.framework.injector.scanner;

import com.github.forax.framework.injector.Inject;

import java.util.Objects;

public class OnlySetter {
  private Dependency dependency;

  @Inject
  public void setDependency(Dependency dependency) {
    this.dependency = Objects.requireNonNull(dependency);
  }

  public Dependency getDependency() {
    return dependency;
  }
}
