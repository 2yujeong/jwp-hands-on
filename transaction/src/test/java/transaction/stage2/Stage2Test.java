package transaction.stage2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 트랜잭션 전파(Transaction Propagation)란?
 * 트랜잭션의 경계에서 이미 진행 중인 트랜잭션이 있을 때 또는 없을 때 어떻게 동작할 것인가를 결정하는 방식을 말한다.
 *
 * FirstUserService 클래스의 메서드를 실행할 때 첫 번째 트랜잭션이 생성된다.
 * SecondUserService 클래스의 메서드를 실행할 때 두 번째 트랜잭션이 어떻게 되는지 관찰해보자.
 *
 * https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#tx-propagation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Stage2Test {

    private static final Logger log = LoggerFactory.getLogger(Stage2Test.class);

    @Autowired
    private FirstUserService firstUserService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    /**
     * 생성된 트랜잭션이 몇 개인가?
     * 왜 그런 결과가 나왔을까?
     * -> 첫 번째 트랜잭션이 secondUserService.saveSecondTransactionWithRequired()에 전파된다.
     */
    @Test
    void testRequired() {
        final var actual = firstUserService.saveFirstTransactionWithRequired();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.FirstUserService.saveFirstTransactionWithRequired");
    }

    /**
     * 생성된 트랜잭션이 몇 개인가?
     * 왜 그런 결과가 나왔을까?
     * -> 첫 번째 트랜잭션과는 다른 새로운 트랜잭션이 secondUserService.saveSecondTransactionWithRequiresNew()에서 열린다.
     */
    @Test
    void testRequiredNew() {
        final var actual = firstUserService.saveFirstTransactionWithRequiredNew();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(2)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithRequiresNew",
                        "transaction.stage2.FirstUserService.saveFirstTransactionWithRequiredNew");
    }

    /**
     * firstUserService.saveAndExceptionWithRequiredNew()에서 강제로 예외를 발생시킨다.
     * REQUIRES_NEW 일 때 예외로 인한 롤백이 발생하면서 어떤 상황이 발생하는 지 확인해보자.
     * -> 첫 번째 트랜잭션에서 발생한 예외가 두 번째 트랜잭션에 영향을 미치지 X
     * -> secondUserService.saveSecondTransactionWithRequiresNew()는 정상적으로 동작한다.
     */
    @Test
    void testRequiredNewWithRollback() {
        assertThat(firstUserService.findAll()).hasSize(0);

        assertThatThrownBy(() -> firstUserService.saveAndExceptionWithRequiredNew())
                .isInstanceOf(RuntimeException.class);

        assertThat(firstUserService.findAll()).hasSize(1);
    }

    /**
     * FirstUserService.saveFirstTransactionWithSupports() 메서드를 보면 @Transactional이 주석으로 되어 있다.
     * 주석인 상태에서 테스트를 실행했을 때와 주석을 해제하고 테스트를 실행했을 때 어떤 차이점이 있는지 확인해보자.
     * 주석 처리 -> firstUserService에서는 트랜잭션 동작 X
     *         -> saveSecondTransactionWithSupports에서는 @Transactional 주석 처리가 되어있지 않지만 전파 레벨이 Supports
     *         -> 전파될 트랜잭션이 없다면 non-transactional로 진행(트랜잭션이 반환되긴 하지만 emptyTransaction이므로 활성화는 X)
     *         -> https://www.baeldung.com/spring-transactional-propagation-isolation#2supports-propagation
     * 주석 해제 -> @Transactional(propagation = Propagation.REQUIRED)이므로 firstUserService 트랜잭션 동작
     *         -> saveSecondTransactionWithSupports에서는 firstUserService의 트랜잭션이 전파됨
     */
    @Test
    void testSupports() {
        final var actual = firstUserService.saveFirstTransactionWithSupports();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithSupports");
    }

    /**
     * FirstUserService.saveFirstTransactionWithMandatory() 메서드를 보면 @Transactional이 주석으로 되어 있다.
     * 주석인 상태에서 테스트를 실행했을 때와 주석을 해제하고 테스트를 실행했을 때 어떤 차이점이 있는지 확인해보자.
     * SUPPORTS와 어떤 점이 다른지도 같이 챙겨보자.
     * -> 활성화된 트랜잭션이 있다면 전파받고 없다면 스프링에서 예외를 발생시킨다.
     * 주석 처리 -> 예외 발생
     * 주석 해제 -> saveSecondTransactionWithMandatory()가 firstUserService에서 열린 트랜잭션을 전파받는다.
     */
    @Test
    void testMandatory() {
        final var actual = firstUserService.saveFirstTransactionWithMandatory();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.FirstUserService.saveFirstTransactionWithMandatory");
    }

    /**
     * 아래 테스트는 몇 개의 물리적 트랜잭션이 동작할까?
     * FirstUserService.saveFirstTransactionWithNotSupported() 메서드의 @Transactional을 주석 처리하자.
     * 다시 테스트를 실행하면 몇 개의 물리적 트랜잭션이 동작할까?
     *
     * -> 전파된 부모 트랜잭션이 있다면 해당 트랜잭션을 일시 정지 시킨 상태로 내 로직을 non-transactional하게 실행한다.
     *    non-transactional : 활성화 되지 않은 emptyTransaction 반환
     *
     * 스프링 공식 문서에서 물리적 트랜잭션과 논리적 트랜잭션의 차이점이 무엇인지 찾아보자.
     * 주석 해제 -> saveFirstTransactionWithNotSupported만 트랜잭션이 열리고 saveSecondTransactionWithNotSupported에서는 emptyTransaction 반환
     * 주석 처리 -> firstUserService에서는 트랜잭션 동작 X
     *         -> saveSecondTransactionWithNotSupported에서만 emptyTransaction 반환
     */
    @Test
    void testNotSupported() {
        final var actual = firstUserService.saveFirstTransactionWithNotSupported();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(2)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNotSupported",
                        "transaction.stage2.FirstUserService.saveFirstTransactionWithNotSupported");
    }

    /**
     * 아래 테스트는 왜 실패할까? -> Hibernate에서 savePointManager 지원 X
     * FirstUserService.saveFirstTransactionWithNested() 메서드의 @Transactional을 주석 처리하면 어떻게 될까?
     *
     * 활성화된 첫 번쨰 트랜잭션이 있다면 전파받는다.
     * 첫 번째 트랜잭션에서 예외가 발생하면 Nested로 전파 받은 트랜잭션까지 rollback 된다.
     * 자식 트랜잭션에서 예외가 발생하면 부모 트랜잭션은 rollback X(부모 트랜잭션에서 자식 트랜잭션을 호출하는 지점까지만 rollback)
     * 이후 부모 트랜잭션에서 문제가 없으면 부모 트랜잭션은 끝까지 commit 된다.
     *
     * 주석 처리 -> if there's no active transaction, it works like REQUIRED.
     */
    @Test
    void testNested() {
        final var actual = firstUserService.saveFirstTransactionWithNested();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNested");
    }

    /**
     * 마찬가지로 @Transactional을 주석처리하면서 관찰해보자.
     * -> 전파 받은 트랜잭션이 있다면 예외를 발생시킨다.
     * -> 없다면 트랜잭션을 새로 열고 정상적으로 동작한다.
     */
    @Test
    void testNever() {
        final var actual = firstUserService.saveFirstTransactionWithNever();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNever");
    }
}
