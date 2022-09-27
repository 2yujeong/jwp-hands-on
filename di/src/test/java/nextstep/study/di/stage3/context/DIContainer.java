package nextstep.study.di.stage3.context;

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

    public DIContainer(final Set<Class<?>> classes) {
        this.beans = createBeans(classes);
        this.beans.forEach(this::setFields);
    }

    // 기본 생성자로 빈을 생성한다.
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
            throw new RuntimeException();
        }
    }

    // 빈 내부에 선언된 필드를 각각 셋팅한다.
    // 각 필드에 빈을 대입(assign)한다.
    private void setFields(final Object bean) {
        final Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            setField(bean, field);
        }
    }

    private void setField(final Object bean, final Field field) {
        if (contains(field)) {
            field.setAccessible(true);
            try {
                final Object injectionBean = getBean(field.getType());
                field.set(bean, injectionBean);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean contains(final Field field) {
        final ArrayList<Object> beansList = new ArrayList<>(beans);
        for (Object bean : beansList) {
            final List<Class<?>> interfaces = List.of(bean.getClass().getInterfaces());
            if (interfaces.contains(field.getType()) || bean.getClass().equals(field.getType())) {
                return true;
            }
        }
        return false;
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
