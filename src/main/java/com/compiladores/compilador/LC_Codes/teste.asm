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
    n           dd 0
    nome        db 256 dup(0)
    naoTerminou db 1
    MAXITER     equ 10
    hexa        dw 10h
    str1        db "Digite seu nome: ", 0
    str2        db "Ola, %s", 13, 10, 0
    str3        db "Seu nome eh: %s", 13, 10, 0
    str4        db "Resultado final da soma: %d", 13, 10, 0
    
    ; CORREÇÃO: Declarar strings a serem usadas na seção .data
    nomeNathan  db "Nathan", 0
    nomeNatan   db "Natan", 0

.code
start:
    invoke crt_printf, addr str1
    ; CORREÇÃO: Usar a função segura 'fgets' para evitar travamentos
    invoke crt_fgets, addr nome, 256, stdin
    mov naoTerminou, 1
    mov n, 0

_loop1:
    cmp naoTerminou, 0
    je _fimLoop1
    invoke crt_printf, addr str2, addr nome
    add n, 1
    cmp n, MAXITER
    jl _loop1
    mov naoTerminou, 0
    jmp _fimLoop1

_fimLoop1:

_if1:
    cmp n, 10
    jne _fimIf1
    mov eax, 25 ; Resultado de 1+3+5+7+9
    mov ebx, 30 ; Resultado de 2+4+6+8+10
    cmp eax, ebx
    jl _naoTerminouSetValue1
    mov naoTerminou, 0
    jmp _fimIf1

_naoTerminouSetValue1:
    mov naoTerminou, 1

_fimIf1:

_if2:
    cmp naoTerminou, 0
    je _setNomeNatan
    ; CORREÇÃO: Usar 'invoke crt_strcpy' para copiar a string
    invoke crt_strcpy, addr nome, addr nomeNathan
    jmp _fimIf2

_setNomeNatan:
    ; CORREÇÃO: Usar 'invoke crt_strcpy' para copiar a string
    invoke crt_strcpy, addr nome, addr nomeNatan

_fimIf2:

    ; CORREÇÃO: Lógica do cálculo para n = 1 + 2*3/2 + (10-2)
    ; O resultado correto é 12
    mov eax, 2
    mov ebx, 3
    mul ebx         ; eax = 6
    xor edx, edx    ; Zera edx para a divisão
    mov ebx, 2
    div ebx         ; eax = 3
    add eax, 1      ; eax = 4
    add eax, 8      ; eax = 12 (somando 10-2)
    mov n, eax

    invoke crt_printf, addr str3, addr nome
    invoke crt_printf, addr str4, n

    invoke ExitProcess, 0
end start