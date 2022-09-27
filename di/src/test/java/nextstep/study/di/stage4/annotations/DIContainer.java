package nextstep.study.di.stage4.annotations;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 스프링의 BeanFactory, ApplicationContext에 해당되는 클래스
 */
class DIContainer {

    private final Set<Object> beans;

    private DIContainer(final Set<Class<?>> classes) {
        this.beans = createBeans(classes);
        this.beans.forEach(this::handleInjection);
    }

    public static DIContainer createContainerForPackage(final String rootPackageName) {
        final Set<Class<?>> classes = ClassPathScanner.getAllClassesInPackage(rootPackageName);
        return new DIContainer(classes);
    }

    private Set<Object> createBeans(final Set<Class<?>> classes) {
        Set<Object> beans = new HashSet<>();
        for (Class<?> bean : classes) {
            beans.add(getNewInstance(bean));
        }
        return beans;
    }

    private Object getNewInstance(final Class<?> it) {
        try {
            final Constructor<?> declaredConstructor = it.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Can not create Bean");
        }
    }

    private void handleInjection(final Object bean) {
        final Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            executeFieldInjection(bean, field);
        }
    }

    private void executeFieldInjection(final Object bean, final Field field) {
        if (field.isAnnotationPresent(Inject.class)) {
            field.setAccessible(true);
            try {
                final Object injectionBean = getBean(field.getType());
                field.set(bean, injectionBean);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(final Class<T> aClass) {
        final List<Object> beans = new ArrayList<>(this.beans);
        for (Object bean : beans) {
            final List<Class<?>> beanInterfaces = List.of(bean.getClass().getInterfaces());
            if (beanInterfaces.contains(aClass) || bean.getClass().equals(aClass)) {
                return (T) bean;
            }
        }
        throw new IllegalStateException();
    }
}
