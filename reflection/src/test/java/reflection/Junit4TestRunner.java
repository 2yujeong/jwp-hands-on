package reflection;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class Junit4TestRunner {

    @Test
    void run() throws Exception {
        // TODO Junit4Test에서 @MyTest 애노테이션이 있는 메소드 실행
        Class<Junit4Test> clazz = Junit4Test.class;
        final Method[] declaredMethods = clazz.getDeclaredMethods();

        for (Method method : declaredMethods) {
            if (method.isAnnotationPresent(MyTest.class)) {
                method.invoke(new Junit4Test());
            }
        }
    }
}
