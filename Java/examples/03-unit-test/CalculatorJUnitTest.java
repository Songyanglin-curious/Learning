import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** JUnit 版本的测试类：没有 main，由 JUnit 调用测试方法。 */
public class CalculatorJUnitTest {

    /** 检查两个正整数相加，由 JUnit 的断言方法判断结果。 */
    @Test
    void addsTwoNumbers() {
        int actual = Calculator.add(2, 3);
        assertEquals(5, actual);
    }

    /** 检查与零相加，展示 JUnit 可以组织多个独立测试。 */
    @Test
    void addingZeroKeepsValue() {
        int actual = Calculator.add(7, 0);
        assertEquals(7, actual);
    }
}
