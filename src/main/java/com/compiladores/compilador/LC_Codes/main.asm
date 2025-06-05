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
    msgPergunta db "Digite seu nome: ", 0
    msgSaudacao db "Ola, %s", 13, 10, 0

.code
start:
    invoke crt_printf, addr msgPergunta
    invoke crt_gets, addr nome
    mov naoTerminou, 1
    mov n, 0

_loop:
    cmp naoTerminou, 0
    je _fimLoop
    invoke crt_printf, addr msgSaudacao, addr nome
    add n, 1
    cmp n, MAXITER
    jl _loop
    mov naoTerminou, 0
    jmp _loop

_fimLoop:
    invoke ExitProcess, 0
end start


; DATATYPES
; --------------------------------------------------------------------------------------------
; byte  ->  db  ->  1 byte  ->  Usado para valores pequenos, caracteres, booleans
; --------------------------------------------------------------------------------------------
; word  ->  dw  ->  2 bytes  ->  Útil para números pequenos e dados da API
; --------------------------------------------------------------------------------------------
; double word  ->  dd  ->  4 bytes  ->  Usado para inteiros, ponteiros e handles
; --------------------------------------------------------------------------------------------
; quad word  ->  dq  ->  8 bytes  ->  Para números maiores ou manipulação especial
; --------------------------------------------------------------------------------------------
; tbyte  ->  dt  ->  10 bytes  ->  Usado para precisão estendida (ex: float de 80 bits em FPU)
; --------------------------------------------------------------------------------------------