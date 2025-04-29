package com.example.demo;

import java.util.ArrayDeque;
import java.util.Deque;

// --- 1. 命令介面 ---
interface Command {
    void execute();
    void undo();
}

// --- 2. 計算機類 (接收者與調用者) ---
class Calculator {
    private double currentValue = 0.0;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public double getCurrentValue() {
        return currentValue;
    }

    // 供 Command 的 undo 直接設置值
    void setCurrentValueInternal(double value) {
        this.currentValue = value;
    }

    // 供 Command 內部呼叫以實際修改 currentValue
    // 注意：這些方法不直接暴露給外部，而是透過 Command 模式間接使用
    void performOperation(char operator, double operand) {
        switch (operator) {
            case '+':
                currentValue += operand;
                break;
            case '-':
                currentValue -= operand;
                break;
            case '*':
                currentValue *= operand;
                break;
            case '/':
                if (operand == 0) {
                    System.err.println("Error: Cannot divide by zero. Operation skipped.");
                    // 拋出異常或返回錯誤狀態
                    throw new ArithmeticException("Division by zero");
                }
                currentValue /= operand;
                break;
        }
    }

    // 執行命令 (外部呼叫的主要方法)
    public void compute(char operator, double operand) {
        Command command = null;
        switch (operator) {
            case '+':
                command = new AddCommand(this, operand);
                break;
            case '-':
                command = new SubtractCommand(this, operand);
                break;
            case '*':
                command = new MultiplyCommand(this, operand);
                break;
            case '/':
                 // 提前檢查除零，避免創建無效命令
                if (operand == 0) {
                    System.err.println("Error: Cannot divide by zero. Operation skipped.");
                    return;
                }
                command = new DivideCommand(this, operand);
                break;
            default:
                System.err.println("Error: Invalid operator '" + operator + "'.");
                return;
        }

        if (command != null) {
            try {
                 // 執行命令
                command.execute();
                // 將命令推入 undo 堆疊
                undoStack.push(command);
                // 執行新命令後，之前的 redo 操作無效，清空 redo 堆疊
                redoStack.clear();
                printState("Executed");
            } catch (ArithmeticException e) {
                // 如果 execute 中發生錯誤 (如除零)，不將命令加入堆疊
                 System.err.println("Operation failed: " + e.getMessage());
            }
        }
    }

    // 撤銷操作
    public void undo() {
        if (!undoStack.isEmpty()) {
            Command commandToUndo = undoStack.pop();
            commandToUndo.undo();
            redoStack.push(commandToUndo); // 將撤銷的命令放入 redo 堆疊
            printState("Undo");
        } else {
            System.out.println("Nothing to undo.");
        }
    }

    // 重做操作 (取消撤銷)
    public void redo() {
        if (!redoStack.isEmpty()) {
            Command commandToRedo = redoStack.pop();
             try {
                commandToRedo.execute(); // 重新執行命令
                undoStack.push(commandToRedo); // 將重做的命令放回 undo 堆疊
                printState("Redo");
             } catch (ArithmeticException e) {
                // 如果重做時出錯 (雖然不太可能，因為之前執行成功過)
                // 需要決定如何處理，例如是否放回 redoStack
                System.err.println("Redo failed: " + e.getMessage());
                // 這裡選擇不放回 redoStack，因為重做失敗了
             }
        } else {
            System.out.println("Nothing to redo.");
        }
    }

    private void printState(String action) {
        System.out.println(action + " => Current Value: " + currentValue +
                           " (Undo stack size: " + undoStack.size() +
                           ", Redo stack size: " + redoStack.size() + ")");
    }
}

// --- 3. 具體命令類 ---
// 抽象命令類，可選，用於提取公共屬性 (操作數和計算器引用)
abstract class AbstractCalculatorCommand implements Command {
    protected Calculator calculator;
    protected double operand;
    protected double valueBeforeExecute; // 保存執行前的值

    public AbstractCalculatorCommand(Calculator calculator, double operand) {
        this.calculator = calculator;
        this.operand = operand;
    }

