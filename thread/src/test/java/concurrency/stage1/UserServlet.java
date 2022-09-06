package concurrency.stage1;

import java.util.ArrayList;
import java.util.List;

public class UserServlet {

    private final List<User> users = new ArrayList<>();

    public void service(final User user) {
        join(user);
    }

    private synchronized void join(final User user) {
        if (!users.contains(user)) {
            users.add(user); // synchronized로 인해 두 번째로 join에 진입한 thread는 if문을 통과하지 못해서 line 16 디버깅에 걸리지 않음
        }
    }

    public int size() {
        return users.size();
    }

    public List<User> getUsers() {
        return users;
    }
}
