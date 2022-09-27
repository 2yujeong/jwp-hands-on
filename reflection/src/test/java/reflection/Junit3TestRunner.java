package reflection;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class Junit3TestRunner {

    @Test
    void run() throws Exception {
        // TODO Junit3Test에서 test로 시작하는 메소드 실행
        Class<Junit3Test> clazz = Junit3Test.class;
        final Method[] declaredMethods = clazz.getDeclaredMethods();

        for (Method method : declaredMethods) {
            if (method.getName().startsWith("test")) {
                method.invoke(new Junit3Test());
            }
        }
    }
}
