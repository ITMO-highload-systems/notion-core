package org.example.notion.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun clock(): Clock {
        return Clock.systemDefaultZone()
    }
}