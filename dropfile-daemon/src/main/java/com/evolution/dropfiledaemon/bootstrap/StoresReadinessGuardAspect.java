package com.evolution.dropfiledaemon.bootstrap;

import com.evolution.dropfile.store.framework.KeyValueStoreInitializationGenericProcedure;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationGenericProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.bootstrap.event.DropFileDaemonBeforeApplicationReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
public class StoresReadinessGuardAspect {

    private static final Set<Class<?>> WHITE_LIST = Set.of(
            KeyValueStoreInitializationProcedure.class,
            SingleValueStoreInitializationProcedure.class,
            KeyValueStoreInitializationGenericProcedure.class,
            SingleValueStoreInitializationGenericProcedure.class
    );

    private boolean ready;

    @EventListener(DropFileDaemonBeforeApplicationReadyEvent.class)
    public void listener() {
        ready = true;
    }

    @Before("execution(* com.evolution.dropfile.store.framework.KeyValueStore.*(..)) || " +
            "execution(* com.evolution.dropfile.store.framework.single.SingleValueStore.*(..))")
    public void checkAccess() {
        if (ready) {
            return;
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean approvedByProcedure = false;

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            try {
                Class<?> clazz = Class.forName(className);
                boolean allow = WHITE_LIST.stream().anyMatch(it -> it.isAssignableFrom(clazz));
                if (allow) {
                    approvedByProcedure = true;
                    break;
                }
            } catch (ClassNotFoundException e) {
            }
        }

        if (!approvedByProcedure) {
            throw new IllegalStateException("Locked! Stores have not initialized yet");
        }
    }
}
