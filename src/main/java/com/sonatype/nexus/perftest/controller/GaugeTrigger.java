package com.sonatype.nexus.perftest.controller;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class GaugeTrigger<T extends Comparable>
    extends AttributeTrigger
    implements Trigger
{
  private final Attribute<T> attribute;

  private final Consumer<T> consumer;

  private T highThreshold;

  private T lowThreshold;

  private Duration sustainPeriod;

  private State lastState = State.NORMAL;

  private LocalDateTime stateStartTime = LocalDateTime.now();

  private boolean notified = true;

  public GaugeTrigger(final Attribute<T> attribute, final Consumer<T> consumer) {
    this.attribute = checkNotNull(attribute);
    this.consumer = checkNotNull(consumer);
  }

  @Override
  protected void check() {
    T currentValue = getClient().get(attribute);
    if (currentValue != null) {
      State currentState = calculateState(currentValue);
      if (!currentState.equals(lastState)) {
        stateStartTime = LocalDateTime.now();
        notified = false;
      }
      if (!notified) {
        Duration sinceStateStart = Duration.between(stateStartTime, LocalDateTime.now());
        if (sustainPeriod == null || sinceStateStart.compareTo(sustainPeriod) >= 0) {
          notified = true;
          consumer.accept(currentState, currentValue);
        }
      }
      lastState = currentState;
    }
  }

  private State calculateState(final T value) {
    if (highThreshold != null && value.compareTo(highThreshold) >= 0) {
      return State.HIGH;
    }
    if (lowThreshold != null && value.compareTo(lowThreshold) < 0) {
      return State.LOW;
    }
    return State.NORMAL;
  }

  public T getHighThreshold() {
    return highThreshold;
  }

  public GaugeTrigger<T> setHighThreshold(final T highThreshold) {
    this.highThreshold = highThreshold;
    return this;
  }

  public T getLowThreshold() {
    return lowThreshold;
  }

  public GaugeTrigger<T> setLowThreshold(final T lowThreshold) {
    this.lowThreshold = lowThreshold;
    return this;
  }

  public Duration getSustainPeriod() {
    return sustainPeriod;
  }

  public GaugeTrigger<T> setSustainPeriod(final Duration sustainPeriod) {
    this.sustainPeriod = sustainPeriod;
    return this;
  }

  @Override
  public String toString() {
    return "Threshold " + attribute + "(" + highThreshold + ")";
  }

  public enum State
  {
    HIGH, NORMAL, LOW
  }

  @FunctionalInterface
  public interface Consumer<T>
  {
    void accept(State state, T t);
  }
}

