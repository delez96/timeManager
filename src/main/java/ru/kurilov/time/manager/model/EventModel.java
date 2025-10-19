package ru.kurilov.time.manager.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Accessors(chain = true)
public class EventModel {

    private Long id;
    private Long userId;
    private String eventName;
    private LocalDateTime eventDateTime;

    public String toMessage(int count) {
        return count + ": Событие: " + eventName + "\nВремя: "
                + eventDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String toMessage() {
        return "Событие: " + eventName + "\nВремя: "
                + eventDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
