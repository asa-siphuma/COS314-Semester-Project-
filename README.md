# Lexical Categories
---

### 1. **Keywords** (Reserved Keywords)
- **Description**: Keywords are reserved words in the language that have a specific, predefined meaning.
- **Function**: They define the structure and flow of the program.
- **Examples**: `main`, `begin`, `end`, `skip`, `halt`, `print`, `if`, `then`, `else`, `num`, `text`.
- **Explanation**: 
  - `main` marks the entry point of the program.
  - `begin` and `end` define the start and end of an algorithm block.
  - `skip` is an empty command where nothing happens.
  - `halt` terminates the execution.
  - `print` outputs values to the console.

---

### 2. **Variable Names (VNAME)**
- **Description**: Variable names are user-defined identifiers that represent variables in the program.
- **Function**: They are used to store values that the program operates on.
- **Pattern (Regular Expression)**: `V_[a-z]([a-z]|[0-9])*`
- **Explanation**: 
  - All variable names must start with the prefix `V_` to distinguish them from other tokens.
  - The first character after the prefix must be a lowercase letter (`a-z`), and subsequent characters can be lowercase letters or digits (`a-z`, `0-9`).
- **Examples**:
  - `V_x`
  - `V_myVar`
  - `V_counter1`

---

### 3. **Function Names (FNAME)**
- **Description**: Function names are user-defined identifiers that represent functions in the program.
- **Function**: They are used to define or call functions, which perform specific tasks.
- **Pattern (Regular Expression)**: `F_[a-z]([a-z]|[0-9])*`
- **Explanation**: 
  - All function names must start with the prefix `F_` to distinguish them from other tokens.
  - The first character after the prefix must be a lowercase letter (`a-z`), and subsequent characters can be lowercase letters or digits (`a-z`, `0-9`).
- **Examples**:
  - `F_add`
  - `F_computeArea`
  - `F_sortArray`

---

### 4. **Text Constants (T)**
- **Description**: Text constants represent string literals (short snippets of text) in the program.
- **Function**: These are used as values for text-based variables or as output for `print` statements.
- **Pattern (Regular Expression)**:
  - Strings are composed of up to 8 characters, starting with a capital letter followed by lowercase letters.
  - Examples of patterns:
    - `"[A-Z][a-z][a-z][a-z][a-z][a-z][a-z][a-z]"` (8 characters)
    - `"[A-Z][a-z]"` (2 characters)
- **Explanation**: 
  - String literals must begin with a capital letter (`A-Z`), and the remaining characters can be lowercase letters (`a-z`).
  - The length of the string is restricted to up to 8 characters for simplicity in this language.
- **Examples**:
  - `"Hello"`
  - `"World"`
  - `"A"`

---

### 5. **Number Constants (N)**
- **Description**: Number constants represent numeric values, which can be integers or real numbers.
- **Function**: These are used as values for numeric variables or in expressions.
- **Pattern (Regular Expression)**:
  - `0`
  - `0.([0-9])* [1-9]`
  - `-0.([0-9])* [1-9]`
  - `[1-9]([0-9])*`
  - `-[1-9]([0-9])*`
  - `[1-9]([0-9])*.([0-9])* [1-9]`
  - `-[1-9]([0-9])*.([0-9])* [1-9]`
- **Explanation**: 
  - The language does not differentiate between integers and real numbers.
  - The pattern allows for both positive and negative numbers, with or without a decimal point.
  - A number cannot start with a zero unless it is exactly `0` or a decimal (e.g., `0.5`).
- **Examples**:
  - `42`
  - `-15`
  - `3.14159`
  - `0.25`

---

### 6. **Operators (BINOP, UNOP)** (Reserved Keywords)
- **Description**: Operators are symbols that represent mathematical or logical operations.
- **Function**: They are used to perform operations on variables and constants.
- **Types**:
  - **Binary Operators (BINOP)**: Operate on two operands.
    - Examples: `add`, `sub`, `mul`, `div`, `eq`, `grt`, `and`, `or`.
  - **Unary Operators (UNOP)**: Operate on one operand.
    - Examples: `not`, `sqrt`.
- **Explanation**:
  - Binary operators include basic arithmetic (`add`, `sub`, `mul`, `div`), comparisons (`eq`, `grt`), and logical operations (`and`, `or`).
  - Unary operators include logical negation (`not`) and square root (`sqrt`).
- **Examples**:
  - `add(5, 3)` evaluates to `8`.
  - `sub(10, 2)` evaluates to `8`.
  - `not(true)` evaluates to `false`.
  - `sqrt(16)` evaluates to `4`.

---

### 7. **Symbols** (Reserved Keywords)
- **Description**: Symbols are punctuation marks and other special characters used to structure the language.
- **Function**: They delimit sections of code or provide structure for statements.
- **Examples**:
  - `;` (semicolon) – Ends a command.
  - `,` (comma) – Separates variables or function parameters.
  - `(` and `)` – Used in function calls or expressions.
  - `{` and `}` – Delimit blocks of code (used in function bodies).
- **Explanation**:
  - Symbols are critical for separating different parts of the program, such as ending a command or grouping function arguments.
- **Examples**:
  - `F_add(V_x, V_y, V_z);` – Function call followed by a semicolon to indicate the end of the statement.

---

### 8. **Reserved Keywords (Other)**
- **Description**: Reserved keywords are tokens that are part of the language syntax and cannot be used as variable names or function names.
- **Function**: These are used to construct expressions, control flow, and define the program structure.
- **Examples**:
  - `if`, `then`, `else` – Used for conditional branching.
  - `begin`, `end` – Used to enclose blocks of code in algorithms.
- **Explanation**:
  - Reserved keywords are critical to defining the structure of RecSPL programs and cannot be redefined by the user.
- **Examples**:
  - `if grt(V_x, 10) then begin print "Hello"; end else begin halt; end`

---

### Summary Table

| Lexical Category   | Example Tokens                              | Pattern/Explanation                                            |
|--------------------|---------------------------------------------|---------------------------------------------------------------|
| **Keywords**        | `main`, `begin`, `skip`, `num`              | Reserved words with fixed meanings in the language.            |
| **Variable Names**  | `V_x`, `V_counter1`                        | `V_[a-z]([a-z]|[0-9])*` (must start with `V_`).                |
| **Function Names**  | `F_add`, `F_computeArea`                   | `F_[a-z]([a-z]|[0-9])*` (must start with `F_`).                |
| **Text Constants**  | `"Hello"`, `"World"`                       | Strings, starting with a capital letter, up to 8 characters.   |
| **Number Constants**| `42`, `-15`, `3.14159`                     | Numbers can be integers or real numbers, positive or negative. |
| **Operators**       | `add`, `sub`, `mul`, `eq`, `not`, `sqrt`   | Binary and unary operators for mathematical and logical operations. |
| **Symbols**         | `;`, `,`, `(`, `)`                        | Special symbols that structure the program.                    |

---

# Lexical Categories
