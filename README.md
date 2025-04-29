# Java 計算機與 Undo/Redo 功能

這是一個使用 Java 實現的簡單命令行計算機，它不僅支援基本的算術運算（加、減、乘、除），還具備撤銷 (Undo) 和重做 (Redo) 功能。

## 主要功能

*   **基本運算:**
    *   加法 (+)
    *   減法 (-)
    *   乘法 (*)
    *   除法 (/)
*   **歷史紀錄:**
    *   **Undo (撤銷):** 撤銷上一步執行的操作。
    *   **Redo (重做):** 取消上一次的撤銷操作，重新執行被撤銷的命令。

## 設計思路：命令模式 (Command Pattern)

本計算機的核心設計採用了 **命令模式**。這種模式的主要優點是：

1.  **解耦:** 將請求的發送者（例如，用戶界面或主程序循環）與請求的接收者（實際執行運算的 `Calculator` 物件）解耦。
2.  **請求封裝:** 將一個請求封裝成一個獨立的物件（`Command` 物件）。每個物件都包含了執行操作所需的所有資訊（例如，操作類型、操作數、接收者引用）。
3.  **可撤銷操作:** 封裝後的命令物件可以很容易地儲存起來，並且可以為每個命令定義一個 `undo()` 方法來實現撤銷功能。

### 核心組件

1.  **`Calculator` (接收者 Receiver & 調用者 Invoker):**
    *   持有當前的計算結果 (`currentValue`)。
    *   包含執行運算的內部方法 (`performOperation`)。
    *   持有兩個堆疊 (`undoStack`, `redoStack`) 來管理命令歷史。
    *   接收外部請求（例如 `compute`, `undo`, `redo`），創建對應的 `Command` 物件並觸發其執行或撤銷。

2.  **`Command` (命令接口):**
    *   定義所有具體命令類必須實現的接口。
    *   包含兩個核心方法：
        *   `execute()`: 執行命令。
        *   `undo()`: 撤銷命令。

3.  **具體命令類 (Concrete Commands):**
    *   例如 `AddCommand`, `SubtractCommand`, `MultiplyCommand`, `DivideCommand`。
    *   每個類實現 `Command` 接口。
    *   持有執行所需的操作數 (`operand`) 和 `Calculator` 實例的引用。
    *   `execute()` 方法調用 `Calculator` 的 `performOperation` 來修改 `currentValue`。
    *   `undo()` 方法執行與 `execute()` 相反的操作來恢復之前的狀態。

4.  **歷史堆疊 (History Stacks):**
    *   `undoStack` (`Deque<Command>`): 儲存已執行的命令序列。執行新命令時壓入，執行 Undo 時彈出。
    *   `redoStack` (`Deque<Command>`): 儲存已被撤銷的命令序列。執行 Undo 時壓入，執行 Redo 時彈出。

### Undo/Redo 機制

*   **執行新命令 (`compute`):**
    1.  創建對應的 `Command` 物件。
    2.  調用 `command.execute()` 執行操作。
    3.  將該 `command` 壓入 `undoStack`。
    4.  **清空 `redoStack`** (因為新的操作使得之前的 Redo 路徑失效)。
*   **撤銷 (`undo`):**
    1.  檢查 `undoStack` 是否為空。
    2.  若不為空，從 `undoStack` 彈出一個 `command`。
    3.  調用 `command.undo()` 恢復狀態。
    4.  將該 `command` 壓入 `redoStack` 以備 Redo。
*   **重做 (`redo`):**
    1.  檢查 `redoStack` 是否為空。
    2.  若不為空，從 `redoStack` 彈出一個 `command`。
    3.  調用 `command.execute()` 重新執行操作。
    4.  將該 `command` 壓回 `undoStack`。

## 優化與考量

1.  **Undo 實現方式 (反向操作 vs. 狀態保存):**
    *   **當前實現:** `undo()` 通過執行數學上的反向操作來實現。
    *   **潛在問題:** 對於某些操作（例如乘以 0），其反向操作（除以 0）是無效或複雜的。
    *   **更健壯的方案:** 讓每個 `Command` 物件在 `execute()` 執行前保存 `Calculator` 的狀態 (`currentValue`)。`undo()` 方法只需將 `currentValue` 恢復到保存的狀態即可。這種方法更通用，但會增加內存消耗。

2.  **錯誤處理:**
    *   對除以零等算術錯誤進行了基本處理。更完善的系統可能需要更細緻的錯誤報告和處理機制。

3.  **內存管理:**
    *   `undoStack` 和 `redoStack` 可能會無限增長。在實際應用中，可能需要限制歷史記錄的大小。

4.  **線程安全:**
    *   當前實現不是線程安全的。在多線程環境下使用需要添加同步機制。

## 如何運行 (示例)

1.  **編譯:**
    ```bash
    javac Calculator.java Command.java CalculatorApp.java # (以及所有具體的 Command 類)
    ```
2.  **運行:**
    ```bash
    java CalculatorApp
    ```
    程序將輸出執行的每一步操作、Undo/Redo 操作及其結果。