package com.evolution.dropfiledaemon.bootstrap.phase;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class ApplicationInitializationPhaseOrderValidatorBeanFactoryPostProcessor
        implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] phaseBeanNames = beanFactory.getBeanNamesForType(ApplicationInitializationPhase.class, false, false);

        Map<Integer, String> observedOrders = new HashMap<>();
        ClassLoader classLoader = beanFactory.getBeanClassLoader();

        for (String beanName : phaseBeanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            String className = beanDefinition.getBeanClassName();

            if (className == null) {
                continue;
            }

            try {
                Class<?> beanClass = ClassUtils.forName(className, classLoader);
                Order order = beanClass.getAnnotation(Order.class);

                if (order != null) {
                    int orderValue = order.value();

                    if (observedOrders.containsKey(orderValue)) {
                        String conflictingBeanName = observedOrders.get(orderValue);
                        throw new IllegalStateException(String.format(
                                "Critical configuration error! Priority conflict detected during BeanDefinition parsing. " +
                                        "Beans [%s] and [%s] declare the same explicit @Order(%d). Application startup aborted.",
                                conflictingBeanName, beanName, orderValue
                        ));
                    }

                    observedOrders.put(orderValue, beanName);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to load phase class for metadata validation: " + className, e);
            }
        }
    }
}
