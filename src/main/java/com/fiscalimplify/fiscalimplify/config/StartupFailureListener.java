package com.fiscalimplify.fiscalimplify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
@Slf4j
public class StartupFailureListener implements ApplicationListener<ApplicationFailedEvent> {

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        Throwable ex = event.getException();
        log.error("========== FALHA AO INICIAR FISCALIMPLIFY ==========", ex);
        log.error("Verifique: Postgres vinculado (PGHOST ou DATABASE_URL), Deploy Logs acima.");
    }
}
