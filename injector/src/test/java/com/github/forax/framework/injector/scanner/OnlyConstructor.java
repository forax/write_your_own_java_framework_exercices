package com.github.forax.framework.injector.scanner;

import com.github.forax.framework.injector.Inject;

import java.util.Objects;

public record OnlyConstructor(Dependency dependency) {
  @Inject
  public OnlyConstructor {
    Objects.requireNonNull(dependency);
  }
}
