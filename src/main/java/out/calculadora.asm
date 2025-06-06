.686
.model flat, stdcall
option casemap :none

include \masm32\include\windows.inc
include \masm32\include\kernel32.inc
include \masm32\include\masm32.inc
include \masm32\include\msvcrt.inc
includelib \masm32\lib\kernel32.lib
includelib \masm32\lib\masm32.lib
includelib \masm32\lib\msvcrt.lib
include \masm32\macros\macros.asm

.data
    num1            dd    0
    num2            dd    0
    resultado       dd    0
    operacao        dd    0
    escolhaContinuar dd    0
    continuarLoop   db    0
    str1            db "--- Calculadora LC ---", 13, 10, 0
    str2            db " ", 13, 10, 0
    str3            db "Digite o primeiro numero: ", 0
    format_d        db "%d", 0
    str4            db "Digite o segundo numero: ", 0
    str5            db " ", 13, 10, 0
    str6            db "Escolha a operacao:", 13, 10, 0
    str7            db "1. Soma (+)", 13, 10, 0
    str8            db "2. Subtracao (-)", 13, 10, 0
    str9            db "3. Multiplicacao (*)", 13, 10, 0
    str10           db "4. Divisao (/)", 13, 10, 0
    str11           db "Opcao (1-4): ", 0
    str12           db " ", 13, 10, 0
    str13           db "Resultado da Soma: %d", 13, 10, 0
    str14           db "Resultado da Subtracao: %d", 13, 10, 0
    str15           db "Resultado da Multiplicacao: %d", 13, 10, 0
    str16           db "Erro: Divisao por zero nao permitida!", 13, 10, 0
    str17           db "Resultado da Divisao: %d", 13, 10, 0
    str18           db " ", 13, 10, 0
    str19           db "Deseja fazer outra operacao? (1 para Sim, 0 para Nao): ", 0
    str20           db " ", 13, 10, 0
    str21           db "-------------------------", 13, 10, 0
    str22           db " ", 13, 10, 0
    str23           db " ", 13, 10, 0
    str24           db "Calculadora encerrada.", 13, 10, 0
.code
start:
    push 1
    pop eax
    mov continuarLoop, al

_loop1:
    mov al, continuarLoop
    cmp al, 1
    jne _fimLoop1

    invoke crt_printf, addr str1
    invoke crt_printf, addr str2
    invoke crt_printf, addr str3
    invoke crt_scanf, addr format_d, addr num1
    invoke crt_printf, addr str4
    invoke crt_scanf, addr format_d, addr num2
    invoke crt_printf, addr str5
    invoke crt_printf, addr str6
    invoke crt_printf, addr str7
    invoke crt_printf, addr str8
    invoke crt_printf, addr str9
    invoke crt_printf, addr str10
    invoke crt_printf, addr str11
    invoke crt_scanf, addr format_d, addr operacao
    invoke crt_printf, addr str12
    mov eax, operacao
    cmp eax, 1
    jne _else1
    push num1
    push num2
    pop ebx
    pop eax
    add eax, ebx
    push eax
    pop eax
    mov resultado, eax
    invoke crt_printf, addr str13, resultado
_else1:
    mov eax, operacao
    cmp eax, 2
    jne _else2
    push num1
    push num2
    pop ebx
    pop eax
    sub eax, ebx
    push eax
    pop eax
    mov resultado, eax
    invoke crt_printf, addr str14, resultado
_else2:
    mov eax, operacao
    cmp eax, 3
    jne _else3
    push num1
    push num2
    pop ebx
    pop eax
    imul eax, ebx
    push eax
    pop eax
    mov resultado, eax
    invoke crt_printf, addr str15, resultado
_else3:
    mov eax, operacao
    cmp eax, 4
    jne _else4
    mov eax, num2
    cmp eax, 0
    jne _else5
    invoke crt_printf, addr str16
_else5:
    mov eax, num2
    cmp eax, 0
    je _else6
    push num1
    push num2
    pop ebx
    pop eax
    cdq
    idiv ebx
    push eax
    pop eax
    mov resultado, eax
    invoke crt_printf, addr str17, resultado
_else6:
_else4:
    invoke crt_printf, addr str18
    invoke crt_printf, addr str19
    invoke crt_scanf, addr format_d, addr escolhaContinuar
    mov eax, escolhaContinuar
    cmp eax, 0
    jne _else7
    push 0
    pop eax
    mov continuarLoop, al
_else7:
    mov eax, escolhaContinuar
    cmp eax, 0
    je _else8
    invoke crt_printf, addr str20
    invoke crt_printf, addr str21
    invoke crt_printf, addr str22
_else8:

    jmp _loop1
_fimLoop1:
    invoke crt_printf, addr str23
    invoke crt_printf, addr str24

    invoke ExitProcess, 0
end start
