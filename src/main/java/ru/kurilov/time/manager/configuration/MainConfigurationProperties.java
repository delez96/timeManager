package ru.kurilov.time.manager.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("time.manager")
@Data
public class MainConfigurationProperties {

    private String userName;
    private String tgApiToken;
}
