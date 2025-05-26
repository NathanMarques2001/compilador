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
naoTerminou db 0
MAXITER equ 10
hexa dw 10h
str1 db "Digite seu nome: ", 0
str2 db "Ola' %s", 13, 10, 0
.code
start:
invoke crt_printf, addr str1
invoke crt_gets, addr nome
_loop1:
cmp naoTerminou, 0
je _fimLoop1
invoke crt_printf, addr str2, addr nome
jmp _loop1
_fimLoop1:
invoke ExitProcess, 0
end start
