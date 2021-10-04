package com.github.forax.framework.injector.scanner;

import com.github.forax.framework.injector.Inject;

import java.util.Objects;

public class AutoScanned {
  private final Dependency dependency;
  private String text;

  @Inject
  public AutoScanned(Dependency dependency) {
    this.dependency = Objects.requireNonNull(dependency);
  }

  @Inject
  public void setText(String text) {
    this.text = Objects.requireNonNull(text);
  }

  public Dependency getDependency() {
    return dependency;
  }

  public String getText() {
    return text;
  }
}
