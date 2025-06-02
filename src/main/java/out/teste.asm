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
    str_Natan       db "Natan", 0
    str_Nathan      db "Nathan", 0
    n               dd    0
    nome            db 256 dup(0)
    naoTerminou     db    0
    MAXITER         equ 10
    hexa            db    0010h
    str1            db "Digite seu nome: ", 0
    str2            db "Ola %s", 13, 10, 0
.code
start:
    invoke crt_printf, addr str1
    invoke crt_gets, addr nome
    pop eax
    mov naoTerminou, eax
    pop eax
    mov n, eax

_loop1:
    mov al, naoTerminou
    cmp al, 1
    jne _fimLoop1

    invoke crt_printf, addr str2, nome
    pop ebx
    pop eax
    add eax, ebx
    push eax
    pop eax
    mov n, eax
    mov eax, n
    cmp eax, MAXITER
    jl _else1
    pop eax
    mov naoTerminou, eax
_else1:

    jmp _loop1
_fimLoop1:
    mov eax, n
    cmp eax, 10
    jne _else2
    pop eax
    mov naoTerminou, eax
_else2:
    mov eax, naoTerminou
    cmp eax, 0h
    jne _else3
    invoke crt_strcpy, addr nome, addr str_Natan
    jmp _fimIf3
_else3:
    invoke crt_strcpy, addr nome, addr str_Nathan
_fimIf3:

    invoke ExitProcess, 0
end start
