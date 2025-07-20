# 🧠 Compilador LC

Este projeto implementa um **compilador completo para a linguagem LC**, desenvolvido como parte da disciplina de Compiladores no curso de Ciência da Computação da Dom Helder Escola Superior.

A linguagem LC é uma linguagem imperativa inspirada em C e Pascal, suportando tipos primitivos (`int`, `byte`, `string`, `boolean`), estruturas de controle (`if`, `while`), variáveis, constantes e operações de entrada/saída. O compilador traduz o código-fonte LC para **Assembly 80x86**, com suporte a otimizações de código.

---

## 🚀 Pipeline de Compilação

Fonte (.lc)

↓

Análise Léxica (Regex + AFD)

↓

Análise Sintática (Descida Recursiva)

↓

Análise Semântica (Verificação de Tipos e Atribuições)

↓

Geração de Código (Assembly via Shunting-Yard)

↓

Otimização Peephole

↓

Saída (.asm)


---

## 🧱 Estrutura do Projeto

- `lexer/` – Analisador Léxico (`LexicalAnalyzer.java`)
- `parser/` – Analisador Sintático (`SyntaticAnalyzer.java`)
- `semantic/` – Verificador Semântico (`SemanticAnalyzer.java`)
- `codegen/` – Gerador de Assembly (`AssemblyGenerator.java`)
- `optimizer/` – Otimizador de Código (`PeepholeOptimizer.java`)
- `io/LC_Codes/` – Casos de teste separados por sucesso e erro

---

## ⚙️ Como Executar

### ✅ Pré-requisitos

- Java JDK 11+
- MASM32 SDK (montador x86 para Windows)

### ▶️ Execução

1. **Escolha o código LC a ser compilado**  
   Altere a variável `filePath` em `Main.java` para apontar para o arquivo desejado.

2. **Compile e execute**  
   Compile `Main.java` e execute. O resultado será exibido no console e o arquivo `.asm` será salvo em `codegen/out/`.

---

## 🧪 Casos de Teste

Inclui casos de sucesso e erro nas análises:

- ✅ Sucesso: `calculadora.lc`, `main-corrigido.lc`, `toda-gramatica.lc`
- ❌ Erros Léxicos: strings malformadas, hexadecimais inválidos
- ❌ Erros Sintáticos: ausência de `end`, falta de `;`
- ❌ Erros Semânticos: uso de variáveis não declaradas, tipos incompatíveis

---

## 🧠 Técnicas Implementadas

- Lexer baseado em expressões regulares
- Parser por descida recursiva
- Verificação de tipos e escopo
- Geração de código com precedência via Shunting-Yard
- Otimização Peephole (eliminação de operações redundantes e saltos inúteis)

---

## 👨‍💻 Autor

- Nathan Marques

---

## 📜 Licença

Projeto acadêmico sem fins comerciais.
