package ru.kurilov.time.manager.service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import ru.kurilov.time.manager.model.EventModel;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AiTextAnalyzer {

    private final ChatClient chatClient;

    public AiTextAnalyzer(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public EventModel parseUserEvent(String text, Long chatId) {
        Event entity = this.chatClient.prompt(new Prompt(
                        new SystemMessage("""
                                Твоя задача выделить из контекста событие и дату с временем.
                                НИЧЕГО НЕ ВЫДУМЫВАЙ.
                                Если не можешь выделить событие и дату со временем, ничего не выводи."""),
                        new UserMessage(text)
                ))
                .call()
                .entity(Event.class);
        if (entity == null || StringUtils.isBlank(entity.eventDateTime) || StringUtils.isBlank(entity.eventName)) {
            return null;
        }
        LocalDateTime eventDateTime = getDateTime(text, entity);
        return new EventModel()
                .setEventName(entity.eventName)
                .setEventDateTime(eventDateTime)
                .setUserId(chatId);
    }

    private LocalDateTime getDateTime(String text, Event entity) {
        LocalDateTime eventDateTime = LocalDateTime.parse(entity.eventDateTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        if (text.contains("сегодня")) {
            eventDateTime = eventDateTime.withMonth(OffsetDateTime.now().getMonthValue());
            eventDateTime = eventDateTime.withDayOfMonth(OffsetDateTime.now().getDayOfMonth());
        } else if (text.contains("послезавтра") || text.contains("после завтра")) {
            eventDateTime = eventDateTime.withMonth(OffsetDateTime.now().getMonthValue());
            eventDateTime = eventDateTime.withDayOfMonth(OffsetDateTime.now().getDayOfMonth() + 2);
        } else if (text.contains("завтра")) {
            eventDateTime = eventDateTime.withMonth(OffsetDateTime.now().getMonthValue());
            eventDateTime = eventDateTime.withDayOfMonth(OffsetDateTime.now().getDayOfMonth() + 1);
        }
        if (eventDateTime.isBefore(LocalDateTime.now())) {
            eventDateTime = eventDateTime.withYear(OffsetDateTime.now().getYear());
        }
        return eventDateTime;
    }

    private record Event(
            @JsonPropertyDescription("Цель события")
            String eventName,
            @JsonPropertyDescription("Дата и время, формат yyyy-MM-ddTHH:mm:ss")
            String eventDateTime
    ) {

    }
}
