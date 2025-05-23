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
    n dd 0
    nome db 256 dup(0)
    naoTerminou db 1
    MAXITER equ 10
    hexa dw 10h
    str1 db "Digite seu nome: ", 0
    str2 db "Ola, %s", 13, 10, 0
    str3 db "Seu nome eh: %s", 13, 10, 0
    str4 db "Resultado final da soma: %d", 13, 10, 0

.code
start:
    invoke crt_printf, addr str1
    invoke crt_gets, addr nome
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
    ; Calcula 1 + 3 + 5 + 7 + 9
    mov eax, 1
    add eax, 3
    add eax, 5
    add eax, 7
    add eax, 9
    ; Calcula 2 + 4 + 6 + 8 + 10
    mov ebx, 2
    add ebx, 4
    add ebx, 6
    add ebx, 8
    add ebx, 10
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
    mov addr nome, "Nathan"
    jmp _fimIf2

_setNomeNatan:
    mov addr nome, "Natan"

_fimIf2:

    ; Calcular expressão: n = 1 + 2*3/2 + (10-2)
    mov eax, 1
    mov ebx, 2
    mov ecx, 3
    imul ebx, ecx   ; 2 * 3 = 6
    idiv ecx        ; 6 / 2 = 3
    add eax, ebx    ; 1 + 3 = 4
    sub eax, 2      ; 4 + (10 - 2) = 4 + 8 = 12
    mov n, eax

    invoke crt_printf, addr str3, addr nome
    invoke crt_printf, addr str4, n

    invoke ExitProcess, 0
end start