    // 模板方法：執行前保存狀態，然後執行具體操作
    @Override
    public void execute() {
            this.valueBeforeExecute = calculator.getCurrentValue(); // 保存狀態
            try {
                 doExecute(); // 執行具體操作
            } catch (ArithmeticException e) {
                 // 如果 doExecute 出錯，需要恢復狀態（雖然這裡 currentValue 還沒變）
                 // 重要的是不將其加入 undo 堆疊
                 System.err.println("Operation failed during execution: " + e.getMessage());
                 throw e; // 重新拋出，讓 Calculator 知道執行失敗
            }
        }
    
        // 子類實現具體的執行邏輯
        protected abstract void doExecute();
    
        @Override
        public void undo() {
            calculator.setCurrentValueInternal(valueBeforeExecute); // 直接恢復狀態
        }
    }
    
    class AddCommand extends AbstractCalculatorCommand {
        public AddCommand(Calculator calculator, double operand) {
            super(calculator, operand);
        }
    
        @Override
        protected void doExecute() {
            calculator.performOperation('+', operand);
        }
    }
    
    class SubtractCommand extends AbstractCalculatorCommand {
        public SubtractCommand(Calculator calculator, double operand) {
            super(calculator, operand);
        }
    
        @Override
        protected void doExecute() {
            calculator.performOperation('-', operand);
        }
    }

    class MultiplyCommand extends AbstractCalculatorCommand {
        public MultiplyCommand(Calculator calculator, double operand) {
            super(calculator, operand);
        }

        @Override
        protected void doExecute() {
            calculator.performOperation('*', operand);
        }
    }

class DivideCommand extends AbstractCalculatorCommand {
    public DivideCommand(Calculator calculator, double operand) {
        // 在 Calculator.compute 中已檢查 operand != 0
        super(calculator, operand);
        if (operand == 0) { // 再次檢查以防萬一
             throw new IllegalArgumentException("Operand cannot be zero for Division Command.");
        }
    }

    @Override
    protected void doExecute() {
        calculator.performOperation('/', operand);
    }
}


// --- 4. 主類 (測試) ---
public class CalculatorApp {
    public static void main(String[] args) {
        Calculator calculator = new Calculator();

        calculator.compute('+', 100); // Value: 100, Undo: 1, Redo: 0
        calculator.compute('-', 30);  // Value: 70,  Undo: 2, Redo: 0
        calculator.compute('*', 3);   // Value: 210, Undo: 3, Redo: 0
        calculator.compute('/', 10);  // Value: 21,  Undo: 4, Redo: 0

        System.out.println("\n--- Undoing ---");
        calculator.undo(); // Undo /, Value: 210, Undo: 3, Redo: 1
        calculator.undo(); // Undo *, Value: 70,  Undo: 2, Redo: 2

        System.out.println("\n--- Redoing ---");
        calculator.redo(); // Redo *, Value: 210, Undo: 3, Redo: 1

        System.out.println("\n--- Performing new operation ---");
        calculator.compute('+', 5);  // Value: 215, Undo: 4, Redo: 0 (Redo stack cleared)

        System.out.println("\n--- Undoing again ---");
        calculator.undo(); // Undo +, Value: 210, Undo: 3, Redo: 1
        calculator.undo(); // Undo *, Value: 70,  Undo: 2, Redo: 2
        calculator.undo(); // Undo -, Value: 100, Undo: 1, Redo: 3
        calculator.undo(); // Undo +, Value: 0,   Undo: 0, Redo: 4
        calculator.undo(); // Nothing to undo

        System.out.println("\n--- Redoing all ---");
        calculator.redo(); // Redo +, Value: 100, Undo: 1, Redo: 3
        calculator.redo(); // Redo -, Value: 70,  Undo: 2, Redo: 2
        calculator.redo(); // Redo *, Value: 210, Undo: 3, Redo: 1
        calculator.redo(); // Redo +, Value: 215, Undo: 4, Redo: 0
        calculator.redo(); // Nothing to redo

        System.out.println("\n--- Testing Division by Zero ---");
        calculator.compute('/', 0); // Error message, state unchanged
        System.out.println("Current Value after attempting division by zero: " + calculator.getCurrentValue()); // Should still be 215

        calculator.compute('/', 2); // Value: 107.5, Undo: 5, Redo: 0
        calculator.undo(); // Undo /, Value: 215, Undo: 4, Redo: 1

        System.out.println("\nFinal Value: " + calculator.getCurrentValue());
    }
}