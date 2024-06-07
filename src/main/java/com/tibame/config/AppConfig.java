package com.tibame.config;

import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;

@Configuration
@PropertySource("classpath:db.properties")
@ComponentScan(value = {"com.tibame"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = Controller.class
        )
)
@Import({RedisConfig.class, DBConfig.class})
public class AppConfig {
}
