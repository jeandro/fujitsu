package com.factory.events.adapter.in.web;

import com.factory.events.adapter.in.web.dto.EventRequestDTO;
import com.factory.events.adapter.in.web.dto.EventResponseDTO;
import com.factory.events.adapter.in.web.dto.EventResponsePageDTO;
import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.RegisterEventCommand;
import com.factory.events.domain.model.result.EventPage;
import java.util.List;
import org.jspecify.annotations.NonNull;

public final class EventApiMapper {

  private EventApiMapper() {}

  public static RegisterEventCommand toCommand(EventRequestDTO dto) {
    return new RegisterEventCommand(
      dto.eventId(),
      dto.eventType(),
      dto.description(),
      dto.timestamp()
    );
  }

  @NonNull
  public static EventResponseDTO toResponse(FactoryEvent event, boolean includeConfidential) {
    return new EventResponseDTO(
      event.eventId(),
      event.eventType(),
      includeConfidential ? event.description() : null,
      event.timestamp()
    );
  }

  @NonNull
  public static EventResponsePageDTO toResponsePage(EventPage page, boolean includeConfidential) {
    List<EventResponseDTO> content = page.content()
      .stream()
      .map(e -> toResponse(e, includeConfidential))
      .toList();
    return new EventResponsePageDTO(
      content,
      page.page(),
      page.size(),
      List.of("timestamp,desc"),
      page.totalElements(),
      page.totalPages(),
      page.first(),
      page.last(),
      page.numberOfElements(),
      page.empty()
    );
  }
}
