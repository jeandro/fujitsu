package com.factory.events.domain.port.out;

import java.time.Instant;

public interface ClockPort {
  Instant now();
}