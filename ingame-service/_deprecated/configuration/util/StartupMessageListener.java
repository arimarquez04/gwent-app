package com.arimar.gwent.ingame_service.configuration.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupMessageListener implements ApplicationListener<ApplicationReadyEvent> {
    @Value("${spring.application.name}")
    private String APP_NAME;
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String message = "¡La aplicación de Spring " + APP_NAME +  " se ha iniciado con éxito!\n";
        log.info(message);
    }
}