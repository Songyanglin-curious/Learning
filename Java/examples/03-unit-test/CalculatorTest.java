/** 最原始的自动化测试：由 main 直接调用测试方法。 */
public class CalculatorTest {

    /** 启动测试；不捕获失败错误，使失败直接显示在终端。 */
    public static void main(String[] args) {
        System.out.println("开始测试：2 + 3 应该等于 5");
        addsTwoNumbers();
        System.out.println("测试通过");
    }

    /** 调用真实业务方法，并用明确的预期值检查返回结果。 */
    static void addsTwoNumbers() {
        int actual = Calculator.add(2, 3);

        if (actual != 5) {
            throw new AssertionError(
                "预期结果是 5，实际结果是 " + actual
            );
        }
    }
}
