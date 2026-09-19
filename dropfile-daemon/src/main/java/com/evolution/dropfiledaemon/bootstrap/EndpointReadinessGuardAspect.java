package com.evolution.dropfiledaemon.bootstrap;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Aspect
@Component
public class EndpointReadinessGuardAspect {

    private volatile boolean ready = false;

    @EventListener(DropFileDaemonApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("DropFileDaemon is ready. Unlocking HTTP endpoints");
        this.ready = true;
    }

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)")
    public void restControllers() {
    }

    @Around("restControllers()")
    public Object checkReadiness(ProceedingJoinPoint pjp) throws Throwable {
        if (ready) {
            return pjp.proceed();
        }

        log.info("Blocking incoming HTTP request: Application is not ready yet");
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletResponse response = servletRequestAttributes.getResponse();
            if (response != null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }
        }
        // fallback if there is no response
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
